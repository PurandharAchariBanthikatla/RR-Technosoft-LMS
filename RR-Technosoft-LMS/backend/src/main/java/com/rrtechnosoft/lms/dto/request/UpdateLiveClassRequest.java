package com.rrtechnosoft.lms.dto.request;

import com.rrtechnosoft.lms.entity.enums.LiveClassPlatform;
import com.rrtechnosoft.lms.entity.enums.LiveClassStatus;
import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;

public record UpdateLiveClassRequest(
        @NotBlank @Size(min = 3, max = 200) String title,
        @NotNull LiveClassPlatform platform,
        @NotBlank String meetingLink,
        @NotNull OffsetDateTime startTime,
        @NotNull OffsetDateTime endTime,
        @NotNull LiveClassStatus status,
        String recordingUrl
) {}
