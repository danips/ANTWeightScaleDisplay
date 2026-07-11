package com.quantrity.antscaledisplay;

/** Supplies a Garmin MFA code; null or empty means the user cancelled. */
interface MfaCodeProvider {
    String requestCode() throws InterruptedException;
}
