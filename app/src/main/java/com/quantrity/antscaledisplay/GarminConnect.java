package com.quantrity.antscaledisplay;

import android.app.Activity;

import java.io.File;
import java.util.ArrayList;

/** Compatibility facade over the separated Garmin authentication and weight services. */
public class GarminConnect {
    private final User user;
    private final GarminAuthenticator authenticator;
    private final GarminWeightService weightService;

    public GarminConnect(User user, ArrayList<User> users, Activity activity) {
        this.user = user;
        GarminHttpClient http = new GarminHttpClient(true);
        GarminTokenStore tokenStore = new GarminTokenStore(
                activity.getApplicationContext(), user, users);
        authenticator = new GarminAuthenticator(
                http, tokenStore, new DialogMfaCodeProvider(activity));
        weightService = new GarminWeightService(http, authenticator);
    }

    public boolean signin(User ignored) {
        if (user == null) return false;
        return authenticator.signIn(user.gc_user, user.gc_pass)
                == GarminAuthenticator.SignInResult.SUCCESS;
    }

    public String uploadFitFile(File fitFile) {
        return weightService.upload(fitFile);
    }

    public boolean downloadHistory(StringBuilder result) {
        String history = weightService.downloadHistory();
        if (history == null) return false;
        result.append(history);
        return true;
    }
}
