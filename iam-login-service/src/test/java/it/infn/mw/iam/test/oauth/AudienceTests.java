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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;

import it.infn.mw.iam.IamLoginService;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = IamLoginService.class)
@WebAppConfiguration
@Transactional
public class AudienceTests {

  public static final String TEST_USERNAME = "test";
  public static final String TEST_PASSWORD = "password";

  public static final String PASSWORD_GRANT_CLIENT_ID = "password-grant";
  public static final String PASSWORD_GRANT_CLIENT_SECRET = "secret";
  
  public static final String CLIENT_CRED_GRANT_CLIENT_ID = "client-cred";
  public static final String CLIENT_CRED_GRANT_CLIENT_SECRET = "secret";

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private ObjectMapper mapper;

  private MockMvc mvc;

  @Before
  public void before() {
    mvc = MockMvcBuilders.webAppContextSetup(context)
        .alwaysDo(log())
        .apply(springSecurity())
        .build();
  }
  @Test
  public void testAudienceRequestPasswordFlow() throws Exception {

    String tokenResponseJson = mvc
      .perform(post("/token").param("grant_type", "password")
        .param("client_id", PASSWORD_GRANT_CLIENT_ID)
        .param("client_secret", PASSWORD_GRANT_CLIENT_SECRET)
        .param("username", TEST_USERNAME)
        .param("password", TEST_PASSWORD)
        .param("scope", "openid profile")
        .param("audience", "example-audience"))
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();
    
    String accessToken = mapper.readTree(tokenResponseJson).get("access_token").asText();

    JWT token = JWTParser.parse(accessToken);

    JWTClaimsSet claims = token.getJWTClaimsSet();

    assertNotNull(claims.getAudience());
    assertThat(claims.getAudience().size(), equalTo(1));
    assertThat(claims.getAudience(), contains("example-audience"));
  }

  @Test
  public void testAudienceRequestClientCredentialsFlow() throws Exception {
    
    String tokenResponseJson = mvc
        .perform(post("/token").param("grant_type", "client_credentials")
          .param("client_id", CLIENT_CRED_GRANT_CLIENT_ID)
          .param("client_secret", CLIENT_CRED_GRANT_CLIENT_SECRET)
          .param("audience", "example-audience"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    
    String accessToken = mapper.readTree(tokenResponseJson).get("access_token").asText();
    JWT token = JWTParser.parse(accessToken);

    JWTClaimsSet claims = token.getJWTClaimsSet();

    assertNotNull(claims.getAudience());
    assertThat(claims.getAudience().size(), equalTo(1));
    assertThat(claims.getAudience(), contains("example-audience"));
  }

}
