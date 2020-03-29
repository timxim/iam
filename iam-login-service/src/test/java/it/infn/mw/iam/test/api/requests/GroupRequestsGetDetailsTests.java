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
package it.infn.mw.iam.test.api.requests;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

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

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.requests.model.GroupRequestDto;
import it.infn.mw.iam.test.util.WithAnonymousUser;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class})
@WebAppConfiguration
@Transactional
public class GroupRequestsGetDetailsTests extends GroupRequestsTestUtils {

  private final static String GET_DETAILS_URL = "/iam/group_requests/{uuid}";

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

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void getGroupRequestDetailsAsAdmin() throws Exception {
    
    GroupRequestDto request = savePendingGroupRequest(TEST_100_USERNAME, TEST_001_GROUPNAME);
    
    // @formatter:off
    mvc.perform(get(GET_DETAILS_URL, request.getUuid()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.uuid", equalTo(request.getUuid())))
      .andExpect(jsonPath("$.username", equalTo(request.getUsername())))
      .andExpect(jsonPath("$.groupName", equalTo(request.getGroupName())))
      .andExpect(jsonPath("$.status", equalTo(request.getStatus())))
      .andExpect(jsonPath("$.notes", equalTo(request.getNotes())));
    // @formatter:on
  }

  @Test
  @WithMockUser(roles = {"USER"}, username = TEST_100_USERNAME)
  public void getGroupRequestDetailsAsUser() throws Exception {
    GroupRequestDto request = savePendingGroupRequest(TEST_100_USERNAME, TEST_001_GROUPNAME);
    
    // @formatter:off
    mvc.perform(get(GET_DETAILS_URL, request.getUuid()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.uuid", equalTo(request.getUuid())))
      .andExpect(jsonPath("$.username", equalTo(request.getUsername())))
      .andExpect(jsonPath("$.groupName", equalTo(request.getGroupName())))
      .andExpect(jsonPath("$.status", equalTo(request.getStatus())));;
    // @formatter:on
  }

  @Test
  @WithMockUser(roles = {"USER"}, username = TEST_100_USERNAME)
  public void getGroupRequestDetailsOfAnotherUser() throws Exception {
    GroupRequestDto request = savePendingGroupRequest("test_101", TEST_001_GROUPNAME);
    // @formatter:off
    mvc.perform(get(GET_DETAILS_URL, request.getUuid()))
      .andExpect(status().isForbidden());
    // @formatter:on
  }

  @Test
  @WithAnonymousUser
  public void getGroupRequestDetailsAsAnonymous() throws Exception {
    GroupRequestDto request = savePendingGroupRequest(TEST_100_USERNAME, TEST_001_GROUPNAME);
    // @formatter:off
    mvc.perform(get(GET_DETAILS_URL, request.getUuid()))
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.error", containsString("unauthorized")))
      .andExpect(jsonPath("$.error_description", containsString("Full authentication is required")));
    // @formatter:on
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void getDetailsOfNotExitingGroupRequest() throws Exception {

    String fakeRequestUuid = UUID.randomUUID().toString();
    // @formatter:off
    mvc.perform(get(GET_DETAILS_URL, fakeRequestUuid))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.error", containsString("does not exist")));
    // @formatter:on
  }

  @Test
  @WithMockUser(roles = {"ADMIN", "USER"})
  public void getGroupRequestDetailsAsUserWithBothRoles() throws Exception {
    GroupRequestDto request = savePendingGroupRequest(TEST_100_USERNAME, TEST_001_GROUPNAME);
    // @formatter:off
    mvc.perform(get(GET_DETAILS_URL, request.getUuid()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.uuid", equalTo(request.getUuid())))
      .andExpect(jsonPath("$.username", equalTo(request.getUsername())))
      .andExpect(jsonPath("$.groupName", equalTo(request.getGroupName())))
      .andExpect(jsonPath("$.status", equalTo(request.getStatus())))
      .andExpect(jsonPath("$.notes", equalTo(request.getNotes())));
    // @formatter:on
  }

}
