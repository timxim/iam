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
package it.infn.mw.iam.test.core;

import static it.infn.mw.iam.test.scim.ScimUtils.SCIM_CLIENT_ID;
import static it.infn.mw.iam.test.scim.ScimUtils.SCIM_READ_SCOPE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.test.scim.ScimRestUtilsMvc;
import it.infn.mw.iam.test.util.WithMockOAuthUser;
import it.infn.mw.iam.test.util.oauth.MockOAuth2Filter;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(
    classes = {IamLoginService.class, CoreControllerTestSupport.class, ScimRestUtilsMvc.class})
@WebAppConfiguration
public class MeControllerTests {

  private final static String TESTUSER_USERNAME = "test_101";
  private final static String NOT_FOUND_USERNAME = "not_found";

  @Autowired
  private ScimRestUtilsMvc restUtils;

  @Autowired
  private MockOAuth2Filter mockOAuth2Filter;

  @After
  public void teardown() {
    mockOAuth2Filter.cleanupSecurityContext();
  }

  @Test
  @WithMockOAuthUser(user = TESTUSER_USERNAME, authorities = {})
  public void insufficientScopeUser() throws Exception {

    restUtils.getMe(FORBIDDEN);
  }

  @Test
  @WithMockOAuthUser(user = NOT_FOUND_USERNAME, authorities = {"ROLE_USER"})
  public void notFoundUser() throws Exception {

    restUtils.getMe(NOT_FOUND);
  }

  @Test
  @WithMockOAuthUser(user = TESTUSER_USERNAME, authorities = {"ROLE_USER"})
  public void authenticatedUser() throws Exception {

    assertThat(restUtils.getMe().getUserName(), equalTo(TESTUSER_USERNAME));
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE})
  public void notAuthorizedClient() throws Exception {

    restUtils.getMe(BAD_REQUEST)
      .andExpect(jsonPath("$.detail", is("No user linked to the current OAuth token")));

  }
}
