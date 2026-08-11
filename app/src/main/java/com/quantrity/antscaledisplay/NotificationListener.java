package com.quantrity.antscaledisplay;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class NotificationListener extends NotificationListenerService {

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        // Check for existing notifications when service starts
        fetchActiveNotifications();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        processNotification(sbn.getNotification(), sbn.getPostTime());
    }

    private void fetchActiveNotifications() {
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active != null) {
                for (StatusBarNotification sbn : active) {
                    processNotification(sbn.getNotification(), sbn.getPostTime());
                }
            }
        } catch (Exception e) {
            Log.e("NotificationListener", "Error fetching active notifications", e);
        }
    }

    void processNotification(Notification notification, long postedAtMillis) {
        if (notification == null || notification.extras == null) return;

        CharSequence title = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence contentChar = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (contentChar == null) {
            contentChar = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
        }
        String code = MfaNotificationParser.findCode(title, contentChar);
        if (code != null) {
            NotificationRepository.getInstance().postMfaCode(code, postedAtMillis);
        }
    }
}
