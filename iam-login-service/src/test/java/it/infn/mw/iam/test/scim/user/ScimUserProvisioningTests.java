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
package it.infn.mw.iam.test.scim.user;

import static it.infn.mw.iam.test.scim.ScimUtils.SCIM_CLIENT_ID;
import static it.infn.mw.iam.test.scim.ScimUtils.SCIM_READ_SCOPE;
import static it.infn.mw.iam.test.scim.ScimUtils.SCIM_WRITE_SCOPE;
import static it.infn.mw.iam.test.scim.ScimUtils.buildUser;
import static it.infn.mw.iam.test.scim.ScimUtils.buildUserWithUUID;
import static it.infn.mw.iam.test.scim.ScimUtils.getUserLocation;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.scim.model.ScimEmail.ScimEmailType;
import it.infn.mw.iam.api.scim.model.ScimGroupRef;
import it.infn.mw.iam.api.scim.model.ScimUser;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.scim.ScimRestUtilsMvc;
import it.infn.mw.iam.test.scim.ScimUtils;
import it.infn.mw.iam.test.util.WithMockOAuthUser;
import it.infn.mw.iam.test.util.oauth.MockOAuth2Filter;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(
    classes = {IamLoginService.class, CoreControllerTestSupport.class, ScimRestUtilsMvc.class})
@WebAppConfiguration
@Transactional
public class ScimUserProvisioningTests extends ScimUserTestSupport {

  @Autowired
  private ScimRestUtilsMvc scimUtils;
  
  @Autowired
  private MockOAuth2Filter mockOAuth2Filter;
  
  @After
  public void teardown() throws Exception {
    mockOAuth2Filter.cleanupSecurityContext();
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE})
  public void testGetUserNotFoundResponse() throws Exception {

    String randomUuid = getRandomUUid();

    scimUtils.getUser(randomUuid, HttpStatus.NOT_FOUND)
      .andExpect(jsonPath("$.status", equalTo("404")))
      .andExpect(jsonPath("$.detail", equalTo("No user mapped to id '" + randomUuid + "'")));
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testUpdateUserNotFoundResponse() throws Exception {

    String randomUuid = getRandomUUid();
    ScimUser user = ScimUtils.buildUser("john_lennon", "lennon@email.test", "John", "Lennon");

    scimUtils.putUser(randomUuid, user, HttpStatus.NOT_FOUND)
      .andExpect(jsonPath("$.status", equalTo("404")))
      .andExpect(jsonPath("$.detail", equalTo("No user mapped to id '" + randomUuid + "'")));
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE})
  public void testExistingUserAccess() throws Exception {

    // Some existing user as defined in the test db
    String uuid = "80e5fb8d-b7c8-451a-89ba-346ae278a66f";

    ScimGroupRef productionRef = ScimGroupRef.builder()
      .display("Production")
      .ref("http://localhost:8080/scim/Groups/c617d586-54e6-411d-8e38-64967798fa8a")
      .value("c617d586-54e6-411d-8e38-64967798fa8a")
      .build();

    ScimGroupRef analysisRef = ScimGroupRef.builder()
      .display("Analysis")
      .ref("http://localhost:8080/scim/Groups/6a384bcd-d4b3-4b7f-a2fe-7d897ada0dd1")
      .value("6a384bcd-d4b3-4b7f-a2fe-7d897ada0dd1")
      .build();

    ScimUser user = scimUtils.getUser(uuid);

    assertThat(user.getId(), equalTo(uuid));
    assertThat(user.getUserName(), equalTo("test"));
    assertThat(user.getDisplayName(), equalTo("test"));
    assertThat(user.getActive(), equalTo(true));
    assertThat(user.getName().getFamilyName(), equalTo("User"));
    assertThat(user.getName().getGivenName(), equalTo("Test"));
    assertThat(user.getName().getFormatted(), equalTo("Test User"));
    assertThat(user.getMeta().getResourceType(), equalTo("User"));
    assertThat(user.getMeta().getLocation(),
        equalTo("http://localhost:8080" + getUserLocation(uuid)));
    assertThat(user.getEmails(), hasSize(equalTo(1)));
    assertThat(user.getEmails().get(0).getValue(), equalTo("test@iam.test"));
    assertThat(user.getEmails().get(0).getType(), equalTo(ScimEmailType.work));
    assertThat(user.getEmails().get(0).getPrimary(), equalTo(true));
    assertThat(user.getGroups(), hasSize(equalTo(2)));
    assertThat(user.getGroups(), contains(analysisRef, productionRef));
    assertThat(user.getIndigoUser().getOidcIds(), hasSize(greaterThan(0)));
    assertThat(user.getIndigoUser().getOidcIds().get(0).getIssuer(),
        equalTo("https://accounts.google.com"));
    assertThat(user.getIndigoUser().getOidcIds().get(0).getSubject(),
        equalTo("105440632287425289613"));
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testEmptyUsernameValidationError() throws Exception {

    ScimUser user = buildUser("", "test@email.test", "Paul", "McCartney");

    scimUtils.postUser(user, HttpStatus.BAD_REQUEST)
      .andExpect(jsonPath("$.status", equalTo("400")))
      .andExpect(jsonPath("$.detail", containsString("scimUser.userName : may not be empty")));
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testEmptyEmailValidationError() throws Exception {

    ScimUser user = ScimUser.builder("paul").buildName("Paul", "McCartney").build();

    scimUtils.postUser(user, HttpStatus.BAD_REQUEST)
      .andExpect(jsonPath("$.status", equalTo("400")))
      .andExpect(jsonPath("$.detail", containsString("scimUser.emails : may not be empty")));
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testInvalidEmailValidationError() throws Exception {

    ScimUser user = buildUser("paul", "this_is_not_an_email", "Paul", "McCartney");

    scimUtils.postUser(user, HttpStatus.BAD_REQUEST)
      .andExpect(jsonPath("$.status", equalTo("400")))
      .andExpect(jsonPath("$.detail",
          containsString("Please provide a valid email address")));
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testUserUpdateChangeUsername() throws Exception {

    ScimUser user = buildUser("john_lennon", "lennon@email.test", "John", "Lennon");

    ScimUser createdUser = scimUtils.postUser(user);
    long creationTimeMs = createdUser.getMeta().getCreated().getTime();

    ScimUser userWithUpdates =
        buildUserWithUUID(createdUser.getId(), "j.lennon", user.getEmails().get(0).getValue(),
            user.getName().getGivenName(), user.getName().getFamilyName());

    ScimUser updatedUser = scimUtils.putUser(userWithUpdates.getId(), userWithUpdates);

    assertThat(updatedUser.getUserName(), equalTo(userWithUpdates.getUserName()));
    long returnedTimeMs = updatedUser.getMeta().getCreated().getTime();

    // We need to do this since milliseconds are truncated for the creation time in MySQL
    assertThat(Math.abs(returnedTimeMs - creationTimeMs), Matchers.lessThan(Long.valueOf(1000)));
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testUpdateUserValidation() throws Exception {

    ScimUser user = buildUser("john_lennon", "lennon@email.test", "John", "Lennon");

    scimUtils.postUser(user);

    ScimUser userWithUpdates = ScimUser.builder("j.lennon").id(user.getId()).active(true).build();

    scimUtils.putUser(user.getId(), userWithUpdates, BAD_REQUEST)
      .andExpect(jsonPath("$.detail", containsString("scimUser.emails : may not be empty")));
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testNonExistentUserDeletionReturns404() throws Exception {

    scimUtils.deleteUser(getRandomUUid(), NOT_FOUND);
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testReplaceUserWithAlreadyUsedUsername() throws Exception {

    ScimUser lennon = buildUser("john_lennon", "lennon@email.test", "John", "Lennon");

    ScimUser lennonCreationResult = scimUtils.postUser(lennon);

    ScimUser mccartney = buildUser("paul_mccartney", "test@email.test", "Paul", "McCartney");

    scimUtils.postUser(mccartney);

    ScimUser lennonWantsToBeMcCartney = buildUserWithUUID(lennonCreationResult.getId(),
        mccartney.getUserName(), mccartney.getEmails().get(0).getValue(),
        mccartney.getName().getGivenName(), mccartney.getName().getFamilyName());

    scimUtils.putUser(lennonCreationResult.getId(), lennonWantsToBeMcCartney, CONFLICT)
      .andExpect(jsonPath("$.detail",
          containsString("username paul_mccartney already assigned to another user")));
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testUniqueUsernameCreationCheck() throws Exception {

    ScimUser user = buildUser("john_lennon", "lennon@email.test", "John", "Lennon");

    scimUtils.postUser(user);
    scimUtils.postUser(user, HttpStatus.CONFLICT);
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testEmailIsNotAlreadyLinkedOnCreate() throws Exception {

    ScimUser user0 = buildUser("test_same_email_0", "same_email@test.org", "Test", "Same Email 0");
    ScimUser user1 = buildUser("test_same_email_1", "same_email@test.org", "Test", "Same Email 1");

    user0 = scimUtils.postUser(user0);

    //@formatter:off
    scimUtils.postUser(user1, HttpStatus.CONFLICT)
      .andExpect(jsonPath("$.detail", containsString("A user linked with email 'same_email@test.org' already exists")));
    //@formatter:on
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testEmailIsNotAlreadyLinkedOnUpdate() throws Exception {

    ScimUser user0 = buildUser("user0", "user0@test.org", "Test", "User 0");
    ScimUser user1 = buildUser("user1", "user1@test.org", "Test", "User 1");

    user0 = scimUtils.postUser(user0);
    user1 = scimUtils.postUser(user1);

    ScimUser updatedUser0 =
        buildUserWithUUID(user0.getId(), "user0", "user1@test.org", "Test", "User 0");

    scimUtils.putUser(user0.getId(), updatedUser0, CONFLICT).andExpect(jsonPath("$.detail",
        containsString("email user1@test.org already assigned to another user")));
  }
}
