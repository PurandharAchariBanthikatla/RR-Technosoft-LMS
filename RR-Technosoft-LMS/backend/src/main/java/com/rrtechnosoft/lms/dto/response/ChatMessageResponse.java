package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.ChatMessage;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Field names pinned to the frontend `ChatMessage` type in src/types/index.ts. */
public record ChatMessageResponse(
        UUID id,
        String role,
        String content,
        OffsetDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage m) {
        return new ChatMessageResponse(m.getId(), m.getRole().name(), m.getContent(), m.getCreatedAt());
    }
}
