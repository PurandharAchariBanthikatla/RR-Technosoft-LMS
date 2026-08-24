package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.*;
import com.rrtechnosoft.lms.dto.response.StudentFeeResponse;
import com.rrtechnosoft.lms.entity.*;
import com.rrtechnosoft.lms.entity.enums.DiscountType;
import com.rrtechnosoft.lms.entity.enums.FeeStatus;
import com.rrtechnosoft.lms.entity.enums.FineStatus;
import com.rrtechnosoft.lms.entity.enums.InstallmentStatus;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentFeeService {

    private final StudentFeeRepository studentFeeRepository;
    private final StudentFeeInstallmentRepository installmentRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final FeeDiscountRepository discountRepository;
    private final FeeFineRepository fineRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<StudentFeeResponse> list(UUID studentId, UUID courseId, FeeStatus status, Pageable pageable) {
        return studentFeeRepository.search(studentId, courseId, status, pageable).map(StudentFeeResponse::from);
    }

    @Transactional(readOnly = true)
    public List<StudentFeeResponse> listForStudent(UUID studentId) {
        return studentFeeRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(StudentFeeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public StudentFee findWithDetails(UUID id) {
        return studentFeeRepository.findWithDetailsById(id).orElseThrow(() -> ApiException.notFound("Student fee record not found"));
    }

    @Transactional(readOnly = true)
    public StudentFeeResponse get(UUID id, UUID viewerId, boolean isStudent) {
        StudentFee fee = findWithDetails(id);
        if (isStudent && !fee.getStudent().getId().equals(viewerId)) {
            throw ApiException.forbidden("You cannot view another student's fee record");
        }
        return StudentFeeResponse.from(fee);
    }

    @Transactional
    public StudentFeeResponse assign(AssignFeeStructureRequest request, UUID actorId) {
        User student = userRepository.findById(request.studentId())
                .orElseThrow(() -> ApiException.notFound("Student not found"));
        Course course = request.courseId() == null ? null : courseRepository.findById(request.courseId())
                .orElseThrow(() -> ApiException.notFound("Course not found"));
        if (course != null && studentFeeRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            throw ApiException.conflict("A fee record already exists for this student and course");
        }

        BigDecimal totalAmount;
        List<FeeInstallmentInput> installmentPlan;
        FeeStructure feeStructure = null;

        if (request.feeStructureId() != null) {
            feeStructure = feeStructureRepository.findWithInstallmentsById(request.feeStructureId())
                    .orElseThrow(() -> ApiException.notFound("Fee structure not found"));
            totalAmount = request.totalAmount() != null ? request.totalAmount() : feeStructure.getTotalAmount();
            installmentPlan = request.installmentOverrides() != null && !request.installmentOverrides().isEmpty()
                    ? request.installmentOverrides()
                    : feeStructure.getInstallments().stream()
                        .map(i -> new FeeInstallmentInput(i.getInstallmentNumber(), i.getAmount(), i.getDueAfterDays()))
                        .toList();
        } else {
            if (request.totalAmount() == null || request.installmentOverrides() == null || request.installmentOverrides().isEmpty()) {
                throw ApiException.badRequest("Either feeStructureId or a custom totalAmount + installments must be supplied");
            }
            totalAmount = request.totalAmount();
            installmentPlan = request.installmentOverrides();
        }

        FeeStructureService.validateInstallments(installmentPlan, totalAmount);

        StudentFee studentFee = StudentFee.builder()
                .student(student)
                .course(course)
                .feeStructure(feeStructure)
                .totalAmount(totalAmount)
                .netPayable(totalAmount)
                .assignedBy(actorId)
                .build();

        for (FeeInstallmentInput input : installmentPlan) {
            studentFee.getInstallments().add(StudentFeeInstallment.builder()
                    .studentFee(studentFee)
                    .installmentNumber(input.installmentNumber())
                    .amount(input.amount())
                    .dueDate(request.startDate().plusDays(input.dueAfterDays()))
                    .build());
        }

        studentFee = studentFeeRepository.save(studentFee);
        auditLogService.log(actorId, "ASSIGN_FEE", "StudentFee", studentFee.getId(), null);
        return StudentFeeResponse.from(studentFee);
    }

    @Transactional
    public StudentFeeResponse addDiscount(UUID studentFeeId, CreateDiscountRequest request, UUID actorId) {
        StudentFee fee = findWithDetails(studentFeeId);
        BigDecimal discountAmount = request.type() == DiscountType.PERCENTAGE
                ? fee.getTotalAmount().multiply(request.value()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : request.value();
        if (discountAmount.compareTo(fee.getTotalAmount()) > 0) {
            throw ApiException.badRequest("Discount cannot exceed the total fee amount");
        }

        FeeDiscount discount = FeeDiscount.builder()
                .studentFee(fee)
                .type(request.type())
                .value(request.value())
                .amount(discountAmount)
                .reason(request.reason())
                .approvedBy(actorId)
                .build();
        discountRepository.save(discount);

        fee.setDiscountAmount(fee.getDiscountAmount().add(discountAmount));
        applyAdjustmentToInstallments(fee, discountAmount.negate());
        recompute(fee);
        fee = studentFeeRepository.save(fee);

        auditLogService.log(actorId, "APPLY_DISCOUNT", "StudentFee", fee.getId(), null);
        return StudentFeeResponse.from(fee);
    }

    @Transactional
    public StudentFeeResponse addFine(UUID studentFeeId, CreateFineRequest request, UUID actorId) {
        StudentFee fee = findWithDetails(studentFeeId);
        StudentFeeInstallment installment = null;
        if (request.installmentId() != null) {
            installment = fee.getInstallments().stream()
                    .filter(i -> i.getId().equals(request.installmentId())).findFirst()
                    .orElseThrow(() -> ApiException.notFound("Installment not found on this fee record"));
        }

        FeeFine fine = FeeFine.builder()
                .studentFee(fee)
                .installment(installment)
                .amount(request.amount())
                .reason(request.reason())
                .imposedBy(actorId)
                .build();
        fineRepository.save(fine);

        fee.setFineAmount(fee.getFineAmount().add(request.amount()));
        if (installment != null) {
            installment.setAmount(installment.getAmount().add(request.amount()));
        } else if (!fee.getInstallments().isEmpty()) {
            // no specific installment named — load onto the next unpaid one
            fee.getInstallments().stream()
                    .filter(i -> i.getStatus() != InstallmentStatus.PAID)
                    .min((a, b) -> a.getInstallmentNumber() - b.getInstallmentNumber())
                    .ifPresent(i -> i.setAmount(i.getAmount().add(request.amount())));
        }
        recompute(fee);
        fee = studentFeeRepository.save(fee);

        auditLogService.log(actorId, "IMPOSE_FINE", "StudentFee", fee.getId(), null);
        return StudentFeeResponse.from(fee);
    }

    @Transactional
    public StudentFeeResponse waiveFine(UUID studentFeeId, UUID fineId, UUID actorId) {
        StudentFee fee = findWithDetails(studentFeeId);
        FeeFine fine = fineRepository.findById(fineId).orElseThrow(() -> ApiException.notFound("Fine not found"));
        if (!fine.getStudentFee().getId().equals(studentFeeId)) {
            throw ApiException.badRequest("Fine does not belong to this fee record");
        }
        if (fine.getStatus() != FineStatus.PENDING) {
            throw ApiException.conflict("Only a pending fine can be waived");
        }
        fine.setStatus(FineStatus.WAIVED);
        fine.setWaivedBy(actorId);
        fine.setWaivedAt(OffsetDateTime.now());
        fineRepository.save(fine);

        fee.setFineAmount(fee.getFineAmount().subtract(fine.getAmount()).max(BigDecimal.ZERO));
        if (fine.getInstallment() != null) {
            StudentFeeInstallment installment = fine.getInstallment();
            installment.setAmount(installment.getAmount().subtract(fine.getAmount()).max(installment.getPaidAmount()));
        }
        recompute(fee);
        fee = studentFeeRepository.save(fee);

        auditLogService.log(actorId, "WAIVE_FINE", "FeeFine", fine.getId(), null);
        return StudentFeeResponse.from(fee);
    }

    /** Distributes a discount (negative) across the remaining unpaid installments, latest first. */
    private void applyAdjustmentToInstallments(StudentFee fee, BigDecimal delta) {
        if (fee.getInstallments().isEmpty() || delta.compareTo(BigDecimal.ZERO) == 0) return;
        BigDecimal remaining = delta.abs();
        List<StudentFeeInstallment> unpaid = fee.getInstallments().stream()
                .filter(i -> i.getStatus() != InstallmentStatus.PAID)
                .sorted((a, b) -> b.getInstallmentNumber() - a.getInstallmentNumber())
                .toList();
        for (StudentFeeInstallment installment : unpaid) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal reducible = installment.getAmount().subtract(installment.getPaidAmount());
            BigDecimal reduceBy = reducible.min(remaining);
            installment.setAmount(installment.getAmount().subtract(reduceBy));
            remaining = remaining.subtract(reduceBy);
        }
    }

    /** Recomputes netPayable and status from totalAmount/discount/fine/amountPaid. Also called after payments. */
    public static void recompute(StudentFee fee) {
        BigDecimal net = fee.getTotalAmount().subtract(fee.getDiscountAmount()).add(fee.getFineAmount());
        fee.setNetPayable(net.max(BigDecimal.ZERO));

        if (fee.getStatus() == FeeStatus.WAIVED || fee.getStatus() == FeeStatus.CANCELLED) {
            return; // manual terminal states are not auto-overwritten
        }
        if (fee.getAmountPaid().compareTo(BigDecimal.ZERO) <= 0) {
            fee.setStatus(hasOverdueInstallment(fee) ? FeeStatus.OVERDUE : FeeStatus.PENDING);
        } else if (fee.getAmountPaid().compareTo(fee.getNetPayable()) >= 0) {
            fee.setStatus(FeeStatus.PAID);
        } else {
            fee.setStatus(hasOverdueInstallment(fee) ? FeeStatus.OVERDUE : FeeStatus.PARTIAL);
        }
    }

    private static boolean hasOverdueInstallment(StudentFee fee) {
        LocalDate today = LocalDate.now();
        return fee.getInstallments().stream().anyMatch(i ->
                i.getStatus() != InstallmentStatus.PAID && i.getStatus() != InstallmentStatus.WAIVED
                        && i.getDueDate() != null && i.getDueDate().isBefore(today));
    }
}
