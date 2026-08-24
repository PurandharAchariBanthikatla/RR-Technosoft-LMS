package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.ChatConversation;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Field names pinned to the frontend `ChatConversation` type in src/types/index.ts. */
public record ChatConversationResponse(
        UUID id,
        String title,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ChatConversationResponse from(ChatConversation c) {
        return new ChatConversationResponse(c.getId(), c.getTitle(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
