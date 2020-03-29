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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mitre.oauth2.model.OAuth2AccessTokenEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.core.IamTokenService;
import it.infn.mw.iam.persistence.repository.IamAccountRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = IamLoginService.class)
@WebAppConfiguration
@Transactional
public class RevocationEndpointTests extends EndpointsTestUtils {

  public static final String REVOKE_ENDPOINT = "/revoke";

  public static final String PASSWORD_GRANT_CLIENT_ID = "password-grant";
  public static final String PASSWORD_GRANT_CLIENT_SECRET = "secret";

  @Autowired
  IamTokenService iamTokenService;

  @Autowired
  IamAccountRepository accountRepo;

  @Before
  public void setup() throws Exception {
    buildMockMvc();
  }
  
  
  @Test
  public void testRevocationEnpointRequiresClientAuth() throws Exception {
    mvc
    .perform(post(REVOKE_ENDPOINT)
      .contentType(APPLICATION_FORM_URLENCODED)
      .param("token", "whatever"))
    .andExpect(status().isUnauthorized());
  }

  @Test
  public void testRevokeInvalidTokenReturns200() throws Exception {
    mvc
      .perform(post(REVOKE_ENDPOINT)
        .with(httpBasic(PASSWORD_GRANT_CLIENT_ID, PASSWORD_GRANT_CLIENT_SECRET))
        .contentType(APPLICATION_FORM_URLENCODED)
        .param("token", "whatever"))
      .andExpect(status().isOk());
  }

  @Test
  public void accessTokenRevocationWorks() throws Exception {

    Set<OAuth2AccessTokenEntity> accessTokens = iamTokenService.getAllAccessTokensForUser("test");

    // Start clean
    accessTokens.forEach(iamTokenService::revokeAccessToken);

    String accessToken = getPasswordAccessToken();

    accessTokens = iamTokenService.getAllAccessTokensForUser("test");

    assertThat(accessTokens, hasSize(1)); // access token

    mvc
      .perform(post(REVOKE_ENDPOINT)
        .with(httpBasic(PASSWORD_GRANT_CLIENT_ID, PASSWORD_GRANT_CLIENT_SECRET))
        .contentType(APPLICATION_FORM_URLENCODED)
        .param("token", accessToken))
      .andExpect(status().isOk());

    accessTokens = iamTokenService.getAllAccessTokensForUser("test");

    assertThat(accessTokens, hasSize(0)); // revoking the access token revokes the linked id token

  }

  @Test
  public void accessTokenRevocationRevokesTheRightToken() throws Exception {
    Set<OAuth2AccessTokenEntity> accessTokens = iamTokenService.getAllAccessTokensForUser("test");

    // Start clean
    accessTokens.forEach(iamTokenService::revokeAccessToken);

    String tokenOne = getPasswordAccessToken();
    String tokenTwo = getPasswordAccessToken();

    accessTokens = iamTokenService.getAllAccessTokensForUser("test");
    assertThat(accessTokens, hasSize(2));

    mvc
      .perform(post(REVOKE_ENDPOINT)
        .with(httpBasic(PASSWORD_GRANT_CLIENT_ID, PASSWORD_GRANT_CLIENT_SECRET))
        .contentType(APPLICATION_FORM_URLENCODED)
        .param("token", tokenOne))
      .andExpect(status().isOk());

    accessTokens = iamTokenService.getAllAccessTokensForUser("test");
    assertThat(accessTokens, hasSize(1));
    accessTokens.stream().filter(t -> t.getValue().equals(tokenTwo)).findAny().orElseThrow(
        () -> new AssertionError("Expected access token not found"));
  }

  @Test
  public void refreshTokenRevocationWorks() throws Exception {

    // Start clean
    iamTokenService.getAllAccessTokensForUser("test").forEach(iamTokenService::revokeAccessToken);
    iamTokenService.getAllRefreshTokensForUser("test").forEach(iamTokenService::revokeRefreshToken);

    AccessTokenGetter tg = buildAccessTokenGetter();
    tg.scope("openid profile offline_access");
    DefaultOAuth2AccessToken tokenResponseObject = tg.getTokenResponseObject();
    String refreshTokenValue = tokenResponseObject.getRefreshToken().getValue();

    assertThat(iamTokenService.getAllRefreshTokensForUser("test"), hasSize(1));

    mvc
      .perform(post(REVOKE_ENDPOINT)
        .with(httpBasic(PASSWORD_GRANT_CLIENT_ID, PASSWORD_GRANT_CLIENT_SECRET))
        .contentType(APPLICATION_FORM_URLENCODED)
        .param("token", refreshTokenValue))
      .andExpect(status().isOk());

    assertThat(iamTokenService.getAllRefreshTokensForUser("test"), hasSize(0));
  }

  @Test
  public void refreshTokenRevocationRevokesTheRightToken() throws Exception {
    // Start clean
    iamTokenService.getAllAccessTokensForUser("test").forEach(iamTokenService::revokeAccessToken);
    iamTokenService.getAllRefreshTokensForUser("test").forEach(iamTokenService::revokeRefreshToken);

    AccessTokenGetter tg = buildAccessTokenGetter();
    tg.scope("openid profile offline_access");

    String rt1 = tg.getTokenResponseObject().getRefreshToken().getValue();
    String rt2 = tg.getTokenResponseObject().getRefreshToken().getValue();

    assertThat(iamTokenService.getAllRefreshTokensForUser("test"), hasSize(2));

    mvc
      .perform(post(REVOKE_ENDPOINT)
        .with(httpBasic(PASSWORD_GRANT_CLIENT_ID, PASSWORD_GRANT_CLIENT_SECRET))
        .contentType(APPLICATION_FORM_URLENCODED)
        .param("token", rt1))
      .andExpect(status().isOk());


    assertThat(iamTokenService.getAllRefreshTokensForUser("test"), hasSize(1));
    
    iamTokenService.getAllRefreshTokensForUser("test")
      .stream()
      .filter(t -> t.getValue().equals(rt2))
      .findAny()
      .orElseThrow(() -> new AssertionError("Expected refresh token not found"));
  }
}
