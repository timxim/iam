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
package it.infn.mw.iam.test.scim.group.patch;

import static it.infn.mw.iam.api.scim.model.ScimConstants.SCIM_CONTENT_TYPE;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.scim.model.ScimGroup;
import it.infn.mw.iam.api.scim.model.ScimGroupPatchRequest;
import it.infn.mw.iam.api.scim.model.ScimUser;
import it.infn.mw.iam.test.TestUtils;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.util.WithMockOAuthUser;
import it.infn.mw.iam.test.util.oauth.MockOAuth2Filter;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class, CoreControllerTestSupport.class})
@WebAppConfiguration
@WithMockOAuthUser(clientId = "scim-client-rw", scopes = {"scim:read", "scim:write"})
public class ScimGroupProvisioningPatchReplaceTests extends ScimGroupPatchUtils {

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private MockOAuth2Filter mockOAuth2Filter;
  
  private ScimGroup engineers;
  private ScimUser lennon, lincoln;

  @Before
  public void initTests() throws Exception {
    mvc = MockMvcBuilders.webAppContextSetup(context)
      .apply(springSecurity())
      .alwaysDo(log())
      .build();

    engineers = addTestGroup("engineers");
    lennon = addTestUser("john_lennon", "lennon@email.test", "John", "Lennon");
    lincoln = addTestUser("abraham_lincoln", "lincoln@email.test", "Abraham", "Lincoln");
  }

  @After
  public void teardownTests() throws Exception {
    deleteScimResource(lennon);
    deleteScimResource(lincoln);
    deleteScimResource(engineers);
    mockOAuth2Filter.cleanupSecurityContext();
  }

  @Test
  public void testGroupPatchReplaceMember() throws Exception {

    List<ScimUser> members = new ArrayList<ScimUser>();
    members.add(lennon);

    ScimGroupPatchRequest patchReq = getPatchAddUsersRequest(members);

    //@formatter:off
    mvc.perform(patch(engineers.getMeta().getLocation())
        .contentType(SCIM_CONTENT_TYPE)
        .content(objectMapper.writeValueAsString(patchReq)))
      .andExpect(status().isNoContent());
    //@formatter:on

    mvc.perform(get(engineers.getMeta().getLocation()).contentType(SCIM_CONTENT_TYPE))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id", equalTo(engineers.getId())))
      .andExpect(jsonPath("$.displayName", equalTo(engineers.getDisplayName())))
      .andExpect(jsonPath("$.members", hasSize(equalTo(1))))
      .andExpect(jsonPath("$.members[0].display", equalTo("John Lennon")))
      .andExpect(jsonPath("$.members[0].value", equalTo(lennon.getId())))
      .andExpect(jsonPath("$.members[0].$ref", equalTo(lennon.getMeta().getLocation())));

    members.remove(lennon);
    members.add(lincoln);
    patchReq = getPatchReplaceUsersRequest(members);

    //@formatter:off
    mvc.perform(patch(engineers.getMeta().getLocation())
        .contentType(SCIM_CONTENT_TYPE)
        .content(objectMapper.writeValueAsString(patchReq)))
      .andExpect(status().isNoContent());
    //@formatter:on

    String result =
        mvc.perform(get(engineers.getMeta().getLocation()).contentType(SCIM_CONTENT_TYPE))
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();

    ScimGroup updatedGroup = objectMapper.readValue(result, ScimGroup.class);

    assertThat(updatedGroup.getMembers(), hasSize(1));
    assertThat(updatedGroup.getMembers(), hasItem(TestUtils.getMemberRef(lincoln)));
  }

  @Test
  public void testGroupPatchReplaceSameMember() throws Exception {

    List<ScimUser> members = new ArrayList<ScimUser>();
    members.add(lennon);

    ScimGroupPatchRequest patchReq = getPatchAddUsersRequest(members);

    //@formatter:off
    mvc.perform(patch(engineers.getMeta().getLocation())
        .contentType(SCIM_CONTENT_TYPE)
        .content(objectMapper.writeValueAsString(patchReq)))
      .andExpect(status().isNoContent());
    //@formatter:on

    mvc.perform(get(engineers.getMeta().getLocation()).contentType(SCIM_CONTENT_TYPE))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id", equalTo(engineers.getId())))
      .andExpect(jsonPath("$.displayName", equalTo(engineers.getDisplayName())))
      .andExpect(jsonPath("$.members", hasSize(equalTo(1))))
      .andExpect(jsonPath("$.members[0].display", equalTo("John Lennon")))
      .andExpect(jsonPath("$.members[0].value", equalTo(lennon.getId())))
      .andExpect(jsonPath("$.members[0].$ref", equalTo(lennon.getMeta().getLocation())));

    patchReq = getPatchReplaceUsersRequest(members);

    //@formatter:off
    mvc.perform(patch(engineers.getMeta().getLocation())
        .contentType(SCIM_CONTENT_TYPE)
        .content(objectMapper.writeValueAsString(patchReq)))
      .andExpect(status().isNoContent());
    //@formatter:on

    String result =
        mvc.perform(get(engineers.getMeta().getLocation()).contentType(SCIM_CONTENT_TYPE))
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();

    ScimGroup updatedGroup = objectMapper.readValue(result, ScimGroup.class);

    assertThat(updatedGroup.getMembers(), hasSize(1));
    assertThat(updatedGroup.getMembers(), hasItem(TestUtils.getMemberRef(lennon)));
  }

  @Test
  public void testGroupPatchReplaceWithEmptyMemberList() throws Exception {

    List<ScimUser> members = new ArrayList<ScimUser>();
    ScimGroupPatchRequest patchReq = getPatchReplaceUsersRequest(members);

    //@formatter:off
    mvc.perform(patch(engineers.getMeta().getLocation())
        .contentType(SCIM_CONTENT_TYPE)
        .content(objectMapper.writeValueAsString(patchReq)))
      .andExpect(status().isNoContent());
    //@formatter:on

    String result =
        mvc.perform(get(engineers.getMeta().getLocation()).contentType(SCIM_CONTENT_TYPE))
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();

    ScimGroup updatedGroup = objectMapper.readValue(result, ScimGroup.class);

    assertThat(updatedGroup.getMembers(), empty());
  }
}
