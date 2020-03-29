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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.aup.model.AupConverter;
import it.infn.mw.iam.api.aup.model.AupDTO;
import it.infn.mw.iam.audit.events.aup.AupCreatedEvent;
import it.infn.mw.iam.audit.events.aup.AupDeletedEvent;
import it.infn.mw.iam.audit.events.aup.AupSignedEvent;
import it.infn.mw.iam.audit.events.aup.AupUpdatedEvent;
import it.infn.mw.iam.persistence.model.IamAup;
import it.infn.mw.iam.persistence.repository.IamAupRepository;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.util.MockTimeProvider;
import it.infn.mw.iam.test.util.WithAnonymousUser;
import it.infn.mw.iam.test.util.oauth.MockOAuth2Filter;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class, CoreControllerTestSupport.class})
@WebAppConfiguration
@Transactional
@WithAnonymousUser
public class AupAuditTests extends AupTestSupport {

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private IamAupRepository aupRepo;

  @Autowired
  private AupConverter converter;

  @Autowired
  private MockOAuth2Filter mockOAuth2Filter;

  @Autowired
  private MockTimeProvider mockTimeProvider;

  @Autowired
  private ApplicationEventPublisher eventPublisher;

  private ArgumentCaptor<ApplicationEvent> eventCaptor;

  private MockMvc mvc;

  @Before
  public void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context)
      .alwaysDo(log())
      .apply(springSecurity())
      .build();
    mockOAuth2Filter.cleanupSecurityContext();
    eventCaptor = ArgumentCaptor.forClass(ApplicationEvent.class);
    reset(eventPublisher);
  }

  @After
  public void cleanupOAuthUser() {
    mockOAuth2Filter.cleanupSecurityContext();
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN", "USER"})
  public void aupCreationRaisesAupCreatedEvent() throws JsonProcessingException, Exception {
    AupDTO aup = converter.dtoFromEntity(buildDefaultAup());

    Date now = new Date();
    mockTimeProvider.setTime(now.getTime());

    mvc
      .perform(
          post("/iam/aup").contentType(APPLICATION_JSON).content(mapper.writeValueAsString(aup)))
      .andExpect(status().isCreated());

    verify(eventPublisher).publishEvent(eventCaptor.capture());
    ApplicationEvent event = eventCaptor.getValue();
    assertThat(event, instanceOf(AupCreatedEvent.class));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN", "USER"})
  public void aupDeletionRaisesAupDeletedEvent() throws JsonProcessingException, Exception {

    IamAup aup = buildDefaultAup();
    aupRepo.saveDefaultAup(aup);

    Date now = new Date();
    mockTimeProvider.setTime(now.getTime());

    mvc.perform(delete("/iam/aup")).andExpect(status().isNoContent());

    verify(eventPublisher).publishEvent(eventCaptor.capture());
    ApplicationEvent event = eventCaptor.getValue();
    assertThat(event, instanceOf(AupDeletedEvent.class));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN", "USER"})
  public void aupUpdateRaisesAupUpdatedEvent() throws JsonProcessingException, Exception {

    Date now = new Date();
    mockTimeProvider.setTime(now.getTime());

    IamAup aup = buildDefaultAup();
    aupRepo.saveDefaultAup(aup);

    aup.setText("Updated aup text");

    // Time travel 1 minute in the future
    Date then = new Date(now.getTime() + TimeUnit.MINUTES.toMillis(1));
    mockTimeProvider.setTime(then.getTime());

    mvc
      .perform(
          patch("/iam/aup").contentType(APPLICATION_JSON).content(mapper.writeValueAsString(aup)))
      .andExpect(status().isOk());
    
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    ApplicationEvent event = eventCaptor.getValue();
    assertThat(event, instanceOf(AupUpdatedEvent.class));
  }
  
  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN", "USER"})
  public void aupSignatureRaisesAupSignedEvent() throws JsonProcessingException, Exception {
    Date now = new Date();
    mockTimeProvider.setTime(now.getTime());

    IamAup aup = buildDefaultAup();
    aupRepo.saveDefaultAup(aup);
    
    mvc.perform(post("/iam/aup/signature")).andExpect(status().isCreated());
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    ApplicationEvent event = eventCaptor.getValue();
    assertThat(event, instanceOf(AupSignedEvent.class));
  }


}
