package com.quantrity.antscaledisplay;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

/** Shared synchronous policy boundary for the API 23-28 TLS provider update. */
final class SecurityProviderPreflight {
    enum Status { READY, REPAIRABLE, UNAVAILABLE }
    enum Action { PROCEED, LAUNCH_REPAIR, FAIL }

    static final class Outcome {
        final Status status;
        final Intent repairIntent;

        private Outcome(Status status, Intent repairIntent) {
            this.status = status;
            this.repairIntent = repairIntent;
        }

        static Outcome ready() {
            return new Outcome(Status.READY, null);
        }

        static Outcome repairable(Intent intent) {
            return intent == null
                    ? unavailable()
                    : new Outcome(Status.REPAIRABLE, intent);
        }

        static Outcome unavailable() {
            return new Outcome(Status.UNAVAILABLE, null);
        }
    }

    Outcome install(Context context) {
        if (Build.VERSION.SDK_INT >= 29) return Outcome.ready();
        try {
            ProviderInstaller.installIfNeeded(context.getApplicationContext());
            return Outcome.ready();
        } catch (GooglePlayServicesRepairableException exception) {
            return Outcome.repairable(exception.getIntent());
        } catch (GooglePlayServicesNotAvailableException exception) {
            return Outcome.unavailable();
        }
    }

    static Action actionFor(Status status, boolean repairAttempted) {
        if (status == Status.READY) return Action.PROCEED;
        if (status == Status.REPAIRABLE && !repairAttempted) return Action.LAUNCH_REPAIR;
        return Action.FAIL;
    }
}
