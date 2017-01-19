package it.infn.mw.iam.test.scim.user;

import static it.infn.mw.iam.api.scim.model.ScimPatchOperation.ScimPatchOperationType.replace;
import static it.infn.mw.iam.test.scim.ScimUtils.SCIM_CLIENT_ID;
import static it.infn.mw.iam.test.scim.ScimUtils.SCIM_READ_SCOPE;
import static it.infn.mw.iam.test.scim.ScimUtils.SCIM_WRITE_SCOPE;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.google.common.collect.Lists;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.scim.model.ScimOidcId;
import it.infn.mw.iam.api.scim.model.ScimSamlId;
import it.infn.mw.iam.api.scim.model.ScimSshKey;
import it.infn.mw.iam.api.scim.model.ScimUser;
import it.infn.mw.iam.api.scim.model.ScimX509Certificate;
import it.infn.mw.iam.api.scim.provisioning.ScimUserProvisioning;
import it.infn.mw.iam.test.OidcIdUtils;
import it.infn.mw.iam.test.SamlIdUtils;
import it.infn.mw.iam.test.SshKeyUtils;
import it.infn.mw.iam.test.X509Utils;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.scim.ScimRestUtilsMvc;
import it.infn.mw.iam.test.util.WithMockOAuthUser;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(
    classes = {IamLoginService.class, CoreControllerTestSupport.class, ScimRestUtilsMvc.class})
@WebAppConfiguration
@WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
public class ScimUserProvisioningPatchReplaceMvcTests {

  @Autowired
  private ScimRestUtilsMvc restUtils;

  @Autowired
  private ScimUserProvisioning provider;

  private ScimUser testUser;

  private List<String> PICTURES =
      Lists.newArrayList("http://iosicongallery.com/img/512/angry-birds-2-2016.png",
          "https://fallofthewall25.com/img/default-user.jpg");

  @Before
  public void initTestUsers() throws Exception {

    testUser = ScimUser.builder("john_lennon")
      .buildEmail("lennon@email.test")
      .buildName("John", "Lennon")
      .buildPhoto(PICTURES.get(0))
      .addOidcId(ScimOidcId.builder()
        .issuer(OidcIdUtils.oidcIds.get(0).issuer)
        .subject(OidcIdUtils.oidcIds.get(0).subject)
        .build())
      .addSshKey(ScimSshKey.builder()
        .value(SshKeyUtils.sshKeys.get(0).key)
        .fingerprint(SshKeyUtils.sshKeys.get(0).fingerprintSHA256)
        .primary(true)
        .build())
      .addSshKey(ScimSshKey.builder()
        .value(SshKeyUtils.sshKeys.get(1).key)
        .fingerprint(SshKeyUtils.sshKeys.get(1).fingerprintSHA256)
        .primary(false)
        .build())
      .addSamlId(ScimSamlId.builder()
        .idpId(SamlIdUtils.samlIds.get(0).idpId)
        .userId(SamlIdUtils.samlIds.get(0).userId)
        .build())
      .addX509Certificate(ScimX509Certificate.builder()
        .display(X509Utils.x509Certs.get(0).display)
        .value(X509Utils.x509Certs.get(0).certificate)
        .primary(true)
        .build())
      .addX509Certificate(ScimX509Certificate.builder()
        .display(X509Utils.x509Certs.get(1).display)
        .value(X509Utils.x509Certs.get(1).certificate)
        .primary(false)
        .build())
      .build();

    testUser = provider.create(testUser);
  }

  @After
  public void teardownTest() throws Exception {

    provider.delete(testUser.getId());
  }

  @Test
  public void testReplaceEmailWithEmptyValue() throws Exception {

    ScimUser updates = ScimUser.builder().buildEmail("").build();

    restUtils.patchUser(testUser.getId(), replace, updates, BAD_REQUEST)
      .andExpect(jsonPath("$.detail", containsString(": may not be empty")));
  }

  @Test
  public void testReplaceEmailWithNullValue() throws Exception {

    ScimUser updates = ScimUser.builder().buildEmail(null).build();

    restUtils.patchUser(testUser.getId(), replace, updates, BAD_REQUEST)
      .andExpect(jsonPath("$.detail", containsString(": may not be empty")));
  }

  @Test
  public void testReplaceEmailWithSameValue() throws Exception {

    ScimUser updates =
        ScimUser.builder().buildEmail(testUser.getEmails().get(0).getValue()).build();

    restUtils.patchUser(testUser.getId(), replace, updates);
  }

  @Test
  public void testReplaceEmailWithInvalidValue() throws Exception {

    ScimUser updates = ScimUser.builder().buildEmail("fakeEmail").build();

    restUtils.patchUser(testUser.getId(), replace, updates, BAD_REQUEST)
      .andExpect(jsonPath("$.detail", containsString(": not a well-formed email address")));
  }

  @Test
  public void testReplacePicture() throws Exception {

    assertThat(testUser.getPhotos(), hasSize(equalTo(1)));
    assertThat(testUser.getPhotos().get(0).getValue(), equalTo(PICTURES.get(0)));

    ScimUser updates = ScimUser.builder().buildPhoto(PICTURES.get(1)).build();

    restUtils.patchUser(testUser.getId(), replace, updates);

    ScimUser updatedUser = restUtils.getUser(testUser.getId());
    assertThat(updatedUser.getPhotos(), hasSize(equalTo(1)));
    assertThat(updatedUser.getPhotos().get(0).getValue(), equalTo(PICTURES.get(1)));
  }

  @Test
  public void testReplaceUsername() throws Exception {

    final String ANOTHERUSER_USERNAME = "test";

    ScimUser updates = ScimUser.builder().userName(ANOTHERUSER_USERNAME).build();

    restUtils.patchUser(testUser.getId(), replace, updates, CONFLICT);
  }

  @Test
  public void testPatchReplaceSshKeyNotSupported() throws Exception {

    String keyValue = testUser.getIndigoUser().getSshKeys().get(0).getValue();

    ScimUser updates = ScimUser.builder()
      .addSshKey(ScimSshKey.builder().value(keyValue).display("NEW LABEL").build())
      .build();

    restUtils.patchUser(testUser.getId(), replace, updates, BAD_REQUEST);
  }

  @Test
  public void testPatchReplaceX509CertificateNotSupported() throws Exception {

    String certValue = testUser.getX509Certificates().get(0).getValue();

    ScimUser updates =
        ScimUser.builder().buildX509Certificate("NEW LABEL", certValue, true).build();

    restUtils.patchUser(testUser.getId(), replace, updates, HttpStatus.BAD_REQUEST);
  }
}
