package com.rrtechnosoft.lms.dto.request;

import com.rrtechnosoft.lms.entity.enums.LiveClassPlatform;
import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateLiveClassRequest(
        @NotNull UUID courseId,
        @NotBlank @Size(min = 3, max = 200) String title,
        @NotNull LiveClassPlatform platform,
        @NotBlank String meetingLink,
        @NotNull OffsetDateTime startTime,
        @NotNull OffsetDateTime endTime
) {}
