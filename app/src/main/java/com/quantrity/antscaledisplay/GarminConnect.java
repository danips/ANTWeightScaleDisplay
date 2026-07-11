package com.quantrity.antscaledisplay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.content.Intent;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.http.HttpRequest;
import oauth.signpost.signature.HmacSha1MessageSigner;

public class GarminConnect {
    private static final String TAG = "GarminConnect";

    // --- Constants ---
    private static final String OAUTH1_CONSUMER_KEY = "fc3e99d2-118c-44b8-8ae3-03370dde24c0";
    private static final String OAUTH1_CONSUMER_SECRET = "E08WAR897WEy2knn7aFBrvegVAf0AFdWBBF";
    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";

    // URLs
    private static final String SSO_ROOT = "https://sso.garmin.com/sso";
    private static final String SSO_EMBED_URL = SSO_ROOT + "/embed";
    private static final String SSO_SIGNIN_URL = SSO_ROOT + "/signin";
    private static final String SSO_MFA_URL = SSO_ROOT + "/verifyMFA/loginEnterMfaCode";
    private static final String OAUTH1_URL = "https://connectapi.garmin.com/oauth-service/oauth/preauthorized";
    private static final String OAUTH2_URL = "https://connectapi.garmin.com/oauth-service/oauth/exchange/user/2.0";
    private static final String UPLOAD_URL = "https://connectapi.garmin.com/upload-service/upload";

    private static final Pattern TICKET_PATTERN = Pattern.compile("ticket=([^\"'&\\\\]+)");

    private final User user;
    private final ArrayList<User> users;
    private final Context context;
    private final Activity currentActivity;
    private OAuth2Token oauth2Token;
    private final CookieManager cookieManager;

    public GarminConnect(User user, ArrayList<User> users, Activity activity) {
        this(user, users, activity.getApplicationContext(), activity);
    }

    GarminConnect(User user, ArrayList<User> users, Context context) {
        this(user, users, context.getApplicationContext(), null);
    }

    private GarminConnect(User user, ArrayList<User> users, Context context, Activity activity) {
        this.user = user;
        this.users = users;
        this.currentActivity = activity;
        this.context = context;

        this.cookieManager = new CookieManager();
        this.cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        // The refresh Worker does not use SSO cookies. Avoid replacing the process-wide cookie
        // handler from a background job while an interactive sign-in may be in progress.
        if (activity != null) CookieHandler.setDefault(this.cookieManager);
        Log.d(TAG, "[DEBUG] GarminConnect initialized for user: " + (user != null ? user.name : "null"));
    }

    public boolean signin(final User user) {
        Log.d(TAG, "[DEBUG] Starting signin flow...");
        if (user == null) return false;
        String username = user.gc_user.trim().replaceAll("[\n\r]", "");
        String password = user.gc_pass.trim().replaceAll("[\n\r]", "");

        try {
            // Check User object directly for valid tokens.
            if (loadOauth2()) {
                GarminTokenRefreshScheduler.schedule(context, user);
                Log.d(TAG, "[DEBUG] Valid token found in User object.");
                return true;
            }
            if (tryRenewAccessToken()) {
                Log.d(TAG, "[DEBUG] Token refreshed in User object.");
                return true;
            }

            Log.d(TAG, "[DEBUG] Starting fresh login...");
            cookieManager.getCookieStore().removeAll();

            // A. Initial GET
            Map<String, String> serviceParams = getServiceParams();
            Map<String, String> pageHeaders = getStandardHeaders();

            HttpResponse response = execute("GET", SSO_SIGNIN_URL, serviceParams, null, pageHeaders, true);

            if (response.code != 200) {
                Log.e(TAG, "[ERROR] Step A Failed: " + response.code);
                return false;
            }

            String csrf = extractCsrf(response.body);
            if (csrf.isEmpty()) {
                Log.e(TAG, "[ERROR] CSRF missing.");
                return false;
            }
            Log.d(TAG, "[DEBUG] CSRF Found: " + csrf.substring(0, 5) + "...");

            // B. POST Credentials
            Map<String, String> loginBody = new HashMap<>();
            loginBody.put("username", username);
            loginBody.put("password", password);
            loginBody.put("embed", "true");
            loginBody.put("_csrf", csrf);

            Map<String, String> formHeaders = getStandardHeaders();
            formHeaders.put("Referer", SSO_SIGNIN_URL);
            formHeaders.put("Origin", "https://sso.garmin.com");
            formHeaders.put("Content-Type", "application/x-www-form-urlencoded");

            response = execute("POST", SSO_SIGNIN_URL, serviceParams, loginBody, formHeaders, true);
            Log.d(TAG, "[DEBUG] Login POST Code: " + response.code);

            // C. Check Ticket or MFA
            String ticket = extractRegex(response.body);
            if (ticket.isEmpty()) ticket = extractRegex(response.url);

            if (ticket.isEmpty()) {
                if (response.body.contains("MFA") || response.body.contains("mfa-code")) {
                    Log.d(TAG, "[DEBUG] MFA Challenge received. Prompting user...");

                    String mfaCsrf = extractCsrf(response.body);
                    if(mfaCsrf.isEmpty()) mfaCsrf = csrf;

                    // Prompt User (Updated to handle permissions)
                    String mfaCode = promptMFAModalDialog();
                    if (mfaCode == null || mfaCode.isEmpty()) return false;

                    Log.d(TAG, "[DEBUG] Sending MFA Code...");
                    Map<String, String> mfaBody = new HashMap<>();
                    mfaBody.put("mfa-code", mfaCode);
                    mfaBody.put("embed", "true");
                    mfaBody.put("_csrf", mfaCsrf);
                    mfaBody.put("fromPage", "setupEnterMfaCode");

                    // Disable Redirects to catch the intermediate hop
                    response = execute("POST", SSO_MFA_URL, serviceParams, mfaBody, formHeaders, false);
                    Log.d(TAG, "[DEBUG] MFA Response Code: " + response.code);

                    // Check Location Header for Ticket
                    List<String> locHeaders = response.headers.get("Location");
                    if (locHeaders != null && !locHeaders.isEmpty()) {
                        String loc = locHeaders.get(0);
                        Log.d(TAG, "[DEBUG] MFA Redirect Location: " + loc);

                        // 1. Check for Ticket directly
                        ticket = extractRegex(loc);

                        // 2. Check for Login Token (Double Hop Fix)
                        if (ticket.isEmpty() && loc.contains("logintoken=")) {
                            Log.d(TAG, "[DEBUG] Login Token found. Following redirect...");
                            response = execute("GET", loc, null, null, pageHeaders, true);
                            ticket = extractRegex(response.url);
                            if (ticket.isEmpty()) ticket = extractRegex(response.body);
                        }
                    }
                }
            }

            if (ticket.isEmpty()) {
                Log.e(TAG, "[ERROR] Login failed. No ticket found.");
                return false;
            }
            Log.d(TAG, "[DEBUG] Ticket Acquired: " + ticket);

            // D. Exchange Ticket for OAuth1
            Log.d(TAG, "[DEBUG] Starting OAuth1 Exchange...");
            OAuthConsumer consumer = new DefaultOAuthConsumer(OAUTH1_CONSUMER_KEY, OAUTH1_CONSUMER_SECRET);
            consumer.setMessageSigner(new HmacSha1MessageSigner());

            String oauth1TokenStr = getOAuth1Token(ticket, consumer);
            if (oauth1TokenStr == null) {
                Log.e(TAG, "[ERROR] OAuth1 exchange returned null.");
                return false;
            }
            Log.d(TAG, "[DEBUG] OAuth1 Success. Token data acquired.");

            // E. Exchange OAuth1 for OAuth2
            Uri uri = Uri.parse("https://dummy?" + oauth1TokenStr);
            String o1Token = uri.getQueryParameter("oauth_token");
            String o1Secret = uri.getQueryParameter("oauth_token_secret");
            if (o1Token == null || o1Secret == null) {
                Log.e(TAG, "[ERROR] OAuth1 response did not contain token credentials.");
                return false;
            }

            // OAuth2 tokens in this legacy Garmin flow are renewed by signing another exchange
            // with these longer-lived OAuth1 credentials. Persist them before the first exchange.
            user.garminOauth1Token = o1Token;
            user.garminOauth1TokenSecret = o1Secret;
            user.garminOauth1MfaToken = uri.getQueryParameter("mfa_token");
            user.garminOauth1MfaExpirationTimestamp = parseMfaExpirationTimestamp(
                    uri.getQueryParameter("mfa_expiration_timestamp"));

            consumer.setTokenWithSecret(o1Token, o1Secret);

            Log.d(TAG, "[DEBUG] Starting OAuth2 Exchange...");
            if (performOAuth2Exchange(consumer) == TokenRenewalResult.SUCCESS) {
                Log.d(TAG, "[DEBUG] Signin Success! Saving tokens to User.");
                return oauth2Token.save(false);
            } else {
                Log.e(TAG, "[ERROR] OAuth2 Exchange Failed.");
            }
        } catch (Exception e) {
            Log.e(TAG, "[CRITICAL] Signin crashed", e);
        }
        return false;
    }

    // --- UI Methods ---
    private String promptMFAModalDialog() throws InterruptedException {
        if (currentActivity == null || currentActivity.isFinishing()) return null;

        BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();

        currentActivity.runOnUiThread(() -> {
            // 1. Check if Notification Permission is missing
            if (!isNotificationServiceEnabled(context)) {
                // Show info dialog, then redirect, then show MFA input
                new AlertDialog.Builder(currentActivity)
                        .setTitle(R.string.auth_notification_permission_title)
                        .setMessage(R.string.auth_notification_permission_message)
                        .setPositiveButton(R.string.auth_notification_permission_enable, (dialog, which) -> {
                            Intent intent;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS);
                                ComponentName componentName = new ComponentName(context, NotificationListener.class);
                                intent.putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, componentName.flattenToString());
                            } else {
                                intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                            }
                            currentActivity.startActivity(intent);
                        })
                        .setNegativeButton(R.string.auth_notification_permission_skip, null)
                        .setOnDismissListener(dialog -> {
                            // 2. Regardless of choice, show the input field
                            showMFAInputField(inputQueue);
                        })
                        .show();
            } else {
                // Permission already exists, show input immediately
                showMFAInputField(inputQueue);
            }
        });

        // Block until user inputs code OR code is auto-detected
        return inputQueue.take();
    }

    private void showMFAInputField(BlockingQueue<String> inputQueue) {
        AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
        builder.setTitle(R.string.auth_garmin_verification_title);
        final EditText input = new EditText(currentActivity);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint(R.string.auth_garmin_verification_hint);
        builder.setView(input);
        builder.setMessage(R.string.auth_garmin_verification_message);

        final Observer<String>[] observerRef = new Observer[1];

        // Buttons
        builder.setPositiveButton(R.string.auth_garmin_verification_submit, (d, id) -> inputQueue.add(input.getText().toString()));
        builder.setNegativeButton(R.string.auth_garmin_verification_cancel, (d, i) -> inputQueue.add(""));

        AlertDialog dialog = builder.create();

        // Observer Logic
        observerRef[0] = code -> {
            if (code != null && code.matches("\\d{6}")) {
                if (dialog.isShowing()) {
                    Log.d(TAG, "[MFA] Auto-detected code: " + code);
                    input.setText(code);
                    inputQueue.add(code);
                    dialog.dismiss();
                }
            }
        };

        // Start observing
        NotificationRepository.getInstance().getLatestNotification().observeForever(observerRef[0]);

        dialog.setOnDismissListener(d -> {
            // Clean up observer when dialog closes (manually or via auto-fill)
            NotificationRepository.getInstance().getLatestNotification().removeObserver(observerRef[0]);

            // Safety: If dialog is dismissed via back button, ensure queue isn't blocked
            if (inputQueue.isEmpty()) {
                inputQueue.add("");
            }
        });

        // Show keyboard automatically
        Objects.requireNonNull(dialog.getWindow()).setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.setOnShowListener(d -> {
            input.requestFocus();
            InputMethodManager imm = (InputMethodManager) currentActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        dialog.show();
    }

    private boolean isNotificationServiceEnabled(Context c) {
        String pkgName = c.getPackageName();
        final String flat = Settings.Secure.getString(c.getContentResolver(), "enabled_notification_listeners");
        if (flat != null && !flat.isEmpty()) {
            final String[] names = flat.split(":");
            for (String name : names) {
                ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null && pkgName.equals(cn.getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public String uploadFitFile(File fitFile) {
        // 1. Check if we have a token loaded
        if (oauth2Token == null) {
            if (!loadOauth2()) return "Not authenticated (No saved token)";
        }

        // 2. Check if Access Token is expired (or close to it)
        if (oauth2Token.expiry < (System.currentTimeMillis() / 1000) + 60) {
            Log.d(TAG, "Access token expired. Attempting refresh...");
            if (!tryRenewAccessToken()) {
                return "Session expired. Please login again.";
            }
        }

        String boundary = "---------------------------" + System.currentTimeMillis();
        String lineEnd = "\r\n";
        String twoHyphens = "--";

        try {
            HttpURLConnection conn = getHttpURLConnection(boundary);

            try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                // Use UTF-8 for filename safety
                String disposition = "Content-Disposition: form-data; name=\"file\"; filename=\"" + fitFile.getName() + "\"" + lineEnd;
                dos.write(disposition.getBytes(StandardCharsets.UTF_8));

                dos.writeBytes("Content-Type: application/octet-stream" + lineEnd);
                dos.writeBytes(lineEnd);

                try (FileInputStream fis = new FileInputStream(fitFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        dos.write(buffer, 0, bytesRead);
                    }
                }
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            }

            int status = conn.getResponseCode();
            if (status == 200 || status == 201) {
                return null; // Success
            } else {
                // Read the error message from the server to debug the 400 error
                String errorMsg;
                InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    errorMsg = readStream(errorStream);
                } else {
                    // Fallback: Check headers if body is empty (common for 400 Bad Request)
                    errorMsg = "Headers: " + conn.getHeaderFields().toString();
                }
                Log.e(TAG, "[ERROR] Upload failed (Code " + status + "): " + errorMsg);
                return "Upload Failed: " + status + " - " + errorMsg;
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @NonNull
    private HttpURLConnection getHttpURLConnection(String boundary) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(UPLOAD_URL).openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");

        conn.setRequestProperty("Authorization", "Bearer " + oauth2Token.accessToken);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("NK", "NT");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        return conn;
    }

    public boolean downloadHistory(StringBuilder result) {
        if (oauth2Token == null) {
            if (!loadOauth2()) {
                Log.e(TAG, "[ERROR] downloadHistory: No saved token.");
                return false;
            }
            Log.d(TAG, "[DEBUG] Token auto-loaded with loadOauth2.");
        }

        // AUTO-REFRESH CHECK
        if (oauth2Token.expiry < (System.currentTimeMillis() / 1000) + 60) {
            if (!tryRenewAccessToken()) {
                Log.e(TAG, "[ERROR] downloadHistory: Refresh failed.");
                return false;
            }
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String today = sdf.format(new Date());
            // Use connectapi.garmin.com (Mobile API) which accepts Bearer tokens
            String url = "https://connectapi.garmin.com/weight-service/weight/range/1970-01-01/" + today + "?includeAll=true";

            Map<String, String> headers = getStandardHeaders();
            headers.put("Authorization", "Bearer " + oauth2Token.accessToken);
            headers.put("NK", "NT");

            HttpResponse resp = execute("GET", url, null, null, headers, true);
            if (resp.code == 200) {
                result.append(resp.body);
                return true;
            } else {
                Log.e(TAG, "[ERROR] Download History Failed: " + resp.code);
            }
        } catch (Exception e) {
            Log.e(TAG, "[ERROR] Download History Exception", e);
        }
        return false;
    }

    // --- Internals ---
    private String getOAuth1Token(String ticket, OAuthConsumer consumer) {
        try {
            String fullUrl = OAUTH1_URL + "?ticket=" + ticket + "&login-url=https://sso.garmin.com/sso/embed&accepts-mfa-tokens=true";

            String signedUrl = consumer.sign(fullUrl);
            HttpResponse resp = execute("GET", signedUrl, null, null, null, true);
            if (resp.code == 200) {
                return resp.body;
            } else {
                Log.e(TAG, "[ERROR] OAuth1 Failed. Code: " + resp.code + ", Body: " + resp.body);
            }
        } catch (Exception e) {
            Log.e(TAG, "[ERROR] OAuth1 Exception", e);
        }
        return null;
    }

    private TokenRenewalResult performOAuth2Exchange(OAuthConsumer consumer) {
        HttpURLConnection conn = null;
        try {
            String body = getOAuth2ExchangeBody();
            VirtualRequest virtualRequest = new VirtualRequest(
                    OAUTH2_URL, "POST", body, "application/x-www-form-urlencoded");
            consumer.sign(virtualRequest);

            conn = (HttpURLConnection) new URL(OAUTH2_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(30_000);
            conn.setReadTimeout(30_000);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String authHeader = virtualRequest.getHeaders().get("Authorization");
            if (authHeader != null) {
                conn.setRequestProperty("Authorization", authHeader);
            }

            conn.setDoOutput(true);
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bodyBytes.length);
            try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
                dos.write(bodyBytes);
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                String responseBody = readStream(conn.getInputStream());
                JSONObject json = new JSONObject(responseBody);
                this.oauth2Token = new OAuth2Token(
                        json.getString("access_token"),
                        json.optLong("expires_in", 3600)
                );
                return TokenRenewalResult.SUCCESS;
            } else {
                Log.e(TAG, "[ERROR] OAuth2 Failed. Code: " + code);
                String err = readStream(conn.getErrorStream());
                Log.e(TAG, "OAuth2 Error Body: " + err);
                if (code == 408 || code == 429 || code >= 500) {
                    return TokenRenewalResult.RETRY;
                }
                return TokenRenewalResult.INVALID;
            }
        } catch (Exception e) {
            Log.e(TAG, "[ERROR] OAuth2 Exception", e);
            return TokenRenewalResult.RETRY;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String getOAuth2ExchangeBody() throws Exception {
        if (user == null || user.garminOauth1MfaToken == null
                || user.garminOauth1MfaToken.isEmpty()) {
            return "";
        }
        return "mfa_token=" + URLEncoder.encode(
                user.garminOauth1MfaToken, StandardCharsets.UTF_8.name());
    }

    // --- Virtual Request Helper ---
    private static class VirtualRequest implements HttpRequest {
        private String url;
        private final String method;
        private final Map<String, String> headers;
        private final byte[] payload;
        private final String contentType;

        public VirtualRequest(String url, String method, String payload, String contentType) {
            this.url = url;
            this.method = method;
            this.headers = new HashMap<>();
            this.payload = payload.getBytes(StandardCharsets.UTF_8);
            this.contentType = contentType;
            if (contentType != null) headers.put("Content-Type", contentType);
        }

        @Override public String getMethod() { return method; }
        @Override public String getRequestUrl() { return url; }
        @Override public void setRequestUrl(String url) { this.url = url; }
        @Override public void setHeader(String name, String value) { headers.put(name, value); }
        @Override public String getHeader(String name) { return headers.get(name); }
        @Override public Map<String, String> getAllHeaders() { return headers; }
        @Override public InputStream getMessagePayload() { return new ByteArrayInputStream(payload); }
        @Override public String getContentType() { return contentType; }
        @Override public Object unwrap() { return null; }

        public Map<String, String> getHeaders() { return headers; }
    }

    private Map<String, String> getServiceParams() {
        Map<String, String> p = new HashMap<>();
        p.put("id", "gauth-widget");
        p.put("embedWidget", "true");
        p.put("gauthHost", SSO_ROOT);
        p.put("clientId", "GarminConnect");
        p.put("locale", "en");
        p.put("redirectAfterAccountLoginUrl", SSO_EMBED_URL);
        p.put("redirectAfterAccountCreationUrl", SSO_EMBED_URL);
        p.put("createAccountShown", "true");
        p.put("openCreateAccount", "false");
        p.put("displayNameShown", "false");
        p.put("consumeServiceTicket", "false");
        p.put("initialFocus", "true");
        p.put("generateExtraServiceTicket", "true");
        p.put("generateTwoFactorAuthTicket", "false");
        p.put("generateNoServiceTicket", "false");
        p.put("globalExitUrl", SSO_EMBED_URL);
        return p;
    }

    private Map<String, String> getStandardHeaders() {
        Map<String, String> h = new HashMap<>();
        h.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
        h.put("Accept-Language", "en-US,en;q=0.9");
        h.put("Sec-Fetch-Dest", "document");
        h.put("Sec-Fetch-Mode", "navigate");
        h.put("Sec-Fetch-Site", "none");
        h.put("Sec-Fetch-User", "?1");
        return h;
    }

    private String extractCsrf(String html) {
        Pattern p1 = Pattern.compile("name=[\"']_csrf[\"'][^>]*value=[\"']([^\"']+)[\"']");
        Matcher m1 = p1.matcher(html);
        if (m1.find()) return m1.group(1);

        Pattern p2 = Pattern.compile("value=[\"']([^\"']+)[\"'][^>]*name=[\"']_csrf[\"']");
        Matcher m2 = p2.matcher(html);
        if (m2.find()) return m1.group(1);

        return "";
    }

    private String extractRegex(String content) {
        if (content == null) return "";
        Matcher m = GarminConnect.TICKET_PATTERN.matcher(content);
        if (m.find()) return m.group(1);
        return "";
    }

    private HttpResponse execute(String method, String urlStr, Map<String, String> urlParams, Map<String, String> bodyParams, Map<String, String> headers, boolean followRedirects) throws Exception {
        StringBuilder sb = new StringBuilder(urlStr);
        if (urlParams != null && !urlParams.isEmpty()) {
            sb.append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : urlParams.entrySet()) {
                if (!first) sb.append("&");
                sb.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                first = false;
            }
        }

        URL url = new URL(sb.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setInstanceFollowRedirects(followRedirects);
        conn.setRequestProperty("User-Agent", USER_AGENT);

        if (headers != null) {
            for (Map.Entry<String, String> h : headers.entrySet()) {
                conn.setRequestProperty(h.getKey(), h.getValue());
            }
        }

        if (bodyParams != null && !bodyParams.isEmpty()) {
            conn.setDoOutput(true);
            StringBuilder bodySb = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : bodyParams.entrySet()) {
                if (!first) bodySb.append("&");
                bodySb.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                first = false;
            }
            try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
                dos.writeBytes(bodySb.toString());
            }
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 400) ? conn.getErrorStream() : conn.getInputStream();
        return new HttpResponse(code, (is != null) ? readStream(is) : "", conn.getURL().toString(), conn.getHeaderFields());
    }

    private String readStream(InputStream is) throws Exception {
        if (is == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        return sb.toString();
    }

    private class OAuth2Token {
        final String accessToken;
        long expiry;

        OAuth2Token(String access, long exp) {
            long now = System.currentTimeMillis() / 1000;
            this.accessToken = access;
            this.expiry = now + exp;
        }

        // Save directly to the user object and persist before reporting success.
        boolean save(boolean persistTokensOnly) {
            if (user != null) {
                user.garminOauth2Token = accessToken;
                user.garminOauth2ExpiryTimestamp = expiry;

                Log.d(TAG, "[DEBUG] Saving tokens to User object: " + user.name);
                boolean saved = persistTokensOnly
                        ? User.persistGarminTokensSynchronously(context, user)
                        : User.serializeUsersSynchronously(context, users);
                if (saved && !persistTokensOnly) {
                    GarminTokenRefreshScheduler.schedule(context, user);
                }
                return saved;
            }
            return false;
        }
    }

    // UPDATED: Load directly from User object
    private boolean loadOauth2() {
        if (user != null && user.garminOauth2Token != null && user.garminOauth2ExpiryTimestamp > (System.currentTimeMillis()/1000)) {
            this.oauth2Token = new OAuth2Token(user.garminOauth2Token, 0);
            this.oauth2Token.expiry = user.garminOauth2ExpiryTimestamp;
            return true;
        }
        return false;
    }

    // Load directly from User object.
    private boolean tryRenewAccessToken() {
        return renewAccessToken(false) == TokenRenewalResult.SUCCESS;
    }

    enum TokenRenewalResult {
        SUCCESS,
        RETRY,
        INVALID
    }

    TokenRenewalResult renewAccessTokenInBackground() {
        return renewAccessToken(true);
    }

    private TokenRenewalResult renewAccessToken(boolean persistTokensOnly) {
        if (!hasOAuth1Credentials()) {
            Log.d(TAG, "[DEBUG] No saved OAuth1 credentials; interactive login is required once.");
            return TokenRenewalResult.INVALID;
        }

        try {
            OAuthConsumer consumer = new DefaultOAuthConsumer(
                    OAUTH1_CONSUMER_KEY, OAUTH1_CONSUMER_SECRET);
            consumer.setMessageSigner(new HmacSha1MessageSigner());
            consumer.setTokenWithSecret(
                    user.garminOauth1Token, user.garminOauth1TokenSecret);

            TokenRenewalResult exchangeResult = performOAuth2Exchange(consumer);
            if (exchangeResult != TokenRenewalResult.SUCCESS) return exchangeResult;
            return this.oauth2Token.save(persistTokensOnly)
                    ? TokenRenewalResult.SUCCESS
                    : TokenRenewalResult.RETRY;
        } catch (Exception e) {
            Log.e(TAG, "[ERROR] Token Refresh failed", e);
            return TokenRenewalResult.RETRY;
        }
    }

    private boolean hasOAuth1Credentials() {
        return user != null
                && user.garminOauth1Token != null
                && !user.garminOauth1Token.isEmpty()
                && user.garminOauth1TokenSecret != null
                && !user.garminOauth1TokenSecret.isEmpty();
    }

    static long parseMfaExpirationTimestamp(String value) {
        if (value == null || value.isEmpty()) return -1;
        try {
            long numericValue = Long.parseLong(value);
            return numericValue > 10_000_000_000L ? numericValue / 1000 : numericValue;
        } catch (NumberFormatException ignored) {
            // Garmin has also returned ISO-8601 timestamps for this field.
        }

        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd HH:mm:ss.SSS"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                return Objects.requireNonNull(format.parse(value)).getTime() / 1000;
            } catch (Exception ignored) {
                // Try the next supported representation.
            }
        }
        return -1;
    }

    private static class HttpResponse {
        final int code;
        final String body;
        final String url;
        final Map<String, List<String>> headers;
        HttpResponse(int c, String b, String u, Map<String, List<String>> h) {
            code = c; body = b; url = u; headers = h;
        }
    }
}
