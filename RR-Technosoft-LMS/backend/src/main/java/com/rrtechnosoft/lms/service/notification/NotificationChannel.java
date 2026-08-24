package com.rrtechnosoft.lms.service.notification;

import com.rrtechnosoft.lms.entity.User;

/**
 * A delivery side-channel for a notification that's already been recorded
 * in-app (the notifications table is always the source of truth — these are
 * best-effort external deliveries layered on top). Spring collects every
 * bean implementing this interface into NotificationService's channel list,
 * so adding SMS/push later is a new @Component, not a change here.
 */
public interface NotificationChannel {
    void send(User recipient, String title, String body);
}
