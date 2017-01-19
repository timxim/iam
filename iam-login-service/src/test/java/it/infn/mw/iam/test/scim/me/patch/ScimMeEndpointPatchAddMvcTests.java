package it.infn.mw.iam.test.scim.me.patch;

import static it.infn.mw.iam.api.scim.model.ScimPatchOperation.ScimPatchOperationType.add;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
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
import it.infn.mw.iam.test.SshKeyUtils;
import it.infn.mw.iam.test.X509Utils;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.scim.ScimRestUtilsMvc;
import it.infn.mw.iam.test.util.WithMockOAuthUser;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(
    classes = {IamLoginService.class, CoreControllerTestSupport.class, ScimRestUtilsMvc.class})
@WebAppConfiguration
@WithMockOAuthUser(user = ScimMeEndpointPatchAddMvcTests.TEST_USERNAME, authorities = {"ROLE_USER"})
@Transactional
public class ScimMeEndpointPatchAddMvcTests {

  final static String TEST_USERNAME = "test_103";

  @Autowired
  private ScimRestUtilsMvc restUtils;
  
  private final ScimName TESTUSER_NEWNAME =
      ScimName.builder().givenName("A").familyName("B").build();
  private final ScimPhoto TESTUSER_NEWPHOTO =
      ScimPhoto.builder().value("http://fakesite.org/user.png").build();
  private final ScimEmail TESTUSER_NEWEMAIL =
      ScimEmail.builder().email("fakeemail@iam.test").build();

  @Test
  public void testPatchGivenAndFamilyName() throws Exception {

    ScimUser updates = ScimUser.builder().name(TESTUSER_NEWNAME).build();

    restUtils.patchMe(add, updates);

    ScimUser userAfter = restUtils.getMe();

    assertThat(userAfter.getName().getGivenName(), equalTo(updates.getName().getGivenName()));
    assertThat(userAfter.getName().getFamilyName(), equalTo(updates.getName().getFamilyName()));
  }

  @Test
  public void testPatchPicture() throws Exception {

    ScimUser updates = ScimUser.builder().addPhoto(TESTUSER_NEWPHOTO).build();

    restUtils.patchMe(add, updates);

    ScimUser userAfter = restUtils.getMe();

    assertThat(userAfter.getPhotos(), hasSize(equalTo(1)));
    assertThat(userAfter.getPhotos().get(0), equalTo(TESTUSER_NEWPHOTO));
  }

  @Test
  public void testPatchEmail() throws Exception {

    ScimUser updates = ScimUser.builder().addEmail(TESTUSER_NEWEMAIL).build();

    restUtils.patchMe(add, updates);

    ScimUser userAfter = restUtils.getMe();

    assertThat(userAfter.getEmails().get(0), equalTo(TESTUSER_NEWEMAIL));
  }

  @Test
  public void testPatchMultiple() throws Exception {

    final ScimUser updates = ScimUser.builder()
      .name(TESTUSER_NEWNAME)
      .addEmail(TESTUSER_NEWEMAIL)
      .addPhoto(TESTUSER_NEWPHOTO)
      .build();

    restUtils.patchMe(add, updates);

    ScimUser userAfter = restUtils.getMe();

    assertThat(userAfter.getName().getGivenName(), equalTo(updates.getName().getGivenName()));
    assertThat(userAfter.getName().getFamilyName(), equalTo(updates.getName().getFamilyName()));
    assertThat(userAfter.getPhotos(), hasSize(equalTo(1)));
    assertThat(userAfter.getPhotos().get(0), equalTo(TESTUSER_NEWPHOTO));
    assertThat(userAfter.getEmails().get(0), equalTo(TESTUSER_NEWEMAIL));
  }

  @Test
  public void testPatchPasswordNotSupported() throws Exception {

    final String NEW_PASSWORD = "newpassword";

    ScimUser updates = ScimUser.builder().password(NEW_PASSWORD).build();

    restUtils.patchMe(add, updates, HttpStatus.BAD_REQUEST);
  }

  @Test
  public void testPatchAddOidcIdNotSupported() throws Exception {

    ScimOidcId NEW_TESTUSER_OIDCID =
        ScimOidcId.builder().issuer("new_test_issuer").subject("new_user_subject").build();

    ScimUser updates = ScimUser.builder().addOidcId(NEW_TESTUSER_OIDCID).build();

    restUtils.patchMe(add, updates, HttpStatus.BAD_REQUEST);
  }

  @Test
  public void testPatchAddSamlIdNotSupported() throws Exception {

    ScimSamlId TESTUSER_SAMLID = ScimSamlId.builder().idpId("AA").userId("BB").build();

    ScimUser updates = ScimUser.builder().addSamlId(TESTUSER_SAMLID).build();

    restUtils.patchMe(add, updates, HttpStatus.BAD_REQUEST);
  }

  @Test
  public void testPatchAddSsHKeyNotSupported() throws Exception {

    ScimSshKey NEW_SSH_KEY =
        ScimSshKey.builder().display("ssh-key").value(SshKeyUtils.sshKeys.get(0).key).build();

    ScimUser updates = ScimUser.builder().addSshKey(NEW_SSH_KEY).build();

    restUtils.patchMe(add, updates, HttpStatus.BAD_REQUEST);
  }

  @Test
  public void testPatchAddX509CertificateNotSupported() throws Exception {

    ScimX509Certificate NEW_X509_CERT = ScimX509Certificate.builder()
      .display("x509-cert")
      .value(X509Utils.x509Certs.get(0).certificate)
      .build();

    ScimUser updates = ScimUser.builder().addX509Certificate(NEW_X509_CERT).build();

    restUtils.patchMe(add, updates, HttpStatus.BAD_REQUEST);
  }
}
