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
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.InputType;
import android.util.Pair;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.Header;
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
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.http.HttpRequest;
import oauth.signpost.signature.HmacSha1MessageSigner;
// Disable custom entity, need to find a fix to avoid heavy external Apache libs
// import org.kochka.android.weightlogger.tools.SimpleMultipartEntity;


public class GarminConnect {
  private static final String TAG = "GarminConnect";

  private User user;
  private ArrayList<User> users;
  private Activity currentActivity;
  GarminConnect (User user, ArrayList<User> users, Activity currentActivity) {
    this.user = user;
    this.users = users;
    this.currentActivity = currentActivity;
  }

  private class OAuth1Token {
    private String oauth1Token;
    private String oauth1TokenSecret;

    // These member are currently unused. Maybe they can be used to issue a new OAuth1 token without
    // requiring the user to re-enter an MFA code.
    private String mfaToken;
    private long mfaExpirationTimestamp;

    public OAuth1Token(String oauth1Token, String oauth1TokenSecret, String mfaToken, long mfaExpirationTimestamp) {
      this.oauth1Token = oauth1Token;
      this.oauth1TokenSecret = oauth1TokenSecret;
      this.mfaToken = mfaToken;
      this.mfaExpirationTimestamp = mfaExpirationTimestamp;
    }

    public String getOauth1Token() {
      return oauth1Token;
    }

    public String getOauth1TokenSecret() {
      return oauth1TokenSecret;
    }

    public String getMfaToken() {
      return mfaToken;
    }

    public long getMfaTokenExpirationTimestamp() {
      return mfaExpirationTimestamp;
    }

    public Boolean saveToSharedPreferences(SharedPreferences.Editor sharedPreferenceEditor) {
      // Todo: support EncryptedSharedPreferences.
      sharedPreferenceEditor.putString("garminOauth1Token", this.oauth1Token);
      sharedPreferenceEditor.putString("garminOauth1TokenSecret", this.oauth1TokenSecret);
      sharedPreferenceEditor.putString("garminOauth1MfaToken", this.mfaToken);
      sharedPreferenceEditor.putLong("garminOauth1MfaExpirationTimestamp", this.mfaExpirationTimestamp);
      return sharedPreferenceEditor.commit();
    }

    public Boolean clearFromSharedPreferences(SharedPreferences.Editor sharedPreferenceEditor) {
      sharedPreferenceEditor.remove("garminOauth1Token");
      sharedPreferenceEditor.remove("garminOauth1TokenSecret");
      sharedPreferenceEditor.remove("garminOauth1MfaToken");
      sharedPreferenceEditor.remove("garminOauth1MfaExpirationTimestamp");
      return sharedPreferenceEditor.commit();
    }
  }

  // In outer scope as static methods in nested classes aren't supported in this version of the JDK.
  private Boolean loadOauth1FromSharedPreferences(SharedPreferences sharedPreferences) {
    String token = user.garminOauth1Token;
    String tokenSecret = user.garminOauth1TokenSecret;
    String mfaToken = user.garminOauth1MfaToken;
    long mfaExpirationTimestamp = user.garminOauth1MfaExpirationTimestamp;

    /*String token = sharedPreferences.getString("garminOauth1Token","");
    String tokenSecret = sharedPreferences.getString("garminOauth1TokenSecret","");
    String mfaToken = sharedPreferences.getString("garminOauth1MfaToken","");
    long mfaExpirationTimestamp = sharedPreferences.getLong("garminOauth1MfaExpirationTimestamp",Long.MAX_VALUE); // Default to unexpired*/

    long currentTime = System.currentTimeMillis() / 1000;
    if (token == null || token.isEmpty() || tokenSecret == null || tokenSecret.isEmpty() || mfaExpirationTimestamp < currentTime) {
      return false;
    }

    this.oauth1Token = new OAuth1Token(token,tokenSecret,mfaToken,mfaExpirationTimestamp);
    return true;
  }

  public class OAuth2Token {
    private String oauth2Token;
    private String oauth2RefreshToken;
    private long timeOfExpiry;
    private long timeOfRefreshExpiry;


    public OAuth2Token(String oauth2Token, String oauth2RefreshToken, long timeOfExpiry, long timeOfRefreshExpiry) {
      this.oauth2Token = oauth2Token;
      this.oauth2RefreshToken = oauth2RefreshToken;
      this.timeOfExpiry = timeOfExpiry;
      this.timeOfRefreshExpiry = timeOfRefreshExpiry;
    }

    public String getOauth2Token() {
      return oauth2Token;
    }

    public Boolean saveToSharedPreferences(SharedPreferences.Editor sharedPreferenceEditor) {
      user.garminOauth2Token = this.oauth2Token;
      user.garminOauth2RefreshToken = this.oauth2RefreshToken;
      user.garminOauth2ExpiryTimestamp = this.timeOfExpiry;
      user.garminOauth2RefreshExpiryTimestamp = this.timeOfRefreshExpiry;
      User.serializeUsers(currentActivity.getApplicationContext(), users);

      /*// Todo: support EncryptedSharedPreferences.
      sharedPreferenceEditor.putString("garminOauth2Token", this.oauth2Token);
      sharedPreferenceEditor.putString("garminOauth2RefreshToken", this.oauth2RefreshToken);
      sharedPreferenceEditor.putLong("garminOauth2ExpiryTimestamp", this.timeOfExpiry);
      sharedPreferenceEditor.putLong("garminOauth2RefreshExpiryTimestamp", this.timeOfRefreshExpiry);
      return sharedPreferenceEditor.commit();*/
      return true;
    }

    public Boolean clearFromSharedPreferences(SharedPreferences.Editor sharedPreferenceEditor) {
      user.garminOauth2Token = null;
      user.garminOauth2RefreshToken = null;
      user.garminOauth2ExpiryTimestamp = -1;
      user.garminOauth2RefreshExpiryTimestamp = -1;
      User.serializeUsers(currentActivity.getApplicationContext(), users);

      /*sharedPreferenceEditor.remove("garminOauth2Token");
      sharedPreferenceEditor.remove("garminOauth2RefreshToken");
      sharedPreferenceEditor.remove("garminOauth2ExpiryTimestamp");
      sharedPreferenceEditor.remove("garminOauth2RefreshExpiryTimestamp");
      return sharedPreferenceEditor.commit();*/
      return true;
    }
  }

  private Boolean loadOauth2FromSharedPreferences(SharedPreferences sharedPreferences) {
    long currentTime = System.currentTimeMillis() / 1000;


    String oauth2Token = user.garminOauth2Token;
    String oauth2RefreshToken = user.garminOauth2RefreshToken;
    long timeOfExpiry = user.garminOauth2ExpiryTimestamp;
    long timeOfRefreshExpiry = user.garminOauth2RefreshExpiryTimestamp;
    /*String oauth2Token = sharedPreferences.getString("garminOauth2Token", "");
    String oauth2RefreshToken = sharedPreferences.getString("garminOauth2RefreshToken", "");
    long timeOfExpiry = sharedPreferences.getLong("garminOauth2ExpiryTimestamp",-1);
    long timeOfRefreshExpiry = sharedPreferences.getLong("garminOauth2RefreshExpiryTimestamp",-1);*/

    if (oauth2Token == null || oauth2Token.isEmpty() || timeOfExpiry < currentTime) {
      // Get a new oauth2 token using the saved oauth1 token.
      // According to https://github.com/matin/garth/blob/316787d1e3ff69c09725b2eb8ded748a4422abb3/garth/http.py#L167
      // Garmin Connect also just uses the OAuth1 token to get a new OAuth2 token.
      return  false;
    }

    this.oauth2Token = new OAuth2Token(oauth2Token,oauth2RefreshToken,timeOfExpiry, timeOfRefreshExpiry);
    return true;
  }

  private static final String GET_TICKET_URL = "https://connect.garmin.com/modern/?ticket=";

  // TODO Fetch oauth consumer_secret from here - is this viable from an Android app (extra perms etc)?.
  // TODO Will store in code for now as URL is public
  // TODO Secrets provided from @matin's https://thegarth.s3.amazonaws.com/oauth_consumer.json
  // TODO How to keep secrets secure in Android https://guides.codepath.com/android/storing-secret-keys-in-android

  private static final String OAUTH_CONSUMER_URL = "https://thegarth.s3.amazonaws.com/oauth_consumer.json";
  private static final String OAUTH1_CONSUMER_KEY = "fc3e99d2-118c-44b8-8ae3-03370dde24c0";
  private static final String OAUTH1_CONSUMER_SECRET = "E08WAR897WEy2knn7aFBrvegVAf0AFdWBBF";
  private static final String GET_OAUTH1_URL = "https://connectapi.garmin.com/oauth-service/oauth/preauthorized?";
  private static final String GET_OAUTH2_URL = "https://connectapi.garmin.com/oauth-service/oauth/exchange/user/2.0";
  private static final String FIT_FILE_UPLOAD_URL = "https://connectapi.garmin.com/upload-service/upload";
  private static final String SSO_URL = "https://sso.garmin.com/sso";
  private static final String SSO_EMBED_URL = SSO_URL + "/embed";
  private static final String SSO_SIGNIN_URL = SSO_URL + "/signin";
  private static final String SSO_MFA_URL = SSO_URL + "/verifyMFA/loginEnterMfaCode";
  private static final Pattern LOCATION_PATTERN = Pattern.compile("location: (.*)");
  private static final String CSRF_TOKEN_PATTERN = "name=\"_csrf\" +value=\"([A-Z0-9]+)\"";
  private static final String TICKET_FINDER_PATTERN = "ticket=([^']+?)\";";

  private static final String USER_AGENT = "com.garmin.android.apps.connectmobile";
  public static final String HISTORY_DOWNLOAD_URL = "https://connect.garmin.com/modern/proxy/weight-service/weight/range/1970-01-01/" +
          Calendar.getInstance().get(Calendar.YEAR) + "-" + (Calendar.getInstance().get(Calendar.MONTH) + 1) + "-" + Calendar.getInstance().get(Calendar.DAY_OF_MONTH) + "?includeAll=true";

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

  // TODO: manage the HTTP client and context in a wrapper class
  private CloseableHttpClient httpclient;
  private HttpClientContext httpContext;

  private SharedPreferences authPreferences;
  private  OAuth1Token oauth1Token;
  private OAuth2Token oauth2Token;

  public boolean signin(final String username, final String password, Activity currentActivity) {
    PoolingHttpClientConnectionManager conman =  new PoolingHttpClientConnectionManager();
    conman.setMaxTotal(20);
    conman.setDefaultMaxPerRoute(20);

    HttpClientBuilder clientBuilder = HttpClientBuilder.create();
    clientBuilder.useSystemProperties();
    clientBuilder.setUserAgent(USER_AGENT);

    httpContext = new HttpClientContext();

    // We need a lax redirect strategy as Garmin will redirect POSTs.
    clientBuilder.setRedirectStrategy(new LaxRedirectStrategy());
    httpclient = clientBuilder.build();

    try {
      // Get the sharedPreferences that (may) contain our auth tokens.
      // TODO: make this an encrypted shared preferences object.
      authPreferences = currentActivity.getSharedPreferences(currentActivity.getApplicationContext().getPackageName() + ".garmintokens", Context.MODE_PRIVATE);

      // If we have an unexpired OAuth2 token then we don't need to go through the login flow.
      // TODO: Allow user to invalidate stored tokens (or do it automatically). Currently, if the
      //  tokens are not expired based on the timestamp but are invalid in some way, they will not
      //  be cleared and the user will be unable to log in.
      if (loadOauth2FromSharedPreferences(authPreferences)) {
        return true;
      }

      // https://github.com/mttkay/signpost/blob/master/docs/GettingStarted.md
      // Using signpost's CommonsHttpOAuth instead of DefaultOAuth as per https://github.com/mttkay/signpost
      OAuthConsumer consumer = new DefaultOAuthConsumer(OAUTH1_CONSUMER_KEY, OAUTH1_CONSUMER_SECRET);
      consumer.setMessageSigner(new HmacSha1MessageSigner());

      if(!loadOauth1FromSharedPreferences(authPreferences)) {
        // Get cookies
        // TODO: are cookies actually passed between calls by the http client/context?
        HttpGet cookieGet = new HttpGet(buildURI(SSO_EMBED_URL,EMBED_PARAMS));
        httpclient.execute(cookieGet,httpContext);

        // Create a session.
        HttpGet sessionGetRequest = new HttpGet(buildURI(SSO_SIGNIN_URL, EMBED_PARAMS));
        sessionGetRequest.setHeader(HttpHeaders.REFERER, getLastUri());
        HttpResponse sessionResponse = httpclient.execute(sessionGetRequest, httpContext);
        HttpEntity sessionEntity = sessionResponse.getEntity();
        String sessionContent = EntityUtils.toString(sessionEntity);
        String csrf = getCSRFToken(sessionContent);

        // Sign in
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
        HttpEntity loginResponseEntity = loginResponse.getEntity();
        String loginContent = EntityUtils.toString(loginResponseEntity);

        String ticket = "";
        if (loginRequiresMFA(loginContent)) {
          csrf = getCSRFToken(loginContent);
          String mfaResponse = handleMfa(csrf, currentActivity);
          ticket = getTicketIdFromResponse(mfaResponse);
        } else {
          ticket = getTicketIdFromResponse(loginContent);
        }

        if (!isSignedIn(username)) {
          return  false;
        }

        boolean success = getOAuth1Token(ticket, consumer);
        if (!success) {
          return false;
        }
        this.oauth1Token.saveToSharedPreferences(authPreferences.edit());
      }

      // Use the OAuth1 token to get an OAuth2 token.
      consumer.setTokenWithSecret(oauth1Token.getOauth1Token(), oauth1Token.getOauth1TokenSecret());
      if (!performOauth2exchange(consumer)) {
        // If OAuth2 exchange fails, we cannot upload.
        return false;
      }
      return oauth2Token.saveToSharedPreferences(authPreferences.edit());


    } catch (Exception e) {
      e.printStackTrace();
      httpclient.getConnectionManager().shutdown();
      return false;
    }
  }

  private String buildURI(String root, List<NameValuePair> params) throws URISyntaxException {
    URIBuilder uriBuilder = new URIBuilder(root);
    uriBuilder.addParameters(params);
    return uriBuilder.build().toString();
  }

  private boolean getOAuth1Token(String ticket, OAuthConsumer consumer) throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException, IOException, URISyntaxException {
    List<NameValuePair> oauth1TokenParams = Arrays.asList(
            new BasicNameValuePair("ticket", ticket),
            new BasicNameValuePair("login-url", "https://sso.garmin.com/sso/embed"),
            new BasicNameValuePair("accepts-mfa-tokens", "true")
    );
    String oauth1RequestURI = buildURI(GET_OAUTH1_URL,oauth1TokenParams);
    String signedOauth1RequestURI = consumer.sign(oauth1RequestURI);
    HttpGet getOauth1 = new HttpGet(signedOauth1RequestURI);
    getOauth1.setHeader(HttpHeaders.REFERER, getLastUri());
    HttpResponse response = httpclient.execute(getOauth1,httpContext);
    String oauth1ResponseAsString = EntityUtils.toString(response.getEntity());
    try {
      this.oauth1Token = getOauth1FromResponse(oauth1ResponseAsString);
    }
    catch (java.text.ParseException e) {
      return false;
    }
    return true;
  }

  public void clearFromSharedPreferences1() {
    user.garminOauth1Token = null;
    user.garminOauth1TokenSecret = null;
    user.garminOauth1MfaToken = null;
    user.garminOauth1MfaExpirationTimestamp = Long.MAX_VALUE;
    User.serializeUsers(currentActivity.getApplicationContext(), users);
  }
  public void clearFromSharedPreferences2() {
    user.garminOauth2Token = null;
    user.garminOauth2RefreshToken = null;
    user.garminOauth2ExpiryTimestamp = -1;
    user.garminOauth2RefreshExpiryTimestamp = -1;
    User.serializeUsers(currentActivity.getApplicationContext(), users);
  }

  private boolean performOauth2exchange(OAuthConsumer consumer) throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException, IOException {
    // Exchange for oauth v2 token
    // We have to manually create a request object here because sign(String url) only signs GET
    // requests.
    //HttpPost exchangeRequest = new HttpPost(GET_OAUTH2_URL);
    URL obj = new URL(GET_OAUTH2_URL);
    HttpURLConnection exchangeRequest = (HttpURLConnection) obj.openConnection();
    exchangeRequest.setRequestMethod("POST");
    HttpRequest signedExchangeRequest = consumer.sign(exchangeRequest);

    HttpPost postOauth2 = new HttpPost(GET_OAUTH2_URL);
    postOauth2.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
    //postOauth2.setHeader(HttpHeaders.REFERER, getLastUri());
    postOauth2.setHeader(HttpHeaders.AUTHORIZATION, signedExchangeRequest.getHeader("Authorization"));
    HttpResponse oauth2Response = httpclient.execute(postOauth2,httpContext);
    int responseStatusCode = oauth2Response.getStatusLine().getStatusCode();
    if (responseStatusCode == HttpStatus.SC_UNAUTHORIZED) {
      // If we are unauthorised, clear the oauth1 token and report failure.
      //OAuth1Token.clearFromSharedPreferences(authPreferences.edit());
      clearFromSharedPreferences1();
      return false;
    }
    HttpEntity oauth2Entity = oauth2Response.getEntity();

    String oauth2ResponseAsString = EntityUtils.toString(oauth2Entity);
    try {
      this.oauth2Token = getOauth2FromResponse(oauth2ResponseAsString);
    }
    catch (JSONException e) {
      return  false;
    }

    return true;
  }

  private HttpGet createHttpGetFromLocationHeader(Header h1) {
    Matcher matcher = LOCATION_PATTERN.matcher(h1.toString());
    matcher.find();
    String redirect = matcher.group(1);

    return new HttpGet(redirect);
  }

  private OAuth1Token getOauth1FromResponse(String responseAsString) throws java.text.ParseException {
    // Garmin returns a bare query string. Turn it into a dummy URI for parsing.
    Uri uri = Uri.parse("http://invalid?"+responseAsString);
    String oauth1Token = uri.getQueryParameter("oauth_token");
    String oauth1TokenSecret = uri.getQueryParameter("oauth_token_secret");

    // The following args aren't always present but getQueryParameter will return just null if they
    // aren't.
    String mfaToken = uri.getQueryParameter("mfa_token");
    String mfaExpirationTimestampString = uri.getQueryParameter("mfa_expiration_timestamp");

    // If there is no MFA expiration timestamp, assume that the token doesn't expire and set expiry
    // to max representable value. If the assumption is wrong, the token will be invalidated when a
    // 401 is received.
    // Otherwise, parse the date to a timestamp.
    long mfaExpirationTimestamp = Long.MAX_VALUE;
    if (mfaExpirationTimestampString != null) {
      SimpleDateFormat mfaExpirationFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
      mfaExpirationFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      mfaExpirationTimestamp = mfaExpirationFormat.parse(mfaExpirationTimestampString).getTime();
    }
    return new OAuth1Token(oauth1Token,oauth1TokenSecret, mfaToken, mfaExpirationTimestamp);
  }

  private OAuth2Token getOauth2FromResponse(String responseAsString) throws JSONException {
    long currentTime = System.currentTimeMillis() / 1000;

    // This time they return JSON.
    JSONObject response = new JSONObject(responseAsString);
    return new OAuth2Token(response.getString("access_token"),
            response.getString("refresh_token"),
            Integer.parseInt(response.getString("expires_in"))+currentTime,
            Integer.parseInt(response.getString("refresh_token_expires_in"))+currentTime);
  }

  private String getTicketIdFromResponse(String responseAsString) {
    return getFirstMatch(TICKET_FINDER_PATTERN, responseAsString);
  }

  private String getCSRFToken(String responseAsString) {
    return getFirstMatch(CSRF_TOKEN_PATTERN, responseAsString);
  }

  private String getFirstMatch(String regex, String within) {
    Log.v(TAG, "regex="+regex);
    Log.v(TAG, "within="+within);
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(within);
    matcher.find();
    return matcher.group(1);
  }

  public boolean isSignedIn(String username) {
    if (httpclient == null) return false;
    try {
      HttpResponse execute = httpclient.execute(new HttpGet("https://connect.garmin.com/modern/currentuser-service/user/info"),httpContext);
      HttpEntity entity = execute.getEntity();
      String json = EntityUtils.toString(entity);
      JSONObject js_user = new JSONObject(json);
      entity.consumeContent();
      return js_user.getString("username") != null && !js_user.getString("username").isEmpty();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public String uploadFitFile(File fitFile, MainActivity activity) {
    if (httpclient == null) return activity.getString(R.string.weight_fragment_msg_uploading_failure) + "\n null DefaultHttpClient";
    try {
      HttpPost post = new HttpPost(FIT_FILE_UPLOAD_URL);

      post.setHeader("origin", "https://connect.garmin.com");
      post.setHeader("nk", "NT");
      post.setHeader("accept", "*/*");
      post.setHeader("referer", "https://connect.garmin.com/modern/import-data");
      post.setHeader("authority", "connect.garmin.com");
      post.setHeader("language", "EN");
      post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.oauth2Token.getOauth2Token());

      MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create();
      multipartEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
      multipartEntity.addBinaryBody("file", fitFile);
      post.setEntity(multipartEntity.build());

      HttpResponse httpResponse = httpclient.execute(post, httpContext);
      int responseStatusCode = httpResponse.getStatusLine().getStatusCode();
      if(responseStatusCode == HttpStatus.SC_ACCEPTED){
        Header locationHeader = httpResponse.getFirstHeader("Location");
        String uploadStatusUrl = locationHeader.getValue();
        HttpResponse getStatusResponse = httpclient.execute(new HttpGet(uploadStatusUrl),httpContext);
        String responseString = EntityUtils.toString(getStatusResponse.getEntity());
        JSONObject js_upload = new JSONObject(responseString);
      } else if (responseStatusCode == HttpStatus.SC_UNAUTHORIZED) {
        // If the status unauthorised, our token is probably invalid, so we need to clear it.
        //OAuth2Token.clearFromSharedPreferences(this.authPreferences.edit());
        clearFromSharedPreferences2();
        return "Unauthorised request, invalid token.";
      }

      HttpEntity entity = httpResponse.getEntity();
      String responseString = EntityUtils.toString(entity);
      JSONObject js_upload = new JSONObject(responseString);
      Log.v(TAG, js_upload.toString());
      JSONArray res = js_upload.getJSONObject("detailedImportResult").getJSONArray("failures");
      if (res.length() != 0)
      {
        StringBuilder result = new StringBuilder();
        result.append("GC error: ");
        for (int i = 0; i < res.length(); i++)
        {
          Object item = res.get(i);
          //Log.v(TAG, i + "" + item);
          if (item instanceof JSONObject)
          {
            //Log.v(TAG, i + "" + ((JSONObject) item).getJSONArray("messages").length());
            JSONObject msg = (JSONObject)((JSONObject) item).getJSONArray("messages").get(0);
            result.append(msg.getString("content"));
            //Log.v(TAG, i + "" + msg.getString("content"));
          }
        }
        return result.toString();
      }

      entity.consumeContent();
      if (js_upload.getJSONObject("detailedImportResult").getJSONArray("failures").length() != 0) return "Upload error: detailedImportResult failures";

      return null;
    } catch (Exception e) {
      e.printStackTrace();
      return activity.getString(R.string.weight_fragment_msg_uploading_failure) + "\n" + e;
    }
  }


  private String promptMFAModalDialog(Activity currentActivity) throws InterruptedException {

    BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();

    currentActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        AlertDialog.Builder mfaModalBuilder = new AlertDialog.Builder(currentActivity);
        mfaModalBuilder.setTitle("MFA");
        final EditText mfaInput = new EditText(currentActivity);
        mfaInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        mfaModalBuilder.setView(mfaInput);
        mfaModalBuilder.setMessage("Enter the 6 digit MFA code you received by SMS or email:");
        mfaModalBuilder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface, int id) {
            String textInput = mfaInput.getText().toString();
            inputQueue.add(textInput);
          }
        });

        mfaModalBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface, int i) {
            inputQueue.add(""); // Add this so that the queue doesn't block.
          }
        });

        AlertDialog mfaDialog = mfaModalBuilder.create();
        mfaDialog.setOnShowListener(dialogInterface -> {
          // Request focus for the EditText
          mfaInput.requestFocus();
          // Show the keyboard
          if (mfaInput.requestFocus()) {
            Window window = mfaDialog.getWindow();
            if (window != null) {
              window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
          }
        });

        mfaDialog.show();
      }
    });
    return inputQueue.take();
  }

private boolean loginRequiresMFA(String responseAsString) {
    // Determine whether we need MFA using the title of the response - it will contain the substring
    // 'MFA' if we get redirected to the MFA page.
    String pageTitlePattern = "<title>(.*?)</title>";
    String pageTitle = getFirstMatch(pageTitlePattern,responseAsString);
    if (pageTitle.toUpperCase().contains("MFA")) {
      return true;
    } else {
      return false;
    }
}

  private String handleMfa(String csrf, Activity currentActivity) throws InterruptedException, URISyntaxException, IOException {
    final String mfaCode = promptMFAModalDialog(currentActivity);

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
    HttpResponse loginResponse = httpclient.execute(loginPostRequest,httpContext);
    int code = loginResponse.getStatusLine().getStatusCode();
    HttpEntity loginResponseEntity = loginResponse.getEntity();
    String loginContent = EntityUtils.toString(loginResponseEntity);
    return  loginContent;
  }

  private String getLastUri() {
    String target = this.httpContext.getTargetHost().toString();
    String partialUri = this.httpContext.getRequest().getRequestLine().getUri();
    return  target+partialUri;
  }

  public boolean downloadHistory(FragmentActivity activity, StringBuilder result) {
    //curl 'https://connect.garmin.com/modern/proxy/weight-service/weight/range/2000-01-01/2021-02-24?includeAll=true' -H 'NK: NT'
    // -H 'Cookie: GARMIN-SSO-GUID=B0FE2E647C9D52E2ACD8CC34D472EF77E7042166; SESSIONID=dad23a72-3177-47b5-bb47-ca542877855b;'
    /*if (httpclient == null) {
      result.append(activity.getString(R.string.weight_fragment_msg_uploading_failure)).append("\n null DefaultHttpClient");
      return false;
    }
    try {
      HttpGet get = new HttpGet(HISTORY_DOWNLOAD_URL);

      Log.v(TAG, "CloseableHttpResponse");
      get.setHeader("NK", "NT");
      CloseableHttpResponse httpResponse = httpclient.execute(get);

      Log.v(TAG, "status code" + httpResponse.getStatusLine().getStatusCode());

      Log.v(TAG, "HttpEntity");
      HttpEntity entity = httpResponse.getEntity();
      if (entity != null)
      {
        result.append(EntityUtils.toString(entity));
        entity.consumeContent();
        return true;
      }
      else
      {
        result.append("null entity");
        return false;
      }
    } catch (Exception e) {
      e.printStackTrace();
      result.append(activity.getString(R.string.weight_fragment_msg_uploading_failure)).append("\n").append(e);
      return false;
    }*/
    return false;
  }

  public void close() {
    if (httpclient != null) {
      httpclient.getConnectionManager().shutdown();
      httpclient = null;
    }
  }

}
