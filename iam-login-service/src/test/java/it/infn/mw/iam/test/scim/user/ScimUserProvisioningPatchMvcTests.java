package it.infn.mw.iam.test.scim.user;

import static it.infn.mw.iam.api.scim.model.ScimPatchOperation.ScimPatchOperationType.add;
import static it.infn.mw.iam.api.scim.model.ScimPatchOperation.ScimPatchOperationType.remove;
import static it.infn.mw.iam.api.scim.model.ScimPatchOperation.ScimPatchOperationType.replace;
import static it.infn.mw.iam.test.scim.ScimUtils.SCIM_CLIENT_ID;
import static it.infn.mw.iam.test.scim.ScimUtils.SCIM_READ_SCOPE;
import static it.infn.mw.iam.test.scim.ScimUtils.SCIM_WRITE_SCOPE;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Base64;
import java.util.Optional;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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
import it.infn.mw.iam.api.scim.model.ScimIndigoUser;
import it.infn.mw.iam.api.scim.model.ScimName;
import it.infn.mw.iam.api.scim.model.ScimOidcId;
import it.infn.mw.iam.api.scim.model.ScimSamlId;
import it.infn.mw.iam.api.scim.model.ScimSshKey;
import it.infn.mw.iam.api.scim.model.ScimUser;
import it.infn.mw.iam.api.scim.model.ScimX509Certificate;
import it.infn.mw.iam.api.scim.provisioning.ScimUserProvisioning;
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
@WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
public class ScimUserProvisioningPatchMvcTests {

  @Autowired
  private ScimRestUtilsMvc restUtils;

  @Autowired
  private ScimUserProvisioning provider;
  @Autowired
  private IamAccountRepository accountRepo;

  PasswordEncoder encoder = new BCryptPasswordEncoder();

  private ScimUser lennon;
  private ScimUser lincoln;

  @Before
  public void init() throws Exception {

    lennon = ScimUtils.buildUser("john_lennon", "lennon@email.test", "John", "Lennon");
    lennon = provider.create(lennon);

    lincoln = ScimUtils.buildUser("abraham_lincoln", "lincoln@email.test", "Abraham", "Lincoln");
    lincoln = provider.create(lincoln);
  }

  private ScimOidcId OIDCID_TEST =
      ScimOidcId.builder().issuer("test_issuer").subject("test_subject").build();

  private ScimSamlId SAMLID_TEST =
      ScimSamlId.builder().idpId("test_idp").userId("test_user").build();

  private ScimX509Certificate X509CERT_TEST = ScimX509Certificate.builder()
    .display("Personal Certificate")
    .value(X509Utils.x509Certs.get(0).certificate)
    .primary(true)
    .build();

  private String SSHKEY_TEST_FINGERPRINT = SshKeyUtils.sshKeys.get(0).fingerprintSHA256;
  private ScimSshKey SSHKEY_TEST = ScimSshKey.builder()
    .display("Personal")
    .value(SshKeyUtils.sshKeys.get(0).key)
    .primary(true)
    .build();

  private final String PICTURE_URL = "http://iosicongallery.com/img/512/angry-birds-2-2016.png";

  @After
  public void testsTeardown() throws Exception {

    provider.delete(lennon.getId());
    provider.delete(lincoln.getId());
  }

  @Test
  public void testPatchUserInfo() throws Exception {

    ScimName name = ScimName.builder().givenName("John jr.").familyName("Lennon II").build();

    /*
     * Update: - email - username - active - name - address
     */
    ScimUser lennon_updates = ScimUser.builder("john_lennon_jr")
      .buildEmail("john_lennon_jr@email.com")
      .active(!lennon.getActive())
      .name(name)
      .build();

    restUtils.patchUser(lennon.getId(), add, lennon_updates);

    ScimUser updatedUser = restUtils.getUser(lennon.getId());
    assertThat(updatedUser.getId(), equalTo(lennon.getId()));
    assertThat(updatedUser.getUserName(), equalTo(lennon_updates.getUserName()));
    assertThat(updatedUser.getDisplayName(), equalTo(lennon_updates.getUserName()));
    assertThat(updatedUser.getName().getGivenName(),
        equalTo(lennon_updates.getName().getGivenName()));
    assertThat(updatedUser.getName().getMiddleName(),
        equalTo(lennon_updates.getName().getMiddleName()));
    assertThat(updatedUser.getName().getFamilyName(),
        equalTo(lennon_updates.getName().getFamilyName()));
    assertThat(updatedUser.getActive(), equalTo(lennon_updates.getActive()));
    assertThat(updatedUser.getEmails(), hasSize(equalTo(1)));
    assertThat(updatedUser.getEmails().get(0).getValue(),
        equalTo(lennon_updates.getEmails().get(0).getValue()));

  }

  @Test
  public void testAddReassignAndRemoveOidcId() throws Exception {

    ScimIndigoUser indigoUser = ScimIndigoUser.builder().addOidcid(OIDCID_TEST).build();

    ScimUser updateOidcId = ScimUser.builder().indigoUserInfo(indigoUser).build();

    restUtils.patchUser(lennon.getId(), add, updateOidcId);

    ScimUser updatedUser = restUtils.getUser(lennon.getId());
    assertThat(updatedUser.getIndigoUser().getOidcIds(), hasSize(equalTo(1)));
    assertThat(updatedUser.getIndigoUser().getOidcIds().get(0).getIssuer(),
        equalTo(OIDCID_TEST.getIssuer()));
    assertThat(updatedUser.getIndigoUser().getOidcIds().get(0).getSubject(),
        equalTo(OIDCID_TEST.getSubject()));

    /* lincoln tryes to add the oidc account: */
    restUtils.patchUser(lincoln.getId(), add, updateOidcId, HttpStatus.CONFLICT);

    /* Remove oidc account */
    restUtils.patchUser(lennon.getId(), remove, updateOidcId);

    updatedUser = restUtils.getUser(lennon.getId());
    assertNull(updatedUser.getIndigoUser());
  }

  @Test
  public void testAddReassignAndRemoveSamlId() throws Exception {

    ScimUser updateSamlId = ScimUser.builder()
      .indigoUserInfo(ScimIndigoUser.builder().addSamlId(SAMLID_TEST).build())
      .build();

    restUtils.patchUser(lennon.getId(), add, updateSamlId);

    ScimUser updatedUser = restUtils.getUser(lennon.getId());
    assertThat(updatedUser.getIndigoUser().getSamlIds(), hasSize(equalTo(1)));
    assertThat(updatedUser.getIndigoUser().getSamlIds().get(0).getIdpId(),
        equalTo(SAMLID_TEST.getIdpId()));
    assertThat(updatedUser.getIndigoUser().getSamlIds().get(0).getUserId(),
        equalTo(SAMLID_TEST.getUserId()));

    /* lincoln tryes to add the oidc account: */
    restUtils.patchUser(lincoln.getId(), add, updateSamlId, HttpStatus.CONFLICT);

    /* Remove oidc account */
    restUtils.patchUser(lennon.getId(), remove, updateSamlId);

    updatedUser = restUtils.getUser(lennon.getId());
    assertThat(updatedUser.getId(), equalTo(lennon.getId()));
    assertNull(updatedUser.getIndigoUser());
  }

  @Test
  public void testRemoveNotExistingOidcId() throws Exception {

    ScimUser updates = ScimUser.builder()
      .indigoUserInfo(ScimIndigoUser.builder().addOidcid(OIDCID_TEST).build())
      .build();

    restUtils.patchUser(lennon.getId(), remove, updates);
  }

  @Test
  public void testAddInvalidBase64X509Certificate() throws Exception {

    ScimUser lennon_update = ScimUser.builder()
      .buildX509Certificate("Personal Certificate", "This is not a certificate", true)
      .build();

    restUtils.patchUser(lennon.getId(), add, lennon_update, HttpStatus.BAD_REQUEST);
  }

  @Test
  public void testAddInvalidX509Certificate() throws Exception {

    String certificate = Base64.getEncoder().encodeToString("this is not a certificate".getBytes());

    ScimUser lennon_update =
        ScimUser.builder().buildX509Certificate("Personal Certificate", certificate, true).build();

    restUtils.patchUser(lennon.getId(), add, lennon_update, HttpStatus.BAD_REQUEST);
  }

  @Test
  public void testAddAndRemoveX509Certificate() throws Exception {

    ScimUser lennon_update = ScimUser.builder().addX509Certificate(X509CERT_TEST).build();

    restUtils.patchUser(lennon.getId(), add, lennon_update);

    ScimUser updatedUser = restUtils.getUser(lennon.getId());
    assertThat(updatedUser.getX509Certificates(), hasSize(equalTo(1)));
    assertThat(updatedUser.getX509Certificates().get(0).getValue(),
        equalTo(X509CERT_TEST.getValue()));
    assertThat(updatedUser.getX509Certificates().get(0).getDisplay(),
        equalTo(X509CERT_TEST.getDisplay()));

    ScimUser lennon_remove =
        ScimUser.builder().buildX509Certificate(null, X509CERT_TEST.getValue(), null).build();

    restUtils.patchUser(lennon.getId(), remove, lennon_remove);

    updatedUser = restUtils.getUser(lennon.getId());
    assertNull(updatedUser.getX509Certificates());
  }

  @Test
  public void testPatchUserPassword() throws Exception {

    final String NEW_PASSWORD = "new_password";

    ScimUser patchedPasswordUser = ScimUser.builder().password(NEW_PASSWORD).build();

    restUtils.patchUser(lennon.getId(), add, patchedPasswordUser);

    Optional<IamAccount> lennonAccount = accountRepo.findByUuid(lennon.getId());
    if (!lennonAccount.isPresent()) {
      Assert.fail("Account not found");
    }

    assertThat(lennonAccount.get().getPassword(), notNullValue());
    assertThat(encoder.matches(NEW_PASSWORD, lennonAccount.get().getPassword()), equalTo(true));
  }

  @Test
  public void testAddReassignAndRemoveSshKey() throws Exception {

    ScimUser updateSshKey = ScimUser.builder().addSshKey(SSHKEY_TEST).build();

    restUtils.patchUser(lennon.getId(), add, updateSshKey);

    ScimUser updatedUser = restUtils.getUser(lennon.getId());
    assertThat(updatedUser.getIndigoUser().getSshKeys(), hasSize(equalTo(1)));
    assertThat(updatedUser.getIndigoUser().getSshKeys().get(0).getValue(),
        equalTo(SSHKEY_TEST.getValue()));
    assertThat(updatedUser.getIndigoUser().getSshKeys().get(0).getDisplay(),
        equalTo(SSHKEY_TEST.getDisplay()));
    assertThat(updatedUser.getIndigoUser().getSshKeys().get(0).getFingerprint(),
        equalTo(SSHKEY_TEST_FINGERPRINT));
    assertThat(updatedUser.getIndigoUser().getSshKeys().get(0).isPrimary(), equalTo(true));

    /* lincoln tryes to add the lennon ssh key: */
    restUtils.patchUser(lincoln.getId(), add, updateSshKey, HttpStatus.CONFLICT);

    restUtils.patchUser(lennon.getId(), remove, updateSshKey);
  }

  @Test
  public void testAddSshKeyWithInvalidBase64Value() throws Exception {

    ScimSshKey SSHKEY_INVALID_BASE64_VALUE =
        ScimSshKey.builder().display("Personal").value("Non Base64 String").primary(true).build();

    ScimUser updateSshKey = ScimUser.builder().addSshKey(SSHKEY_INVALID_BASE64_VALUE).build();

    restUtils.patchUser(lennon.getId(), add, updateSshKey, HttpStatus.BAD_REQUEST)
      .andExpect(jsonPath("$.status", equalTo("400")))
      .andExpect(jsonPath("$.detail",
          equalTo("Error during fingerprint generation: RSA key is not base64 encoded")));
  }

  @Test
  @Ignore
  public void testRemoveSshKeyWithLabel() throws Exception {

    ScimUser updateSshKey = ScimUser.builder().addSshKey(SSHKEY_TEST).build();

    restUtils.patchUser(lennon.getId(), add, updateSshKey);

    updateSshKey = ScimUser.builder()
      .indigoUserInfo(ScimIndigoUser.builder()
        .addSshKey(ScimSshKey.builder().display("Personal").build())
        .build())
      .build();

    restUtils.patchUser(lennon.getId(), remove, updateSshKey);

    ScimUser updatedUser = restUtils.getUser(lennon.getId());
    assertNull(updatedUser.getIndigoUser());
  }

  @Test
  @Ignore
  public void testRemoveSshKeyWithFingerprint() throws Exception {

    ScimUser updateSshKey = ScimUser.builder().addSshKey(SSHKEY_TEST).build();

    restUtils.patchUser(lennon.getId(), add, updateSshKey);

    updateSshKey = ScimUser.builder()
      .indigoUserInfo(ScimIndigoUser.builder()
        .addSshKey(ScimSshKey.builder().fingerprint(SSHKEY_TEST_FINGERPRINT).build())
        .build())
      .build();

    restUtils.patchUser(lennon.getId(), remove, updateSshKey);

    ScimUser updatedUser = restUtils.getUser(lennon.getId());
    assertNull(updatedUser.getIndigoUser());
  }

  @Test
  public void testRemoveSshKeyWithValue() throws Exception {

    ScimUser updateSshKey = ScimUser.builder().addSshKey(SSHKEY_TEST).build();

    restUtils.patchUser(lennon.getId(), add, updateSshKey);

    updateSshKey = ScimUser.builder()
      .indigoUserInfo(ScimIndigoUser.builder()
        .addSshKey(ScimSshKey.builder().value(SSHKEY_TEST.getValue()).build())
        .build())
      .build();

    restUtils.patchUser(lennon.getId(), remove, updateSshKey);

    ScimUser updatedUser = restUtils.getUser(lennon.getId());
    assertNull(updatedUser.getIndigoUser());
  }

  @Test
  public void testAddOidcIdDuplicateInASingleRequest() throws Exception {

    ScimUser updates = ScimUser.builder().addOidcId(OIDCID_TEST).addOidcId(OIDCID_TEST).build();

    restUtils.patchUser(lennon.getId(), add, updates);

    ScimUser updatedUser = restUtils.getUser(lennon.getId());
    assertThat(updatedUser.getId(), equalTo(lennon.getId()));
    assertThat(updatedUser.getIndigoUser().getOidcIds(), hasSize(equalTo(1)));
    assertThat(updatedUser.getIndigoUser().getOidcIds().get(0).getIssuer(),
        equalTo(OIDCID_TEST.getIssuer()));
    assertThat(updatedUser.getIndigoUser().getOidcIds().get(0).getSubject(),
        equalTo(OIDCID_TEST.getSubject()));
  }

  @Test
  public void testAddSshKeyDuplicateInASingleRequest() throws Exception {

    ScimUser updates = ScimUser.builder().addSshKey(SSHKEY_TEST).addSshKey(SSHKEY_TEST).build();

    restUtils.patchUser(lennon.getId(), add, updates);

    ScimUser updatedUser = restUtils.getUser(lennon.getId());
    assertThat(updatedUser.getIndigoUser().getSshKeys(), hasSize(equalTo(1)));
    assertThat(updatedUser.getIndigoUser().getSshKeys().get(0).getValue(),
        equalTo(SSHKEY_TEST.getValue()));
    assertThat(updatedUser.getIndigoUser().getSshKeys().get(0).getDisplay(),
        equalTo(SSHKEY_TEST.getDisplay()));
    assertThat(updatedUser.getIndigoUser().getSshKeys().get(0).getFingerprint(),
        equalTo(SSHKEY_TEST_FINGERPRINT));
    assertThat(updatedUser.getIndigoUser().getSshKeys().get(0).isPrimary(), equalTo(true));
  }

  @Test
  public void testAddSamlIdDuplicateInASingleRequest() throws Exception {

    ScimUser updates = ScimUser.builder().addSamlId(SAMLID_TEST).addSamlId(SAMLID_TEST).build();

    restUtils.patchUser(lennon.getId(), add, updates);

    ScimUser updatedUser = restUtils.getUser(lennon.getId());
    assertThat(updatedUser.getIndigoUser().getSamlIds(), hasSize(equalTo(1)));
    assertThat(updatedUser.getIndigoUser().getSamlIds().get(0).getIdpId(),
        equalTo(SAMLID_TEST.getIdpId()));
    assertThat(updatedUser.getIndigoUser().getSamlIds().get(0).getUserId(),
        equalTo(SAMLID_TEST.getUserId()));
  }

  @Test
  public void testAddX509DuplicateInASingleRequest() throws Exception {

    ScimUser updates = ScimUser.builder()
      .addX509Certificate(X509CERT_TEST)
      .addX509Certificate(X509CERT_TEST)
      .build();

    restUtils.patchUser(lennon.getId(), add, updates);

    ScimUser updatedUser = restUtils.getUser(lennon.getId());
    assertThat(updatedUser.getX509Certificates(), hasSize(equalTo(1)));
    assertThat(updatedUser.getX509Certificates().get(0).getValue(),
        equalTo(X509CERT_TEST.getValue()));
    assertThat(updatedUser.getX509Certificates().get(0).getDisplay(),
        equalTo(X509CERT_TEST.getDisplay()));
  }

  @Test
  public void testPatchAddOidIdAndSshKeyAndSamlId() throws Exception {

    ScimUser updates = ScimUser.builder()
      .addX509Certificate(X509CERT_TEST)
      .addOidcId(OIDCID_TEST)
      .addSshKey(SSHKEY_TEST)
      .addSamlId(SAMLID_TEST)
      .build();

    restUtils.patchUser(lennon.getId(), add, updates);

    ScimUser updatedUser = restUtils.getUser(lennon.getId());
    assertThat(updatedUser.getX509Certificates(), hasSize(equalTo(1)));
    assertThat(updatedUser.getX509Certificates().get(0).getValue(),
        equalTo(X509CERT_TEST.getValue()));
    assertThat(updatedUser.getX509Certificates().get(0).getDisplay(),
        equalTo(X509CERT_TEST.getDisplay()));
    assertThat(updatedUser.getIndigoUser().getSamlIds(), hasSize(equalTo(1)));
    assertThat(updatedUser.getIndigoUser().getSamlIds().get(0).getIdpId(),
        equalTo(SAMLID_TEST.getIdpId()));
    assertThat(updatedUser.getIndigoUser().getSamlIds().get(0).getUserId(),
        equalTo(SAMLID_TEST.getUserId()));
    assertThat(updatedUser.getIndigoUser().getSshKeys(), hasSize(equalTo(1)));
    assertThat(updatedUser.getIndigoUser().getSshKeys().get(0).getValue(),
        equalTo(SSHKEY_TEST.getValue()));
    assertThat(updatedUser.getIndigoUser().getSshKeys().get(0).getDisplay(),
        equalTo(SSHKEY_TEST.getDisplay()));
    assertThat(updatedUser.getIndigoUser().getSshKeys().get(0).getFingerprint(),
        equalTo(SSHKEY_TEST_FINGERPRINT));
    assertThat(updatedUser.getIndigoUser().getSshKeys().get(0).isPrimary(), equalTo(true));
    assertThat(updatedUser.getIndigoUser().getOidcIds(), hasSize(equalTo(1)));
    assertThat(updatedUser.getIndigoUser().getOidcIds().get(0).getIssuer(),
        equalTo(OIDCID_TEST.getIssuer()));
    assertThat(updatedUser.getIndigoUser().getOidcIds().get(0).getSubject(),
        equalTo(OIDCID_TEST.getSubject()));
  }

  @Test
  @Ignore
  public void testAddMultipleX509CertificateAndNoPrimary() throws Exception {

    ScimUser updates = ScimUser.builder()
      .buildX509Certificate("Personal1", X509Utils.x509Certs.get(0).certificate, false)
      .buildX509Certificate("Personal2", X509Utils.x509Certs.get(1).certificate, false)
      .build();

    restUtils.patchUser(lennon.getId(), add, updates);

    ScimUser updatedUser = restUtils.getUser(lennon.getId());
    assertThat(updatedUser.getX509Certificates(), hasSize(equalTo(2)));
    assertThat(updatedUser.getX509Certificates().get(0).getDisplay(), equalTo("Personal1"));
    assertThat(updatedUser.getX509Certificates().get(0).getValue(),
        equalTo(X509Utils.x509Certs.get(0).certificate));
    assertThat(updatedUser.getX509Certificates().get(0).isPrimary(), equalTo(true));
    assertThat(updatedUser.getX509Certificates().get(1).getDisplay(), equalTo("Personal2"));
    assertThat(updatedUser.getX509Certificates().get(1).getValue(),
        equalTo(X509Utils.x509Certs.get(1).certificate));
    assertThat(updatedUser.getX509Certificates().get(1).isPrimary(), equalTo(false));
  }

  @Test
  @Ignore
  public void testReplacePrimaryWithMultipleX509Certificate() throws Exception {

    ScimUser updates = ScimUser.builder()
      .buildX509Certificate("Personal1", X509Utils.x509Certs.get(0).certificate, false)
      .buildX509Certificate("Personal2", X509Utils.x509Certs.get(1).certificate, false)
      .build();

    restUtils.patchUser(lennon.getId(), add, updates);

    updates = ScimUser.builder()
      .buildX509Certificate("Personal1", X509Utils.x509Certs.get(0).certificate, false)
      .buildX509Certificate("Personal2", X509Utils.x509Certs.get(1).certificate, true)
      .build();

    restUtils.patchUser(lennon.getId(), replace, updates);

    ScimUser updatedUser = restUtils.getUser(lennon.getId());
    assertThat(updatedUser.getX509Certificates(), hasSize(equalTo(2)));
    assertThat(updatedUser.getX509Certificates().get(0).getDisplay(), equalTo("Personal1"));
    assertThat(updatedUser.getX509Certificates().get(0).getValue(),
        equalTo(X509Utils.x509Certs.get(0).certificate));
    assertThat(updatedUser.getX509Certificates().get(0).isPrimary(), equalTo(false));
    assertThat(updatedUser.getX509Certificates().get(1).getDisplay(), equalTo("Personal2"));
    assertThat(updatedUser.getX509Certificates().get(1).getValue(),
        equalTo(X509Utils.x509Certs.get(1).certificate));
    assertThat(updatedUser.getX509Certificates().get(1).isPrimary(), equalTo(true));

  }

  @Test
  public void testEmailIsNotAlreadyLinkedOnPatch() throws Exception {

    String alreadyBoundEmail = lincoln.getEmails().get(0).getValue();
    ScimUser lennonUpdates = ScimUser.builder().buildEmail(alreadyBoundEmail).build();

    restUtils.patchUser(lennon.getId(), add, lennonUpdates, HttpStatus.CONFLICT)
      .andExpect(jsonPath("$.detail",
          containsString("Email " + alreadyBoundEmail + " already bound to another user")));
  }

  @Test
  public void testAddPicture() throws Exception {

    ScimUser updates = ScimUser.builder().buildPhoto(PICTURE_URL).build();

    restUtils.patchUser(lennon.getId(), add, updates);

    ScimUser updatedUser = restUtils.getUser(lennon.getId());
    assertThat(updatedUser.getPhotos(), hasSize(equalTo(1)));
    assertThat(updatedUser.getPhotos().get(0).getValue(), equalTo(PICTURE_URL));
  }
}
