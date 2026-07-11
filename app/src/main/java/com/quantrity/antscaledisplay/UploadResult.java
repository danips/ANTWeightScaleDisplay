package com.quantrity.antscaledisplay;

/** Immutable outcome of the non-UI portion of a measurement upload. */
final class UploadResult {
    final boolean garminAttempted;
    final boolean garminSucceeded;
    final String garminError;
    final MeasurementTextFormatter.EmailMessage emailMessage;
    final String emailError;

    UploadResult(boolean garminAttempted, boolean garminSucceeded, String garminError,
                 MeasurementTextFormatter.EmailMessage emailMessage, String emailError) {
        this.garminAttempted = garminAttempted;
        this.garminSucceeded = garminSucceeded;
        this.garminError = garminError;
        this.emailMessage = emailMessage;
        this.emailError = emailError;
    }
}
