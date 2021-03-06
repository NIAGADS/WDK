package org.gusdb.wdk.session;

import static org.gusdb.fgputil.FormatUtil.NL;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map.Entry;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.config.OAuthConfig;
import org.gusdb.wdk.model.user.User;
import org.gusdb.wdk.model.user.UserFactory;
import org.json.JSONException;
import org.json.JSONObject;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.TextCodec;

public class OAuthClient {

  private static final Logger LOG = Logger.getLogger(OAuthClient.class);

  private final String _oauthServerBase;
  private final boolean _googleSpecific;
  private final String _clientId;
  private final String _clientSecret;
  private final UserFactory _userFactory;
  private final TrustManager _trustManager;

  public OAuthClient(OAuthConfig config, UserFactory userFactory) throws WdkModelException {
    _oauthServerBase = config.getOauthUrl();
    _googleSpecific = _oauthServerBase.contains("google");
    _clientId = config.getOauthClientId();
    _clientSecret = config.getOauthClientSecret();
    _userFactory = userFactory;
    _trustManager = getTrustManager(config);
  }

  private static TrustManager getTrustManager(OAuthConfig config) throws WdkModelException {
    String keyStoreFile = config.getKeyStoreFile();
    return (keyStoreFile.isEmpty() ? new WdkTrustManager() :
      new WdkTrustManager(Paths.get(keyStoreFile), config.getKeyStorePassPhrase()));
  }

  public long getUserIdFromAuthCode(String authCode, String redirectUri) throws WdkModelException {

    try {
      String oauthUrl = _oauthServerBase + "/token";

      // build form parameters for token request
      MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
      formData.add("grant_type", "authorization_code");
      formData.add("code", authCode);
      formData.add("redirect_uri", redirectUri);
      formData.add("client_id", _clientId);
      formData.add("client_secret", _clientSecret);

      SSLContext sslContext = SSLContext.getInstance("SSL");
      sslContext.init(null, new TrustManager[]{ _trustManager }, null);

      LOG.info("Building token request with the following URL: " + oauthUrl +
          " and params: " + dumpMultiMap(formData));

      // build request and get token response
      Response response = ClientBuilder.newBuilder()
          .withConfig(new ClientConfig())
          .sslContext(sslContext)
          .build()
          .target(oauthUrl)
          .request(MediaType.APPLICATION_JSON)
          .post(Entity.form(formData));
  
      if (response.getStatus() == 200) {
        // Success!  Read result into buffer and convert to JSON
        InputStream resultStream = (InputStream)response.getEntity();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        IoUtil.transferStream(buffer, resultStream);
        JSONObject json = new JSONObject(new String(buffer.toByteArray()));
        LOG.debug("Response received from OAuth server for token request: " + json.toString(2));
        // get id_token from object and decode to user ID
        String idToken = json.getString("id_token");
        return (_googleSpecific ?
            getUserIdFromGoogleIdToken(idToken) :
            getUserIdFromIdToken(idToken, _clientSecret));
      }
      else {
        // Failure; throw exception
        throw new WdkModelException("OAuth2 token request failed with status " +
            response.getStatus() + ": " + response.getStatusInfo().getReasonPhrase() + NL + response.getEntity());
      }
    }
    catch(WdkModelException e) {
      throw e;
    }
    catch(Exception e) {
      throw new WdkModelException("Unable to complete OAuth token request to fetch user id", e);
    }
  }

  private long getUserIdFromGoogleIdToken(String idToken) throws WdkModelException {
    String gmailAddress = GoogleClientUtil.getEmailFromTokenResponse(idToken, _clientId);
    User user = _userFactory.getUserByEmail(gmailAddress);
    if (user != null) {
      return user.getUserId();
    }
    try {
      // user does not exist; automatically create user for this gmail user
      user = _userFactory.createUser(gmailAddress, null, null, null, true, false);
      return user.getUserId();
    }
    catch (WdkUserException ue2) {
      throw new WdkModelException("Could not create WDK user from validated Gmail email", ue2);
    }
  }

  private static long getUserIdFromIdToken(String idToken, String clientSecret) throws WdkModelException {
    try {
      LOG.debug("Attempting parse of id token [" + idToken + "] using client secret '" + clientSecret +"'");
      String encodedKey = TextCodec.BASE64.encode(clientSecret);
      Claims claims = Jwts.parser().setSigningKey(encodedKey).parseClaimsJws(idToken).getBody();
      // TODO: verify additional claims for security
      String userIdStr = claims.getSubject();
      LOG.debug("Received token for sub '" + userIdStr + "' and preferred_username '" + claims.get("preferred_username"));
      if (FormatUtil.isInteger(userIdStr)) {
        return Long.valueOf(userIdStr);
      }
      throw new WdkModelException("Subject returned by OAuth server [" + userIdStr + "] is not a valid user ID.");
    }
    catch (JSONException e) {
      throw new WdkModelException("JWT body returned is not a valid JSON object.");
    }
  }

  private static String dumpMultiMap(MultivaluedMap<String, String> formData) {
    StringBuilder str = new StringBuilder("{").append(NL);
    for (Entry<String,List<String>> entry : formData.entrySet()) {
      str.append("  ").append(entry.getKey()).append(": ")
         .append(FormatUtil.arrayToString(entry.getValue().toArray())).append(NL);
    }
    return str.append("}").append(NL).toString();
  }
}
