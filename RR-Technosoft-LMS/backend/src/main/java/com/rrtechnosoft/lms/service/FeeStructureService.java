package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.CreateFeeStructureRequest;
import com.rrtechnosoft.lms.dto.request.FeeInstallmentInput;
import com.rrtechnosoft.lms.dto.request.UpdateFeeStructureRequest;
import com.rrtechnosoft.lms.dto.response.FeeStructureResponse;
import com.rrtechnosoft.lms.entity.Course;
import com.rrtechnosoft.lms.entity.FeeStructure;
import com.rrtechnosoft.lms.entity.FeeStructureInstallment;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.CourseRepository;
import com.rrtechnosoft.lms.repository.FeeStructureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeeStructureService {

    private final FeeStructureRepository feeStructureRepository;
    private final CourseRepository courseRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<FeeStructureResponse> list(UUID courseId, boolean activeOnly, Pageable pageable) {
        return feeStructureRepository.search(courseId, activeOnly, pageable).map(FeeStructureResponse::from);
    }

    @Transactional(readOnly = true)
    public FeeStructureResponse get(UUID id) {
        return FeeStructureResponse.from(findWithInstallments(id));
    }

    @Transactional
    public FeeStructureResponse create(CreateFeeStructureRequest request, UUID actorId) {
        validateInstallments(request.installments(), request.totalAmount());
        Course course = request.courseId() == null ? null : courseRepository.findById(request.courseId())
                .orElseThrow(() -> ApiException.notFound("Course not found"));

        FeeStructure structure = FeeStructure.builder()
                .course(course)
                .name(request.name())
                .description(request.description())
                .totalAmount(request.totalAmount())
                .currency(request.currency() == null || request.currency().isBlank() ? "INR" : request.currency())
                .installmentCount(request.installments().size())
                .createdBy(actorId)
                .build();

        for (FeeInstallmentInput input : request.installments()) {
            structure.getInstallments().add(FeeStructureInstallment.builder()
                    .feeStructure(structure)
                    .installmentNumber(input.installmentNumber())
                    .amount(input.amount())
                    .dueAfterDays(input.dueAfterDays())
                    .build());
        }

        structure = feeStructureRepository.save(structure);
        auditLogService.log(actorId, "CREATE_FEE_STRUCTURE", "FeeStructure", structure.getId(), null);
        return FeeStructureResponse.from(structure);
    }

    @Transactional
    public FeeStructureResponse update(UUID id, UpdateFeeStructureRequest request, UUID actorId) {
        FeeStructure structure = findWithInstallments(id);
        if (request.name() != null && !request.name().isBlank()) structure.setName(request.name());
        if (request.description() != null) structure.setDescription(request.description());
        if (request.isActive() != null) structure.setIsActive(request.isActive());
        structure = feeStructureRepository.save(structure);
        auditLogService.log(actorId, "UPDATE_FEE_STRUCTURE", "FeeStructure", structure.getId(), null);
        return FeeStructureResponse.from(structure);
    }

    @Transactional
    public void delete(UUID id, UUID actorId) {
        FeeStructure structure = findWithInstallments(id);
        structure.setIsActive(false);
        feeStructureRepository.save(structure);
        auditLogService.log(actorId, "DEACTIVATE_FEE_STRUCTURE", "FeeStructure", id, null);
    }

    @Transactional(readOnly = true)
    public FeeStructure findWithInstallments(UUID id) {
        return feeStructureRepository.findWithInstallmentsById(id)
                .orElseThrow(() -> ApiException.notFound("Fee structure not found"));
    }

    static void validateInstallments(List<FeeInstallmentInput> installments, BigDecimal totalAmount) {
        if (installments == null || installments.isEmpty()) {
            throw ApiException.badRequest("At least one installment is required");
        }
        BigDecimal sum = installments.stream().map(FeeInstallmentInput::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(totalAmount) != 0) {
            throw ApiException.badRequest("Installment amounts (" + sum + ") must add up to the total amount (" + totalAmount + ")");
        }
        List<Integer> numbers = installments.stream().map(FeeInstallmentInput::installmentNumber).sorted().toList();
        for (int i = 0; i < numbers.size(); i++) {
            if (numbers.get(i) != i + 1) {
                throw ApiException.badRequest("Installment numbers must be sequential starting at 1");
            }
        }
    }
}
