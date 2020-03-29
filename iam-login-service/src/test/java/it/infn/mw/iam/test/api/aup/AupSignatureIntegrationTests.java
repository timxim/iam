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
package it.infn.mw.iam.test.api.aup;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.aup.model.AupSignatureDTO;
import it.infn.mw.iam.core.user.IamAccountService;
import it.infn.mw.iam.persistence.model.IamAccount;
import it.infn.mw.iam.persistence.model.IamAup;
import it.infn.mw.iam.persistence.repository.IamAccountRepository;
import it.infn.mw.iam.persistence.repository.IamAupRepository;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.util.DateEqualModulo1Second;
import it.infn.mw.iam.test.util.MockTimeProvider;
import it.infn.mw.iam.test.util.WithAnonymousUser;
import it.infn.mw.iam.test.util.oauth.MockOAuth2Filter;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class, CoreControllerTestSupport.class})
@WebAppConfiguration
@Transactional
@WithAnonymousUser
public class AupSignatureIntegrationTests extends AupTestSupport {

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private IamAupRepository aupRepo;

  @Autowired
  private IamAccountService accountService;

  @Autowired
  private IamAccountRepository accountRepo;

  private MockMvc mvc;

  @Autowired
  private MockOAuth2Filter mockOAuth2Filter;

  @Autowired
  private MockTimeProvider mockTimeProvider;

  @Before
  public void setup() {
    mockOAuth2Filter.cleanupSecurityContext();
    mvc = MockMvcBuilders.webAppContextSetup(context)
      .alwaysDo(log())
      .apply(springSecurity())
      .build();
    mockOAuth2Filter.cleanupSecurityContext();
  }

  @After
  public void cleanupOAuthUser() {
    mockOAuth2Filter.cleanupSecurityContext();
  }

  @Test
  public void getAupSignatureRequiresAuthenticatedUser() throws Exception {
    mvc.perform(get("/iam/aup/signature")).andExpect(status().isUnauthorized());
  }

  @Test
  public void signAupSignatureRequiresAuthenticatedUser() throws Exception {
    mvc.perform(post("/iam/aup/signature")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "test", roles = {"USER"})
  public void getAupSignatureWithDefaultAupReturns404() throws Exception {
    mvc.perform(get("/iam/aup/signature"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.error", equalTo("AUP signature not found for user 'test'")));
  }

  @Test
  @WithMockUser(username = "test", roles = {"USER"})
  public void getAupSignatureWithNoSignatureRecordReturns404() throws Exception {
    IamAup aup = buildDefaultAup();
    aupRepo.save(aup);
    mvc.perform(get("/iam/aup/signature"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.error", equalTo("AUP signature not found for user 'test'")));
  }

  @Test
  @WithMockUser(username = "test", roles = {"USER"})
  public void signatureCreationReturns204() throws Exception {
    IamAup aup = buildDefaultAup();
    aupRepo.save(aup);

    Date now = new Date();

    mockTimeProvider.setTime(now.getTime());

    mvc.perform(post("/iam/aup/signature")).andExpect(status().isCreated());

    String sigString = mvc.perform(get("/iam/aup/signature"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.aup").exists())
      .andExpect(jsonPath("$.account.uuid").exists())
      .andExpect(jsonPath("$.account.username", equalTo("test")))
      .andExpect(jsonPath("$.account.name", equalTo("Test User")))
      .andExpect(jsonPath("$.signatureTime").exists())
      .andReturn()
      .getResponse()
      .getContentAsString();

    AupSignatureDTO sig = mapper.readValue(sigString, AupSignatureDTO.class);
    assertThat(sig.getSignatureTime(), new DateEqualModulo1Second(now));

    Date then = new Date();
    mockTimeProvider.setTime(then.getTime());

    mvc.perform(post("/iam/aup/signature")).andExpect(status().isCreated());
    sigString = mvc.perform(get("/iam/aup/signature"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.aup").exists())
      .andExpect(jsonPath("$.account.uuid").exists())
      .andExpect(jsonPath("$.account.username", equalTo("test")))
      .andExpect(jsonPath("$.account.name", equalTo("Test User")))
      .andExpect(jsonPath("$.signatureTime").exists())
      .andReturn()
      .getResponse()
      .getContentAsString();

    sig = mapper.readValue(sigString, AupSignatureDTO.class);
    assertThat(sig.getSignatureTime(), new DateEqualModulo1Second(then));

  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN", "USER"})
  public void aupRemovalRemovesSignatureRecords() throws Exception {
    IamAup aup = buildDefaultAup();

    mvc
      .perform(
          post("/iam/aup").contentType(APPLICATION_JSON).content(mapper.writeValueAsString(aup)))
      .andExpect(status().isCreated());


    mvc.perform(post("/iam/aup/signature")).andExpect(status().isCreated());
    mvc.perform(delete("/iam/aup")).andExpect(status().isNoContent());
    mvc.perform(get("/iam/aup/signature"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.error", equalTo("AUP signature not found for user 'admin'")));

  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN", "USER"})
  public void accountRemovalRemovesSignatureRecords() throws Exception {

    IamAup aup = buildDefaultAup();

    mvc
      .perform(
          post("/iam/aup").contentType(APPLICATION_JSON).content(mapper.writeValueAsString(aup)))
      .andExpect(status().isCreated());

    mvc.perform(post("/iam/aup/signature")).andExpect(status().isCreated());

    IamAccount account = accountRepo.findByUsername("admin")
      .orElseThrow(() -> new AssertionError("Expected admin account not found"));

    accountService.deleteAccount(account);

    // if we get this far, the persistence layer is working as expected
  }

  @Test
  @WithMockUser(username = "test", roles = {"USER"})
  public void normalUserCannotSeeOtherUserAup() throws Exception {
    IamAup aup = buildDefaultAup();
    aupRepo.save(aup);

    Date now = new Date();

    mockTimeProvider.setTime(now.getTime());
    mvc.perform(post("/iam/aup/signature")).andExpect(status().isCreated());
    mvc.perform(get("/iam/aup/signature")).andExpect(status().isOk());
    mvc.perform(get("/iam/aup/signature/" + TEST_USER_UUID)).andExpect(status().isOk());
    mvc.perform(get("/iam/aup/signature/" + TEST_100_USER_UUID)).andExpect(status().isForbidden());
  }

  @Test
  public void adminUserCanSeeOtherUserAup() throws Exception {
    IamAup aup = buildDefaultAup();
    aupRepo.save(aup);

    Date now = new Date();


    mockTimeProvider.setTime(now.getTime());
    mvc.perform(post("/iam/aup/signature").with(user("test").roles("USER")))
      .andExpect(status().isCreated());
    mvc.perform(get("/iam/aup/signature").with(user("test").roles("USER")))
      .andExpect(status().isOk());
    mvc.perform(get("/iam/aup/signature/" + TEST_USER_UUID).with(user("test").roles("USER")))
      .andExpect(status().isOk());
    mvc
      .perform(
          get("/iam/aup/signature/" + TEST_USER_UUID).with(user("admin").roles("USER", "ADMIN")))
      .andExpect(status().isOk());
  }
}
