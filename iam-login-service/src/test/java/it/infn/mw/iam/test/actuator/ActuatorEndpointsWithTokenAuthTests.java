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
package it.infn.mw.iam.test.actuator;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.google.common.collect.Sets;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.util.WithMockOAuthUser;
import it.infn.mw.iam.test.util.oauth.MockOAuth2Filter;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class, CoreControllerTestSupport.class})
@WebAppConfiguration
public class ActuatorEndpointsWithTokenAuthTests {

  private static final String ADMIN_USERNAME = "admin";
  private static final String ADMIN_AUTORITY = "ROLE_ADMIN";

  private static final String STATUS_UP = "UP";

  private static final Set<String> SENSITIVE_ENDPOINTS = Sets.newHashSet("/metrics");

  private static final Set<String> PRIVILEGED_ENDPOINTS = Sets.newHashSet("/configprops", "/env",
      "/mappings", "/flyway", "/autoconfig", "/beans", "/dump", "/trace");

  @Value("${spring.mail.host}")
  private String mailHost;

  @Value("${spring.mail.port}")
  private Integer mailPort;

  @Autowired
  private WebApplicationContext context;

  private MockMvc mvc;

  @Autowired
  private MockOAuth2Filter mockOAuth2Filter;

  @Before
  public void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context)
      .apply(springSecurity())
      .alwaysDo(log())
      .build();
  }

  @After
  public void cleanupOAuthUser() {
    mockOAuth2Filter.cleanupSecurityContext();
  }

  @Test
  @WithMockOAuthUser(clientId = "client-cred", scopes = {"read-tasks", "write-tasks"})
  public void testHealthEndpointWithToken() throws Exception {
    // @formatter:off
    mvc.perform(get("/health"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status", equalTo(STATUS_UP)))
      .andExpect(jsonPath("$.db").doesNotExist())
      .andExpect(jsonPath("$.diskSpace").doesNotExist())
      ;
    // @formatter:on
  }

  @Test
  @WithMockOAuthUser(clientId = "client-cred",
      scopes = {"openid", "profile", "read-tasks", "write-tasks"}, user = ADMIN_USERNAME,
      authorities = {ADMIN_AUTORITY})
  public void testHealthEndpointWithTokenAsAdmin() throws Exception {
    // @formatter:off
    mvc.perform(get("/health"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status", equalTo(STATUS_UP)))
      .andExpect(jsonPath("$.diskSpace.status", equalTo(STATUS_UP)))
      .andExpect(jsonPath("$.db.status", equalTo(STATUS_UP)))
      .andExpect(jsonPath("$.mail").doesNotExist())
      ;
    // @formatter:on
  }

  @Test
  @WithMockOAuthUser(clientId = "client-cred",
      scopes = {"openid", "profile", "read-tasks", "write-tasks"})
  public void testSensitiveEndpointWithTokenAsUser() throws Exception {
    for (String endpoint : SENSITIVE_ENDPOINTS) {
      // @formatter:off
      mvc.perform(get(endpoint))
        .andExpect(status().isForbidden())
        ;
      // @formatter:on
    }
  }

  @Test
  @WithMockOAuthUser(clientId = "client-cred",
      scopes = {"openid", "profile", "read-tasks", "write-tasks"}, user = ADMIN_USERNAME,
      authorities = {ADMIN_AUTORITY})
  public void testSensitiveEndpointWithTokenAsAdmin() throws Exception {
    for (String endpoint : SENSITIVE_ENDPOINTS) {
      // @formatter:off
      mvc.perform(get(endpoint))
        .andExpect(status().isOk())
        ;
      // @formatter:on
    }
  }

  @Test
  @WithMockOAuthUser(clientId = "client-cred",
      scopes = {"openid", "profile", "read-tasks", "write-tasks"})
  public void testPrivilegedEndpointWithTokenAsUser() throws Exception {
    for (String endpoint : PRIVILEGED_ENDPOINTS) {
      // @formatter:off
      mvc.perform(get(endpoint))
        .andExpect(status().isForbidden())
        ;
      // @formatter:on
    }
  }

  @Test
  @WithMockOAuthUser(clientId = "client-cred",
      scopes = {"openid", "profile", "read-tasks", "write-tasks"}, user = ADMIN_USERNAME,
      authorities = {ADMIN_AUTORITY})
  public void testPrivilegedEndpointWithTokenAsAdmin() throws Exception {
    for (String endpoint : PRIVILEGED_ENDPOINTS) {
      // @formatter:off
      mvc.perform(get(endpoint))
        .andExpect(status().isForbidden())
        ;
      // @formatter:on
    }
  }
}
