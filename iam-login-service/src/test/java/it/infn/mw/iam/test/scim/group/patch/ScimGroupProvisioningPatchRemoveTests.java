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
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
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

import com.google.common.collect.Lists;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.scim.model.ScimGroup;
import it.infn.mw.iam.api.scim.model.ScimGroupPatchRequest;
import it.infn.mw.iam.api.scim.model.ScimUser;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.util.WithMockOAuthUser;
import it.infn.mw.iam.test.util.oauth.MockOAuth2Filter;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class, CoreControllerTestSupport.class})
@WebAppConfiguration
@WithMockOAuthUser(clientId = "scim-client-rw", scopes = {"scim:read", "scim:write"})
public class ScimGroupProvisioningPatchRemoveTests extends ScimGroupPatchUtils {

  @Autowired
  private WebApplicationContext context;
  
  @Autowired
  private MockOAuth2Filter mockOAuth2Filter;

  private ScimGroup engineers;
  private ScimUser lennon, lincoln, kennedy;

  List<ScimUser> members;

  @Before
  public void initTests() throws Exception {
    mvc = MockMvcBuilders.webAppContextSetup(context)
      .apply(springSecurity())
      .alwaysDo(log())
      .build();

    engineers = addTestGroup("engineers");
    lennon = addTestUser("john_lennon", "lennon@email.test", "John", "Lennon");
    lincoln = addTestUser("abraham_lincoln", "lincoln@email.test", "Abraham", "Lincoln");
    kennedy = addTestUser("jfk", "jfk@whitehouse.us", "John", "Kennedy");

    members = new ArrayList<ScimUser>();
    members.add(lennon);
    members.add(lincoln);
    members.add(kennedy);

    addMembers(engineers, members);
  }

  @After
  public void teardownTests() throws Exception {
    deleteScimResource(lennon);
    deleteScimResource(lincoln);
    deleteScimResource(kennedy);
    deleteScimResource(engineers);
    mockOAuth2Filter.cleanupSecurityContext();
  }

  @Test
  public void testGroupPatchRemoveMember() throws Exception {

    ScimGroupPatchRequest patchRemoveRequest =
        getPatchRemoveUsersRequest(Lists.newArrayList(lennon));

    ScimGroup engineersBeforeUpdate = getGroup(engineers.getMeta().getLocation());

    assertThat(engineersBeforeUpdate.getMembers().size(), equalTo(3));
    assertMembership(lennon, engineersBeforeUpdate, true);
    assertMembership(lincoln, engineersBeforeUpdate, true);
    assertMembership(kennedy, engineersBeforeUpdate, true);

    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
    }

    //@formatter:off
    mvc.perform(patch(engineers.getMeta().getLocation())
        .contentType(SCIM_CONTENT_TYPE)
        .content(objectMapper.writeValueAsString(patchRemoveRequest)))
      .andExpect(status().isNoContent());
    //@formatter:on

    ScimGroup engineersAfterUpdate = getGroup(engineers.getMeta().getLocation());

    assertThat(engineersAfterUpdate.getMembers().size(), equalTo(2));
    assertMembership(lennon, engineersAfterUpdate, false);
    assertMembership(lincoln, engineersAfterUpdate, true);
    assertMembership(kennedy, engineersAfterUpdate, true);

    final long dateBeforeUpdate = engineersBeforeUpdate.getMeta().getLastModified().getTime();
    final long dateAfterUpdate = engineersAfterUpdate.getMeta().getLastModified().getTime();

    assertThat(dateBeforeUpdate, lessThan(dateAfterUpdate));
  }

  @Test
  public void testGroupPatchRemoveMultipleMembers() throws Exception {

    List<ScimUser> membersToRemove = new ArrayList<ScimUser>();
    membersToRemove.add(lennon);
    membersToRemove.add(lincoln);

    ScimGroupPatchRequest patchReq = getPatchRemoveUsersRequest(membersToRemove);

    //@formatter:off
    mvc.perform(patch(engineers.getMeta().getLocation())
        .contentType(SCIM_CONTENT_TYPE)
        .content(objectMapper.writeValueAsString(patchReq)))
      .andExpect(status().isNoContent());
    //@formatter:on

    ScimGroup updatedGroup = getGroup(engineers.getMeta().getLocation());

    assertMembership(kennedy, updatedGroup, true);
    assertMembership(lennon, updatedGroup, false);
    assertMembership(lincoln, updatedGroup, false);
  }

  @Test
  public void testGroupPatchRemoveAllListOfMembers() throws Exception {

    List<ScimUser> membersToRemove = new ArrayList<ScimUser>();
    membersToRemove.add(lennon);
    membersToRemove.add(lincoln);
    membersToRemove.add(kennedy);

    ScimGroupPatchRequest patchReq = getPatchRemoveUsersRequest(membersToRemove);

    //@formatter:off
    mvc.perform(patch(engineers.getMeta().getLocation())
        .contentType(SCIM_CONTENT_TYPE)
        .content(objectMapper.writeValueAsString(patchReq)))
      .andExpect(status().isNoContent());
    //@formatter:on

    ScimGroup updatedGroup = getGroup(engineers.getMeta().getLocation());

    assertThat(updatedGroup.getMembers().isEmpty(), equalTo(true));
  }

  @Test
  public void testGroupPatchRemoveAllMembers() throws Exception {

    List<ScimUser> emptyMembers = new ArrayList<ScimUser>();
    ScimGroupPatchRequest patchReq = getPatchRemoveUsersRequest(emptyMembers);

    //@formatter:off
    mvc.perform(patch(engineers.getMeta().getLocation())
        .contentType(SCIM_CONTENT_TYPE)
        .content(objectMapper.writeValueAsString(patchReq)))
      .andExpect(status().isNoContent());
    //@formatter:on

    ScimGroup updatedGroup = getGroup(engineers.getMeta().getLocation());
    assertThat(updatedGroup.getMembers(), empty());
  }

  @Test
  public void testGroupPatchRemoveMemberTwice() throws Exception {

    ScimGroupPatchRequest patchRemoveRequest =
        getPatchRemoveUsersRequest(Lists.newArrayList(lennon));

    //@formatter:off
    mvc.perform(patch(engineers.getMeta().getLocation())
        .contentType(SCIM_CONTENT_TYPE)
        .content(objectMapper.writeValueAsString(patchRemoveRequest)))
      .andExpect(status().isNoContent());
    //@formatter:on

    ScimGroup engineersBeforeUpdate = getGroup(engineers.getMeta().getLocation());

    //@formatter:off
    mvc.perform(patch(engineers.getMeta().getLocation())
        .contentType(SCIM_CONTENT_TYPE)
        .content(objectMapper.writeValueAsString(patchRemoveRequest)))
      .andExpect(status().isNoContent());
    //@formatter:on

    ScimGroup engineersAfterUpdate = getGroup(engineers.getMeta().getLocation());

    assertMembership(lennon, engineersAfterUpdate, false);

    assertThat(engineersBeforeUpdate.getMeta().getLastModified(),
        equalTo(engineersAfterUpdate.getMeta().getLastModified()));
  }
}
