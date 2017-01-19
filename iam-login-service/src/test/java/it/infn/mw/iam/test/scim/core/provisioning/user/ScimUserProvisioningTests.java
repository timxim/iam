package it.infn.mw.iam.test.scim.core.provisioning.user;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.scim.model.ScimEmail;
import it.infn.mw.iam.api.scim.model.ScimName;
import it.infn.mw.iam.api.scim.model.ScimOidcId;
import it.infn.mw.iam.api.scim.model.ScimPhoto;
import it.infn.mw.iam.api.scim.model.ScimSamlId;
import it.infn.mw.iam.api.scim.model.ScimSshKey;
import it.infn.mw.iam.api.scim.model.ScimUser;
import it.infn.mw.iam.api.scim.model.ScimX509Certificate;
import it.infn.mw.iam.api.scim.provisioning.ScimUserProvisioning;
import it.infn.mw.iam.persistence.model.IamAccount;
import it.infn.mw.iam.test.SshKeyUtils;
import it.infn.mw.iam.test.X509Utils;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = IamLoginService.class)
@Transactional
public class ScimUserProvisioningTests {

  @Autowired
  private ScimUserProvisioning userService;

  @Autowired
  private PasswordEncoder passwordEncoder;

  final String TESTUSER_USERNAME = "testProvisioningUser";
  final String TESTUSER_PASSWORD = "password";
  final ScimName TESTUSER_NAME = ScimName.builder().givenName("John").familyName("Lennon").build();
  final ScimEmail TESTUSER_EMAIL = ScimEmail.builder().email("john.lennon@liverpool.uk").build();
  final ScimPhoto TESTUSER_PHOTO = ScimPhoto.builder().value("http://site.org/user.png").build();
  final ScimOidcId TESTUSER_OIDCID =
      ScimOidcId.builder().issuer("urn:oidc:test:issuer").subject("1234").build();
  final ScimSamlId TESTUSER_SAMLID = ScimSamlId.builder().idpId("idpID").userId("userId").build();
  final ScimSshKey TESTUSER_SSHKEY = ScimSshKey.builder()
    .primary(true)
    .display("Personal Key")
    .value(SshKeyUtils.sshKeys.get(0).key)
    .build();
  final ScimX509Certificate TESTUSER_X509CERT = ScimX509Certificate.builder()
    .display("Personal X509 Certificate")
    .value(X509Utils.x509Certs.get(0).certificate)
    .primary(true)
    .build();

  @Test
  public void createUserTest() {

    ScimUser scimUser = ScimUser.builder()
      .active(true)
      .userName(TESTUSER_USERNAME)
      .password(TESTUSER_PASSWORD)
      .name(TESTUSER_NAME)
      .addEmail(TESTUSER_EMAIL)
      .addPhoto(TESTUSER_PHOTO)
      .addOidcId(TESTUSER_OIDCID)
      .addSamlId(TESTUSER_SAMLID)
      .addSshKey(TESTUSER_SSHKEY)
      .addX509Certificate(TESTUSER_X509CERT)
      .build();

    IamAccount iamAccount = userService.createAccount(scimUser);

    assertNotNull(iamAccount);

    assertThat(iamAccount.isActive(), equalTo(true));

    assertThat(iamAccount.getUsername(), equalTo(TESTUSER_USERNAME));

    assertTrue(passwordEncoder.matches(TESTUSER_PASSWORD, iamAccount.getPassword()));

    assertThat(iamAccount.getUserInfo().getGivenName(), equalTo(TESTUSER_NAME.getGivenName()));
    assertThat(iamAccount.getUserInfo().getMiddleName(), equalTo(TESTUSER_NAME.getMiddleName()));
    assertThat(iamAccount.getUserInfo().getFamilyName(), equalTo(TESTUSER_NAME.getFamilyName()));

    assertThat(iamAccount.getUserInfo().getPicture(), equalTo(TESTUSER_PHOTO.getValue()));

    assertThat(iamAccount.getUserInfo().getEmail(), equalTo(TESTUSER_EMAIL.getValue()));

    assertThat(iamAccount.getOidcIds().get(0).getIssuer(), equalTo(TESTUSER_OIDCID.getIssuer()));
    assertThat(iamAccount.getOidcIds().get(0).getSubject(), equalTo(TESTUSER_OIDCID.getSubject()));

    assertThat(iamAccount.getSamlIds().get(0).getIdpId(), equalTo(TESTUSER_SAMLID.getIdpId()));
    assertThat(iamAccount.getSamlIds().get(0).getUserId(), equalTo(TESTUSER_SAMLID.getUserId()));

    assertThat(iamAccount.getSshKeys().get(0).getLabel(), equalTo(TESTUSER_SSHKEY.getDisplay()));
    assertThat(iamAccount.getSshKeys().get(0).getFingerprint(),
        equalTo(SshKeyUtils.sshKeys.get(0).fingerprintSHA256));
    assertThat(iamAccount.getSshKeys().get(0).getValue(), equalTo(TESTUSER_SSHKEY.getValue()));
    assertThat(iamAccount.getSshKeys().get(0).isPrimary(), equalTo(TESTUSER_SSHKEY.isPrimary()));

    assertThat(iamAccount.getX509Certificates().get(0).getLabel(),
        equalTo(TESTUSER_X509CERT.getDisplay()));
    assertThat(iamAccount.getX509Certificates().get(0).getCertificate(),
        equalTo(TESTUSER_X509CERT.getValue()));
    assertThat(iamAccount.getX509Certificates().get(0).isPrimary(),
        equalTo(TESTUSER_X509CERT.isPrimary()));

    userService.delete(iamAccount.getUuid());
  }
}
