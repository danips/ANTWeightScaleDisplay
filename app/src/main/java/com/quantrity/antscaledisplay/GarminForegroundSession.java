package com.quantrity.antscaledisplay;

import android.app.Activity;

import java.io.File;
import java.util.ArrayList;

/** Composes interactive authentication and weight operations for one foreground workflow. */
final class GarminForegroundSession {
    private final User user;
    private final GarminAuthenticator authenticator;
    private final GarminWeightService weightService;

    GarminForegroundSession(User user, ArrayList<User> users, Activity activity) {
        this.user = user;
        GarminHttpClient http = new GarminHttpClient(true);
        GarminTokenStore tokenStore = new GarminTokenStore(
                activity.getApplicationContext(), user, users);
        authenticator = new GarminAuthenticator(
                http, tokenStore, new DialogMfaCodeProvider(activity));
        weightService = new GarminWeightService(http, authenticator);
    }

    boolean signIn() {
        if (user == null) return false;
        return authenticator.signIn(user.gc_user, user.gc_pass)
                == GarminAuthenticator.SignInResult.SUCCESS;
    }

    String upload(File fitFile) {
        return weightService.upload(fitFile);
    }

    String downloadHistory() {
        return weightService.downloadHistory();
    }
}
