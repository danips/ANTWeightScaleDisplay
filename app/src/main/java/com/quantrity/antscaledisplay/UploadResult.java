package com.quantrity.antscaledisplay;

/** Immutable outcome of the non-UI portion of a measurement upload. */
final class UploadResult {
    final boolean garminSucceeded;
    final String garminError;
    final MeasurementTextFormatter.EmailMessage emailMessage;
    final String emailError;

    UploadResult(boolean garminSucceeded, String garminError,
                 MeasurementTextFormatter.EmailMessage emailMessage, String emailError) {
        this.garminSucceeded = garminSucceeded;
        this.garminError = garminError;
        this.emailMessage = emailMessage;
        this.emailError = emailError;
    }
}
