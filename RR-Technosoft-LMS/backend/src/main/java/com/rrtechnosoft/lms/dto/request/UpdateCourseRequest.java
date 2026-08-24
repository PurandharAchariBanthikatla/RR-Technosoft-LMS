package com.rrtechnosoft.lms.dto.request;

import com.rrtechnosoft.lms.entity.enums.CourseLevel;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateCourseRequest(
        @NotBlank @Size(min = 3, max = 200) String title,
        @NotBlank @Size(min = 20) String description,
        @NotBlank @Size(max = 100) String category,
        @NotNull CourseLevel level,
        @NotNull @Min(1) Integer durationWeeks,
        @NotNull @DecimalMin(value = "0", inclusive = true) BigDecimal price,
        @NotBlank @Size(min = 2, max = 150) String instructorName,
        String thumbnailUrl
) {}
