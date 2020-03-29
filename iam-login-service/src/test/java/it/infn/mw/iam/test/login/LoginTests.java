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
package it.infn.mw.iam.test.login;


import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Instant;
import java.util.Date;

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

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.persistence.model.IamAccount;
import it.infn.mw.iam.persistence.model.IamAup;
import it.infn.mw.iam.persistence.repository.IamAccountRepository;
import it.infn.mw.iam.persistence.repository.IamAupRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class})
@WebAppConfiguration
@Transactional
public class LoginTests {

  public static final String LOGIN_URL = "/login";
  public static final String ADMIN_USERNAME = "admin";
  public static final String ADMIN_PASSWORD = "password";

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private IamAccountRepository accountRepo;

  @Autowired
  private IamAupRepository aupRepo;

  private MockMvc mvc;

  @Before
  public void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context)
      .apply(springSecurity())
      .alwaysDo(log())
      .build();
  }

  @Test
  public void loginForAdminUserWorks() throws Exception {

    Instant now = Instant.now();

    //@formatter:off
    MockHttpSession session = (MockHttpSession) mvc
      .perform(
          post(LOGIN_URL)
            .param("username", ADMIN_USERNAME)
            .param("password", ADMIN_PASSWORD)
            .param("submit", "Login"))
      .andExpect(status().isFound())
      .andExpect(redirectedUrl("/dashboard"))
      .andReturn()
      .getRequest()
      .getSession();
    //@formatter:on

    mvc.perform(get("/dashboard").session(session))
      .andExpect(status().isOk())
      .andExpect(view().name("iam/dashboard"))
      .andReturn();

    IamAccount adminAccount = accountRepo.findByUsername(ADMIN_USERNAME)
      .orElseThrow(() -> new AssertionError("Admin user not found!"));

    assertNotNull(adminAccount.getLastLoginTime());
    assertThat(adminAccount.getLastLoginTime().toInstant(), greaterThan(now));
  }

  @Test
  public void loginWithInvalidCredentialsIsBlocked() throws Exception {
    //@formatter:off
    mvc.perform(post(LOGIN_URL)
        .param("username", ADMIN_USERNAME)
        .param("password", "whatever")
        .param("submit", "Login"))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/login?error=failure"));

    mvc.perform(post(LOGIN_URL)
        .param("username", "whatever")
        .param("password", "whatever")
        .param("submit", "Login"))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/login?error=failure"));
    //@formatter:on
  }

  @Test
  public void loginRedirectsToSignAupPageWhenNeeded() throws Exception {
    IamAup aup = new IamAup();

    aup.setCreationTime(new Date());
    aup.setLastUpdateTime(new Date());
    aup.setName("default-aup");
    aup.setText("AUP text");
    aup.setDescription("AUP description");
    aup.setSignatureValidityInDays(0L);

    aupRepo.save(aup);

    mvc
      .perform(
          post(LOGIN_URL).param("username", ADMIN_USERNAME).param("password", ADMIN_PASSWORD).param(
              "submit", "Login"))
      .andExpect(status().isFound())
      .andExpect(redirectedUrl("/iam/aup/sign"));

  }
}
