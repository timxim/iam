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
package it.infn.mw.iam.test.rcauth;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

import java.text.ParseException;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.authn.oidc.RestTemplateFactory;
import it.infn.mw.iam.authn.oidc.model.TokenEndpointErrorResponse;
import it.infn.mw.iam.rcauth.RCAuthError;
import it.infn.mw.iam.rcauth.RCAuthTokenRequestor;
import it.infn.mw.iam.rcauth.RCAuthTokenResponse;
import it.infn.mw.iam.test.util.oidc.IdTokenBuilder;
import it.infn.mw.iam.test.util.oidc.MockRestTemplateFactory;
import it.infn.mw.iam.test.util.oidc.TokenResponse;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(
    classes = {IamLoginService.class, RCAuthTestSupport.class, RCAuthTokenRequestorTests.class})
@WebAppConfiguration
@Transactional
@TestPropertySource(
    properties = {"rcauth.enabled=true", "rcauth.client-id=" + RCAuthTestSupport.CLIENT_ID,
        "rcauth.client-secret=" + RCAuthTestSupport.CLIENT_SECRET,
        "rcauth.issuer=" + RCAuthTestSupport.ISSUER})
public class RCAuthTokenRequestorTests extends RCAuthTestSupport {

  @Bean
  @Primary
  public RestTemplateFactory mockRestTemplateFactory() {
    return new MockRestTemplateFactory();
  }

  @Autowired
  RCAuthTokenRequestor tokenRequestor;

  @Autowired
  RestTemplateFactory rtf;

  @Autowired
  ObjectMapper mapper;

  MockRestTemplateFactory mockRtf;

  @Before
  public void setup() {
    mockRtf = (MockRestTemplateFactory) rtf;
    mockRtf.resetTemplate();
  }

  @Test
  public void testGetAccessTokenSuccess()
      throws JsonProcessingException, JOSEException, ParseException {

    prepareTokenResponse(NONCE);

    RCAuthTokenResponse response = tokenRequestor.getAccessToken(RANDOM_AUTHZ_CODE);

    verifyMockServerCalls();
    String subjectDnClaim = (String) SignedJWT.parse(response.getIdToken())
      .getJWTClaimsSet()
      .getClaim(CERT_SUBJECT_DN_CLAIM);

    assertThat(subjectDnClaim, is(DN));
  }


  @Test(expected = RCAuthError.class)
  public void testGetAccessTokenError() throws JsonProcessingException {
    prepareErrorRespose();
    try {
      tokenRequestor.getAccessToken(RANDOM_AUTHZ_CODE);
    } catch (RCAuthError e) {
      Assert.assertThat(e.getMessage(), containsString("Token request error: invalid_request"));
      throw e;
    } finally {
      verifyMockServerCalls();
    }

  }

  @Test(expected = RCAuthError.class)
  public void testGetAccessTokenBogusError() throws JsonProcessingException {
    prepareBogusErrorRespose();
    try {
      tokenRequestor.getAccessToken(RANDOM_AUTHZ_CODE);
    } catch (RCAuthError e) {
      Assert.assertThat(e.getMessage(), containsString("Token request error:"));
      throw e;
    } finally {
      verifyMockServerCalls();
    }
  }
  
  @Test(expected = RCAuthError.class)
  public void testGetAccessTokenInternalServerError() throws JsonProcessingException {
    prepareInternalServerErrorResponse();
    try {
      tokenRequestor.getAccessToken(RANDOM_AUTHZ_CODE);
    } catch (RCAuthError e) {
      Assert.assertThat(e.getMessage(), containsString("Token request error:"));
      throw e;
    } finally {
      verifyMockServerCalls();
    }
  }
  private void prepareInternalServerErrorResponse() {
    mockRtf.getMockServer()
    .expect(requestTo(TOKEN_URI))
    .andExpect(method(HttpMethod.POST))
    .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
    .andRespond(MockRestResponseCreators.withServerError()
      .body("internal server error"));
  }
  
  private void prepareBogusErrorRespose() {

    mockRtf.getMockServer()
      .expect(requestTo(TOKEN_URI))
      .andExpect(method(HttpMethod.POST))
      .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
      .andRespond(MockRestResponseCreators.withBadRequest()
        .body("64372tfgd")
        .contentType(MediaType.APPLICATION_JSON));
  }

  void prepareErrorRespose() throws JsonProcessingException {
    TokenEndpointErrorResponse response = new TokenEndpointErrorResponse();
    response.setError("invalid_request");
    response.setErrorDescription("I do not like you");

    mockRtf.getMockServer()
      .expect(requestTo(TOKEN_URI))
      .andExpect(method(HttpMethod.POST))
      .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
      .andRespond(MockRestResponseCreators.withBadRequest()
        .body(mapper.writeValueAsString(response))
        .contentType(MediaType.APPLICATION_JSON));
  }

  void prepareTokenResponse(String nonce) throws JsonProcessingException, JOSEException {
    IdTokenBuilder builder = new IdTokenBuilder(rcAuthKeyStore, jwsAlgo);

    String idToken = builder.sub(SUB).issuer(ISSUER).customClaim(CERT_SUBJECT_DN_CLAIM, DN).build();

    TokenResponse tr = new TokenResponse();
    tr.setAccessToken(UUID.randomUUID().toString());
    tr.setExpiresIn(3600);
    tr.setIdToken(idToken);

    mockRtf.getMockServer()
      .expect(requestTo(TOKEN_URI))
      .andExpect(method(HttpMethod.POST))
      .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
      .andRespond(MockRestResponseCreators.withSuccess(mapper.writeValueAsString(tr),
          MediaType.APPLICATION_JSON));
  }

  void verifyMockServerCalls() {
    mockRtf.getMockServer().verify();
    mockRtf.resetTemplate();
  }

}
