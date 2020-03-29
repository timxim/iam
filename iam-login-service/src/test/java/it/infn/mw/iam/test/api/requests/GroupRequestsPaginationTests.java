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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import it.infn.mw.iam.IamLoginService;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class})
@WebAppConfiguration
@Transactional
public class GroupRequestsPaginationTests extends GroupRequestsTestUtils{
  
  private final static String LIST_REQUESTS_URL = "/iam/group_requests/";
  
  public static final String GROUP_NAME_TEMPLATE = "Test-%03d";
  
  @Autowired
  private WebApplicationContext context;

  private MockMvc mvc;
  
  void saveNPendingGroupRequests(String username, int numRequests) {
    for (int i=1; i <= numRequests; i++) {
      savePendingGroupRequest(username, String.format(GROUP_NAME_TEMPLATE,i));
    }
  }
  
  @Before
  public void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context)
      .apply(springSecurity())
      .alwaysDo(log())
      .build();
  }
  
  
  @Test
  @WithMockUser(username="test", roles="USER")
  public void testNoGroupManagersPaginationResult() throws Exception {
    mvc.perform(get(LIST_REQUESTS_URL)
        .contentType(MediaType.APPLICATION_JSON))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.totalResults", equalTo(0)))
    .andExpect(jsonPath("$.startIndex", equalTo(1)))
    .andExpect(jsonPath("$.itemsPerPage", equalTo(0)))
    .andExpect(jsonPath("$.Resources", hasSize(0)));
  }
  
  @Test
  @WithMockUser(username="test", roles="USER")
  public void testPaginatedAccess() throws Exception {
    
    saveNPendingGroupRequests("test", 20);
    
    mvc.perform(get(LIST_REQUESTS_URL)
        .contentType(MediaType.APPLICATION_JSON))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.totalResults", equalTo(20)))
    .andExpect(jsonPath("$.startIndex", equalTo(1)))
    .andExpect(jsonPath("$.itemsPerPage", equalTo(10)))
    .andExpect(jsonPath("$.Resources", hasSize(10)))
    .andExpect(jsonPath("$.Resources[?(@.groupName=='Test-001')]").exists())
    .andExpect(jsonPath("$.Resources[?(@.groupName=='Test-002')]").exists())
    .andExpect(jsonPath("$.Resources[?(@.groupName=='Test-003')]").exists())
    .andExpect(jsonPath("$.Resources[?(@.groupName=='Test-004')]").exists())
    .andExpect(jsonPath("$.Resources[?(@.groupName=='Test-005')]").exists())
    .andExpect(jsonPath("$.Resources[?(@.groupName=='Test-006')]").exists())
    .andExpect(jsonPath("$.Resources[?(@.groupName=='Test-007')]").exists())
    .andExpect(jsonPath("$.Resources[?(@.groupName=='Test-008')]").exists())
    .andExpect(jsonPath("$.Resources[?(@.groupName=='Test-009')]").exists())
    .andExpect(jsonPath("$.Resources[?(@.groupName=='Test-010')]").exists());
    
    mvc.perform(get(LIST_REQUESTS_URL).param("startIndex", "11")
        .contentType(MediaType.APPLICATION_JSON))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.totalResults", equalTo(20)))
    .andExpect(jsonPath("$.startIndex", equalTo(11)))
    .andExpect(jsonPath("$.itemsPerPage", equalTo(10)))
    .andExpect(jsonPath("$.Resources", hasSize(10)))
    .andExpect(jsonPath("$.Resources[?(@.groupName=='Test-011')]").exists())
    .andExpect(jsonPath("$.Resources[?(@.groupName=='Test-012')]").exists())
    .andExpect(jsonPath("$.Resources[?(@.groupName=='Test-013')]").exists())
    .andExpect(jsonPath("$.Resources[?(@.groupName=='Test-014')]").exists())
    .andExpect(jsonPath("$.Resources[?(@.groupName=='Test-015')]").exists())
    .andExpect(jsonPath("$.Resources[?(@.groupName=='Test-016')]").exists())
    .andExpect(jsonPath("$.Resources[?(@.groupName=='Test-017')]").exists())
    .andExpect(jsonPath("$.Resources[?(@.groupName=='Test-018')]").exists())
    .andExpect(jsonPath("$.Resources[?(@.groupName=='Test-019')]").exists())
    .andExpect(jsonPath("$.Resources[?(@.groupName=='Test-020')]").exists());
  }

}
