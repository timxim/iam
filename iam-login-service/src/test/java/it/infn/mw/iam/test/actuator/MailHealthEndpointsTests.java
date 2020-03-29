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

import static java.lang.String.format;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.test.util.MockMailHealthIndicator;
import it.infn.mw.iam.test.util.WithAnonymousUser;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(
    classes = {IamLoginService.class, MailHealthEndpointsTestsConfig.class})
@WebAppConfiguration
public class MailHealthEndpointsTests {

  private static final String ADMIN_USERNAME = "admin";
  private static final String ADMIN_ROLE = "ADMIN";

  private static final String USER_USERNAME = "test";
  private static final String USER_ROLE = "USER";

  private static final String STATUS_UP = "UP";

  @Value("${spring.mail.host}")
  private String mailHost;

  @Value("${spring.mail.port}")
  private Integer mailPort;

  @Value("${health.mailProbe.path}")
  private String mailEndpointPath;



  @Autowired
  private WebApplicationContext context;

  @Autowired
  private MockMailHealthIndicator mhi;

  private MockMvc mvc;

  @Before
  public synchronized void setup() throws InterruptedException {

    mvc = MockMvcBuilders.webAppContextSetup(context)
      .apply(springSecurity())
      .alwaysDo(log())
      .build();

    SecurityContextHolder.clearContext();
  }

  @After
  public void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @WithAnonymousUser
  public void testMailHealthEndpointWithSmtp() throws Exception {

    mhi.setActive(true);
    // @formatter:off
    mvc.perform(get(mailEndpointPath))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status", equalTo(STATUS_UP)))
      .andExpect(jsonPath("$.mail").doesNotExist());
    // @formatter:on
  }

  @Test
  @WithMockUser(username = USER_USERNAME, roles = {USER_ROLE})
  public void testMailHealthEndpointWithSmtpAsUser() throws Exception {

    mhi.setActive(true);

    // @formatter:off
    mvc.perform(get(mailEndpointPath))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status", equalTo(STATUS_UP)))
      .andExpect(jsonPath("$.mail").doesNotExist());
      ;
    // @formatter:on
  }

  @Test
  @WithMockUser(username = ADMIN_USERNAME, roles = {ADMIN_ROLE})
  public void testMailHealthEndpointWithSmtpAsAdmin() throws Exception {

    mhi.setActive(true);
    mhi.setMailhost(mailHost);
    mhi.setMailPort(mailPort);

    // @formatter:off
    mvc.perform(get(mailEndpointPath))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status", equalTo(STATUS_UP)))
      .andExpect(jsonPath("$.mail.status", equalTo(STATUS_UP)))
      .andExpect(jsonPath("$.mail.location", equalTo(format("%s:%d", mailHost, mailPort))))
      ;
    // @formatter:on
  }

}
