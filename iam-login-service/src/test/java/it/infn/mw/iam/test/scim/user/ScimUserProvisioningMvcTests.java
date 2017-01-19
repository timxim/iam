package it.infn.mw.iam.test.scim.user;

import static it.infn.mw.iam.test.scim.ScimUtils.SCIM_CLIENT_ID;
import static it.infn.mw.iam.test.scim.ScimUtils.SCIM_READ_SCOPE;
import static it.infn.mw.iam.test.scim.ScimUtils.SCIM_WRITE_SCOPE;
import static it.infn.mw.iam.test.scim.ScimUtils.buildUser;
import static it.infn.mw.iam.test.scim.ScimUtils.buildUserWithPassword;
import static it.infn.mw.iam.test.scim.ScimUtils.buildUserWithUUID;
import static it.infn.mw.iam.test.scim.ScimUtils.getUserLocation;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Optional;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.scim.model.ScimEmail.ScimEmailType;
import it.infn.mw.iam.api.scim.model.ScimGroupRef;
import it.infn.mw.iam.api.scim.model.ScimIndigoUser;
import it.infn.mw.iam.api.scim.model.ScimSshKey;
import it.infn.mw.iam.api.scim.model.ScimUser;
import it.infn.mw.iam.persistence.model.IamAccount;
import it.infn.mw.iam.persistence.repository.IamAccountRepository;
import it.infn.mw.iam.test.SshKeyUtils;
import it.infn.mw.iam.test.X509Utils;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.scim.ScimRestUtilsMvc;
import it.infn.mw.iam.test.scim.ScimUtils;
import it.infn.mw.iam.test.util.WithMockOAuthUser;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(
    classes = {IamLoginService.class, CoreControllerTestSupport.class, ScimRestUtilsMvc.class})
@WebAppConfiguration
public class ScimUserProvisioningMvcTests {

  @Autowired
  private IamAccountRepository accountRepo;

  PasswordEncoder encoder = new BCryptPasswordEncoder();

  @Autowired
  private ScimRestUtilsMvc restUtils;

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE})
  public void testGetUserNotFoundResponse() throws Exception {

    String randomUuid = UUID.randomUUID().toString();

    restUtils.getUser(randomUuid, HttpStatus.NOT_FOUND)
      .andExpect(jsonPath("$.status", equalTo("404")))
      .andExpect(jsonPath("$.detail", equalTo("No user mapped to id '" + randomUuid + "'")));
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testUpdateUserNotFoundResponse() throws Exception {

    String randomUuid = UUID.randomUUID().toString();

    ScimUser user = ScimUtils.buildUser("john_lennon", "lennon@email.test", "John", "Lennon");

    restUtils.putUser(randomUuid, user, HttpStatus.NOT_FOUND)
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

    ScimUser user = restUtils.getUser(uuid);

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
    assertThat(user.getGroups(), contains(productionRef, analysisRef));
    assertThat(user.getIndigoUser().getOidcIds(), hasSize(greaterThan(0)));
    assertThat(user.getIndigoUser().getOidcIds().get(0).getIssuer(),
        equalTo("https://accounts.google.com"));
    assertThat(user.getIndigoUser().getOidcIds().get(0).getSubject(),
        equalTo("105440632287425289613"));
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testUserCreationAccessDeletion() throws Exception {

    ScimUser user = buildUser("paul_mccartney", "test@email.test", "Paul", "McCartney");
    ScimUser createdUser = restUtils.postUser(user);

    assertThat(user.getUserName(), equalTo(createdUser.getUserName()));
    assertThat(user.getEmails(), hasSize(equalTo(1)));
    assertThat(user.getEmails().get(0).getValue(),
        equalTo(createdUser.getEmails().get(0).getValue()));

    restUtils.deleteUser(createdUser);
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testEmptyUsernameValidationError() throws Exception {

    ScimUser user = buildUser("", "test@email.test", "Paul", "McCartney");

    restUtils.postUser(user, HttpStatus.BAD_REQUEST)
      .andExpect(jsonPath("$.status", equalTo("400")))
      .andExpect(jsonPath("$.detail", containsString("scimUser.userName : may not be empty")));
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testEmptyEmailValidationError() throws Exception {

    ScimUser user = ScimUser.builder("paul").buildName("Paul", "McCartney").build();

    restUtils.postUser(user, HttpStatus.BAD_REQUEST)
      .andExpect(jsonPath("$.status", equalTo("400")))
      .andExpect(jsonPath("$.detail", containsString("scimUser.emails : may not be empty")));
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testInvalidEmailValidationError() throws Exception {

    ScimUser user = buildUser("paul", "this_is_not_an_email", "Paul", "McCartney");

    restUtils.postUser(user, HttpStatus.BAD_REQUEST)
      .andExpect(jsonPath("$.status", equalTo("400")))
      .andExpect(jsonPath("$.detail",
          containsString("scimUser.emails[0].value : not a well-formed email address")));
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testUserUpdateChangeUsername() throws Exception {

    ScimUser user = buildUser("john_lennon", "lennon@email.test", "John", "Lennon");

    ScimUser createdUser = restUtils.postUser(user);
    long creationTimeMs = createdUser.getMeta().getCreated().getTime();

    ScimUser userWithUpdates =
        buildUserWithUUID(createdUser.getId(), "j.lennon", user.getEmails().get(0).getValue(),
            user.getName().getGivenName(), user.getName().getFamilyName());

    ScimUser updatedUser = restUtils.putUser(userWithUpdates.getId(), userWithUpdates);

    assertThat(updatedUser.getUserName(), equalTo(userWithUpdates.getUserName()));
    long returnedTimeMs = updatedUser.getMeta().getCreated().getTime();

    // We need to do this since milliseconds are truncated for the creation time in MySQL
    assertThat(Math.abs(returnedTimeMs - creationTimeMs), Matchers.lessThan(Long.valueOf(1000)));

    restUtils.deleteUser(createdUser);
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testUpdateUserValidation() throws Exception {

    ScimUser user = buildUser("john_lennon", "lennon@email.test", "John", "Lennon");

    ScimUser createdUser = restUtils.postUser(user);

    ScimUser userWithUpdates = ScimUser.builder("j.lennon").id(user.getId()).active(true).build();

    restUtils.putUser(user.getId(), userWithUpdates, BAD_REQUEST)
      .andExpect(jsonPath("$.detail", containsString("scimUser.emails : may not be empty")));

    restUtils.deleteUser(createdUser);
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testNonExistentUserDeletionReturns404() throws Exception {

    restUtils.deleteUser(UUID.randomUUID().toString(), NOT_FOUND);

  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testUserCreationWithPassword() throws Exception {

    ScimUser user =
        buildUserWithPassword("john_lennon", "password", "lennon@email.test", "John", "Lennon");

    ScimUser createdUser = restUtils.postUser(user);

    assertNull(createdUser.getPassword());

    Optional<IamAccount> createdAccount = accountRepo.findByUuid(createdUser.getId());
    if (!createdAccount.isPresent()) {
      Assert.fail("Account not created");
    }

    assertThat(createdAccount.get().getPassword(), notNullValue());
    assertThat(encoder.matches("password", createdAccount.get().getPassword()), equalTo(true));

    restUtils.deleteUser(createdUser);
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testReplaceUserWithAlreadyUsedUsername() throws Exception {

    ScimUser lennon = buildUser("john_lennon", "lennon@email.test", "John", "Lennon");

    ScimUser lennonCreationResult = restUtils.postUser(lennon);

    ScimUser mccartney = buildUser("paul_mccartney", "test@email.test", "Paul", "McCartney");

    ScimUser mccartneyCreationResult = restUtils.postUser(mccartney);

    ScimUser lennonWantsToBeMcCartney = buildUserWithUUID(lennonCreationResult.getId(),
        mccartney.getUserName(), mccartney.getEmails().get(0).getValue(),
        mccartney.getName().getGivenName(), mccartney.getName().getFamilyName());

    restUtils.putUser(lennonCreationResult.getId(), lennonWantsToBeMcCartney, CONFLICT)
      .andExpect(jsonPath("$.detail",
          containsString("username paul_mccartney already assigned to another user")));

    restUtils.deleteUsers(lennonCreationResult, mccartneyCreationResult);
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testUserCreationWithOidcAccount() throws Exception {

    ScimUser user = ScimUser.builder("user_with_oidc")
      .buildEmail("test_user@test.org")
      .buildName("User", "With OIDC Account")
      .buildOidcId("urn:oidc:test:issuer", "1234")
      .active(true)
      .build();

    ScimUser createdUser = restUtils.postUser(user);
    assertNotNull(createdUser.getIndigoUser());
    assertThat(createdUser.getIndigoUser().getOidcIds(), hasSize(equalTo(1)));
    assertThat(createdUser.getIndigoUser().getOidcIds().get(0).getIssuer(),
        equalTo("urn:oidc:test:issuer"));
    assertThat(createdUser.getIndigoUser().getOidcIds().get(0).getSubject(), equalTo("1234"));

    createdUser = restUtils.getUser(createdUser.getId());
    assertNotNull(createdUser.getIndigoUser());
    assertThat(createdUser.getIndigoUser().getOidcIds(), hasSize(equalTo(1)));
    assertThat(createdUser.getIndigoUser().getOidcIds().get(0).getIssuer(),
        equalTo("urn:oidc:test:issuer"));
    assertThat(createdUser.getIndigoUser().getOidcIds().get(0).getSubject(), equalTo("1234"));

    restUtils.deleteUser(createdUser);
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testUserCreationWithStolenOidcAccountFailure() throws Exception {

    ScimUser user = ScimUser.builder("user_with_oidc")
      .buildEmail("test_user@test.org")
      .buildName("User", "With OIDC Account")
      .buildOidcId("urn:oidc:test:issuer", "1234")
      .active(true)
      .build();

    ScimUser createdUser = restUtils.postUser(user);
    assertNotNull(createdUser.getIndigoUser());
    assertThat(createdUser.getIndigoUser().getOidcIds(), hasSize(equalTo(1)));
    assertThat(createdUser.getIndigoUser().getOidcIds().get(0).getIssuer(),
        equalTo("urn:oidc:test:issuer"));
    assertThat(createdUser.getIndigoUser().getOidcIds().get(0).getSubject(), equalTo("1234"));

    ScimUser anotherUser = ScimUser.builder("another_user_with_oidc")
      .buildEmail("another_test_user@test.org")
      .buildName("Another User", "With OIDC Account")
      .buildOidcId("urn:oidc:test:issuer", "1234")
      .active(true)
      .build();

    restUtils.postUser(anotherUser, HttpStatus.CONFLICT)
      .andExpect(jsonPath("$.detail", containsString("already bounded to another user")));

    restUtils.deleteUser(createdUser);

  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testUserCreationWithSshKey() throws Exception {

    ScimUser user = ScimUser.builder("user_with_sshkey")
      .buildEmail("test_user@test.org")
      .buildName("User", "With ssh key Account")
      .buildSshKey("Personal", SshKeyUtils.sshKeys.get(0).key, null, true)
      .active(true)
      .build();

    ScimUser createdUser = restUtils.postUser(user);
    assertNotNull(createdUser.getIndigoUser());
    assertThat(createdUser.getIndigoUser().getSshKeys(), hasSize(equalTo(1)));
    assertThat(createdUser.getIndigoUser().getSshKeys().get(0).getDisplay(), equalTo("Personal"));
    assertThat(createdUser.getIndigoUser().getSshKeys().get(0).getValue(),
        equalTo(SshKeyUtils.sshKeys.get(0).key));
    assertThat(createdUser.getIndigoUser().getSshKeys().get(0).getFingerprint(),
        equalTo(SshKeyUtils.sshKeys.get(0).fingerprintSHA256));

    createdUser = restUtils.getUser(createdUser.getId());
    assertNotNull(createdUser.getIndigoUser());
    assertThat(createdUser.getIndigoUser().getSshKeys(), hasSize(equalTo(1)));
    assertThat(createdUser.getIndigoUser().getSshKeys().get(0).getDisplay(), equalTo("Personal"));
    assertThat(createdUser.getIndigoUser().getSshKeys().get(0).getValue(),
        equalTo(SshKeyUtils.sshKeys.get(0).key));
    assertThat(createdUser.getIndigoUser().getSshKeys().get(0).getFingerprint(),
        equalTo(SshKeyUtils.sshKeys.get(0).fingerprintSHA256));

    restUtils.deleteUser(createdUser);
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testUserCreationWithSshKeyValueOnly() throws Exception {

    ScimUser user = ScimUser.builder("user_with_sshkey")
      .buildEmail("test_user@test.org")
      .buildName("User", "With ssh key Account")
      .indigoUserInfo(ScimIndigoUser.builder()
        .addSshKey(ScimSshKey.builder().value(SshKeyUtils.sshKeys.get(0).key).build())
        .build())
      .active(true)
      .build();

    ScimUser createdUser = restUtils.postUser(user);
    assertNotNull(createdUser.getIndigoUser());
    assertThat(createdUser.getIndigoUser().getSshKeys(), hasSize(equalTo(1)));
    assertThat(createdUser.getIndigoUser().getSshKeys().get(0).getDisplay(),
        equalTo(user.getUserName() + "'s personal ssh key"));
    assertThat(createdUser.getIndigoUser().getSshKeys().get(0).getValue(),
        equalTo(SshKeyUtils.sshKeys.get(0).key));
    assertThat(createdUser.getIndigoUser().getSshKeys().get(0).getFingerprint(),
        equalTo(SshKeyUtils.sshKeys.get(0).fingerprintSHA256));

    createdUser = restUtils.getUser(createdUser.getId());
    assertNotNull(createdUser.getIndigoUser());
    assertThat(createdUser.getIndigoUser().getSshKeys(), hasSize(equalTo(1)));
    assertThat(createdUser.getIndigoUser().getSshKeys().get(0).getDisplay(),
        equalTo(user.getUserName() + "'s personal ssh key"));
    assertThat(createdUser.getIndigoUser().getSshKeys().get(0).getValue(),
        equalTo(SshKeyUtils.sshKeys.get(0).key));
    assertThat(createdUser.getIndigoUser().getSshKeys().get(0).getFingerprint(),
        equalTo(SshKeyUtils.sshKeys.get(0).fingerprintSHA256));

    restUtils.deleteUser(createdUser);
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testUserCreationWithStolenSshKeyFailure() throws Exception {

    ScimUser user = ScimUser.builder("user_with_sshkey")
      .buildEmail("test_user@test.org")
      .buildName("User", "With ssh key")
      .buildSshKey("Personal", SshKeyUtils.sshKeys.get(0).key, null, true)
      .active(true)
      .build();

    ScimUser createdUser = restUtils.postUser(user);
    assertNotNull(createdUser.getIndigoUser());
    assertThat(createdUser.getIndigoUser().getSshKeys(), hasSize(equalTo(1)));
    assertThat(createdUser.getIndigoUser().getSshKeys().get(0).getDisplay(), equalTo("Personal"));
    assertThat(createdUser.getIndigoUser().getSshKeys().get(0).getValue(),
        equalTo(SshKeyUtils.sshKeys.get(0).key));
    assertThat(createdUser.getIndigoUser().getSshKeys().get(0).getFingerprint(),
        equalTo(SshKeyUtils.sshKeys.get(0).fingerprintSHA256));

    ScimUser anotherUser = ScimUser.builder("another_user_with_sshkey")
      .buildEmail("another_test_user@test.org")
      .buildName("Another User", "With ssh key")
      .buildSshKey("Personal", SshKeyUtils.sshKeys.get(0).key, null, true)
      .active(true)
      .build();

    restUtils.postUser(anotherUser, HttpStatus.CONFLICT)
      .andExpect(jsonPath("$.detail", containsString("already bounded to another user")));

    restUtils.deleteUser(createdUser);

  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testUserCreationWithSamlId() throws Exception {

    ScimUser user = ScimUser.builder("user_with_samlId")
      .buildEmail("test_user@test.org")
      .buildName("User", "With saml id Account")
      .buildSamlId("IdpID", "UserID")
      .active(true)
      .build();

    ScimUser createdUser = restUtils.postUser(user);
    assertNotNull(createdUser.getIndigoUser());
    assertThat(createdUser.getIndigoUser().getSamlIds(), hasSize(equalTo(1)));
    assertThat(createdUser.getIndigoUser().getSamlIds().get(0).getIdpId(),
        equalTo(user.getIndigoUser().getSamlIds().get(0).getIdpId()));
    assertThat(createdUser.getIndigoUser().getSamlIds().get(0).getUserId(),
        equalTo(user.getIndigoUser().getSamlIds().get(0).getUserId()));

    createdUser = restUtils.getUser(createdUser.getId());
    assertNotNull(createdUser.getIndigoUser());
    assertThat(createdUser.getIndigoUser().getSamlIds(), hasSize(equalTo(1)));
    assertThat(createdUser.getIndigoUser().getSamlIds().get(0).getIdpId(),
        equalTo(user.getIndigoUser().getSamlIds().get(0).getIdpId()));
    assertThat(createdUser.getIndigoUser().getSamlIds().get(0).getUserId(),
        equalTo(user.getIndigoUser().getSamlIds().get(0).getUserId()));

    restUtils.deleteUser(createdUser);
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testUniqueUsernameCreationCheck() throws Exception {

    ScimUser user = buildUser("john_lennon", "lennon@email.test", "John", "Lennon");

    ScimUser createdUser = restUtils.postUser(user);
    restUtils.postUser(user, HttpStatus.CONFLICT);

    restUtils.deleteUser(createdUser);
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testUserCreationWithX509Certificate() throws Exception {

    ScimUser user = ScimUser.builder("user_with_x509")
      .buildEmail("test_user@test.org")
      .buildName("User", "With x509 Certificate")
      .buildX509Certificate("Personal1", X509Utils.x509Certs.get(0).certificate, false)
      .active(true)
      .build();

    ScimUser createdUser = restUtils.postUser(user);
    assertNotNull(createdUser.getX509Certificates());
    assertThat(createdUser.getX509Certificates(), hasSize(equalTo(1)));
    assertThat(createdUser.getX509Certificates().get(0).getDisplay(),
        equalTo(user.getX509Certificates().get(0).getDisplay()));
    assertThat(createdUser.getX509Certificates().get(0).getValue(),
        equalTo(user.getX509Certificates().get(0).getValue()));

    createdUser = restUtils.getUser(createdUser.getId());
    assertNotNull(createdUser.getX509Certificates());
    assertThat(createdUser.getX509Certificates(), hasSize(equalTo(1)));
    assertThat(createdUser.getX509Certificates().get(0).getDisplay(),
        equalTo(user.getX509Certificates().get(0).getDisplay()));
    assertThat(createdUser.getX509Certificates().get(0).getValue(),
        equalTo(user.getX509Certificates().get(0).getValue()));

    restUtils.deleteUser(createdUser);
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testUserCreationWithMultipleX509Certificate() throws Exception {

    ScimUser user = ScimUser.builder("user_with_x509")
      .buildEmail("test_user@test.org")
      .buildName("User", "With x509 Certificate")
      .buildX509Certificate("Personal1", X509Utils.x509Certs.get(0).certificate, false)
      .buildX509Certificate("Personal2", X509Utils.x509Certs.get(1).certificate, true)
      .active(true)
      .build();

    ScimUser createdUser = restUtils.postUser(user);
    assertNotNull(createdUser.getX509Certificates());
    assertThat(createdUser.getX509Certificates(), hasSize(equalTo(2)));
    assertThat(createdUser.getX509Certificates().get(0).getDisplay(),
        equalTo(user.getX509Certificates().get(0).getDisplay()));
    assertThat(createdUser.getX509Certificates().get(0).getValue(),
        equalTo(user.getX509Certificates().get(0).getValue()));
    assertThat(createdUser.getX509Certificates().get(0).isPrimary(),
        equalTo(user.getX509Certificates().get(0).isPrimary()));
    assertThat(createdUser.getX509Certificates().get(1).getDisplay(),
        equalTo(user.getX509Certificates().get(1).getDisplay()));
    assertThat(createdUser.getX509Certificates().get(1).isPrimary(),
        equalTo(user.getX509Certificates().get(1).isPrimary()));

    restUtils.deleteUser(createdUser);
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testUserCreationWithMultipleX509CertificateAndNoPrimary() throws Exception {

    ScimUser user = ScimUser.builder("user_with_x509")
      .buildEmail("test_user@test.org")
      .buildName("User", "With x509 Certificate")
      .buildX509Certificate("Personal1", X509Utils.x509Certs.get(0).certificate, false)
      .buildX509Certificate("Personal2", X509Utils.x509Certs.get(1).certificate, false)
      .active(true)
      .build();

    ScimUser createdUser = restUtils.postUser(user);
    assertNotNull(createdUser.getX509Certificates());
    assertThat(createdUser.getX509Certificates(), hasSize(equalTo(2)));
    assertThat(createdUser.getX509Certificates().get(0).getDisplay(),
        equalTo(user.getX509Certificates().get(0).getDisplay()));
    assertThat(createdUser.getX509Certificates().get(0).getValue(),
        equalTo(user.getX509Certificates().get(0).getValue()));
    assertThat(createdUser.getX509Certificates().get(0).isPrimary(), equalTo(true));
    assertThat(createdUser.getX509Certificates().get(1).getDisplay(),
        equalTo(user.getX509Certificates().get(1).getDisplay()));
    assertThat(createdUser.getX509Certificates().get(1).isPrimary(), equalTo(false));

    restUtils.deleteUser(createdUser);
  }


  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testEmailIsNotAlreadyLinkedOnCreate() throws Exception {

    ScimUser user0 = buildUser("test_same_email_0", "same_email@test.org", "Test", "Same Email 0");
    ScimUser user1 = buildUser("test_same_email_1", "same_email@test.org", "Test", "Same Email 1");

    user0 = restUtils.postUser(user0);

    restUtils.postUser(user1, HttpStatus.CONFLICT).andExpect(
        jsonPath("$.detail", containsString("email already assigned to an existing user")));

    restUtils.deleteUser(user0);

  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
  public void testEmailIsNotAlreadyLinkedOnUpdate() throws Exception {

    ScimUser user0 = buildUser("user0", "user0@test.org", "Test", "User 0");
    ScimUser user1 = buildUser("user1", "user1@test.org", "Test", "User 1");

    user0 = restUtils.postUser(user0);
    user1 = restUtils.postUser(user1);

    ScimUser updatedUser0 =
        buildUserWithUUID(user0.getId(), "user0", "user1@test.org", "Test", "User 0");

    restUtils.putUser(user0.getId(), updatedUser0, CONFLICT).andExpect(jsonPath("$.detail",
        containsString("email user1@test.org already assigned to another user")));

    restUtils.deleteUsers(user0, user1);
  }
}
