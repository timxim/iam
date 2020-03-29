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

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

import it.infn.mw.iam.IamLoginService;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = IamLoginService.class)
@WebAppConfiguration
@Transactional
public class TokenEndpointClientAuthenticationTests {

  private static final String TOKEN_ENDPOINT = "/token";
  private static final String GRANT_TYPE = "client_credentials";
  private static final String SCOPE = "read-tasks";

  @Autowired
  private WebApplicationContext context;

  private MockMvc mvc;

  @Before
  public void setup() throws Exception {
    mvc = MockMvcBuilders.webAppContextSetup(context)
      .apply(springSecurity())
      .alwaysDo(log())
      .build();
  }


  @Test
  public void testTokenEndpointFormClientAuthentication() throws Exception {

    String clientId = "post-client";
    String clientSecret = "secret";

    // @formatter:off
    mvc.perform(post(TOKEN_ENDPOINT)
        .param("grant_type", GRANT_TYPE)
        .param("client_id", clientId)
        .param("client_secret", clientSecret)
        .param("scope", SCOPE))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.scope", equalTo(SCOPE)));
    // @formatter:on
  }

  @Test
  public void testTokenEndpointFormClientAuthenticationInvalidCredentials() throws Exception {

    String clientId = "post-client";
    String clientSecret = "wrong-password";

    // @formatter:off
    mvc.perform(post(TOKEN_ENDPOINT)
        .param("grant_type", GRANT_TYPE)
        .param("client_id", clientId)
        .param("client_secret", clientSecret)
        .param("scope", SCOPE))
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.error", equalTo("invalid_client")))
      .andExpect(jsonPath("$.error_description", equalTo("Bad client credentials")));
    // @formatter:on
  }

  @Test
  public void testTokenEndpointFormClientAuthenticationUnknownClient() throws Exception {

    String clientId = "unknown-client";
    String clientSecret = "password";

    // @formatter:off
    mvc.perform(post(TOKEN_ENDPOINT)
        .param("grant_type", GRANT_TYPE)
        .param("client_id", clientId)
        .param("client_secret", clientSecret)
        .param("scope", SCOPE))
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.error", equalTo("invalid_client")))
      .andExpect(jsonPath("$.error_description", equalTo("Bad client credentials")));
    // @formatter:on
  }

  @Test
  public void testTokenEndpointBasicClientAuthentication() throws Exception {

    String clientId = "post-client";
    String clientSecret = "secret";

    // @formatter:off
    mvc.perform(post(TOKEN_ENDPOINT)
        .with(httpBasic(clientId, clientSecret))
        .param("grant_type", GRANT_TYPE)
        .param("scope", SCOPE))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.scope", equalTo(SCOPE)));
    // @formatter:on
  }
  
  @Test
  public void testTokenEndpointOptionsMethodAllowed() throws Exception {
    mvc.perform(options(TOKEN_ENDPOINT))
      .andExpect(status().isOk());
  }
}
