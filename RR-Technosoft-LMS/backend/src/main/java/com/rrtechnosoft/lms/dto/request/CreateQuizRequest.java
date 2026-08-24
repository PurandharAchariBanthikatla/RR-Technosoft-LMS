package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CreateQuizRequest(
        UUID moduleId,
        @NotBlank @Size(min = 3, max = 200) String title,
        @Min(1) Integer durationMinutes,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal passScorePct,
        @NotNull OffsetDateTime availableFrom,
        @NotNull OffsetDateTime availableTo,
        @NotEmpty @Valid List<QuestionInput> questions
) {
    public record QuestionInput(
            @NotBlank String question,
            @NotEmpty @Size(min = 2) List<@NotBlank String> options,
            @NotNull @Min(0) Integer correctOptionIndex
    ) {}
}
