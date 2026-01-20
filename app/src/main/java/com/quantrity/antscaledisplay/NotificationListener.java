package com.quantrity.antscaledisplay;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationListener extends NotificationListenerService {

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        // Check for existing notifications when service starts
        fetchActiveNotifications();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        processNotification(sbn);
    }

    private void fetchActiveNotifications() {
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active != null) {
                for (StatusBarNotification sbn : active) {
                    processNotification(sbn);
                }
            }
        } catch (Exception e) {
            Log.e("NotificationListener", "Error fetching active notifications", e);
        }
    }

    private void processNotification(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();

        Notification notification = sbn.getNotification();
        if (notification == null || notification.extras == null) return;

        // 1. Get the content (Prefer Big Text, fallback to standard Text)
        CharSequence contentChar = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (contentChar == null) {
            contentChar = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
        }
        String content = (contentChar != null) ? contentChar.toString() : "";

        // 2. Apply Regex directly (Look for ANY standalone 6-digit number)
        // Pattern: No digit before, exactly 6 digits, no digit after
        Pattern pattern = Pattern.compile("(?<!\\d)(\\d{6})(?!\\d)");
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            String code = matcher.group(1);

            // 3. Post the code immediately
            NotificationRepository.getInstance().postNotification(code);
        }
    }
}