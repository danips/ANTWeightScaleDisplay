package com.quantrity.antscaledisplay;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Garmin weight upload and history operations using bearer authentication. */
final class GarminWeightService {
    private static final String TAG = "GarminUpload";
    private static final int MAX_ERROR_DETAIL_LENGTH = 240;
    private static final String UPLOAD_URL =
            "https://connectapi.garmin.com/upload-service/upload";
    private static final String HISTORY_ROOT =
            "https://connectapi.garmin.com/weight-service/weight/range/1970-01-01/";

    private final GarminHttpClient http;
    private final GarminAuthenticator authenticator;

    GarminWeightService(GarminHttpClient http, GarminAuthenticator authenticator) {
        this.http = http;
        this.authenticator = authenticator;
    }

    String upload(File fitFile) {
        String accessToken = authenticator.accessToken();
        if (accessToken == null) return "Session expired. Please login again.";
        String boundary = "---------------------------" + System.currentTimeMillis();
        try {
            byte[] payload = multipart(fitFile, boundary);
            Map<String, String> headers = apiHeaders(accessToken);
            headers.put("Content-Type", "multipart/form-data; boundary=" + boundary);
            GarminHttpClient.Response response = http.executeRaw(
                    "POST", UPLOAD_URL, headers, payload, true);
            log("http=" + response.code + " endpoint=connectapi.garmin.com/upload-service/upload"
                    + " requestBytes=" + payload.length
                    + " responseBytes=" + response.body.length());
            if (response.code == 200 || response.code == 201) return null;
            String detail = uploadFailureDetail(response);
            return "Upload Failed: " + response.code + " - " + detail;
        } catch (Exception exception) {
            return "Error: " + exception.getMessage();
        }
    }

    String downloadHistory() {
        String accessToken = authenticator.accessToken();
        if (accessToken == null) return null;
        try {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            GarminHttpClient.Response response = http.execute("GET",
                    HISTORY_ROOT + today + "?includeAll=true", null, null,
                    apiHeaders(accessToken), true);
            return response.code == 200 ? response.body : null;
        } catch (Exception exception) {
            return null;
        }
    }

    private static Map<String, String> apiHeaders(String accessToken) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + accessToken);
        headers.put("NK", "NT");
        headers.put("Accept", "application/json");
        headers.put("User-Agent", "GCM-Android-5.23");
        headers.put("X-Garmin-User-Agent", "com.garmin.android.apps.connectmobile/5.23; ; "
                + "Google/sdk_gphone64_arm64/google; Android/33; Dalvik/2.1.0");
        headers.put("X-Garmin-Paired-App-Version", "10861");
        headers.put("X-Garmin-Client-Platform", "Android");
        headers.put("X-App-Ver", "10861");
        headers.put("X-Lang", "en");
        headers.put("X-GCExperience", "GC5");
        headers.put("Accept-Language", "en-US,en;q=0.9");
        return headers;
    }

    private static String uploadFailureDetail(GarminHttpClient.Response response) {
        if (response.body.isEmpty()) return "Garmin returned an empty error response";
        String content = firstImportFailureMessage(response.body);
        if (content.isEmpty()) content = firstJsonErrorMessage(response.body);
        String lower = content.toLowerCase(Locale.US);
        if (lower.contains("upload consent is not yet granted")
                || (lower.contains("upload consent") && lower.contains("revoked"))) {
            return "Garmin upload consent is disabled for this EU account. In Garmin Connect, "
                    + "open More/Menu > Settings > Profile & Privacy > Data > Device Upload and "
                    + "enable uploads, then try again.";
        }
        if (!content.isEmpty()) return sanitizeServerMessage(content);

        String trimmed = response.body.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")
                || trimmed.matches("(?is).*<(?:!doctype|html|body)\\b.*")) {
            return "Garmin returned an unrecognized upload error response";
        }
        return sanitizeServerMessage(trimmed);
    }

    private static String firstImportFailureMessage(String body) {
        try {
            JSONObject detailed = new JSONObject(body).optJSONObject("detailedImportResult");
            JSONArray failures = detailed == null ? null : detailed.optJSONArray("failures");
            if (failures == null) return "";
            for (int failureIndex = 0; failureIndex < failures.length(); failureIndex++) {
                JSONObject failure = failures.optJSONObject(failureIndex);
                JSONArray messages = failure == null ? null : failure.optJSONArray("messages");
                if (messages == null) continue;
                for (int messageIndex = 0; messageIndex < messages.length(); messageIndex++) {
                    JSONObject message = messages.optJSONObject(messageIndex);
                    String content = message == null ? "" : message.optString("content", "").trim();
                    if (!content.isEmpty()) return content;
                }
            }
        } catch (Exception ignored) {
            // Generic response handling below provides a safe fallback for unknown formats.
        }
        return "";
    }

    private static String firstJsonErrorMessage(String body) {
        try {
            JSONObject json = new JSONObject(body);
            String[] keys = {"message", "error_description", "error", "detail"};
            for (String key : keys) {
                Object value = json.opt(key);
                if (value instanceof String && !((String) value).trim().isEmpty()) {
                    return ((String) value).trim();
                }
            }
        } catch (Exception ignored) {
            // Plain-text and HTML responses are handled by the caller.
        }
        return "";
    }

    private static String sanitizeServerMessage(String value) {
        String sanitized = value.replaceAll("\\s+", " ").trim();
        sanitized = sanitized.replaceAll(
                "(?i)(https?://[^\\s?]+)\\?[^\\s]+", "$1?<query-redacted>");
        sanitized = sanitized.replaceAll(
                "(?i)(password|ticket|token|secret|code|oauth_[a-z_]+)=([^&\\s]+)",
                "$1=<redacted>");
        sanitized = sanitized.replaceAll(
                "(?i)bearer\\s+[a-z0-9._~-]+", "Bearer <redacted>");
        sanitized = sanitized.replaceAll(
                "(?i)[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9.-]+\\.[a-z]{2,}",
                "<email-redacted>");
        if (sanitized.isEmpty()) return "Garmin returned an empty error response";
        return sanitized.length() > MAX_ERROR_DETAIL_LENGTH
                ? sanitized.substring(0, MAX_ERROR_DETAIL_LENGTH) + "…" : sanitized;
    }

    private static void log(String message) {
        try {
            Log.i(TAG, message);
        } catch (RuntimeException ignored) {
            // android.util.Log is unavailable in local JVM unit tests.
        }
    }

    private static byte[] multipart(File fitFile, String boundary) throws Exception {
        String lineEnd = "\r\n";
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(("--" + boundary + lineEnd).getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"file\"; filename=\""
                + fitFile.getName() + "\"" + lineEnd).getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: application/octet-stream" + lineEnd + lineEnd)
                .getBytes(StandardCharsets.UTF_8));
        try (FileInputStream input = new FileInputStream(fitFile)) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        }
        output.write((lineEnd + "--" + boundary + "--" + lineEnd)
                .getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }
}
