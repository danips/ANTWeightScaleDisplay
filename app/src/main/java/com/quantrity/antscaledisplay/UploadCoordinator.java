package com.quantrity.antscaledisplay;

import android.util.Log;

import java.io.File;

/** Runs upload work synchronously; its caller owns the single background executor. */
final class UploadCoordinator {
    private static final String TAG = "UploadCoordinator";

    interface ProgressCallback { void operationCompleted(); }

    private final FitFileFactory fitFileFactory;
    private final MeasurementTextFormatter textFormatter;

    UploadCoordinator() {
        this(new FitFileFactory(), new MeasurementTextFormatter());
    }

    UploadCoordinator(FitFileFactory fitFileFactory, MeasurementTextFormatter textFormatter) {
        this.fitFileFactory = fitFileFactory;
        this.textFormatter = textFormatter;
    }

    UploadResult run(MainActivity activity, Weight weight, User user, boolean uploadToGarmin,
                     boolean prepareEmail, ProgressCallback progress) {
        boolean garminSucceeded = false;
        String garminError = null;
        MeasurementTextFormatter.EmailMessage email = null;
        String emailError = null;

        if (uploadToGarmin && !Thread.currentThread().isInterrupted()) {
            try {
                File fitFile = fitFileFactory.create(
                        new File(activity.getFilesDir(), "weight.fit"), weight);
                GarminForegroundSession garmin = new GarminForegroundSession(user,
                        AppRepository.get(activity).usersSnapshot(), activity);
                if (garmin.signIn()) {
                    garminError = garmin.upload(fitFile);
                    garminSucceeded = garminError == null;
                } else {
                    garminError = activity.getString(R.string.weight_fragment_msg_wrong_credentials);
                }
            } catch (RuntimeException exception) {
                Log.e(TAG, "Unable to create or upload the FIT file", exception);
                garminError = activity.getString(
                        R.string.weight_fragment_msg_uploading_encoding_failure);
            } finally {
                progress.operationCompleted();
            }
        }

        if (prepareEmail && !Thread.currentThread().isInterrupted()) {
            try {
                email = textFormatter.email(
                        new MeasurementTextFormatter.AndroidStrings(activity), user, weight);
            } catch (RuntimeException exception) {
                Log.e(TAG, "Unable to format the measurement email", exception);
                emailError = exception.getMessage();
            } finally {
                progress.operationCompleted();
            }
        }

        return new UploadResult(uploadToGarmin, garminSucceeded, garminError, email, emailError);
    }
}
