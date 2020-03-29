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

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import it.infn.mw.iam.IamLoginService;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class})
@WebAppConfiguration
@Transactional
public class IdTokenEnhancerTests {

  private static final String CLIENT_ID = "password-grant";
  private static final String CLIENT_SECRET = "secret";
  private static final String GRANT_TYPE = "password";
  private static final String USERNAME = "test";
  private static final String PASSWORD = "password";

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private ObjectMapper mapper;

  private MockMvc mvc;

  @Before
  public void setup() throws Exception {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).alwaysDo(log())
        .build();
  }

  private String getIdToken(String scopes) throws Exception {

    // @formatter:off
    String response = mvc.perform(post("/token")
        .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
        .param("grant_type", GRANT_TYPE)
        .param("username", USERNAME)
        .param("password", PASSWORD)
        .param("scope", scopes))
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();
    // @formatter:on

    DefaultOAuth2AccessToken tokenResponse =
        mapper.readValue(response, DefaultOAuth2AccessToken.class);

    return tokenResponse.getAdditionalInformation().get("id_token").toString();
  }

  @Test
  public void testEnhancedEmailOk() throws Exception {

    JWT token = JWTParser.parse(getIdToken("openid email"));
    System.out.println(token.getJWTClaimsSet());
    assertThat(token.getJWTClaimsSet().getClaim("email"), is(notNullValue()));
  }

  @Test
  public void testEnhancedProfileClaimsOk() throws Exception {

    JWT token = JWTParser.parse(getIdToken("openid profile"));
    System.out.println(token.getJWTClaimsSet());
    
    assertThat(token.getJWTClaimsSet().getClaim("name"), is(notNullValue()));
    assertThat(token.getJWTClaimsSet().getClaim("preferred_username"), is(notNullValue()));
    assertThat(token.getJWTClaimsSet().getClaim("organisation_name"), is(notNullValue()));
    assertThat(token.getJWTClaimsSet().getClaim("groups"), is(notNullValue()));
    
  }

  @Test
  public void testEnhancedEmailNotEnhanced() throws Exception {

    JWT token = JWTParser.parse(getIdToken("openid"));

    System.out.println(token.getJWTClaimsSet());
    assertThat(token.getJWTClaimsSet().getClaim("email"), is(nullValue()));
  }

  @Test
  public void testEnhancedProfileClaimsNotEnhanced() throws Exception {

    JWT token = JWTParser.parse(getIdToken("openid"));
    System.out.println(token.getJWTClaimsSet());
    assertThat(token.getJWTClaimsSet().getClaim("name"), is(nullValue()));
    assertThat(token.getJWTClaimsSet().getClaim("preferred_username"), is(nullValue()));
    assertThat(token.getJWTClaimsSet().getClaim("organisation_name"), is(nullValue()));
    assertThat(token.getJWTClaimsSet().getClaim("groups"), is(nullValue()));
  }
}
