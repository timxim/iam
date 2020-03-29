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
package it.infn.mw.iam.test.api.account.search;

import static it.infn.mw.iam.api.account.search.AbstractSearchController.DEFAULT_ITEMS_PER_PAGE;
import static it.infn.mw.iam.api.account.search.GroupSearchController.GROUP_SEARCH_ENDPOINT;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.common.ListResponseDTO;
import it.infn.mw.iam.api.common.OffsetPageable;
import it.infn.mw.iam.api.scim.converter.GroupConverter;
import it.infn.mw.iam.api.scim.model.ScimGroup;
import it.infn.mw.iam.persistence.model.IamGroup;
import it.infn.mw.iam.persistence.repository.IamGroupRepository;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.util.WithMockOAuthUser;
import it.infn.mw.iam.test.util.oauth.MockOAuth2Filter;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class, CoreControllerTestSupport.class})
@WebAppConfiguration
public class GroupSearchControllerTests {

  public static final String APPLICATION_JSON_CONTENT_TYPE = "application/json";

  @Autowired
  private WebApplicationContext context;

  private MockMvc mvc;

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private MockOAuth2Filter mockOAuth2Filter;

  @Autowired
  private IamGroupRepository groupRepository;

  @Autowired
  private GroupConverter scimGroupConverter;

  @Before
  public void setup() {
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mockOAuth2Filter.cleanupSecurityContext();
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).alwaysDo(log())
        .build();
  }

  @After
  public void teardown() {
    mockOAuth2Filter.cleanupSecurityContext();
  }

  @Test
  @WithMockOAuthUser(user = "admin", authorities = {"ROLE_ADMIN"})
  public void getFirstPageOfAllGroups() throws JsonParseException, JsonMappingException,
      UnsupportedEncodingException, IOException, Exception {

    long expectedSize = groupRepository.count();

    ListResponseDTO<ScimGroup> response = mapper.readValue(
        mvc.perform(get(GROUP_SEARCH_ENDPOINT).contentType(APPLICATION_JSON_CONTENT_TYPE))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
        new TypeReference<ListResponseDTO<ScimGroup>>() {});
    assertThat(response.getTotalResults(), equalTo(expectedSize));
    assertThat(response.getResources().size(), equalTo(DEFAULT_ITEMS_PER_PAGE));
    assertThat(response.getStartIndex(), equalTo(1));
    assertThat(response.getItemsPerPage(), equalTo(DEFAULT_ITEMS_PER_PAGE));
  }

  @Test
  @WithMockOAuthUser(user = "admin", authorities = {"ROLE_ADMIN"})
  public void getSecondPageOfAllGroups() throws JsonParseException, JsonMappingException,
      UnsupportedEncodingException, IOException, Exception {

    long expectedSize = groupRepository.count();

    ListResponseDTO<ScimGroup> response = mapper.readValue(
        mvc.perform(get(GROUP_SEARCH_ENDPOINT).contentType(APPLICATION_JSON_CONTENT_TYPE)
            .param("startIndex", String.valueOf(DEFAULT_ITEMS_PER_PAGE))).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(),
        new TypeReference<ListResponseDTO<ScimGroup>>() {});
    assertThat(response.getTotalResults(), equalTo(expectedSize));
    assertThat(response.getResources().size(), equalTo(DEFAULT_ITEMS_PER_PAGE));
    assertThat(response.getStartIndex(), equalTo(DEFAULT_ITEMS_PER_PAGE));
    assertThat(response.getItemsPerPage(), equalTo(DEFAULT_ITEMS_PER_PAGE));
  }

  @Test
  @WithMockOAuthUser(user = "admin", authorities = {"ROLE_ADMIN"})
  public void getGroupsWithCustomStartIndexAndCount() throws JsonParseException,
      JsonMappingException, UnsupportedEncodingException, IOException, Exception {

    long expectedSize = groupRepository.count();
    int startIndex = 3;
    int count = 2;

    ListResponseDTO<ScimGroup> response = mapper.readValue(
        mvc.perform(get(GROUP_SEARCH_ENDPOINT).contentType(APPLICATION_JSON_CONTENT_TYPE)
            .param("startIndex", String.valueOf(startIndex)).param("count", String.valueOf(count)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
        new TypeReference<ListResponseDTO<ScimGroup>>() {});
    assertThat(response.getTotalResults(), equalTo(expectedSize));
    assertThat(response.getResources().size(), equalTo(count));
    assertThat(response.getStartIndex(), equalTo(startIndex));
    assertThat(response.getItemsPerPage(), equalTo(count));
  }

  @Test
  @WithMockOAuthUser(user = "admin", authorities = {"ROLE_ADMIN"})
  public void getCountOfAllGroups() throws JsonParseException, JsonMappingException,
      UnsupportedEncodingException, IOException, Exception {

    long expectedSize = groupRepository.count();

    ListResponseDTO<ScimGroup> response = mapper.readValue(mvc
        .perform(get(GROUP_SEARCH_ENDPOINT).contentType(APPLICATION_JSON_CONTENT_TYPE)
            .param("count", "0"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
        new TypeReference<ListResponseDTO<ScimGroup>>() {});
    assertThat(response.getTotalResults(), equalTo(expectedSize));
    assertThat(response.getResources(), equalTo(null));
    assertThat(response.getStartIndex(), equalTo(null));
    assertThat(response.getItemsPerPage(), equalTo(null));
  }

  @Test
  @WithMockOAuthUser(user = "admin", authorities = {"ROLE_ADMIN"})
  public void getFirstFilteredPageOfGroups() throws JsonParseException, JsonMappingException,
      UnsupportedEncodingException, IOException, Exception {

    final String filter = "duction";
    OffsetPageable op = new OffsetPageable(0, 10);
    Page<IamGroup> page = groupRepository.findByNameIgnoreCaseContainingOrUuidIgnoreCaseContaining(filter, filter, op);

    ListResponseDTO<ScimGroup> response = mapper.readValue(mvc
        .perform(get(GROUP_SEARCH_ENDPOINT).contentType(APPLICATION_JSON_CONTENT_TYPE)
            .param("filter", filter))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
        new TypeReference<ListResponseDTO<ScimGroup>>() {});

    assertThat(response.getResources().size(), equalTo(1));
    assertThat(response.getStartIndex(), equalTo(1));
    assertThat(response.getItemsPerPage(), equalTo(1));

    List<ScimGroup> expectedGroups = Lists.newArrayList();
    page.getContent().forEach(g -> expectedGroups.add(scimGroupConverter.dtoFromEntity(g)));
    assertThat(response.getResources().containsAll(expectedGroups), equalTo(true));
  }

  @Test
  @WithMockOAuthUser(user = "admin", authorities = {"ROLE_ADMIN"})
  public void getCountOfFilteredGroups() throws JsonParseException, JsonMappingException,
      UnsupportedEncodingException, IOException, Exception {

    final String filter = "duction";
    long expectedSize = 1;

    ListResponseDTO<ScimGroup> response =
        mapper.readValue(
            mvc.perform(get(GROUP_SEARCH_ENDPOINT).contentType(APPLICATION_JSON_CONTENT_TYPE)
                .param("count", "0").param("filter", filter)).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString(),
            new TypeReference<ListResponseDTO<ScimGroup>>() {});
    assertThat(response.getTotalResults(), equalTo(expectedSize));
    assertThat(response.getResources(), equalTo(null));
    assertThat(response.getStartIndex(), equalTo(null));
    assertThat(response.getItemsPerPage(), equalTo(null));
  }

  @Test
  public void getGroupsAsAnonymousUser() throws Exception {
    mvc.perform(get(GROUP_SEARCH_ENDPOINT).contentType(APPLICATION_JSON_CONTENT_TYPE))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockOAuthUser(user = "admin", authorities = {"ROLE_ADMIN"})
  public void getGroupsWithNegativeStartIndex() throws JsonParseException, JsonMappingException,
      UnsupportedEncodingException, IOException, Exception {

    long expectedSize = groupRepository.count();

    ListResponseDTO<ScimGroup> response = mapper.readValue(mvc
        .perform(get(GROUP_SEARCH_ENDPOINT).contentType(APPLICATION_JSON_CONTENT_TYPE)
            .param("startIndex", "-1"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
        new TypeReference<ListResponseDTO<ScimGroup>>() {});
    assertThat(response.getTotalResults(), equalTo(expectedSize));
    assertThat(response.getResources().size(), equalTo(DEFAULT_ITEMS_PER_PAGE));
    assertThat(response.getStartIndex(), equalTo(1));
    assertThat(response.getItemsPerPage(), equalTo(DEFAULT_ITEMS_PER_PAGE));
  }

  @Test
  @WithMockOAuthUser(user = "admin", authorities = {"ROLE_ADMIN"})
  public void getGroupsWithStartIndexZero() throws JsonParseException, JsonMappingException,
      UnsupportedEncodingException, IOException, Exception {

    long expectedSize = groupRepository.count();

    ListResponseDTO<ScimGroup> response = mapper.readValue(mvc
        .perform(get(GROUP_SEARCH_ENDPOINT).contentType(APPLICATION_JSON_CONTENT_TYPE)
            .param("startIndex", "0"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
        new TypeReference<ListResponseDTO<ScimGroup>>() {});
    assertThat(response.getTotalResults(), equalTo(expectedSize));
    assertThat(response.getResources().size(), equalTo(DEFAULT_ITEMS_PER_PAGE));
    assertThat(response.getStartIndex(), equalTo(1));
    assertThat(response.getItemsPerPage(), equalTo(DEFAULT_ITEMS_PER_PAGE));
  }

  @Test
  @WithMockOAuthUser(user = "admin", authorities = {"ROLE_ADMIN"})
  public void getGroupsWithCountBiggerThanPageSize() throws JsonParseException, JsonMappingException,
      UnsupportedEncodingException, IOException, Exception {

    long expectedSize = groupRepository.count();

    ListResponseDTO<ScimGroup> response =
        mapper.readValue(
            mvc.perform(get(GROUP_SEARCH_ENDPOINT).contentType(APPLICATION_JSON_CONTENT_TYPE)
                .param("count", "" + DEFAULT_ITEMS_PER_PAGE * 2)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
            new TypeReference<ListResponseDTO<ScimGroup>>() {});
    assertThat(response.getTotalResults(), equalTo(expectedSize));
    assertThat(response.getResources().size(), equalTo(DEFAULT_ITEMS_PER_PAGE));
    assertThat(response.getResources().get(0).getIndigoGroup(), not(nullValue()));
    assertThat(response.getStartIndex(), equalTo(1));
    assertThat(response.getItemsPerPage(), equalTo(DEFAULT_ITEMS_PER_PAGE));
  }

  @Test
  @WithMockOAuthUser(user = "admin", authorities = {"ROLE_ADMIN"})
  public void getGroupsWithNegativeCount() throws JsonParseException, JsonMappingException,
      UnsupportedEncodingException, IOException, Exception {

    long expectedSize = groupRepository.count();

    ListResponseDTO<ScimGroup> response = mapper.readValue(mvc
        .perform(get(GROUP_SEARCH_ENDPOINT).contentType(APPLICATION_JSON_CONTENT_TYPE)
            .param("count", "-1"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
        new TypeReference<ListResponseDTO<ScimGroup>>() {});
    assertThat(response.getTotalResults(), equalTo(expectedSize));
    assertThat(response.getResources().size(), equalTo(DEFAULT_ITEMS_PER_PAGE));
    assertThat(response.getStartIndex(), equalTo(1));
    assertThat(response.getItemsPerPage(), equalTo(DEFAULT_ITEMS_PER_PAGE));
  }
}
