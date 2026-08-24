package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.response.NotificationResponse;
import com.rrtechnosoft.lms.entity.Notification;
import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.entity.enums.NotificationType;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.NotificationRepository;
import com.rrtechnosoft.lms.repository.UserRepository;
import com.rrtechnosoft.lms.service.notification.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * The one place that both records an in-app notification AND fans it out to
 * external channels (email always-on, WhatsApp opt-in via config — see
 * EmailNotificationChannel / WhatsAppNotificationChannel). Other services
 * (CertificateService, AssignmentService, ...) call notify() as a side
 * effect; they don't talk to JavaMailSender/Twilio directly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final List<NotificationChannel> channels;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> ApiException.notFound("Notification not found"));
        if (!notification.getUser().getId().equals(userId)) {
            throw ApiException.forbidden("This notification does not belong to you");
        }
        notification.setRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllRead(userId);
    }

    /**
     * Records the notification and fans it out across every registered
     * channel. Runs in the caller's transaction for the DB write; channel
     * failures are swallowed inside each channel implementation so they
     * never roll back the triggering action (e.g. grading a submission).
     */
    @Transactional
    public void notify(UUID userId, NotificationType type, String title, String body, String link) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Skipped notification — user {} not found", userId);
            return;
        }
        Notification notification = notificationRepository.save(Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .link(link)
                .build());

        for (NotificationChannel channel : channels) {
            channel.send(user, title, body);
        }
    }
}
