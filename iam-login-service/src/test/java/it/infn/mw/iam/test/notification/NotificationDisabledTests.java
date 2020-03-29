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
package it.infn.mw.iam.test.notification;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.UnsupportedEncodingException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.persistence.model.IamAccount;
import it.infn.mw.iam.persistence.repository.IamAccountRepository;
import it.infn.mw.iam.registration.RegistrationRequestDto;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.util.notification.MockNotificationDelivery;
import it.infn.mw.iam.test.util.oauth.MockOAuth2Filter;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class, NotificationTestConfig.class, CoreControllerTestSupport.class})
@WebAppConfiguration
@Transactional
@TestPropertySource(properties = {"notification.disable=true"})
public class NotificationDisabledTests {

  public static final String REGISTRATION_CREATE_ENDPOINT = "/registration/create";

  @Autowired
  private MockNotificationDelivery notificationDelivery;

  @Value("${spring.mail.host}")
  private String mailHost;

  @Value("${spring.mail.port}")
  private Integer mailPort;

  @Autowired
  ObjectMapper mapper;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private IamAccountRepository accountRepository;
  
  @Autowired
  private MockOAuth2Filter mockOAuth2Filter;

  private MockMvc mvc;

  @Before
  public void setUp() throws InterruptedException {
    mockOAuth2Filter.cleanupSecurityContext();
    mvc = MockMvcBuilders.webAppContextSetup(context)
      .apply(springSecurity())
      .alwaysDo(log())
      .build();
    notificationDelivery.clearDeliveredNotifications();
  }

  @After
  public void tearDown() throws InterruptedException {
    mockOAuth2Filter.cleanupSecurityContext();
    notificationDelivery.clearDeliveredNotifications();
  }



  @Test
  public void testDisableNotificationOption() throws UnsupportedEncodingException, Exception {
    RegistrationRequestDto req = new RegistrationRequestDto();

    req.setGivenname("testDisableNotificationOption");
    req.setFamilyname("User");
    req.setEmail("testDisableNotificationOption@example.org");
    req.setUsername("testDisableNotificationOption");
    req.setNotes("testDisableNotificationOption");

    String jsonReq = mapper.writeValueAsString(req);

    String response = mvc
      .perform(post(REGISTRATION_CREATE_ENDPOINT).content(jsonReq)
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();

    RegistrationRequestDto savedRequest = mapper.readValue(response, RegistrationRequestDto.class);

    notificationDelivery.sendPendingNotifications();

    assertThat(notificationDelivery.getDeliveredNotifications(), hasSize(0));
    
    IamAccount account = accountRepository.findByUuid(savedRequest.getAccountId())
      .orElseThrow(() -> new AssertionError("Expected account not found!"));
    
    accountRepository.delete(account);

  }

}
