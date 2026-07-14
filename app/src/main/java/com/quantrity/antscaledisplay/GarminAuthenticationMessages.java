package com.quantrity.antscaledisplay;

import android.content.Context;

/** Formats structured Garmin authentication results without exposing credentials or tokens. */
final class GarminAuthenticationMessages {
    private GarminAuthenticationMessages() {}

    static String failure(Context context, GarminAuthenticator.SignInReport report) {
        int summary;
        switch (report.failure) {
            case INVALID_CREDENTIALS:
                summary = R.string.garmin_login_invalid_credentials;
                break;
            case RATE_LIMITED:
                summary = R.string.garmin_login_rate_limited;
                break;
            case NETWORK:
                summary = R.string.garmin_login_network_error;
                break;
            case SERVER:
                summary = R.string.garmin_login_server_error;
                break;
            case STORAGE:
                summary = R.string.garmin_login_storage_error;
                break;
            case CANCELLED:
                summary = R.string.garmin_login_cancelled;
                break;
            case PROTOCOL:
            case NONE:
            default:
                summary = R.string.garmin_login_protocol_error;
                break;
        }
        String http = report.httpStatus > 0
                ? Integer.toString(report.httpStatus)
                : context.getString(R.string.garmin_login_not_available);
        return context.getString(R.string.garmin_login_error_details,
                context.getString(summary), report.stage.name(), http, report.detail);
    }

    static String success(Context context, GarminAuthenticator.SignInReport report) {
        return context.getString(report.usedMfa
                ? R.string.garmin_login_success_mfa : R.string.garmin_login_success_direct);
    }
}
