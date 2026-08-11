package com.quantrity.antscaledisplay;

import android.content.Intent;

/** One retained request for the Activity to launch Google Play services repair UI. */
final class SecurityProviderRepairRequest {
    final long generation;
    final Intent intent;

    SecurityProviderRepairRequest(long generation, Intent intent) {
        this.generation = generation;
        this.intent = intent;
    }
}
