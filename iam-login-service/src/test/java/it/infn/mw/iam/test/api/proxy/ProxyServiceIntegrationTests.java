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
package it.infn.mw.iam.test.api.proxy;

import static it.infn.mw.iam.api.proxy.ProxyCertificatesApiController.PROXY_API_PATH;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Date;
import java.time.Clock;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.config.IamProperties;
import it.infn.mw.iam.persistence.model.IamAccount;
import it.infn.mw.iam.persistence.model.IamX509Certificate;
import it.infn.mw.iam.persistence.model.IamX509ProxyCertificate;
import it.infn.mw.iam.persistence.repository.IamAccountRepository;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.rcauth.RCAuthTestSupport;
import it.infn.mw.iam.test.util.WithAnonymousUser;
import it.infn.mw.iam.test.util.WithMockOAuthUser;
import it.infn.mw.iam.test.util.oauth.MockOAuth2Filter;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class, RCAuthTestSupport.class,
    CoreControllerTestSupport.class, ProxyServiceIntegrationTests.class})
@WebAppConfiguration
@Transactional
@TestPropertySource(
    properties = {"rcauth.enabled=true", "rcauth.client-id=" + RCAuthTestSupport.CLIENT_ID,
        "rcauth.client-secret=" + RCAuthTestSupport.CLIENT_SECRET,
        "rcauth.issuer=" + RCAuthTestSupport.ISSUER})
public class ProxyServiceIntegrationTests extends ProxyCertificateTestSupport {

  @Autowired
  private WebApplicationContext context;

  @Autowired
  IamProperties iamProperties;

  @Autowired
  private MockOAuth2Filter mockOAuth2Filter;

  @Autowired
  private IamAccountRepository accountRepo;


  private MockMvc mvc;

  @Bean
  @Primary
  public Clock clock() {
    return clock;
  }

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

  private void linkProxyToTestAccount() throws Exception {
    IamAccount testAccount = accountRepo.findByUsername("test")
      .orElseThrow(() -> new AssertionError("Expected test account not found"));

    linkTest0CertificateToAccount(testAccount);
    IamX509Certificate cert = testAccount.getX509Certificates().iterator().next();
    IamX509ProxyCertificate proxyCert = new IamX509ProxyCertificate();

    proxyCert.setChain(generateTest0Proxy(NOW, ONE_YEAR_FROM_NOW));
    proxyCert.setExpirationTime(Date.from(ONE_YEAR_FROM_NOW));
    proxyCert.setCertificate(cert);
    cert.setProxy(proxyCert);

    accountRepo.save(testAccount);
  }

  @WithAnonymousUser
  @Test
  public void proxyApiRequiresAuthentication() throws Exception {
    mvc.perform(post(PROXY_API_PATH)).andExpect(status().isUnauthorized());
  }

  @WithMockUser
  @Test
  public void proxyApiRequiresClientAuthentication() throws Exception {
    mvc.perform(post(PROXY_API_PATH)).andExpect(status().isUnauthorized());
  }

  @WithMockOAuthUser(user = "test", authorities = {"ROLE_USER", "ROLE_CLIENT"})
  @Test
  public void proxyApiRequiresProxyGenScope() throws Exception {
    mvc.perform(post(PROXY_API_PATH))
      .andExpect(status().isForbidden())
      .andExpect(jsonPath("$.error").value("insufficient_scope"));
  }

  @WithMockOAuthUser(user = "test", authorities = {"ROLE_USER", "ROLE_CLIENT"},
      scopes = {"proxy:generate"})
  @Test
  public void proxyApiRequiresRegisteredProxy() throws Exception {
    mvc.perform(post(PROXY_API_PATH))
      .andExpect(status().isPreconditionFailed())
      .andExpect(jsonPath("$.error", startsWith("No proxy found")));
  }

  @WithMockOAuthUser(user = "test", authorities = {"ROLE_USER", "ROLE_CLIENT"},
      scopes = {"proxy:generate"})
  @Test
  public void proxyRequestIsValidated() throws Exception {
    linkProxyToTestAccount();

    mvc.perform(post(PROXY_API_PATH).param("lifetimeSecs", "60"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.error", startsWith("invalid lifetime")));
  }

  @WithMockOAuthUser(user = "test", authorities = {"ROLE_USER", "ROLE_CLIENT"},
      scopes = {"proxy:generate"})
  @Test
  public void proxyRequestIssuerIsValidated() throws Exception {

    String longIssuerString = RandomStringUtils.random(129);

    mvc.perform(post(PROXY_API_PATH).param("issuer", longIssuerString))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.error", startsWith("invalid issuer")));
  }

  @WithMockOAuthUser(user = "test", authorities = {"ROLE_USER", "ROLE_CLIENT"},
      scopes = {"proxy:generate"})
  @Test
  public void proxyGenerationWorks() throws Exception {
    linkProxyToTestAccount();

    mvc.perform(post(PROXY_API_PATH))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.not_after").exists())
      .andExpect(jsonPath("$.subject").exists())
      .andExpect(jsonPath("$.issuer").exists())
      .andExpect(jsonPath("$.identity", is(TEST_0_SUBJECT)))
      .andExpect(jsonPath("$.certificate_chain").exists());

  } 
}
