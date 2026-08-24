package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** conversationId is null to start a new conversation, or an existing one to continue it. */
public record SendChatMessageRequest(
        UUID conversationId,
        @NotBlank @Size(max = 4000) String message
) {}
