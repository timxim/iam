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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import it.infn.mw.iam.IamLoginService;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class})
@WebAppConfiguration
@Transactional
public class IntrospectionEndpointAuthenticationTests extends EndpointsTestUtils {

  private static final String ENDPOINT = "/introspect";

  @Autowired
  private WebApplicationContext context;

  private String accessToken;

  @Before
  public void setup() throws Exception {
    mvc = MockMvcBuilders.webAppContextSetup(context)
      .apply(springSecurity())
      .alwaysDo(log())
      .build();

    accessToken = getPasswordAccessToken("openid profile offline_access");
  }


  @Test
  public void testTokenIntrospectionEndpointBasicAuthentication() throws Exception {
    // @formatter:off
    mvc.perform(post(ENDPOINT)
        .with(httpBasic("password-grant", "secret"))
        .param("token", accessToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.active", equalTo(true)));
    // @formatter:on
  }

  @Test
  public void testTokenIntrospectionEndpointFormAuthentication() throws Exception {
    // @formatter:off
    mvc.perform(post(ENDPOINT)
        .param("token", accessToken)
        .param("client_id", "client-cred")
        .param("client_secret", "secret"))
      .andExpect(status().isUnauthorized());
    // @formatter:on
  }

  @Test
  public void testTokenIntrospectionEndpointNoAuthenticationFailure() throws Exception {
    // @formatter:off
    mvc.perform(post(ENDPOINT)
        .param("token", accessToken))
      .andExpect(status().isUnauthorized());
   // @formatter:on
  }
}
