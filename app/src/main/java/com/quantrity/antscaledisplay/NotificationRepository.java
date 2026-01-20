package com.quantrity.antscaledisplay;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class NotificationRepository {
    private static final NotificationRepository instance = new NotificationRepository();
    private final MutableLiveData<String> latestNotification = new MutableLiveData<>();

    private NotificationRepository() {}

    public static NotificationRepository getInstance() {
        return instance;
    }

    public LiveData<String> getLatestNotification() {
        return latestNotification;
    }

    public void postNotification(String text) {
        latestNotification.postValue(text);
    }
}