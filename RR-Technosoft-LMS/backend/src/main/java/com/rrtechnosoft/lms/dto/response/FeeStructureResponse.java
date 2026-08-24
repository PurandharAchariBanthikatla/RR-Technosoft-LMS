package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.FeeStructure;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record FeeStructureResponse(
        UUID id,
        UUID courseId,
        String courseTitle,
        String name,
        String description,
        BigDecimal totalAmount,
        String currency,
        Integer installmentCount,
        Boolean isActive,
        List<FeeStructureInstallmentResponse> installments,
        OffsetDateTime createdAt
) {
    public static FeeStructureResponse from(FeeStructure f) {
        return new FeeStructureResponse(
                f.getId(),
                f.getCourse() != null ? f.getCourse().getId() : null,
                f.getCourse() != null ? f.getCourse().getTitle() : null,
                f.getName(), f.getDescription(), f.getTotalAmount(), f.getCurrency(),
                f.getInstallmentCount(), f.getIsActive(),
                f.getInstallments().stream().map(FeeStructureInstallmentResponse::from).toList(),
                f.getCreatedAt()
        );
    }
}
