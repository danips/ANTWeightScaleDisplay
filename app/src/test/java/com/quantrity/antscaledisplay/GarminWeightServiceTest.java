package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class GarminWeightServiceTest {
    @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void uploadUsesCurrentGarminMobileClientHeaders() throws Exception {
        CapturingTransport transport = new CapturingTransport(
                new GarminHttpClient.Response(201, "{}", "https://response", null));
        GarminAuthenticator authenticator = new GarminAuthenticator(
                new GarminHttpClient(transport), validTokens(), () -> null);
        File fitFile = temporaryFolder.newFile("weight.fit");

        String error = new GarminWeightService(
                new GarminHttpClient(transport), authenticator).upload(fitFile);

        assertNull(error);
        GarminHttpClient.Request request = transport.request;
        assertEquals("Bearer access", request.headers.get("Authorization"));
        assertEquals("GCM-Android-5.23", request.headers.get("User-Agent"));
        assertEquals("Android", request.headers.get("X-Garmin-Client-Platform"));
        assertEquals("10861", request.headers.get("X-App-Ver"));
        assertTrue(request.headers.get("Content-Type").startsWith("multipart/form-data;"));
    }

    @Test
    public void uploadReturnsSafePlainTextErrorForPopupAndLogging() throws Exception {
        CapturingTransport transport = new CapturingTransport(
                new GarminHttpClient.Response(412, "precondition detail",
                        "https://response", null));
        GarminAuthenticator authenticator = new GarminAuthenticator(
                new GarminHttpClient(transport), validTokens(), () -> null);

        String error = new GarminWeightService(new GarminHttpClient(transport), authenticator)
                .upload(temporaryFolder.newFile("weight.fit"));

        assertEquals("Upload Failed: 412 - precondition detail", error);
    }

    @Test
    public void unknownJsonUploadFailureDoesNotExposeRawResponse() throws Exception {
        String body = "{\"owner\":148590834,\"internalData\":\"private\"}";
        CapturingTransport transport = new CapturingTransport(
                new GarminHttpClient.Response(400, body, "https://response", null));
        GarminAuthenticator authenticator = new GarminAuthenticator(
                new GarminHttpClient(transport), validTokens(), () -> null);

        String error = new GarminWeightService(new GarminHttpClient(transport), authenticator)
                .upload(temporaryFolder.newFile("weight.fit"));

        assertEquals("Upload Failed: 400 - Garmin returned an unrecognized upload error response",
                error);
        assertFalse(error.contains("148590834"));
        assertFalse(error.contains("private"));
    }

    @Test
    public void jsonUploadMessageIsSanitizedAndBounded() throws Exception {
        String body = "{\"message\":\"Account person@example.com token=abc123 "
                + new String(new char[300]).replace('\0', 'x') + "\"}";
        CapturingTransport transport = new CapturingTransport(
                new GarminHttpClient.Response(400, body, "https://response", null));
        GarminAuthenticator authenticator = new GarminAuthenticator(
                new GarminHttpClient(transport), validTokens(), () -> null);

        String error = new GarminWeightService(new GarminHttpClient(transport), authenticator)
                .upload(temporaryFolder.newFile("weight.fit"));

        assertTrue(error.contains("Account <email-redacted> token=<redacted>"));
        assertFalse(error.contains("person@example.com"));
        assertFalse(error.contains("abc123"));
        assertTrue(error.endsWith("…"));
        assertTrue(error.length() <= "Upload Failed: 400 - ".length() + 241);
    }

    @Test
    public void euConsentFailureReturnsActionablePopupMessage() throws Exception {
        String body = "{\"detailedImportResult\":{\"failures\":[{\"messages\":[{"
                + "\"code\":2,\"content\":\"The user is from EU location, but upload "
                + "consent is not yet granted or revoked\"}]}]}}";
        CapturingTransport transport = new CapturingTransport(
                new GarminHttpClient.Response(412, body, "https://response", null));
        GarminAuthenticator authenticator = new GarminAuthenticator(
                new GarminHttpClient(transport), validTokens(), () -> null);

        String error = new GarminWeightService(new GarminHttpClient(transport), authenticator)
                .upload(temporaryFolder.newFile("weight.fit"));

        assertTrue(error.contains("Garmin upload consent is disabled"));
        assertTrue(error.contains("Profile & Privacy > Data > Device Upload"));
        assertFalse(error.contains("detailedImportResult"));
    }

    private static GarminAuthenticator.TokenStore validTokens() {
        return new GarminAuthenticator.TokenStore() {
            @Override public String accessToken() { return "access"; }
            @Override public long accessExpiry() { return Long.MAX_VALUE; }
            @Override public String oauth1Token() { return null; }
            @Override public String oauth1Secret() { return null; }
            @Override public String mfaToken() { return null; }
            @Override public void storeOAuth1(
                    String token, String secret, String mfaToken, long mfaExpiry) {}
            @Override public boolean storeAccess(String token, long expiry, boolean tokensOnly) {
                return true;
            }
            @Override public void scheduleRefresh() {}
        };
    }

    private static final class CapturingTransport implements GarminHttpClient.Transport {
        private final GarminHttpClient.Response response;
        private GarminHttpClient.Request request;

        CapturingTransport(GarminHttpClient.Response response) { this.response = response; }

        @Override
        public GarminHttpClient.Response execute(GarminHttpClient.Request request) {
            this.request = request;
            return response;
        }
    }
}
