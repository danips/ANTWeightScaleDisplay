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

import android.util.Base64;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
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
import cz.msebera.android.httpclient.entity.mime.HttpMultipartMode;
import cz.msebera.android.httpclient.entity.mime.MultipartEntityBuilder;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.impl.conn.PoolingClientConnectionManager;
import cz.msebera.android.httpclient.impl.conn.SchemeRegistryFactory;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.params.BasicHttpParams;
import cz.msebera.android.httpclient.params.CoreProtocolPNames;
import cz.msebera.android.httpclient.params.HttpParams;
import cz.msebera.android.httpclient.util.EntityUtils;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.signature.HmacSha1MessageSigner;
// Disable custom entity, need to find a fix to avoid heavy external Apache libs
// import org.kochka.android.weightlogger.tools.SimpleMultipartEntity;


public class GarminConnect {
  private static final String TAG = "GarminConnect";

  private static final String GET_TICKET_URL = "https://connect.garmin.com/modern/?ticket=";

  // TODO Fetch oauth consumer_secret from here - is this viable from an Android app (extra perms etc)?.
  // TODO Will store in code for now as URL is public
  // TODO Secrets provided from @matin's https://thegarth.s3.amazonaws.com/oauth_consumer.json
  // TODO How to keep secrets secure in Android https://guides.codepath.com/android/storing-secret-keys-in-android

  private static final String OAUTH_CONSUMER_URL = "https://thegarth.s3.amazonaws.com/oauth_consumer.json";
  private static final String OAUTH1_CONSUMER_KEY = "fc3e99d2-118c-44b8-8ae3-03370dde24c0";
  private static final String OAUTH1_CONSUMER_SECRET = "E08WAR897WEy2knn7aFBrvegVAf0AFdWBBF";
  private static final String GET_OAUTH1_URL = "https://connectapi.garmin.com/oauth-service/oauth/preauthorized?login-url=https://sso.garmin.com/sso/embed";
  private static final String GET_OAUTH2_URL = "https://connectapi.garmin.com/oauth-service/oauth/exchange/user/2.0";
  private static final String FIT_FILE_UPLOAD_URL = "https://connect.garmin.com/upload-service/upload/.fit";

  private static final Pattern LOCATION_PATTERN = Pattern.compile("location: (.*)");
  private static final String CSRF_TOKEN_PATTERN = "name=\"_csrf\" *value=\"([A-Z0-9]+)\"";
  private static final String TICKET_FINDER_PATTERN = "ticket=([^']+?)\";";

  private static final String OAUTH1_FINDER_PATTERN = "token\":\"([a-z0-9]+?)\"";
  private static final String OAUTH2_FINDER_PATTERN = "token=([^']+?)\"";

  private static final String USER_AGENT = "com.garmin.android.apps.connectmobile";
  public static final String HISTORY_DOWNLOAD_URL = "https://connect.garmin.com/modern/proxy/weight-service/weight/range/1970-01-01/" +
          Calendar.getInstance().get(Calendar.YEAR) + "-" + (Calendar.getInstance().get(Calendar.MONTH) + 1) + "-" + Calendar.getInstance().get(Calendar.DAY_OF_MONTH) + "?includeAll=true";

  private DefaultHttpClient httpclient;
  private String oauth2;

  public boolean signin(final String username, final String password) {
    PoolingClientConnectionManager conman = new PoolingClientConnectionManager(SchemeRegistryFactory.createDefault());
    conman.setMaxTotal(20);
    conman.setDefaultMaxPerRoute(20);
    httpclient = new DefaultHttpClient(conman);
    httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, USER_AGENT);

    final String signin_url = "https://sso.garmin.com/sso/signin?service=" +
            "https%3A%2F%2Fconnect.garmin.com%2Fmodern%2F" +
            "&webhost=https%3A%2F2%Fconnect.garmin.com%2Fmodern%2F" +
            "&source=https%3A%2F%2Fconnect.garmin.com%2Fsignin" +
            "&redirectAfterAccountLoginUrl=https%3A%2F%2Fconnect.garmin.com%2Fmodern%2F" +
            "&redirectAfterAccountCreationUrl=https%3A%2F%2Fconnect.garmin.com%2Fmodern%2F" +
            "&gauthHost=https%3A%2F%2Fsso.garmin.com%2Fsso" +
            "&locale=en_US" +
            "&id=gauth-widget" +
            "&cssUrl=https%3A%2F%2Fconnect.garmin.com%2Fgauth-custom-v1.2-min.css" +
            "&privacyStatementUrl=https%3A%2F%2Fwww.garmin.com%2Fen-US%2Fprivacy%2Fconnect%2F" +
            "&clientId=GarminConnect" +
            "&rememberMeShown=true" +
            "&rememberMeChecked=false" +
            "&createAccountShown=true" +
            "&openCreateAccount=false" +
            "&displayNameShown=false" +
            "&consumeServiceTicket=false" +
            "&initialFocus=true" +
            "&embedWidget=false" +
            "&generateExtraServiceTicket=true" +
            "&generateTwoExtraServiceTickets=true" +
            "&generateNoServiceTicket=false" +
            "&globalOptInShown=true" +
            "&globalOptInChecked=false" +
            "&mobile=false" +
            "&connectLegalTerms=true" +
            "&showTermsOfUse=false" +
            "&showPrivacyPolicy=false" +
            "&showConnectLegalAge=false" +
            "&locationPromptShown=true" +
            "&showPassword=true" +
            "&useCustomHeader=false" +
            "&mfaRequired=false" +
            "&performMFACheck=false" +
            "&rememberMyBrowserShown=false" +
            "&rememberMyBrowserChecked=false";

    try {
      HttpParams params = new BasicHttpParams();
      params.setParameter("http.protocol.handle-redirects", false);

      // Create session
      Log.v(TAG, "Create session");
      HttpEntity loginEntity = httpclient.execute(new HttpGet(signin_url)).getEntity();
      String loginContent = EntityUtils.toString(loginEntity);
      String csrf = getCSRFToken(loginContent);

      // Sign in
      Log.v(TAG, "Sign in");
      HttpPost post = new HttpPost(signin_url);
      post.setHeader("Referer", signin_url);
      post.setParams(params);
      List<NameValuePair> nvp = new ArrayList<>();
      nvp.add(new BasicNameValuePair("embed", "false"));
      nvp.add(new BasicNameValuePair("username", username));
      nvp.add(new BasicNameValuePair("password", password));
      nvp.add(new BasicNameValuePair("_csrf", csrf));
      post.setEntity(new UrlEncodedFormEntity(nvp));
      HttpEntity entity1 = httpclient.execute(post).getEntity();

      String responseAsString = EntityUtils.toString(entity1);
      Log.v(TAG, responseAsString);
      String ticket = getTicketIdFromResponse(responseAsString);

      // Ticket
      Log.v(TAG, "Ticket " + ticket);
      HttpGet get = new HttpGet(GET_TICKET_URL + ticket);
      get.setParams(params);
      Header getTicketLocation = httpclient.execute(get).getFirstHeader("location");

      // Follow redirections
      Log.v(TAG, "Follow redirections");
      get = createHttpGetFromLocationHeader(getTicketLocation);
      get.setParams(params);
      HttpEntity entity2 = httpclient.execute(get).getEntity();

      if (isSignedIn(username)) {

        // https://github.com/mttkay/signpost/blob/master/docs/GettingStarted.md
        // Using signpost's CommonsHttpOAuth instead of DefaultOAuth as per https://github.com/mttkay/signpost
        OAuthConsumer consumer = new CommonsHttpOAuthConsumer(OAUTH1_CONSUMER_KEY, OAUTH1_CONSUMER_SECRET);
        consumer.setMessageSigner(new HmacSha1MessageSigner());
//        consumer.setTokenWithSecret(OAUTH1_CONSUMER_KEY, OAUTH1_CONSUMER_SECRET);

        org.apache.http.client.methods.HttpGet request = new org.apache.http.client.methods.HttpGet(GET_OAUTH1_URL);
        String signed = consumer.sign(GET_OAUTH1_URL);
        HttpGet getOauth1 = new HttpGet(signed + "&accepts-mfa-tokens=true&ticket=" + ticket);
        HttpResponse response = httpclient.execute(getOauth1);
        String oauth1ResponseAsString = EntityUtils.toString(response.getEntity());
        String oauth1Token = getOauth1FromResponse(oauth1ResponseAsString);

        // Exchange for oauth v2 token
        HttpPost postOauth2 = new HttpPost(GET_OAUTH2_URL);
        post.addHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate");
        post.addHeader(HttpHeaders.ACCEPT, "/");
        post.addHeader(HttpHeaders.AUTHORIZATION, "OAuth " + oauth1Token);
        HttpEntity oauth2Entity = httpclient.execute(postOauth2).getEntity();
        String oauth2ResponseAsString = EntityUtils.toString(oauth2Entity);
        String oauth2Token = getOauth2FromResponse(oauth2ResponseAsString);
      }

      Log.v(TAG, "return");
      return isSignedIn(username);
    } catch (Exception e) {
      e.printStackTrace();
      httpclient.getConnectionManager().shutdown();
      return false;
    }
  }

  @NonNull
  private HttpGet createHttpGetFromLocationHeader(Header h1) {
    Matcher matcher = LOCATION_PATTERN.matcher(h1.toString());
    matcher.find();
    String redirect = matcher.group(1);

    return new HttpGet(redirect);
  }

  private String getOauth1FromResponse(String responseAsString) {
    return getFirstMatch(OAUTH1_FINDER_PATTERN, responseAsString);
  }

  private String getOauth2FromResponse(String responseAsString) {
    return getFirstMatch(OAUTH2_FINDER_PATTERN, responseAsString);
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
    matcher.find();
    return matcher.group(1);
  }

  public boolean isSignedIn(String username) {
    if (httpclient == null) return false;
    try {
      HttpResponse execute = httpclient.execute(new HttpGet("https://connect.garmin.com/modern/currentuser-service/user/info"));
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

      Log.v(TAG, "HttpPost");
      post.setHeader("origin", "https://connect.garmin.com");
      post.setHeader("nk", "NT");
      post.setHeader("accept", "*/*");
      post.setHeader("referer", "https://connect.garmin.com/modern/import-data");
      post.setHeader("authority", "connect.garmin.com");
      post.setHeader("language", "EN");

      Log.v(TAG, "MultipartEntityBuilder");
      MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create();
      multipartEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
      multipartEntity.addBinaryBody("file", fitFile);
      post.setEntity(multipartEntity.build());

      Log.v(TAG, "HttpResponse");
      HttpResponse httpResponse = httpclient.execute(post);
      if(httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED){
        Log.v(TAG, "locationHeader");
        Header locationHeader = httpResponse.getFirstHeader("Location");
        String uploadStatusUrl = locationHeader.getValue();
        HttpResponse getStatusResponse = httpclient.execute(new HttpGet(uploadStatusUrl));
        String responseString = EntityUtils.toString(getStatusResponse.getEntity());
        JSONObject js_upload = new JSONObject(responseString);
      }

      Log.v(TAG, "HttpEntity");
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

  public boolean downloadHistory(FragmentActivity activity, StringBuilder result) {
    //curl 'https://connect.garmin.com/modern/proxy/weight-service/weight/range/2000-01-01/2021-02-24?includeAll=true' -H 'NK: NT'
    // -H 'Cookie: GARMIN-SSO-GUID=B0FE2E647C9D52E2ACD8CC34D472EF77E7042166; SESSIONID=dad23a72-3177-47b5-bb47-ca542877855b;'
    if (httpclient == null) {
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
    }
  }

  public void close() {
    if (httpclient != null) {
      httpclient.getConnectionManager().shutdown();
      httpclient = null;
    }
  }

}
