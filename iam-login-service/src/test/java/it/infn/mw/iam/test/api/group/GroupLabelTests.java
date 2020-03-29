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
package it.infn.mw.iam.test.api.group;

import static it.infn.mw.iam.api.group.GroupLabelsController.RESOURCE;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.List;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.common.LabelDTO;
import it.infn.mw.iam.persistence.repository.IamGroupRepository;
import it.infn.mw.iam.test.api.TestSupport;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.util.WithAnonymousUser;
import it.infn.mw.iam.test.util.WithMockOAuthUser;
import it.infn.mw.iam.test.util.oauth.MockOAuth2Filter;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class, CoreControllerTestSupport.class})
@WebAppConfiguration
@Transactional
@WithMockUser(username = "admin", roles = "ADMIN")
public class GroupLabelTests extends TestSupport {

  private static final ResultMatcher GROUP_NOT_FOUND_ERROR_MESSAGE =
      jsonPath("$.error", containsString("Group not found"));

  private static final String EXPECTED_GROUP_NOT_FOUND = "Expected group not found";

  @Autowired
  private IamGroupRepository repo;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private MockOAuth2Filter mockOAuth2Filter;

  @Autowired
  private ObjectMapper mapper;

  private MockMvc mvc;

  @Before
  public void setup() {

    mvc = MockMvcBuilders.webAppContextSetup(context)
      .apply(springSecurity())
      .alwaysDo(log())
      .build();
    mockOAuth2Filter.cleanupSecurityContext();
  }

  @After
  public void cleanupOAuthUser() {
    mockOAuth2Filter.cleanupSecurityContext();
  }

  private Supplier<AssertionError> assertionError(String message) {
    return () -> new AssertionError(message);
  }

  @Test
  @WithAnonymousUser
  public void managingLabelsRequiresAuthenticatedUser() throws Exception {

    mvc.perform(get(RESOURCE, TEST_001_GROUP_UUID)).andExpect(UNAUTHORIZED);

    mvc
      .perform(put(RESOURCE, TEST_001_GROUP_UUID).contentType(APPLICATION_JSON_UTF8)
        .content(mapper.writeValueAsString(TEST_LABEL)))
      .andExpect(UNAUTHORIZED);

    mvc.perform(delete(RESOURCE, TEST_001_GROUP_UUID).param("name", LABEL_NAME))
      .andExpect(UNAUTHORIZED);
  }

  @Test
  @WithMockUser(username = "test", roles = "USER")
  public void managingLabelsRequiresPrivilegedUser() throws Exception {

    mvc.perform(get(RESOURCE, TEST_001_GROUP_UUID)).andExpect(FORBIDDEN);

    mvc
      .perform(put(RESOURCE, TEST_001_GROUP_UUID).contentType(APPLICATION_JSON_UTF8)
        .content(mapper.writeValueAsString(TEST_LABEL)))
      .andExpect(FORBIDDEN);

    mvc.perform(delete(RESOURCE, TEST_001_GROUP_UUID).param("name", LABEL_NAME))
      .andExpect(FORBIDDEN);
  }

  @Test
  public void gettingLabelsWorksForAdminUser() throws Exception {

    mvc.perform(get(RESOURCE, TEST_001_GROUP_UUID)).andExpect(OK);

    mvc.perform(get(RESOURCE, TEST_001_GROUP_UUID))
      .andExpect(OK)
      .andExpect(jsonPath("$").isArray())
      .andExpect(jsonPath("$").isEmpty());
  }
  
  @Test
  @WithMockOAuthUser(user="admin", authorities= {"ROLE_ADMIN", "ROLE_USER"})
  public void gettingLabelsWorksForAdminOAuthUser() throws Exception {
    mvc.perform(get(RESOURCE, TEST_001_GROUP_UUID)).andExpect(OK);

    mvc.perform(get(RESOURCE, TEST_001_GROUP_UUID))
      .andExpect(OK)
      .andExpect(jsonPath("$").isArray())
      .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  public void setLabelWorks() throws Exception {

    mvc
      .perform(put(RESOURCE, TEST_001_GROUP_UUID).contentType(APPLICATION_JSON_UTF8)
        .content(mapper.writeValueAsString(TEST_LABEL)))
      .andExpect(OK);

    mvc.perform(get(RESOURCE, TEST_001_GROUP_UUID))
      .andExpect(OK)
      .andExpect(jsonPath("$").isArray())
      .andExpect(jsonPath("$[0].prefix", is(TEST_LABEL.getPrefix())))
      .andExpect(jsonPath("$[0].name", is(TEST_LABEL.getName())))
      .andExpect(jsonPath("$[0].value", is(TEST_LABEL.getValue())));

    // No value
    LabelDTO label = LabelDTO.builder().prefix(LABEL_PREFIX).name(LABEL_NAME).build();

    mvc
      .perform(put(RESOURCE, TEST_001_GROUP_UUID).contentType(APPLICATION_JSON_UTF8)
        .content(mapper.writeValueAsString(label)))
      .andExpect(OK);

    mvc.perform(get(RESOURCE, TEST_001_GROUP_UUID))
      .andExpect(OK)
      .andExpect(jsonPath("$").isArray())
      .andExpect(jsonPath("$", hasSize(1)))
      .andExpect(jsonPath("$[0].prefix", is(label.getPrefix())))
      .andExpect(jsonPath("$[0].name", is(label.getName())))
      .andExpect(jsonPath("$[0].value").doesNotExist());
  }

  @Test
  public void deleteLabelWorks() throws Exception {

    LabelDTO unqualified = LabelDTO.builder().name(LABEL_NAME).build();

    mvc
      .perform(put(RESOURCE, TEST_001_GROUP_UUID).contentType(APPLICATION_JSON_UTF8)
        .content(mapper.writeValueAsString(TEST_LABEL)))
      .andExpect(OK);

    mvc
      .perform(put(RESOURCE, TEST_001_GROUP_UUID).contentType(APPLICATION_JSON_UTF8)
        .content(mapper.writeValueAsString(unqualified)))
      .andExpect(OK);

    String response = mvc.perform(get(RESOURCE, TEST_001_GROUP_UUID))
      .andExpect(OK)
      .andExpect(jsonPath("$").isArray())
      .andExpect(jsonPath("$", hasSize(2)))
      .andReturn()
      .getResponse()
      .getContentAsString();

    List<LabelDTO> results = mapper.readValue(response, LIST_OF_LABEL_DTO);

    assertThat(results, hasItem(TEST_LABEL));
    assertThat(results, hasItem(unqualified));

    mvc.perform(delete(RESOURCE, TEST_001_GROUP_UUID).param("name", unqualified.getName()))
      .andExpect(NO_CONTENT);

    mvc.perform(get(RESOURCE, TEST_001_GROUP_UUID))
      .andExpect(OK)
      .andExpect(jsonPath("$").isArray())
      .andExpect(jsonPath("$", hasSize(1)))
      .andExpect(jsonPath("$[0].prefix", is(TEST_LABEL.getPrefix())))
      .andExpect(jsonPath("$[0].name", is(TEST_LABEL.getName())))
      .andExpect(jsonPath("$[0].value", is(TEST_LABEL.getValue())));

    mvc
      .perform(delete(RESOURCE, TEST_001_GROUP_UUID).param("name", TEST_LABEL.getName())
        .param("prefix", TEST_LABEL.getPrefix()))
      .andExpect(NO_CONTENT);

    mvc.perform(get(RESOURCE, TEST_001_GROUP_UUID))
      .andExpect(OK)
      .andExpect(jsonPath("$").isArray())
      .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  public void nonExistingResourceHandledCorrectly() throws Exception {
    mvc.perform(get(RESOURCE, RANDOM_UUID))
      .andExpect(NOT_FOUND)
      .andExpect(GROUP_NOT_FOUND_ERROR_MESSAGE);

    mvc
      .perform(put(RESOURCE, RANDOM_UUID).contentType(APPLICATION_JSON_UTF8)
        .content(mapper.writeValueAsString(TEST_LABEL)))
      .andExpect(NOT_FOUND)
      .andExpect(GROUP_NOT_FOUND_ERROR_MESSAGE);

    mvc
      .perform(delete(RESOURCE, RANDOM_UUID).param("name", TEST_LABEL.getName())
        .param("prefix", TEST_LABEL.getPrefix()))
      .andExpect(NOT_FOUND)
      .andExpect(GROUP_NOT_FOUND_ERROR_MESSAGE);
  }

  @Test
  public void multipleLabelsHandledCorrectly() throws Exception {
    List<LabelDTO> labels = Lists.newArrayList();

    for (int i = 0; i < 10; i++) {
      labels
        .add(LabelDTO.builder().prefix(LABEL_PREFIX).name("name." + i).value("value." + i).build());
    }

    for (LabelDTO l : labels) {
      mvc
        .perform(put(RESOURCE, TEST_001_GROUP_UUID).contentType(APPLICATION_JSON_UTF8)
          .content(mapper.writeValueAsString(l)))
        .andExpect(OK);

      mvc
        .perform(put(RESOURCE, TEST_002_GROUP_UUID).contentType(APPLICATION_JSON_UTF8)
          .content(mapper.writeValueAsString(l)))
        .andExpect(OK);
    }

    for (String uuid : asList(TEST_001_GROUP_UUID, TEST_002_GROUP_UUID)) {
      String resultString = mvc.perform(get(RESOURCE, uuid))
        .andExpect(OK)
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$", hasSize(10)))
        .andReturn()
        .getResponse()
        .getContentAsString();

      List<LabelDTO> results = mapper.readValue(resultString, LIST_OF_LABEL_DTO);
      labels.forEach(l -> assertThat(results, hasItem(l)));
    }

    repo.delete(
        repo.findByUuid(TEST_001_GROUP_UUID).orElseThrow(assertionError(EXPECTED_GROUP_NOT_FOUND)));

    mvc.perform(get(RESOURCE, TEST_001_GROUP_UUID))
      .andExpect(NOT_FOUND)
      .andExpect(GROUP_NOT_FOUND_ERROR_MESSAGE);

    String resultString = mvc.perform(get(RESOURCE, TEST_002_GROUP_UUID))
      .andExpect(OK)
      .andExpect(jsonPath("$").isArray())
      .andExpect(jsonPath("$", hasSize(10)))
      .andReturn()
      .getResponse()
      .getContentAsString();

    List<LabelDTO> results = mapper.readValue(resultString, LIST_OF_LABEL_DTO);
    labels.forEach(l -> assertThat(results, hasItem(l)));

  }

  @Test
  public void labelValidationTests() throws Exception {

    final String[] SOME_INVALID_PREFIXES = {"aword", "-starts-with-dash.com", "ends-with-dash-.com",
        "contains_underscore.org", "contains/slashes.org"};

    for (String p : SOME_INVALID_PREFIXES) {
      LabelDTO l = LabelDTO.builder().prefix(p).value(LABEL_VALUE).name(LABEL_NAME).build();
      mvc
        .perform(put(RESOURCE, TEST_001_GROUP_UUID).contentType(APPLICATION_JSON_UTF8)
          .content(mapper.writeValueAsString(l)))
        .andExpect(BAD_REQUEST)
        .andExpect(INVALID_PREFIX_ERROR_MESSAGE);
    }

    LabelDTO noNameLabel = LabelDTO.builder().prefix(LABEL_PREFIX).build();

    mvc
      .perform(put(RESOURCE, TEST_001_GROUP_UUID).contentType(APPLICATION_JSON_UTF8)
        .content(mapper.writeValueAsString(noNameLabel)))
      .andExpect(BAD_REQUEST)
      .andExpect(NAME_REQUIRED_ERROR_MESSAGE);

    final String SOME_INVALID_NAMES[] = {"-pippo", "/ciccio/paglia", ".starts-with-dot"};

    for (String in : SOME_INVALID_NAMES) {
      LabelDTO invalidNameLabel = LabelDTO.builder().prefix(LABEL_PREFIX).name(in).build();
      mvc
        .perform(put(RESOURCE, TEST_001_GROUP_UUID).contentType(APPLICATION_JSON_UTF8)
          .content(mapper.writeValueAsString(invalidNameLabel)))
        .andExpect(BAD_REQUEST)
        .andExpect(INVALID_NAME_ERROR_MESSAGE);
    }

    LabelDTO longNameLabel =
        LabelDTO.builder().prefix(LABEL_PREFIX).name(randomAlphabetic(65)).build();

    mvc
      .perform(put(RESOURCE, TEST_001_GROUP_UUID).contentType(APPLICATION_JSON_UTF8)
        .content(mapper.writeValueAsString(longNameLabel)))
      .andExpect(BAD_REQUEST)
      .andExpect(NAME_TOO_LONG_ERROR_MESSAGE);


    LabelDTO longValueLabel = LabelDTO.builder()
      .prefix(LABEL_PREFIX)
      .name(LABEL_NAME)
      .value(randomAlphabetic(257))
      .build();

    mvc
      .perform(put(RESOURCE, TEST_001_GROUP_UUID).contentType(APPLICATION_JSON_UTF8)
        .content(mapper.writeValueAsString(longValueLabel)))
      .andExpect(BAD_REQUEST)
      .andExpect(VALUE_TOO_LONG_ERROR_MESSAGE);

  }
}
