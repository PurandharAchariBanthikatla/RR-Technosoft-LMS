package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.*;

public record CreateLessonRequest(
        @NotBlank @Size(min = 2, max = 200) String title,
        @NotBlank @Pattern(regexp = "VIDEO|ARTICLE|RESOURCE", message = "type must be VIDEO, ARTICLE or RESOURCE") String type,
        @Min(0) Integer durationMinutes,
        /** URL for VIDEO/RESOURCE lessons, or markdown body for ARTICLE lessons. */
        String contentUrl
) {}
