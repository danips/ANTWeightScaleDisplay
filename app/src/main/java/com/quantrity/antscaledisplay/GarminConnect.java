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
import cz.msebera.android.httpclient.HttpStatus;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.CloseableHttpResponse;
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
// Disable custom entity, need to find a fix to avoid heavy external Apache libs
// import org.kochka.android.weightlogger.tools.SimpleMultipartEntity;


public class GarminConnect {
  private static final String TAG = "GarminConnect";

  private static final String GET_TICKET_URL = "https://connect.garmin.com/modern/?ticket=";

  private static final Pattern LOCATION_PATTERN = Pattern.compile("location: (.*)");
  private static final String CSRF_TOKEN_PATTERN = "name=\"_csrf\" *value=\"([A-Z0-9]+)\"";
  private static final String TICKET_FINDER_PATTERN = "ticket=([^']+?)\";";
  public static final String FIT_FILE_UPLOAD_URL = "https://connect.garmin.com/modern/proxy/upload-service/upload/.fit";
  public static final String HISTORY_DOWNLOAD_URL = "https://connect.garmin.com/modern/proxy/weight-service/weight/range/1970-01-01/" +
          Calendar.getInstance().get(Calendar.YEAR) + "-" + (Calendar.getInstance().get(Calendar.MONTH) + 1) + "-" + Calendar.getInstance().get(Calendar.DAY_OF_MONTH) + "?includeAll=true";

  private DefaultHttpClient httpclient;

  public boolean signin(final String username, final String password) {
    String up = username + "//" + password;
    StringBuilder sb = new StringBuilder();
    char[] tmp = up.toCharArray();
    for (char ch: tmp) sb.append(String.format("%02x", (int)ch));//((ch & 0xf0) >> 4) + ((ch & 0x0f) << 4)));
    tmp = sb.toString().toCharArray();
    sb.setLength(0);
    for (int idx = tmp.length - 1; idx >= 0; idx--) sb.append(tmp[idx]);
    String base64_up = Base64.encodeToString(sb.toString().getBytes(), Base64.DEFAULT);
    Log.v(TAG, "signin " + base64_up);

    PoolingClientConnectionManager conman = new PoolingClientConnectionManager(SchemeRegistryFactory.createDefault());
    conman.setMaxTotal(20);
    conman.setDefaultMaxPerRoute(20);
    httpclient = new DefaultHttpClient(conman);
    httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Mozilla/5.0 (Linux; Android 10; SM-A205U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.210 Mobile Safari/537.36");

    final String signin_url = "https://sso.garmin.com/sso/signin?service=" +
            "https%3A%2F%2Fconnect.garmin.com%2Fmodern%2F" +
            "&webhost=https%3A%2F%2Fconnect.garmin.com%2Fmodern%2F" +
            "&source=https%3A%2F%2Fconnect.garmin.com%2Fsignin%2F" +
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
      httpclient.execute(get);

      Log.v(TAG, "return");
      return isSignedIn();
    } catch (Exception e) {
      e.printStackTrace();
      httpclient.getConnectionManager().shutdown();
      return false;
    }
  }

  private HttpGet createHttpGetFromLocationHeader(Header h1) {
    Matcher matcher = LOCATION_PATTERN.matcher(h1.toString());
    matcher.find();
    String redirect = matcher.group(1);

    return new HttpGet(redirect);
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

  public boolean isSignedIn() {
    if (httpclient == null) return false;
    try {
      CloseableHttpResponse execute = httpclient.execute(new HttpGet("https://connect.garmin.com/modern/currentuser-service/user/info"));
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

      Log.v(TAG, "CloseableHttpResponse");
      CloseableHttpResponse httpResponse = httpclient.execute(post);
      if(httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED){
        Log.v(TAG, "locationHeader");
        Header locationHeader = httpResponse.getFirstHeader("Location");
        String uploadStatusUrl = locationHeader.getValue();
        CloseableHttpResponse getStatusResponse = httpclient.execute(new HttpGet(uploadStatusUrl));
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
