/*
  Copyright 2012 Sébastien Vrillaud
  
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
      http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/
package com.quantrity.antscaledisplay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.InputType;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
//import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpHeaders;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.HttpStatus;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.protocol.HttpClientContext;
import cz.msebera.android.httpclient.client.utils.URIBuilder;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.entity.mime.HttpMultipartMode;
import cz.msebera.android.httpclient.entity.mime.MultipartEntityBuilder;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;
import cz.msebera.android.httpclient.impl.client.LaxRedirectStrategy;
import cz.msebera.android.httpclient.impl.conn.PoolingHttpClientConnectionManager;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.util.EntityUtils;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.signature.HmacSha1MessageSigner;

public class GarminConnect {
    private static final String TAG = "GarminConnect";

    private final User user;
    private final ArrayList<User> users;
    private final Context context;

    // Use Context (getApplicationContext()) to prevent Activity memory leaks
    GarminConnect(User user, ArrayList<User> users, Context context) {
        this.user = user;
        this.users = users;
        this.context = context;
        // Initialize client immediately if needed, or lazily later
        getOrCreateClient();
    }

    private static class OAuth1Token {
        private final String oauth1Token;
        private final String oauth1TokenSecret;
        private final String mfaToken;
        private final long mfaExpirationTimestamp;

        public OAuth1Token(String oauth1Token, String oauth1TokenSecret, String mfaToken, long mfaExpirationTimestamp) {
            this.oauth1Token = oauth1Token;
            this.oauth1TokenSecret = oauth1TokenSecret;
            this.mfaToken = mfaToken;
            this.mfaExpirationTimestamp = mfaExpirationTimestamp;
        }

        public String getOauth1Token() { return oauth1Token; }
        public String getOauth1TokenSecret() { return oauth1TokenSecret; }

        public void saveToSharedPreferences(SharedPreferences.Editor sharedPreferenceEditor) {
            sharedPreferenceEditor.putString("garminOauth1Token", this.oauth1Token);
            sharedPreferenceEditor.putString("garminOauth1TokenSecret", this.oauth1TokenSecret);
            sharedPreferenceEditor.putString("garminOauth1MfaToken", this.mfaToken);
            sharedPreferenceEditor.putLong("garminOauth1MfaExpirationTimestamp", this.mfaExpirationTimestamp);
            sharedPreferenceEditor.commit();
        }
    }

    private Boolean loadOauth1FromSharedPreferences() {
        String token = user.garminOauth1Token;
        String tokenSecret = user.garminOauth1TokenSecret;
        String mfaToken = user.garminOauth1MfaToken;
        long mfaExpirationTimestamp = user.garminOauth1MfaExpirationTimestamp;

        long currentTime = System.currentTimeMillis() / 1000;
        if (token == null || token.isEmpty() || tokenSecret == null || tokenSecret.isEmpty() || mfaExpirationTimestamp < currentTime) {
            return false;
        }

        this.oauth1Token = new OAuth1Token(token, tokenSecret, mfaToken, mfaExpirationTimestamp);
        return true;
    }

    public class OAuth2Token {
        private final String oauth2Token;
        private final String oauth2RefreshToken;
        private final long timeOfExpiry;
        private final long timeOfRefreshExpiry;

        public OAuth2Token(String oauth2Token, String oauth2RefreshToken, long timeOfExpiry, long timeOfRefreshExpiry) {
            this.oauth2Token = oauth2Token;
            this.oauth2RefreshToken = oauth2RefreshToken;
            this.timeOfExpiry = timeOfExpiry;
            this.timeOfRefreshExpiry = timeOfRefreshExpiry;
        }

        public String getOauth2Token() { return oauth2Token; }

        public Boolean saveToSharedPreferences() {
            user.garminOauth2Token = this.oauth2Token;
            user.garminOauth2RefreshToken = this.oauth2RefreshToken;
            user.garminOauth2ExpiryTimestamp = this.timeOfExpiry;
            user.garminOauth2RefreshExpiryTimestamp = this.timeOfRefreshExpiry;
            User.serializeUsers(context, users);
            return true;
        }
    }

    private Boolean loadOauth2FromSharedPreferences() {
        long currentTime = System.currentTimeMillis() / 1000;
        String oauth2Token = user.garminOauth2Token;
        String oauth2RefreshToken = user.garminOauth2RefreshToken;
        long timeOfExpiry = user.garminOauth2ExpiryTimestamp;
        long timeOfRefreshExpiry = user.garminOauth2RefreshExpiryTimestamp;

        if ((oauth2Token == null || timeOfExpiry < currentTime) &&
                (oauth2RefreshToken != null && timeOfRefreshExpiry > currentTime)) {
            return false;
        }

        if (oauth2Token == null || oauth2Token.isEmpty() || timeOfExpiry < currentTime) {
            return false;
        }

        this.oauth2Token = new OAuth2Token(oauth2Token, oauth2RefreshToken, timeOfExpiry, timeOfRefreshExpiry);
        return true;
    }

    private static final String OAUTH1_CONSUMER_KEY = "fc3e99d2-118c-44b8-8ae3-03370dde24c0";
    private static final String OAUTH1_CONSUMER_SECRET = "E08WAR897WEy2knn7aFBrvegVAf0AFdWBBF";
    private static final String GET_OAUTH1_URL = "https://connectapi.garmin.com/oauth-service/oauth/preauthorized?";
    private static final String GET_OAUTH2_URL = "https://connectapi.garmin.com/oauth-service/oauth/exchange/user/2.0";
    private static final String FIT_FILE_UPLOAD_URL = "https://connectapi.garmin.com/upload-service/upload";
    private static final String SSO_URL = "https://sso.garmin.com/sso";
    private static final String SSO_EMBED_URL = SSO_URL + "/embed";
    private static final String SSO_SIGNIN_URL = SSO_URL + "/signin";
    private static final String SSO_MFA_URL = SSO_URL + "/verifyMFA/loginEnterMfaCode";
    private static final String CSRF_TOKEN_PATTERN = "name=\"_csrf\" +value=\"([A-Z0-9]+)\"";
    private static final String TICKET_FINDER_PATTERN = "ticket=([^']+?)\";";
    private static final String USER_AGENT = "com.garmin.android.apps.connectmobile";

    /*public String getHistoryDownloadUrl() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String today = sdf.format(new Date());
        return "https://connect.garmin.com/modern/proxy/weight-service/weight/range/1970-01-01/" + today + "?includeAll=true";
    }*/

    private final List<NameValuePair> EMBED_PARAMS = Arrays.asList(
            new BasicNameValuePair("id", "gauth-widget"),
            new BasicNameValuePair("embedWidget", "true"),
            new BasicNameValuePair("gauthHost", SSO_URL)
    );

    private final List<NameValuePair> SIGNIN_PARAMS = Arrays.asList(
            new BasicNameValuePair("id", "gauth-widget"),
            new BasicNameValuePair("embedWidget", "true"),
            new BasicNameValuePair("gauthHost", SSO_EMBED_URL),
            new BasicNameValuePair("service", SSO_EMBED_URL),
            new BasicNameValuePair("source", SSO_EMBED_URL),
            new BasicNameValuePair("redirectAfterAccountLoginUrl", SSO_EMBED_URL),
            new BasicNameValuePair("redirectAfterAccountCreationUrl", SSO_EMBED_URL)
    );

    private CloseableHttpClient httpclient;
    private HttpClientContext httpContext;
    private OAuth1Token oauth1Token;
    private OAuth2Token oauth2Token;

    // Helper to ensure client exists even if signin() wasn't called (e.g. app restart)
    private void getOrCreateClient() {
        if (httpclient == null) {
            PoolingHttpClientConnectionManager conman = new PoolingHttpClientConnectionManager();
            conman.setMaxTotal(20);
            conman.setDefaultMaxPerRoute(20);

            HttpClientBuilder clientBuilder = HttpClientBuilder.create();
            clientBuilder.setConnectionManager(conman);
            clientBuilder.useSystemProperties();

            clientBuilder.addInterceptorFirst((cz.msebera.android.httpclient.HttpRequest request, cz.msebera.android.httpclient.protocol.HttpContext context) -> {
                if (!request.containsHeader(HttpHeaders.USER_AGENT)) {
                    request.addHeader(HttpHeaders.USER_AGENT, USER_AGENT);
                }
                if (!request.containsHeader("NK")) {
                    request.addHeader("NK", "NT");
                }
            });

            clientBuilder.setRedirectStrategy(new LaxRedirectStrategy());
            httpclient = clientBuilder.build();
            httpContext = new HttpClientContext();
        }
    }

    public boolean signin(final String username, final String password, Activity currentActivity) {
        getOrCreateClient(); // Ensure client is ready

        try {
            SharedPreferences authPreferences = context.getSharedPreferences(context.getPackageName() + ".garmintokens", Context.MODE_PRIVATE);

            // 1. Check existing tokens
            if (loadOauth2FromSharedPreferences()) {
                return true;
            }

            // 2. Try refresh
            OAuthConsumer consumer = new DefaultOAuthConsumer(OAUTH1_CONSUMER_KEY, OAUTH1_CONSUMER_SECRET);
            consumer.setMessageSigner(new HmacSha1MessageSigner());

            if (refreshOauth2Token(consumer)) {
                return true;
            }

            // 3. Full login flow
            if (!loadOauth1FromSharedPreferences()) {
                HttpGet cookieGet = new HttpGet(buildURI(SSO_EMBED_URL, EMBED_PARAMS));
                httpclient.execute(cookieGet, httpContext).close();

                HttpGet sessionGetRequest = new HttpGet(buildURI(SSO_SIGNIN_URL, EMBED_PARAMS));
                sessionGetRequest.setHeader(HttpHeaders.REFERER, getLastUri());
                HttpResponse sessionResponse = httpclient.execute(sessionGetRequest, httpContext);
                String csrf = getCSRFToken(EntityUtils.toString(sessionResponse.getEntity()));
                EntityUtils.consumeQuietly(sessionResponse.getEntity());

                HttpPost loginPostRequest = new HttpPost(buildURI(SSO_SIGNIN_URL, SIGNIN_PARAMS));
                loginPostRequest.setHeader(HttpHeaders.REFERER, getLastUri());
                List<NameValuePair> loginPostEntity = Arrays.asList(
                        new BasicNameValuePair("username", username),
                        new BasicNameValuePair("password", password),
                        new BasicNameValuePair("embed", "true"),
                        new BasicNameValuePair("_csrf", csrf)
                );
                loginPostRequest.setEntity(new UrlEncodedFormEntity(loginPostEntity, "UTF-8"));

                HttpResponse loginResponse = httpclient.execute(loginPostRequest, httpContext);
                String loginContent = EntityUtils.toString(loginResponse.getEntity());
                EntityUtils.consumeQuietly(loginResponse.getEntity());

                String ticket;
                if (loginRequiresMFA(loginContent)) {
                    csrf = getCSRFToken(loginContent);
                    String mfaResponse = handleMfa(csrf, currentActivity);
                    ticket = getTicketIdFromResponse(mfaResponse);
                } else {
                    ticket = getTicketIdFromResponse(loginContent);
                }

                if (!isSignedIn()) {
                    return false;
                }

                if (!getOAuth1Token(ticket, consumer)) {
                    return false;
                }
                this.oauth1Token.saveToSharedPreferences(authPreferences.edit());
            }

            consumer.setTokenWithSecret(oauth1Token.getOauth1Token(), oauth1Token.getOauth1TokenSecret());
            if (!performOauth2exchange(consumer)) {
                return false;
            }
            return oauth2Token.saveToSharedPreferences();

        } catch (Exception e) {
            Log.e(TAG, "Signin failed", e);
            close();
            return false;
        }
    }

    private boolean refreshOauth2Token(OAuthConsumer consumer) {
        if (user.garminOauth2RefreshToken == null) return false;

        try {
            String signedUrl = consumer.sign(GET_OAUTH2_URL);
            HttpPost post = new HttpPost(signedUrl);

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("grant_type", "refresh_token"));
            params.add(new BasicNameValuePair("refresh_token", user.garminOauth2RefreshToken));
            post.setEntity(new UrlEncodedFormEntity(params));

            HttpResponse response = httpclient.execute(post, httpContext);
            try {
                if (response.getStatusLine().getStatusCode() == 200) {
                    String json = EntityUtils.toString(response.getEntity());
                    this.oauth2Token = getOauth2FromResponse(json);
                    return this.oauth2Token.saveToSharedPreferences();
                }
            } finally {
                EntityUtils.consumeQuietly(response.getEntity());
            }
        } catch (Exception e) {
            Log.e(TAG, "Refresh failed", e);
        }
        return false;
    }

    private String buildURI(String root, List<NameValuePair> params) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(root);
        uriBuilder.addParameters(params);
        return uriBuilder.build().toString();
    }

    private boolean getOAuth1Token(String ticket, OAuthConsumer consumer) throws Exception {
        List<NameValuePair> oauth1TokenParams = Arrays.asList(
                new BasicNameValuePair("ticket", ticket),
                new BasicNameValuePair("login-url", "https://sso.garmin.com/sso/embed"),
                new BasicNameValuePair("accepts-mfa-tokens", "true")
        );
        String oauth1RequestURI = buildURI(GET_OAUTH1_URL, oauth1TokenParams);
        String signedOauth1RequestURI = consumer.sign(oauth1RequestURI);

        HttpGet getOauth1 = new HttpGet(signedOauth1RequestURI);
        getOauth1.setHeader(HttpHeaders.REFERER, getLastUri());

        HttpResponse response = httpclient.execute(getOauth1, httpContext);
        try {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String oauth1ResponseAsString = EntityUtils.toString(entity);
                this.oauth1Token = getOauth1FromResponse(oauth1ResponseAsString);
                return true;
            }
            return false;
        } finally {
            EntityUtils.consumeQuietly(response.getEntity());
        }
    }

    public void clearFromSharedPreferences1() {
        user.garminOauth1Token = null;
        user.garminOauth1TokenSecret = null;
        user.garminOauth1MfaToken = null;
        user.garminOauth1MfaExpirationTimestamp = Long.MAX_VALUE;
        User.serializeUsers(context, users);
    }

    public void clearFromSharedPreferences2() {
        user.garminOauth2Token = null;
        user.garminOauth2RefreshToken = null;
        user.garminOauth2ExpiryTimestamp = -1;
        user.garminOauth2RefreshExpiryTimestamp = -1;
        User.serializeUsers(context, users);
    }

    private boolean performOauth2exchange(OAuthConsumer consumer) {
        try {
            String signedUrl = consumer.sign(GET_OAUTH2_URL);
            HttpPost postOauth2 = new HttpPost(signedUrl);

            StringEntity emptyEntity = new StringEntity("", "UTF-8");
            emptyEntity.setContentType("application/x-www-form-urlencoded");
            postOauth2.setEntity(emptyEntity);

            HttpResponse oauth2Response = httpclient.execute(postOauth2, httpContext);
            try {
                int responseStatusCode = oauth2Response.getStatusLine().getStatusCode();
                if (responseStatusCode == HttpStatus.SC_OK) {
                    String oauth2ResponseAsString = EntityUtils.toString(oauth2Response.getEntity());
                    this.oauth2Token = getOauth2FromResponse(oauth2ResponseAsString);
                    return true;
                } else if (responseStatusCode == HttpStatus.SC_UNAUTHORIZED) {
                    clearFromSharedPreferences1();
                } else {
                    Log.e(TAG, "OAuth2 Error Code: " + responseStatusCode);
                }
            } finally {
                EntityUtils.consumeQuietly(oauth2Response.getEntity());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in OAuth2 exchange", e);
        }
        return false;
    }

    /*public boolean downloadHistory(StringBuilder result) {
        getOrCreateClient(); // Ensure client exists
        if (oauth2Token == null) return false;
        try {
            HttpGet get = new HttpGet(getHistoryDownloadUrl());
            get.setHeader("NK", "NT");
            get.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.oauth2Token.getOauth2Token());
            get.setHeader("Accept", "application/json");

            HttpResponse response = httpclient.execute(get, httpContext);
            try {
                int statusCode = response.getStatusLine().getStatusCode();
                String content = EntityUtils.toString(response.getEntity());

                if (statusCode == 200) {
                    result.append(content);
                    return true;
                } else {
                    Log.e(TAG, "Download History Failed: " + statusCode);
                    return false;
                }
            } finally {
                EntityUtils.consumeQuietly(response.getEntity());
            }
        } catch (Exception e) {
            Log.e(TAG, "Download History Exception", e);
            return false;
        }
    }*/

    private OAuth1Token getOauth1FromResponse(String responseAsString) throws java.text.ParseException {
        Uri uri = Uri.parse("https://invalid?" + responseAsString);
        String oauth1Token = uri.getQueryParameter("oauth_token");
        String oauth1TokenSecret = uri.getQueryParameter("oauth_token_secret");
        String mfaToken = uri.getQueryParameter("mfa_token");
        String mfaExpirationTimestampString = uri.getQueryParameter("mfa_expiration_timestamp");

        long mfaExpirationTimestamp = Long.MAX_VALUE;
        if (mfaExpirationTimestampString != null) {
            SimpleDateFormat mfaExpirationFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
            mfaExpirationFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            mfaExpirationTimestamp = mfaExpirationFormat.parse(mfaExpirationTimestampString).getTime();
        }
        return new OAuth1Token(oauth1Token, oauth1TokenSecret, mfaToken, mfaExpirationTimestamp);
    }

    private OAuth2Token getOauth2FromResponse(String responseAsString) throws JSONException {
        long currentTime = System.currentTimeMillis() / 1000;
        JSONObject response = new JSONObject(responseAsString);
        return new OAuth2Token(response.getString("access_token"),
                response.getString("refresh_token"),
                Integer.parseInt(response.getString("expires_in")) + currentTime,
                Integer.parseInt(response.getString("refresh_token_expires_in")) + currentTime);
    }

    private String getTicketIdFromResponse(String responseAsString) {
        return getFirstMatch(TICKET_FINDER_PATTERN, responseAsString);
    }

    private String getCSRFToken(String responseAsString) {
        return getFirstMatch(CSRF_TOKEN_PATTERN, responseAsString);
    }

    private String getFirstMatch(String regex, String within) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(within);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    public boolean isSignedIn() {
        getOrCreateClient(); // Ensure client exists
        if (httpclient == null) return false;
        try {
            HttpGet get = new HttpGet("https://connect.garmin.com/modern/currentuser-service/user/info");

            if (this.oauth2Token != null) {
                get.setHeader("Authorization", "Bearer " + this.oauth2Token.getOauth2Token());
            }

            HttpResponse execute = httpclient.execute(get, httpContext);
            HttpEntity entity = execute.getEntity();

            try {
                if (entity != null) {
                    String json = EntityUtils.toString(entity);
                    if (json.trim().startsWith("{")) {
                        JSONObject js_user = new JSONObject(json);
                        return js_user.has("username") && !js_user.getString("username").isEmpty();
                    }
                }
            } finally {
                EntityUtils.consumeQuietly(entity);
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Check sign-in failed: " + e.getMessage());
            return false;
        }
    }

    // Keep 'MainActivity' in signature to be compatible with existing code,
    // even though we use 'context' internally now.
    public String uploadFitFile(File fitFile) {
        getOrCreateClient(); // Ensure client exists
        if (httpclient == null) return "Client not initialized";

        HttpPost post = new HttpPost(FIT_FILE_UPLOAD_URL);
        post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.oauth2Token.getOauth2Token());

        try {
            MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create();
            multipartEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            multipartEntity.addBinaryBody("file", fitFile);
            post.setEntity(multipartEntity.build());

            HttpResponse httpResponse = httpclient.execute(post, httpContext);
            try {
                int responseStatusCode = httpResponse.getStatusLine().getStatusCode();
                if (responseStatusCode == HttpStatus.SC_UNAUTHORIZED) {
                    clearFromSharedPreferences2();
                    return "Unauthorised request, invalid token.";
                }

                String responseString = EntityUtils.toString(httpResponse.getEntity());
                JSONObject js_upload = new JSONObject(responseString);

                if (js_upload.has("detailedImportResult") &&
                        js_upload.getJSONObject("detailedImportResult").getJSONArray("failures").length() > 0) {
                    return "Upload error: detailedImportResult failures";
                }
                return null; // Success
            } finally {
                EntityUtils.consumeQuietly(httpResponse.getEntity());
            }
        } catch (Exception e) {
            Log.e(TAG, "Upload failed", e);
            return e.getMessage();
        }
    }

    private String promptMFAModalDialog(Activity currentActivity) throws InterruptedException {
        BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
        currentActivity.runOnUiThread(() -> {
            AlertDialog.Builder mfaModalBuilder = new AlertDialog.Builder(currentActivity);
            mfaModalBuilder.setTitle("MFA");
            final EditText mfaInput = new EditText(currentActivity);
            mfaInput.setInputType(InputType.TYPE_CLASS_NUMBER);
            mfaModalBuilder.setView(mfaInput);
            mfaModalBuilder.setMessage("Enter the 6 digit MFA code you received by SMS or email:");
            mfaModalBuilder.setPositiveButton("Submit", (dialogInterface, id) -> inputQueue.add(mfaInput.getText().toString()));
            mfaModalBuilder.setNegativeButton("Cancel", (dialogInterface, i) -> inputQueue.add(""));

            AlertDialog mfaDialog = mfaModalBuilder.create();
            mfaDialog.setOnShowListener(dialogInterface -> {
                mfaInput.requestFocus();
                Window window = mfaDialog.getWindow();
                if (window != null) {
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            });
            mfaDialog.show();
        });
        return inputQueue.take();
    }

    private boolean loginRequiresMFA(String responseAsString) {
        String pageTitlePattern = "<title>(.*?)</title>";
        String pageTitle = getFirstMatch(pageTitlePattern, responseAsString);
        return pageTitle.toUpperCase().contains("MFA");
    }

    private String handleMfa(String csrf, Activity currentActivity) throws InterruptedException, URISyntaxException, IOException {
        final String mfaCode = promptMFAModalDialog(currentActivity);
        if (mfaCode == null || mfaCode.isEmpty()) {
            throw new IOException("MFA Verification cancelled by user");
        }

        URIBuilder mfaURIBuilder = new URIBuilder(SSO_MFA_URL);
        mfaURIBuilder.addParameters(SIGNIN_PARAMS);
        HttpPost loginPostRequest = new HttpPost(mfaURIBuilder.build());
        loginPostRequest.setHeader(HttpHeaders.REFERER, getLastUri());
        List<NameValuePair> loginPostEntity = Arrays.asList(
                new BasicNameValuePair("mfa-code", mfaCode),
                new BasicNameValuePair("embed", "true"),
                new BasicNameValuePair("_csrf", csrf),
                new BasicNameValuePair("fromPage", "setupEnterMfaCode")
        );
        loginPostRequest.setEntity(new UrlEncodedFormEntity(loginPostEntity, "UTF-8"));

        HttpResponse loginResponse = httpclient.execute(loginPostRequest, httpContext);
        try {
            return EntityUtils.toString(loginResponse.getEntity());
        } finally {
            EntityUtils.consumeQuietly(loginResponse.getEntity());
        }
    }

    private String getLastUri() {
        if (this.httpContext.getRequest() == null || this.httpContext.getTargetHost() == null) {
            return SSO_EMBED_URL;
        }
        try {
            String target = this.httpContext.getTargetHost().toURI();
            String partialUri = this.httpContext.getRequest().getRequestLine().getUri();
            return target + partialUri;
        } catch (Exception e) {
            return SSO_EMBED_URL;
        }
    }

    public void close() {
        if (httpclient != null) {
            try {
                httpclient.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing client", e);
            }
            httpclient = null;
        }
    }
}