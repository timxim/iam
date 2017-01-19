package it.infn.mw.iam.test.scim.user;

import static it.infn.mw.iam.api.scim.model.ScimConstants.INDIGO_USER_SCHEMA;
import static it.infn.mw.iam.api.scim.model.ScimListResponse.SCHEMA;
import static it.infn.mw.iam.test.TestUtils.TOTAL_USERS_COUNT;
import static it.infn.mw.iam.test.scim.ScimUtils.SCIM_CLIENT_ID;
import static it.infn.mw.iam.test.scim.ScimUtils.SCIM_READ_SCOPE;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.scim.ScimRestUtilsMvc;
import it.infn.mw.iam.test.scim.ScimUtils.ParamsBuilder;
import it.infn.mw.iam.test.util.WithMockOAuthUser;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(
    classes = {IamLoginService.class, CoreControllerTestSupport.class, ScimRestUtilsMvc.class})
@WebAppConfiguration
@WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE})
public class ScimUserProvisioningAttributeFilterMvcTests {

  @Autowired
  private ScimRestUtilsMvc restUtils;

  @Test
  public void testReuturnOnlyUsernameRequest() throws Exception {

    restUtils.getUsers(ParamsBuilder.builder().count(1).attributes("userName").build())
      .andExpect(jsonPath("$.totalResults", equalTo(TOTAL_USERS_COUNT)))
      .andExpect(jsonPath("$.itemsPerPage", equalTo(1)))
      .andExpect(jsonPath("$.startIndex", equalTo(1)))
      .andExpect(jsonPath("$.schemas", contains(SCHEMA)))
      .andExpect(jsonPath("$.Resources", hasSize(equalTo(1))))
      .andExpect(jsonPath("$.Resources[0].id").exists())
      .andExpect(jsonPath("$.Resources[0].schemas").exists())
      .andExpect(jsonPath("$.Resources[0].userName").exists())
      .andExpect(jsonPath("$.Resources[0].emails").doesNotExist())
      .andExpect(jsonPath("$.Resources[0].displayName").doesNotExist())
      .andExpect(jsonPath("$.Resources[0].nickName").doesNotExist())
      .andExpect(jsonPath("$.Resources[0].profileUrl").doesNotExist())
      .andExpect(jsonPath("$.Resources[0].locale").doesNotExist())
      .andExpect(jsonPath("$.Resources[0].timezone").doesNotExist())
      .andExpect(jsonPath("$.Resources[0].active").doesNotExist())
      .andExpect(jsonPath("$.Resources[0].title").doesNotExist())
      .andExpect(jsonPath("$.Resources[0].addresses").doesNotExist())
      .andExpect(jsonPath("$.Resources[0].certificates").doesNotExist())
      .andExpect(jsonPath("$.Resources[0].groups").doesNotExist())
      .andExpect(jsonPath("$.Resources[0].urn:indigo-dc:scim:schemas:IndigoUser").doesNotExist());

  }

  @Test
  public void testMultipleAttrsRequest() throws Exception {

    restUtils
      .getUsers(ParamsBuilder.builder()
        .count(2)
        .attributes("userName,emails," + INDIGO_USER_SCHEMA)
        .build())
      .andExpect(jsonPath("$.totalResults", equalTo(TOTAL_USERS_COUNT)))
      .andExpect(jsonPath("$.itemsPerPage", equalTo(2)))
      .andExpect(jsonPath("$.startIndex", equalTo(1)))
      .andExpect(jsonPath("$.schemas", contains(SCHEMA)))
      .andExpect(jsonPath("$.Resources", hasSize(equalTo(2))))
      .andExpect(jsonPath("$.Resources[0].id").exists())
      .andExpect(jsonPath("$.Resources[0].schemas").exists())
      .andExpect(jsonPath("$.Resources[0].userName").exists())
      .andExpect(jsonPath("$.Resources[0].emails").exists())
      .andExpect(jsonPath("$.Resources[0].displayName").doesNotExist())
      .andExpect(jsonPath("$.Resources[0].nickName").doesNotExist())
      .andExpect(jsonPath("$.Resources[0].profileUrl").doesNotExist())
      .andExpect(jsonPath("$.Resources[0].locale").doesNotExist())
      .andExpect(jsonPath("$.Resources[0].timezone").doesNotExist())
      .andExpect(jsonPath("$.Resources[0].active").doesNotExist())
      .andExpect(jsonPath("$.Resources[0].title").doesNotExist())
      .andExpect(jsonPath("$.Resources[0].addresses").doesNotExist())
      .andExpect(jsonPath("$.Resources[0].certificates").doesNotExist())
      .andExpect(jsonPath("$.Resources[0].groups").doesNotExist())
      .andExpect(jsonPath("$.Resources[0].urn:indigo-dc:scim:schemas:IndigoUser").exists());

  }

}
