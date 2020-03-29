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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.infn.mw.iam.api.scim.model.ScimGroup;
import it.infn.mw.iam.api.scim.model.ScimGroupPatchRequest;
import it.infn.mw.iam.api.scim.model.ScimResource;
import it.infn.mw.iam.api.scim.model.ScimUser;
import it.infn.mw.iam.test.TestUtils;
import it.infn.mw.iam.test.scim.ScimUtils;

public class ScimGroupPatchUtils {

  public final static String GROUP_URI = ScimUtils.getGroupsLocation();
  public final static String USER_URI = ScimUtils.getUsersLocation();

  @Autowired
  protected ObjectMapper objectMapper;

  protected MockMvc mvc;

  protected ScimGroupPatchRequest getPatchAddUsersRequest(List<ScimUser> users) {

    return ScimGroupPatchRequest.builder().add(TestUtils.buildScimMemberRefList(users)).build();
  }

  protected ScimGroupPatchRequest getPatchRemoveUsersRequest(List<ScimUser> users) {

    return ScimGroupPatchRequest.builder().remove(TestUtils.buildScimMemberRefList(users)).build();
  }

  protected ScimGroupPatchRequest getPatchReplaceUsersRequest(List<ScimUser> users) {

    return ScimGroupPatchRequest.builder().replace(TestUtils.buildScimMemberRefList(users)).build();
  }

  protected ScimGroup addTestGroup(String displayName) throws Exception {

    ScimGroup group = ScimGroup.builder(displayName).build();
    String result = mvc
      .perform(post(GROUP_URI).contentType(SCIM_CONTENT_TYPE)
        .content(objectMapper.writeValueAsString(group)))
      .andExpect(status().isCreated())
      .andReturn()
      .getResponse()
      .getContentAsString();

    return objectMapper.readValue(result, ScimGroup.class);
  }

  protected ScimUser addTestUser(String userName, String email, String firstName, String LastName)
      throws Exception {

    ScimUser user =
        ScimUser.builder(userName).buildEmail(email).buildName(firstName, LastName).build();

    String result = mvc
      .perform(post(USER_URI).contentType(SCIM_CONTENT_TYPE)
        .content(objectMapper.writeValueAsString(user)))
      .andExpect(status().isCreated())
      .andReturn()
      .getResponse()
      .getContentAsString();

    return objectMapper.readValue(result, ScimUser.class);
  }

  protected void addMembers(ScimGroup group, List<ScimUser> members) throws Exception {

    ScimGroupPatchRequest patchAddReq = getPatchAddUsersRequest(members);

    mvc.perform(patch(group.getMeta().getLocation()).contentType(SCIM_CONTENT_TYPE)
      .content(objectMapper.writeValueAsString(patchAddReq))).andExpect(status().isNoContent());

    ScimGroup g = getGroup(group.getMeta().getLocation());

    assertThat(g.getMembers(), hasItem(TestUtils.getMemberRef(members.get(0))));
    assertThat(g.getMembers(), hasItem(TestUtils.getMemberRef(members.get(1))));
    assertThat(g.getMembers(), hasItem(TestUtils.getMemberRef(members.get(2))));
  }

  protected void assertMembership(ScimUser user, ScimGroup group, boolean isMember) {

    assertThat(group.getMembers().stream().anyMatch(m -> m.getValue().equals(user.getId())),
        equalTo(isMember));
  }

  protected ScimGroup getGroup(String location) throws Exception {
    String result = mvc.perform(get(location).contentType(SCIM_CONTENT_TYPE))
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();
    return objectMapper.readValue(result, ScimGroup.class);
  }

  protected void deleteScimResource(ScimResource resource) throws Exception {
    //@formatter:off
    mvc.perform(delete(resource.getMeta().getLocation()))
      .andExpect(status().isNoContent());
    //@formatter:on
  }
}
