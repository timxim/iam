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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
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
public class GroupRequestsDeleteTests extends GroupRequestsTestUtils {

  private final static String DELETE_URL = "/iam/group_requests/{uuid}";

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
  public void deletePendingGroupRequestAsAdmin() throws Exception {

    GroupRequestDto request = savePendingGroupRequest(TEST_100_USERNAME, TEST_001_GROUPNAME);

    // @formatter:off
    mvc.perform(delete(DELETE_URL, request.getUuid()))
      .andExpect(status().isNoContent());
    // @formatter:on
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void deleteApprovedGroupRequestAsAdmin() throws Exception {
    GroupRequestDto request = saveApprovedGroupRequest(TEST_100_USERNAME, TEST_001_GROUPNAME);
    // @formatter:off
    mvc.perform(delete(DELETE_URL, request.getUuid()))
      .andExpect(status().isNoContent());
    // @formatter:on
  }

  @Test
  @WithMockUser(roles = {"USER"}, username = TEST_100_USERNAME)
  public void deletePendingGroupRequestAsUser() throws Exception {

    GroupRequestDto request = savePendingGroupRequest(TEST_100_USERNAME, TEST_001_GROUPNAME);

    // @formatter:off
    mvc.perform(delete(DELETE_URL, request.getUuid()))
      .andExpect(status().isNoContent());
    // @formatter:on
  }

  @Test
  @WithMockUser(roles = {"USER"}, username = TEST_100_USERNAME)
  public void deleteApprovedGroupRequestAsUser() throws Exception {
    GroupRequestDto request = saveApprovedGroupRequest(TEST_100_USERNAME, TEST_001_GROUPNAME);

    // @formatter:off
    mvc.perform(delete(DELETE_URL, request.getUuid()))
      .andExpect(status().isForbidden());
    // @formatter:on
  }

  @Test
  @WithMockUser(roles = {"USER"}, username = TEST_100_USERNAME)
  public void deleteGroupRequestOfAnotherUser() throws Exception {

    GroupRequestDto request = savePendingGroupRequest("test_101", TEST_001_GROUPNAME);

    // @formatter:off
    mvc.perform(delete(DELETE_URL, request.getUuid()))
      .andExpect(status().isForbidden());
    // @formatter:on
  }

  @Test
  @WithAnonymousUser
  public void deleteGroupRequestAsAnonymous() throws Exception {
    
    GroupRequestDto request = savePendingGroupRequest(TEST_100_USERNAME, TEST_001_GROUPNAME);

    // @formatter:off
    mvc.perform(delete(DELETE_URL, request.getUuid()))
      .andExpect(status().isUnauthorized());
    // @formatter:on
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void deleteNotExitingGroupRequest() throws Exception {

    String fakeRequestUuid = UUID.randomUUID().toString();

    // @formatter:off
    mvc.perform(delete(DELETE_URL, fakeRequestUuid))
      .andExpect(status().isBadRequest());
    // @formatter:on
  }

  @Test
  @WithMockUser(roles = {"ADMIN", "USER"})
  public void deletePendingGroupRequestAsUserWithBothRoles() throws Exception {
    
    GroupRequestDto request = savePendingGroupRequest(TEST_100_USERNAME, TEST_001_GROUPNAME);
    
    // @formatter:off
    mvc.perform(delete(DELETE_URL, request.getUuid()))
      .andExpect(status().isNoContent());
    // @formatter:on
  }

}
