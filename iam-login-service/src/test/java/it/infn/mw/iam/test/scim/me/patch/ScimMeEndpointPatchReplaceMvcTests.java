package it.infn.mw.iam.test.scim.me.patch;

import static it.infn.mw.iam.api.scim.model.ScimPatchOperation.ScimPatchOperationType.replace;
import static it.infn.mw.iam.test.SshKeyUtils.sshKeys;
import static it.infn.mw.iam.test.X509Utils.x509Certs;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.scim.model.ScimEmail;
import it.infn.mw.iam.api.scim.model.ScimName;
import it.infn.mw.iam.api.scim.model.ScimOidcId;
import it.infn.mw.iam.api.scim.model.ScimPatchOperation;
import it.infn.mw.iam.api.scim.model.ScimPhoto;
import it.infn.mw.iam.api.scim.model.ScimSamlId;
import it.infn.mw.iam.api.scim.model.ScimSshKey;
import it.infn.mw.iam.api.scim.model.ScimUser;
import it.infn.mw.iam.api.scim.model.ScimX509Certificate;
import it.infn.mw.iam.api.scim.provisioning.ScimUserProvisioning;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.scim.ScimRestUtilsMvc;
import it.infn.mw.iam.test.util.WithMockOAuthUser;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(
    classes = {IamLoginService.class, CoreControllerTestSupport.class, ScimRestUtilsMvc.class})
@WebAppConfiguration
@Transactional
@WithMockOAuthUser(user = "test_105", authorities = {"ROLE_USER"})
public class ScimMeEndpointPatchReplaceMvcTests {

  @Autowired
  private ScimRestUtilsMvc restUtils;
  @Autowired
  private ScimUserProvisioning provider;

  private final ScimEmail ANOTHERUSER_EMAIL =
      ScimEmail.builder().email("test-100@test.org").build();

  private final ScimName TESTUSER_NEWNAME =
      ScimName.builder().givenName("A").familyName("B").build();
  private final ScimPhoto TESTUSER_NEWPHOTO =
      ScimPhoto.builder().value("http://fakesite.org/user.png").build();
  private final ScimEmail TESTUSER_NEWEMAIL =
      ScimEmail.builder().email("fakeemail@iam.test").build();

  private final ScimOidcId TESTUSER_OIDCID =
      ScimOidcId.builder().issuer("ISS").subject("SUB").build();
  private final ScimSamlId TESTUSER_SAMLID =
      ScimSamlId.builder().idpId("IDP").userId("UID").build();
  private final ScimSshKey TESTUSER_SSHKEY =
      ScimSshKey.builder().display("KEY").value(sshKeys.get(0).key).primary(true).build();
  private final ScimX509Certificate TESTUSER_X509CERT = ScimX509Certificate.builder()
    .display(x509Certs.get(0).display)
    .value(x509Certs.get(0).certificate)
    .primary(true)
    .build();

  @Before
  public void init() throws Exception {

    String uuid = restUtils.getMe().getId();

    ScimUser updates = ScimUser.builder()
      .buildPhoto("http://site.org/user.png")
      .addOidcId(TESTUSER_OIDCID)
      .addSamlId(TESTUSER_SAMLID)
      .addX509Certificate(TESTUSER_X509CERT)
      .addSshKey(TESTUSER_SSHKEY)
      .build();

    List<ScimPatchOperation<ScimUser>> operations = Lists.newArrayList();

    operations.add(new ScimPatchOperation.Builder<ScimUser>().add().value(updates).build());

    provider.update(uuid, operations);
  }

  @Test
  public void testPatchReplacePasswordNotSupported() throws Exception {

    final String NEW_PASSWORD = "newpassword";

    ScimUser updates = ScimUser.builder().password(NEW_PASSWORD).build();

    restUtils.patchMe(replace, updates, HttpStatus.BAD_REQUEST);
  }

  @Test
  public void testPatchReplaceGivenAndFamilyName() throws Exception {

    ScimUser updates = ScimUser.builder().name(TESTUSER_NEWNAME).build();

    restUtils.patchMe(replace, updates);

    ScimUser userAfter = restUtils.getMe();

    assertThat(userAfter.getName().getGivenName(), equalTo(updates.getName().getGivenName()));
    assertThat(userAfter.getName().getFamilyName(), equalTo(updates.getName().getFamilyName()));
  }

  @Test

  public void testPatchReplacePicture() throws Exception {

    ScimUser updates = ScimUser.builder().addPhoto(TESTUSER_NEWPHOTO).build();

    restUtils.patchMe(replace, updates);

    ScimPhoto updatedPhoto = restUtils.getMe().getPhotos().get(0);

    assertThat(updatedPhoto, equalTo(TESTUSER_NEWPHOTO));
  }

  @Test

  public void testPatchReplaceEmail() throws Exception {

    ScimUser updates = ScimUser.builder().addEmail(TESTUSER_NEWEMAIL).build();

    restUtils.patchMe(replace, updates);

    ScimUser updatedUser = restUtils.getMe();

    assertThat(updatedUser.getEmails().get(0), equalTo(TESTUSER_NEWEMAIL));
  }

  @Test
  public void testPatchReplaceAlreadyUsedEmail() throws Exception {

    ScimUser updates = ScimUser.builder().addEmail(ANOTHERUSER_EMAIL).build();

    restUtils.patchMe(replace, updates, HttpStatus.CONFLICT)
      .andExpect(jsonPath("$.detail", containsString("already bound to another user")));
  }

  @Test
  public void testPatchReplaceOidcIdNotSupported() throws Exception {

    ScimUser updates = ScimUser.builder().addOidcId(TESTUSER_OIDCID).build();

    restUtils.patchMe(replace, updates, HttpStatus.BAD_REQUEST)
      .andExpect(jsonPath("$.detail", containsString("replace operation not supported")));
  }

  @Test
  public void testPatchReplaceSamlIdNotSupported() throws Exception {

    ScimUser updates = ScimUser.builder().addSamlId(TESTUSER_SAMLID).build();

    restUtils.patchMe(replace, updates, HttpStatus.BAD_REQUEST)
      .andExpect(jsonPath("$.detail", containsString("replace operation not supported")));
  }

  @Test
  public void testPatchReplaceX509CertificateNotSupported() throws Exception {

    ScimUser updates = ScimUser.builder().addX509Certificate(TESTUSER_X509CERT).build();

    restUtils.patchMe(replace, updates, HttpStatus.BAD_REQUEST)
      .andExpect(jsonPath("$.detail", containsString("replace operation not supported")));
  }

  @Test
  public void testPatchReplaceSshKeyNotSupported() throws Exception {

    ScimUser updates = ScimUser.builder().addSshKey(TESTUSER_SSHKEY).build();

    restUtils.patchMe(replace, updates, HttpStatus.BAD_REQUEST)
      .andExpect(jsonPath("$.detail", containsString("replace operation not supported")));
  }
}
