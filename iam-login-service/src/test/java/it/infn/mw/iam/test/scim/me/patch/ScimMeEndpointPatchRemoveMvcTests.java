package it.infn.mw.iam.test.scim.me.patch;

import static it.infn.mw.iam.api.scim.model.ScimPatchOperation.ScimPatchOperationType.remove;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.scim.model.ScimOidcId;
import it.infn.mw.iam.api.scim.model.ScimPatchOperation;
import it.infn.mw.iam.api.scim.model.ScimPhoto;
import it.infn.mw.iam.api.scim.model.ScimSamlId;
import it.infn.mw.iam.api.scim.model.ScimUser;
import it.infn.mw.iam.api.scim.provisioning.ScimUserProvisioning;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.scim.ScimRestUtilsMvc;
import it.infn.mw.iam.test.util.WithMockOAuthUser;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(
    classes = {IamLoginService.class, CoreControllerTestSupport.class, ScimRestUtilsMvc.class})
@WebAppConfiguration
@WithMockOAuthUser(user = "test_104", authorities = {"ROLE_USER"})
@Transactional
public class ScimMeEndpointPatchRemoveMvcTests {

  @Autowired
  private ScimRestUtilsMvc restUtils;
  @Autowired
  private ScimUserProvisioning provider;

  @Before
  public void init() throws Exception {

    String uuid = restUtils.getMe().getId();

    ScimUser updates = ScimUser.builder()
      .buildPhoto("http://site.org/user.png")
      .buildOidcId("ISS", "SUB")
      .buildSamlId("IDP", "UID")
      .build();

    List<ScimPatchOperation<ScimUser>> operations = Lists.newArrayList();

    operations.add(new ScimPatchOperation.Builder<ScimUser>().add().value(updates).build());

    provider.update(uuid, operations);
  }

  @Test
  public void testPatchRemovePicture() throws Exception {

    ScimPhoto currentPhoto = restUtils.getMe().getPhotos().get(0);

    ScimUser updates = ScimUser.builder().addPhoto(currentPhoto).build();

    restUtils.patchMe(remove, updates);

    assertThat(restUtils.getMe().hasPhotos(), equalTo(false));
  }

  @Test
  public void testPatchRemoveOidcId() throws Exception {

    ScimOidcId currentOidcId = restUtils.getMe().getIndigoUser().getOidcIds().get(0);

    ScimUser updates = ScimUser.builder().addOidcId(currentOidcId).build();

    restUtils.patchMe(remove, updates);

    assertThat(restUtils.getMe().getIndigoUser().getOidcIds(), hasSize(equalTo(0)));
  }

  @Test
  public void testPatchRemoveSamlId() throws Exception {

    ScimSamlId currentSamlId = restUtils.getMe().getIndigoUser().getSamlIds().get(0);

    ScimUser updates = ScimUser.builder().addSamlId(currentSamlId).build();

    restUtils.patchMe(remove, updates);

    assertThat(restUtils.getMe().getIndigoUser().getSamlIds(), hasSize(equalTo(0)));
  }
}
