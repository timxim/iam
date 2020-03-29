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
package it.infn.mw.iam.test.oauth.scope;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.google.common.collect.Sets;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.persistence.model.IamAccount;
import it.infn.mw.iam.persistence.model.IamScopePolicy;
import it.infn.mw.iam.persistence.model.IamScopePolicy.Rule;
import it.infn.mw.iam.persistence.repository.IamAccountRepository;
import it.infn.mw.iam.persistence.repository.IamScopePolicyRepository;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.repository.ScopePolicyTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class, CoreControllerTestSupport.class})
@WebAppConfiguration
@Transactional
public class ScopePolicyFilteringIntegrationTests extends ScopePolicyTestUtils {

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private IamAccountRepository accountRepo;

  @Autowired
  private IamScopePolicyRepository scopePolicyRepo;

  private MockMvc mvc;

  @Before
  public void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context)
      .alwaysDo(log())
      .apply(springSecurity())
      .build();
  }

  IamAccount findTestAccount() {
    return accountRepo.findByUsername("test")
      .orElseThrow(() -> new AssertionError("Expected test account not found!"));
  }

  @Test
  public void passwordFlowScopeFilteringByAccountWorks() throws Exception {

    IamAccount testAccount = findTestAccount();

    IamScopePolicy up = initDenyScopePolicy();
    up.setAccount(testAccount);
    up.setRule(Rule.DENY);
    up.setScopes(Sets.newHashSet(SCIM_READ));

    scopePolicyRepo.save(up);

    String clientId = "password-grant";
    String clientSecret = "secret";

    mvc
      .perform(post("/token").with(httpBasic(clientId, clientSecret))
        .param("grant_type", "password")
        .param("username", "test")
        .param("password", "password")
        .param("scope", "openid profile scim:read"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.scope", equalTo("openid profile")));
  }

  @Test
  public void passwordFlowDenyAllScopesWorks() throws Exception {

    IamAccount testAccount = findTestAccount();

    IamScopePolicy up = initDenyScopePolicy();
    up.setAccount(testAccount);
    up.setRule(Rule.DENY);

    scopePolicyRepo.save(up);

    String clientId = "password-grant";
    String clientSecret = "secret";

    mvc
      .perform(post("/token").with(httpBasic(clientId, clientSecret))
        .param("grant_type", "password")
        .param("username", "test")
        .param("password", "password")
        .param("scope", "openid profile scim:read"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.scope").doesNotExist())
      .andExpect(jsonPath("$.id_token").exists());
  }

  @Test
  public void authzCodeFlowScopeFilteringByAccountWorks() throws Exception {

    IamAccount testAccount = findTestAccount();

    IamScopePolicy up = initDenyScopePolicy();
    up.setAccount(testAccount);
    up.setRule(Rule.DENY);
    up.setScopes(Sets.newHashSet("read-tasks"));

    scopePolicyRepo.save(up);

    String clientId = "client";

    MockHttpSession session = (MockHttpSession) mvc
      .perform(get("/authorize").param("scope", "openid profile read-tasks")
        .param("response_type", "code")
        .param("client_id", clientId)
        .param("redirect_uri", "https://iam.local.io/iam-test-client/openid_connect_login")
        .param("state", "1234567"))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("http://localhost/login"))
      .andReturn()
      .getRequest()
      .getSession();

    session = (MockHttpSession) mvc
      .perform(post("/login").param("username", "test")
        .param("password", "password")
        .param("submit", "Login").session(session))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("http://localhost/authorize"))
      .andReturn()
      .getRequest()
      .getSession();
    
    session = (MockHttpSession) mvc
        .perform(get("/authorize").session(session))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl( "/oauth/confirm_access"))
        .andExpect(model().attribute("scope", equalTo("openid profile")))
        .andReturn()
        .getRequest()
        .getSession();



  }

}
