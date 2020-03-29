/**
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2016-2019
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.infn.mw.iam.test.oauth;

import static it.infn.mw.iam.test.oauth.ClientRegistrationTestSupport.REGISTER_ENDPOINT;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.io.UnsupportedEncodingException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.service.ClientDetailsEntityService;
import org.mitre.openid.connect.ClientDetailsEntityJsonProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.test.oauth.ClientRegistrationTestSupport.ClientJsonStringBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = IamLoginService.class)
@WebAppConfiguration
@Transactional
public class DeviceCodeTests extends EndpointsTestUtils {

  public static final String DEVICE_CODE_ENDPOINT = "/devicecode";
  public static final String DEVICE_CODE_USER_ENDPOINT = "/device";
  public static final String TOKEN_ENDPOINT = "/token";
  public static final String USERINFO_ENDPOINT = "/userinfo";
  public static final String INTROSPECTION_ENDPOINT = "/introspect";

  public static final String DEVICE_CODE_CLIENT_ID = "device-code-client";
  public static final String DEVICE_CODE_CLIENT_SECRET = "secret";
  public static final String DEVICE_CODE_GRANT_TYPE =
      "urn:ietf:params:oauth:grant-type:device_code";

  public static final String DEVICE_USER_URL = "http://localhost:8080/device";
  public static final String DEVICE_USER_VERIFY_URL = "http://localhost:8080/device/verify";
  public static final String DEVICE_USER_APPROVE_URL = "http://localhost:8080/device/approve";

  public static final String LOGIN_URL = "/login";
  public static final String TEST_USERNAME = "test";
  public static final String TEST_PASSWORD = "password";

  @Autowired
  ObjectMapper mapper;

  @Autowired
  ClientDetailsEntityService clientService;

  @Before
  public void setup() throws Exception {
    buildMockMvc();
  }


  @Test
  public void testDeviceCodeEndpointRequiresClientAuth() throws Exception {

    mvc
      .perform(post(DEVICE_CODE_ENDPOINT).contentType(APPLICATION_FORM_URLENCODED)
        .param("client_id", "whatever"))
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.error", equalTo("unauthorized")))
      .andExpect(jsonPath("$.error_description", startsWith("Full authentication is required")));

  }

  @Test
  public void testDeviceCodeEndpointRequiresClientWithDeviceCodeGrantEnabled() throws Exception {

    mvc
      .perform(post(DEVICE_CODE_ENDPOINT).contentType(APPLICATION_FORM_URLENCODED)
        .with(httpBasic(DEVICE_CODE_CLIENT_ID, DEVICE_CODE_CLIENT_SECRET))
        .param("client_id", "device-code-client"))
      .andExpect(status().isOk());

    mvc
      .perform(post(DEVICE_CODE_ENDPOINT).contentType(APPLICATION_FORM_URLENCODED)
        .with(httpBasic("client", "secret"))
        .param("client_id", "client"))
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.error", equalTo("invalid_client")))
      .andExpect(jsonPath("$.error_description",
          equalTo("Unauthorized grant type: " + DEVICE_CODE_GRANT_TYPE)));

  }



  @Test
  public void testDeviceCodeNoApproval() throws Exception {

    String response = mvc
      .perform(post(DEVICE_CODE_ENDPOINT).contentType(APPLICATION_FORM_URLENCODED)
        .with(httpBasic(DEVICE_CODE_CLIENT_ID, DEVICE_CODE_CLIENT_SECRET))
        .param("client_id", "device-code-client")
        .param("scope", "openid profile offline_access"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.user_code").isString())
      .andExpect(jsonPath("$.device_code").isString())
      .andExpect(jsonPath("$.verification_uri", equalTo(DEVICE_USER_URL)))
      .andReturn()
      .getResponse()
      .getContentAsString();

    JsonNode responseJson = mapper.readTree(response);

    String userCode = responseJson.get("user_code").asText();
    String deviceCode = responseJson.get("device_code").asText();

    mvc
      .perform(
          post(TOKEN_ENDPOINT).with(httpBasic(DEVICE_CODE_CLIENT_ID, DEVICE_CODE_CLIENT_SECRET))
            .param("grant_type", DEVICE_CODE_GRANT_TYPE)
            .param("device_code", deviceCode))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.error", equalTo("authorization_pending")))
      .andExpect(jsonPath("$.error_description",
          equalTo("Authorization pending for code: " + deviceCode)));

    MockHttpSession session = (MockHttpSession) mvc.perform(get(DEVICE_USER_URL))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("http://localhost:8080/login"))
      .andReturn()
      .getRequest()
      .getSession();

    session = (MockHttpSession) mvc.perform(get("http://localhost:8080/login").session(session))
      .andExpect(status().isOk())
      .andExpect(view().name("iam/login"))
      .andReturn()
      .getRequest()
      .getSession();

    session = (MockHttpSession) mvc
      .perform(post(LOGIN_URL).param("username", TEST_USERNAME)
        .param("password", TEST_PASSWORD)
        .param("submit", "Login")
        .session(session))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl(DEVICE_USER_URL))
      .andReturn()
      .getRequest()
      .getSession();

    session = (MockHttpSession) mvc.perform(get(DEVICE_USER_URL).session(session))
      .andExpect(status().isOk())
      .andExpect(view().name("requestUserCode"))
      .andReturn()
      .getRequest()
      .getSession();

    session = (MockHttpSession) mvc
      .perform(post(DEVICE_USER_VERIFY_URL).param("user_code", userCode).session(session))
      .andExpect(status().isOk())
      .andExpect(view().name("approveDevice"))
      .andReturn()
      .getRequest()
      .getSession();

    session = (MockHttpSession) mvc
      .perform(post(DEVICE_USER_APPROVE_URL).param("user_code", userCode)
        .param("user_oauth_approval", "false")
        .session(session))
      .andExpect(status().isOk())
      .andExpect(view().name("deviceApproved"))
      .andReturn()
      .getRequest()
      .getSession();


    mvc
      .perform(
          post(TOKEN_ENDPOINT).with(httpBasic(DEVICE_CODE_CLIENT_ID, DEVICE_CODE_CLIENT_SECRET))
            .param("grant_type", DEVICE_CODE_GRANT_TYPE)
            .param("device_code", deviceCode))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.error", equalTo("authorization_pending")))
      .andExpect(jsonPath("$.error_description",
          equalTo("Authorization pending for code: " + deviceCode)));
  }


  @Test
  public void testDevideCodeFlowWithAudience() throws Exception {
    String response = mvc
      .perform(post(DEVICE_CODE_ENDPOINT).contentType(APPLICATION_FORM_URLENCODED)
        .with(httpBasic(DEVICE_CODE_CLIENT_ID, DEVICE_CODE_CLIENT_SECRET))
        .param("client_id", "device-code-client")
        .param("scope", "openid profile offline_access"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.user_code").isString())
      .andExpect(jsonPath("$.device_code").isString())
      .andExpect(jsonPath("$.verification_uri", equalTo(DEVICE_USER_URL)))
      .andReturn()
      .getResponse()
      .getContentAsString();

    JsonNode responseJson = mapper.readTree(response);
    String userCode = responseJson.get("user_code").asText();
    String deviceCode = responseJson.get("device_code").asText();

    mvc
      .perform(
          post(TOKEN_ENDPOINT).with(httpBasic(DEVICE_CODE_CLIENT_ID, DEVICE_CODE_CLIENT_SECRET))
            .param("aud", "example-audience")
            .param("grant_type", DEVICE_CODE_GRANT_TYPE)
            .param("device_code", deviceCode))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.error", equalTo("authorization_pending")))
      .andExpect(jsonPath("$.error_description",
          equalTo("Authorization pending for code: " + deviceCode)));

    MockHttpSession session = (MockHttpSession) mvc.perform(get(DEVICE_USER_URL))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("http://localhost:8080/login"))
      .andReturn()
      .getRequest()
      .getSession();

    session = (MockHttpSession) mvc.perform(get("http://localhost:8080/login").session(session))
      .andExpect(status().isOk())
      .andExpect(view().name("iam/login"))
      .andReturn()
      .getRequest()
      .getSession();

    session = (MockHttpSession) mvc
      .perform(post(LOGIN_URL).param("username", TEST_USERNAME)
        .param("password", TEST_PASSWORD)
        .param("submit", "Login")
        .session(session))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl(DEVICE_USER_URL))
      .andReturn()
      .getRequest()
      .getSession();

    session = (MockHttpSession) mvc.perform(get(DEVICE_USER_URL).session(session))
      .andExpect(status().isOk())
      .andExpect(view().name("requestUserCode"))
      .andReturn()
      .getRequest()
      .getSession();

    session = (MockHttpSession) mvc
      .perform(post(DEVICE_USER_VERIFY_URL).param("user_code", userCode).session(session))
      .andExpect(status().isOk())
      .andExpect(view().name("approveDevice"))
      .andReturn()
      .getRequest()
      .getSession();

    session = (MockHttpSession) mvc
      .perform(post(DEVICE_USER_APPROVE_URL).param("user_code", userCode)
        .param("user_oauth_approval", "true")
        .session(session))
      .andExpect(status().isOk())
      .andExpect(view().name("deviceApproved"))
      .andReturn()
      .getRequest()
      .getSession();
    
    String tokenResponse = mvc
        .perform(
            post(TOKEN_ENDPOINT).with(httpBasic(DEVICE_CODE_CLIENT_ID, DEVICE_CODE_CLIENT_SECRET))
              .param("grant_type", DEVICE_CODE_GRANT_TYPE)
              .param("device_code", deviceCode)
              .param("aud", "example-audience"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.access_token").exists())
        .andExpect(jsonPath("$.refresh_token").exists())
        .andExpect(jsonPath("$.id_token").exists())
        .andExpect(jsonPath("$.scope").exists())
        .andExpect(jsonPath("$.scope", containsString("openid")))
        .andExpect(jsonPath("$.scope", containsString("profile")))
        .andExpect(jsonPath("$.scope", containsString("offline_access")))
        .andReturn()
        .getResponse()
        .getContentAsString();

      JsonNode tokenResponseJson = mapper.readTree(tokenResponse);

      String accessToken = tokenResponseJson.get("access_token").asText();
      JWT token = JWTParser.parse(accessToken);
      JWTClaimsSet claims = token.getJWTClaimsSet();

      assertNotNull(claims.getAudience());
      assertThat(claims.getAudience().size(), equalTo(1));
      assertThat(claims.getAudience(), contains("example-audience"));
  }

  @Test
  public void testDeviceCodeApprovalFlowWorks() throws Exception {

    String response = mvc
      .perform(post(DEVICE_CODE_ENDPOINT).contentType(APPLICATION_FORM_URLENCODED)
        .with(httpBasic(DEVICE_CODE_CLIENT_ID, DEVICE_CODE_CLIENT_SECRET))
        .param("client_id", "device-code-client")
        .param("scope", "openid profile offline_access"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.user_code").isString())
      .andExpect(jsonPath("$.device_code").isString())
      .andExpect(jsonPath("$.verification_uri", equalTo(DEVICE_USER_URL)))
      .andReturn()
      .getResponse()
      .getContentAsString();

    JsonNode responseJson = mapper.readTree(response);

    String userCode = responseJson.get("user_code").asText();
    String deviceCode = responseJson.get("device_code").asText();

    mvc
      .perform(
          post(TOKEN_ENDPOINT).with(httpBasic(DEVICE_CODE_CLIENT_ID, DEVICE_CODE_CLIENT_SECRET))
            .param("grant_type", DEVICE_CODE_GRANT_TYPE)
            .param("device_code", deviceCode))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.error", equalTo("authorization_pending")))
      .andExpect(jsonPath("$.error_description",
          equalTo("Authorization pending for code: " + deviceCode)));

    MockHttpSession session = (MockHttpSession) mvc.perform(get(DEVICE_USER_URL))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("http://localhost:8080/login"))
      .andReturn()
      .getRequest()
      .getSession();

    session = (MockHttpSession) mvc.perform(get("http://localhost:8080/login").session(session))
      .andExpect(status().isOk())
      .andExpect(view().name("iam/login"))
      .andReturn()
      .getRequest()
      .getSession();

    session = (MockHttpSession) mvc
      .perform(post(LOGIN_URL).param("username", TEST_USERNAME)
        .param("password", TEST_PASSWORD)
        .param("submit", "Login")
        .session(session))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl(DEVICE_USER_URL))
      .andReturn()
      .getRequest()
      .getSession();

    session = (MockHttpSession) mvc.perform(get(DEVICE_USER_URL).session(session))
      .andExpect(status().isOk())
      .andExpect(view().name("requestUserCode"))
      .andReturn()
      .getRequest()
      .getSession();

    session = (MockHttpSession) mvc
      .perform(post(DEVICE_USER_VERIFY_URL).param("user_code", userCode).session(session))
      .andExpect(status().isOk())
      .andExpect(view().name("approveDevice"))
      .andReturn()
      .getRequest()
      .getSession();

    session = (MockHttpSession) mvc
      .perform(post(DEVICE_USER_APPROVE_URL).param("user_code", userCode)
        .param("user_oauth_approval", "true")
        .session(session))
      .andExpect(status().isOk())
      .andExpect(view().name("deviceApproved"))
      .andReturn()
      .getRequest()
      .getSession();


    String tokenResponse = mvc
      .perform(
          post(TOKEN_ENDPOINT).with(httpBasic(DEVICE_CODE_CLIENT_ID, DEVICE_CODE_CLIENT_SECRET))
            .param("grant_type", DEVICE_CODE_GRANT_TYPE)
            .param("device_code", deviceCode))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.access_token").exists())
      .andExpect(jsonPath("$.refresh_token").exists())
      .andExpect(jsonPath("$.id_token").exists())
      .andExpect(jsonPath("$.scope").exists())
      .andExpect(jsonPath("$.scope", containsString("openid")))
      .andExpect(jsonPath("$.scope", containsString("profile")))
      .andExpect(jsonPath("$.scope", containsString("offline_access")))
      .andReturn()
      .getResponse()
      .getContentAsString();

    JsonNode tokenResponseJson = mapper.readTree(tokenResponse);

    String accessToken = tokenResponseJson.get("access_token").asText();

    String authorizationHeader = String.format("Bearer %s", accessToken);


    // Check that the token can be used for userinfo and introspection
    mvc.perform(get(USERINFO_ENDPOINT).header("Authorization", authorizationHeader))
      .andExpect(status().isOk());

    mvc
      .perform(post(INTROSPECTION_ENDPOINT)
        .with(httpBasic(DEVICE_CODE_CLIENT_ID, DEVICE_CODE_CLIENT_SECRET))
        .param("token", accessToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.active", equalTo(true)));
  }

  @Test
  public void deviceCodeWorksForDynamicallyRegisteredClient()
      throws UnsupportedEncodingException, Exception {

    String jsonInString = ClientJsonStringBuilder.builder()
      .grantTypes("urn:ietf:params:oauth:grant-type:device_code")
      .scopes("openid", "profile", "offline_access")
      .build();

    String clientJson =
        mvc.perform(post(REGISTER_ENDPOINT).contentType(APPLICATION_JSON).content(jsonInString))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.registration_access_token").exists())
          .andExpect(jsonPath("$.registration_client_uri").exists())
          .andReturn()
          .getResponse()
          .getContentAsString();

    ClientDetailsEntity newClient = ClientDetailsEntityJsonProcessor.parse(clientJson);

    assertThat(newClient, notNullValue());

    RequestPostProcessor clientBasicAuth =
        httpBasic(newClient.getClientId(), newClient.getClientSecret());

    String response = mvc
      .perform(post(DEVICE_CODE_ENDPOINT).contentType(APPLICATION_FORM_URLENCODED)
        .with(clientBasicAuth)
        .param("client_id", newClient.getClientId())
        .param("scope", "openid profile offline_access"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.user_code").isString())
      .andExpect(jsonPath("$.device_code").isString())
      .andExpect(jsonPath("$.verification_uri", equalTo(DEVICE_USER_URL)))
      .andExpect(jsonPath("$.expires_in", is(600)))
      .andReturn()
      .getResponse()
      .getContentAsString();

    JsonNode responseJson = mapper.readTree(response);

    String userCode = responseJson.get("user_code").asText();
    String deviceCode = responseJson.get("device_code").asText();

    mvc
      .perform(post(TOKEN_ENDPOINT).with(clientBasicAuth)
        .param("grant_type", DEVICE_CODE_GRANT_TYPE)
        .param("device_code", deviceCode))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.error", equalTo("authorization_pending")))
      .andExpect(jsonPath("$.error_description",
          equalTo("Authorization pending for code: " + deviceCode)));

    MockHttpSession session = (MockHttpSession) mvc.perform(get(DEVICE_USER_URL))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("http://localhost:8080/login"))
      .andReturn()
      .getRequest()
      .getSession();

    session = (MockHttpSession) mvc.perform(get("http://localhost:8080/login").session(session))
      .andExpect(status().isOk())
      .andExpect(view().name("iam/login"))
      .andReturn()
      .getRequest()
      .getSession();

    session = (MockHttpSession) mvc
      .perform(post(LOGIN_URL).param("username", TEST_USERNAME)
        .param("password", TEST_PASSWORD)
        .param("submit", "Login")
        .session(session))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl(DEVICE_USER_URL))
      .andReturn()
      .getRequest()
      .getSession();

    session = (MockHttpSession) mvc.perform(get(DEVICE_USER_URL).session(session))
      .andExpect(status().isOk())
      .andExpect(view().name("requestUserCode"))
      .andReturn()
      .getRequest()
      .getSession();

    session = (MockHttpSession) mvc
      .perform(post(DEVICE_USER_VERIFY_URL).param("user_code", userCode).session(session))
      .andExpect(status().isOk())
      .andExpect(view().name("approveDevice"))
      .andReturn()
      .getRequest()
      .getSession();

    session = (MockHttpSession) mvc
      .perform(post(DEVICE_USER_APPROVE_URL).param("user_code", userCode)
        .param("user_oauth_approval", "true")
        .session(session))
      .andExpect(status().isOk())
      .andExpect(view().name("deviceApproved"))
      .andReturn()
      .getRequest()
      .getSession();


    String tokenResponse = mvc
      .perform(post(TOKEN_ENDPOINT).with(clientBasicAuth)
        .param("grant_type", DEVICE_CODE_GRANT_TYPE)
        .param("device_code", deviceCode))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.access_token").exists())
      .andExpect(jsonPath("$.refresh_token").exists())
      .andExpect(jsonPath("$.id_token").exists())
      .andExpect(jsonPath("$.scope").exists())
      .andExpect(jsonPath("$.scope", containsString("openid")))
      .andExpect(jsonPath("$.scope", containsString("profile")))
      .andExpect(jsonPath("$.scope", containsString("offline_access")))
      .andReturn()
      .getResponse()
      .getContentAsString();

    JsonNode tokenResponseJson = mapper.readTree(tokenResponse);

    String accessToken = tokenResponseJson.get("access_token").asText();

    String authorizationHeader = String.format("Bearer %s", accessToken);

    // Check that the token can be used for userinfo and introspection
    mvc.perform(get(USERINFO_ENDPOINT).header("Authorization", authorizationHeader))
      .andExpect(status().isOk());

    mvc
      .perform(post(INTROSPECTION_ENDPOINT)
        .with(httpBasic(DEVICE_CODE_CLIENT_ID, DEVICE_CODE_CLIENT_SECRET))
        .param("token", accessToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.active", equalTo(true)));
  }
}
