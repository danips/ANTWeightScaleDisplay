package com.quantrity.antscaledisplay;

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
            if (response.code == 200 || response.code == 201) return null;
            String detail = response.body.isEmpty()
                    ? "Headers: " + response.headers : response.body;
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
        return headers;
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
