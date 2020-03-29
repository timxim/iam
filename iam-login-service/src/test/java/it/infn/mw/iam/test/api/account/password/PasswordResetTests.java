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
package it.infn.mw.iam.test.api.account.password;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.registration.PersistentUUIDTokenGenerator;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.notification.NotificationTestConfig;
import it.infn.mw.iam.test.util.WithAnonymousUser;
import it.infn.mw.iam.test.util.notification.MockNotificationDelivery;
import it.infn.mw.iam.test.util.oauth.MockOAuth2Filter;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class, NotificationTestConfig.class,
    CoreControllerTestSupport.class})
@WebAppConfiguration
@Transactional
@WithAnonymousUser
public class PasswordResetTests {

  
  @Autowired
  private PersistentUUIDTokenGenerator tokenGenerator;

  @Autowired
  private MockNotificationDelivery notificationDelivery;

  @Autowired
  private MockOAuth2Filter mockOAuth2Filter;

  @Autowired
  private WebApplicationContext context;

  private MockMvc mvc;

  @Before
  public void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context)
      .apply(springSecurity())
      .alwaysDo(log())
      .build();
  }

  @After
  public void tearDown() {
    notificationDelivery.clearDeliveredNotifications();
    mockOAuth2Filter.cleanupSecurityContext();
  }

  @Test
  public void testChangePassword() throws Exception {
    String testEmail = "test@iam.test";

    String newPassword = "secure_password";

    mvc.perform(post("/iam/password-reset/token").param("email", testEmail))
      .andExpect(status().isOk());

    String resetToken = tokenGenerator.getLastToken();

    mvc.perform(head("/iam/password-reset/token/{token}", resetToken)).andExpect(status().isOk());

    mvc
      .perform(
          post("/iam/password-reset").param("token", resetToken).param("password", newPassword))
      .andExpect(status().isOk());

    mvc.perform(head("/iam/password-reset/token/{token}", resetToken))
      .andExpect(status().isNotFound());

  }

  @Test
  public void testResetPasswordWithInvalidResetToken() throws Exception {

    String resetToken = "abcdefghilmnopqrstuvz";

    mvc.perform(head("/iam/password-reset/token/{token}", resetToken)).andExpect(status().isNotFound());
   
  }

  @Test
  public void testCreatePasswordResetTokenReturnsOkForUnknownAddress() throws Exception {

    String testEmail = "test@foo.bar";
   
    mvc.perform(post("/iam/password-reset/token").param("email", testEmail))
    .andExpect(status().isOk());

  }

  @Test
  public void testEmailValidationForPasswordResetTokenCreation() throws Exception {
    String invalidEmailAddress = "this_is_not_an_email";
    
    mvc.perform(post("/iam/password-reset/token").param("email", invalidEmailAddress))
    .andExpect(status().isBadRequest())
    .andExpect(MockMvcResultMatchers.content().string("validation error: please specify a valid email address"));
     
  }

}
