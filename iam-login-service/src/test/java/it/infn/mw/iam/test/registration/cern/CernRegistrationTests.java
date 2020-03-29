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
package it.infn.mw.iam.test.registration.cern;

import static it.infn.mw.iam.authn.ExternalAuthenticationHandlerSupport.EXT_AUTHN_UNREGISTERED_USER_AUTH_STRING;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.registration.cern.CernHrDBApiService;
import it.infn.mw.iam.api.registration.cern.CernHrDbApiError;
import it.infn.mw.iam.test.util.WithAnonymousUser;
import it.infn.mw.iam.test.util.WithMockSAMLUser;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class, CernRegistrationTests.class})
@WebAppConfiguration
@Transactional
@ActiveProfiles("h2-test,cern")
public class CernRegistrationTests {

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private CernHrDBApiService mockService;

  private MockMvc mvc;

  @Bean
  @Primary
  public CernHrDBApiService hrDbService() {
    return mock(CernHrDBApiService.class);
  }

  @Before
  public void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context)
      .apply(springSecurity())
      .alwaysDo(log())
      .build();
  }

  @After
  public void cleanup() {
    reset(mockService);
  }

  @Test
  @WithAnonymousUser
  public void testRedirectToCernSso() throws Exception {

    mvc.perform(get("/start-registration"))
      .andExpect(MockMvcResultMatchers.status().isFound())
      .andExpect(redirectedUrl("/cern-registration"));

    mvc.perform(get("/cern-registration"))
      .andExpect(MockMvcResultMatchers.status().isFound())
      .andExpect(redirectedUrl("http://localhost/saml/login?idp=https://cern.ch/login"));
  }

  @Test
  @WithMockSAMLUser(issuer = "https://idp.example",
      authorities = EXT_AUTHN_UNREGISTERED_USER_AUTH_STRING)
  public void testCernAuthIsRequired() throws Exception {
    mvc.perform(get("/cern-registration"))
      .andExpect(status().isOk())
      .andExpect(forwardedUrl("/login"))
      .andExpect(request().attribute("accessDeniedError", containsString("CERN SSO")));
  }

  @Test
  @WithMockSAMLUser(issuer = "https://cern.ch/login",
      authorities = EXT_AUTHN_UNREGISTERED_USER_AUTH_STRING, cernPersonId = "12345678")
  public void testCernAuthHonoured() throws Exception {
    when(mockService.hasValidExperimentParticipation(Mockito.anyString())).thenReturn(true);

    mvc.perform(get("/cern-registration"))
      .andExpect(status().isOk())
      .andExpect(view().name("iam/cern/register"));

    ArgumentCaptor<String> personIdCaptor = ArgumentCaptor.forClass(String.class);

    verify(mockService).hasValidExperimentParticipation(personIdCaptor.capture());
    assertThat(personIdCaptor.getValue(), is("12345678"));
  }

  @Test
  @WithMockSAMLUser(issuer = "https://cern.ch/login",
      authorities = EXT_AUTHN_UNREGISTERED_USER_AUTH_STRING, cernPersonId = "12345678")
  public void testNotVoMemberBehaviour() throws Exception {
    when(mockService.hasValidExperimentParticipation(Mockito.anyString())).thenReturn(false);

    mvc.perform(get("/cern-registration"))
      .andExpect(status().isOk())
      .andExpect(view().name("iam/cern/not-a-vo-member"));

    ArgumentCaptor<String> personIdCaptor = ArgumentCaptor.forClass(String.class);

    verify(mockService).hasValidExperimentParticipation(personIdCaptor.capture());
    assertThat(personIdCaptor.getValue(), is("12345678"));
  }

  @Test
  @WithMockSAMLUser(issuer = "https://cern.ch/login",
      authorities = EXT_AUTHN_UNREGISTERED_USER_AUTH_STRING, cernPersonId = "12345678")
  public void testCernHrDbApiExceptionHandling() throws Exception {
    when(mockService.hasValidExperimentParticipation(Mockito.anyString()))
      .thenThrow(new CernHrDbApiError("error"));
    mvc.perform(get("/cern-registration"))
      .andExpect(status().isInternalServerError())
      .andExpect(view().name("iam/cern/hr-error"))
      .andExpect(model().attributeExists("hrError"));
  }
}
