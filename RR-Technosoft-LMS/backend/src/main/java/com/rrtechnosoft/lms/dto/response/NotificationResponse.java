package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.Notification;
import com.rrtechnosoft.lms.entity.enums.NotificationType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String body,
        String link,
        boolean read,
        OffsetDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getLink(), n.isRead(), n.getCreatedAt());
    }
}
