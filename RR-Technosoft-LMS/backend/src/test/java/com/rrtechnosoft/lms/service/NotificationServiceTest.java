package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.entity.Notification;
import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.entity.enums.NotificationType;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.NotificationRepository;
import com.rrtechnosoft.lms.repository.UserRepository;
import com.rrtechnosoft.lms.service.notification.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationChannel emailChannel;
    @Mock private NotificationChannel whatsAppChannel;

    private NotificationService notificationService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, userRepository,
                List.of(emailChannel, whatsAppChannel));
    }

    @Test
    void notify_savesRecordAndFansOutToEveryChannel() {
        User user = User.builder().id(userId).fullName("Kiran").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.notify(userId, NotificationType.ASSIGNMENT, "Graded", "You scored 90/100", "/link");

        verify(notificationRepository).save(any(Notification.class));
        verify(emailChannel).send(eq(user), eq("Graded"), eq("You scored 90/100"));
        verify(whatsAppChannel).send(eq(user), eq("Graded"), eq("You scored 90/100"));
    }

    @Test
    void notify_skipsSilentlyWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        notificationService.notify(userId, NotificationType.SYSTEM, "title", "body", null);

        verify(notificationRepository, never()).save(any());
        verifyNoInteractions(emailChannel, whatsAppChannel);
    }

    @Test
    void markRead_rejectsWhenNotificationBelongsToSomeoneElse() {
        UUID notificationId = UUID.randomUUID();
        User otherUser = User.builder().id(UUID.randomUUID()).build();
        Notification notification = Notification.builder().id(notificationId).user(otherUser).build();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markRead(notificationId, userId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("does not belong to you");
    }

    @Test
    void markRead_setsReadTrueForOwner() {
        UUID notificationId = UUID.randomUUID();
        User owner = User.builder().id(userId).build();
        Notification notification = Notification.builder().id(notificationId).user(owner).read(false).build();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = notificationService.markRead(notificationId, userId);

        assertThat(response.read()).isTrue();
    }
}
