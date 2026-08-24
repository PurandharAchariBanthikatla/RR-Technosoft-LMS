package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCourseModuleRequest(
        @NotBlank @Size(min = 2, max = 200) String title
) {}
