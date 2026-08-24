package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.AssignFeeStructureRequest;
import com.rrtechnosoft.lms.dto.request.CreateDiscountRequest;
import com.rrtechnosoft.lms.dto.request.CreateFineRequest;
import com.rrtechnosoft.lms.dto.request.FeeInstallmentInput;
import com.rrtechnosoft.lms.entity.*;
import com.rrtechnosoft.lms.entity.enums.DiscountType;
import com.rrtechnosoft.lms.entity.enums.FeeStatus;
import com.rrtechnosoft.lms.entity.enums.FineStatus;
import com.rrtechnosoft.lms.entity.enums.InstallmentStatus;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentFeeServiceTest {

    @Mock private StudentFeeRepository studentFeeRepository;
    @Mock private StudentFeeInstallmentRepository installmentRepository;
    @Mock private FeeStructureRepository feeStructureRepository;
    @Mock private FeeDiscountRepository discountRepository;
    @Mock private FeeFineRepository fineRepository;
    @Mock private UserRepository userRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private StudentFeeService studentFeeService;

    private final UUID actorId = UUID.randomUUID();

    // --------------------------------------------------------- recompute

    @Test
    void recompute_noPaymentYetAndNoOverdue_isPending() {
        StudentFee fee = feeWithInstallment(new BigDecimal("1000"), BigDecimal.ZERO, LocalDate.now().plusDays(10), InstallmentStatus.PENDING);

        StudentFeeService.recompute(fee);

        assertThat(fee.getNetPayable()).isEqualByComparingTo("1000");
        assertThat(fee.getStatus()).isEqualTo(FeeStatus.PENDING);
    }

    @Test
    void recompute_noPaymentAndOverdueInstallment_isOverdue() {
        StudentFee fee = feeWithInstallment(new BigDecimal("1000"), BigDecimal.ZERO, LocalDate.now().minusDays(3), InstallmentStatus.PENDING);

        StudentFeeService.recompute(fee);

        assertThat(fee.getStatus()).isEqualTo(FeeStatus.OVERDUE);
    }

    @Test
    void recompute_fullyPaid_isPaid() {
        StudentFee fee = feeWithInstallment(new BigDecimal("1000"), new BigDecimal("1000"), LocalDate.now().plusDays(10), InstallmentStatus.PAID);

        StudentFeeService.recompute(fee);

        assertThat(fee.getStatus()).isEqualTo(FeeStatus.PAID);
    }

    @Test
    void recompute_partiallyPaidAndNotOverdue_isPartial() {
        StudentFee fee = feeWithInstallment(new BigDecimal("1000"), new BigDecimal("400"), LocalDate.now().plusDays(10), InstallmentStatus.PARTIAL);

        StudentFeeService.recompute(fee);

        assertThat(fee.getStatus()).isEqualTo(FeeStatus.PARTIAL);
    }

    @Test
    void recompute_partiallyPaidButOverdue_isOverdueNotPartial() {
        StudentFee fee = feeWithInstallment(new BigDecimal("1000"), new BigDecimal("400"), LocalDate.now().minusDays(1), InstallmentStatus.PARTIAL);

        StudentFeeService.recompute(fee);

        assertThat(fee.getStatus()).isEqualTo(FeeStatus.OVERDUE);
    }

    @Test
    void recompute_waivedStatusIsNeverAutoOverwritten() {
        StudentFee fee = feeWithInstallment(new BigDecimal("1000"), BigDecimal.ZERO, LocalDate.now().minusDays(30), InstallmentStatus.PENDING);
        fee.setStatus(FeeStatus.WAIVED);

        StudentFeeService.recompute(fee);

        assertThat(fee.getStatus()).isEqualTo(FeeStatus.WAIVED);
    }

    @Test
    void recompute_netPayableNeverGoesNegative() {
        StudentFee fee = feeWithInstallment(new BigDecimal("100"), BigDecimal.ZERO, LocalDate.now().plusDays(1), InstallmentStatus.PENDING);
        fee.setDiscountAmount(new BigDecimal("500")); // discount larger than total

        StudentFeeService.recompute(fee);

        assertThat(fee.getNetPayable()).isEqualByComparingTo("0");
    }

    // --------------------------------------------------------- addDiscount

    @Test
    void addDiscount_percentageIsComputedAgainstTotalAmount() {
        UUID feeId = UUID.randomUUID();
        StudentFee fee = feeWithInstallment(new BigDecimal("1000"), BigDecimal.ZERO, LocalDate.now().plusDays(10), InstallmentStatus.PENDING);
        fee.setId(feeId);
        when(studentFeeRepository.findWithDetailsById(feeId)).thenReturn(Optional.of(fee));
        when(studentFeeRepository.save(any(StudentFee.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = studentFeeService.addDiscount(feeId,
                new CreateDiscountRequest(DiscountType.PERCENTAGE, new BigDecimal("10"), "Early bird"), actorId);

        assertThat(response.discountAmount()).isEqualByComparingTo("100");
        assertThat(response.netPayable()).isEqualByComparingTo("900");
    }

    @Test
    void addDiscount_exceedingTotalAmount_throwsBadRequest() {
        UUID feeId = UUID.randomUUID();
        StudentFee fee = feeWithInstallment(new BigDecimal("100"), BigDecimal.ZERO, LocalDate.now().plusDays(10), InstallmentStatus.PENDING);
        fee.setId(feeId);
        when(studentFeeRepository.findWithDetailsById(feeId)).thenReturn(Optional.of(fee));

        assertThatThrownBy(() -> studentFeeService.addDiscount(feeId,
                new CreateDiscountRequest(DiscountType.FLAT, new BigDecimal("500"), "Too generous"), actorId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot exceed the total fee amount");
    }

    // --------------------------------------------------------- addFine / waiveFine

    @Test
    void addFine_withoutInstallmentId_loadsOntoEarliestUnpaidInstallment() {
        UUID feeId = UUID.randomUUID();
        StudentFee fee = feeWithInstallment(new BigDecimal("1000"), BigDecimal.ZERO, LocalDate.now().plusDays(10), InstallmentStatus.PENDING);
        fee.setId(feeId);
        BigDecimal originalInstallmentAmount = fee.getInstallments().get(0).getAmount();
        when(studentFeeRepository.findWithDetailsById(feeId)).thenReturn(Optional.of(fee));
        when(studentFeeRepository.save(any(StudentFee.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = studentFeeService.addFine(feeId, new CreateFineRequest(null, new BigDecimal("50"), "Late payment"), actorId);

        assertThat(response.fineAmount()).isEqualByComparingTo("50");
        assertThat(fee.getInstallments().get(0).getAmount()).isEqualByComparingTo(originalInstallmentAmount.add(new BigDecimal("50")));
    }

    @Test
    void waiveFine_onlyPendingFineCanBeWaived() {
        UUID feeId = UUID.randomUUID();
        StudentFee fee = feeWithInstallment(new BigDecimal("1000"), BigDecimal.ZERO, LocalDate.now().plusDays(10), InstallmentStatus.PENDING);
        fee.setId(feeId);
        UUID fineId = UUID.randomUUID();
        FeeFine fine = FeeFine.builder().id(fineId).studentFee(fee).amount(new BigDecimal("50"))
                .reason("x").status(FineStatus.PAID).build();
        when(studentFeeRepository.findWithDetailsById(feeId)).thenReturn(Optional.of(fee));
        when(fineRepository.findById(fineId)).thenReturn(Optional.of(fine));

        assertThatThrownBy(() -> studentFeeService.waiveFine(feeId, fineId, actorId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only a pending fine can be waived");
    }

    @Test
    void waiveFine_reducesFineAmountAndCannotGoNegative() {
        UUID feeId = UUID.randomUUID();
        StudentFee fee = feeWithInstallment(new BigDecimal("1000"), BigDecimal.ZERO, LocalDate.now().plusDays(10), InstallmentStatus.PENDING);
        fee.setId(feeId);
        fee.setFineAmount(new BigDecimal("30"));
        UUID fineId = UUID.randomUUID();
        FeeFine fine = FeeFine.builder().id(fineId).studentFee(fee).amount(new BigDecimal("50"))
                .reason("x").status(FineStatus.PENDING).build();
        when(studentFeeRepository.findWithDetailsById(feeId)).thenReturn(Optional.of(fee));
        when(fineRepository.findById(fineId)).thenReturn(Optional.of(fine));
        when(studentFeeRepository.save(any(StudentFee.class))).thenAnswer(inv -> inv.getArgument(0));

        studentFeeService.waiveFine(feeId, fineId, actorId);

        assertThat(fee.getFineAmount()).isEqualByComparingTo("0"); // 30 - 50, clamped at 0
        assertThat(fine.getStatus()).isEqualTo(FineStatus.WAIVED);
    }

    // --------------------------------------------------------- assign

    @Test
    void assign_courseAlreadyHasFeeRecord_throwsConflict() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        User student = User.builder().id(studentId).build();
        Course course = Course.builder().id(courseId).build();
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(studentFeeRepository.existsByStudentIdAndCourseId(studentId, courseId)).thenReturn(true);

        AssignFeeStructureRequest request = new AssignFeeStructureRequest(
                studentId, courseId, null, new BigDecimal("500"), LocalDate.now(),
                List.of(new FeeInstallmentInput(1, new BigDecimal("500"), 0)));

        assertThatThrownBy(() -> studentFeeService.assign(request, actorId))
                .isInstanceOf(ApiException.class)
                .hasMessage("A fee record already exists for this student and course");
    }

    @Test
    void assign_withoutFeeStructureOrCustomPlan_throwsBadRequest() {
        UUID studentId = UUID.randomUUID();
        when(userRepository.findById(studentId)).thenReturn(Optional.of(User.builder().id(studentId).build()));

        AssignFeeStructureRequest request = new AssignFeeStructureRequest(
                studentId, null, null, null, LocalDate.now(), null);

        assertThatThrownBy(() -> studentFeeService.assign(request, actorId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Either feeStructureId or a custom totalAmount");
    }

    // --------------------------------------------------------- get (access control)

    @Test
    void get_studentCannotViewAnotherStudentsFeeRecord() {
        UUID feeId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        StudentFee fee = feeWithInstallment(new BigDecimal("1000"), BigDecimal.ZERO, LocalDate.now().plusDays(5), InstallmentStatus.PENDING);
        fee.setId(feeId);
        fee.setStudent(User.builder().id(ownerId).build());
        when(studentFeeRepository.findWithDetailsById(feeId)).thenReturn(Optional.of(fee));

        assertThatThrownBy(() -> studentFeeService.get(feeId, viewerId, true))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot view another student's fee record");
    }

    @Test
    void get_studentCanViewOwnFeeRecord() {
        UUID feeId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        StudentFee fee = feeWithInstallment(new BigDecimal("1000"), BigDecimal.ZERO, LocalDate.now().plusDays(5), InstallmentStatus.PENDING);
        fee.setId(feeId);
        fee.setStudent(User.builder().id(studentId).build());
        when(studentFeeRepository.findWithDetailsById(feeId)).thenReturn(Optional.of(fee));

        var response = studentFeeService.get(feeId, studentId, true);

        assertThat(response.id()).isEqualTo(feeId);
    }

    // ------------------------------------------------------------ helpers

    private StudentFee feeWithInstallment(BigDecimal totalAmount, BigDecimal amountPaid, LocalDate dueDate, InstallmentStatus installmentStatus) {
        StudentFee fee = StudentFee.builder()
                .student(User.builder().id(UUID.randomUUID()).build())
                .totalAmount(totalAmount)
                .netPayable(totalAmount)
                .amountPaid(amountPaid)
                .status(FeeStatus.PENDING)
                .build();
        StudentFeeInstallment installment = StudentFeeInstallment.builder()
                .studentFee(fee)
                .installmentNumber(1)
                .amount(totalAmount)
                .paidAmount(amountPaid)
                .dueDate(dueDate)
                .status(installmentStatus)
                .build();
        fee.getInstallments().add(installment);
        return fee;
    }
}
