package it.infn.mw.iam.test.scim.user;

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
import it.infn.mw.iam.api.scim.model.ScimListResponse;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.scim.ScimRestUtilsMvc;
import it.infn.mw.iam.test.scim.ScimUtils.ParamsBuilder;
import it.infn.mw.iam.test.util.WithMockOAuthUser;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(
    classes = {IamLoginService.class, CoreControllerTestSupport.class, ScimRestUtilsMvc.class})
@WebAppConfiguration
@WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE})
public class ScimUserProvisioningListMvcTests {

  @Autowired
  private ScimRestUtilsMvc restUtils;

  @Test
  public void testNoParameterListRequest() throws Exception {

    restUtils.getUsers()
      .andExpect(jsonPath("$.totalResults", equalTo(TOTAL_USERS_COUNT)))
      .andExpect(jsonPath("$.itemsPerPage", equalTo(100)))
      .andExpect(jsonPath("$.startIndex", equalTo(1)))
      .andExpect(jsonPath("$.schemas", contains(ScimListResponse.SCHEMA)))
      .andExpect(jsonPath("$.Resources", hasSize(equalTo(100))));
  }

  @Test
  public void testCountAs10Returns10Items() throws Exception {

    restUtils.getUsers(ParamsBuilder.builder().count(10).build())
      .andExpect(jsonPath("$.totalResults", equalTo(TOTAL_USERS_COUNT)))
      .andExpect(jsonPath("$.itemsPerPage", equalTo(10)))
      .andExpect(jsonPath("$.startIndex", equalTo(1)))
      .andExpect(jsonPath("$.schemas", contains(ScimListResponse.SCHEMA)))
      .andExpect(jsonPath("$.Resources", hasSize(equalTo(10))));
  }

  @Test
  public void testCount1Returns1Item() throws Exception {

    restUtils.getUsers(ParamsBuilder.builder().count(1).build())
      .andExpect(jsonPath("$.totalResults", equalTo(TOTAL_USERS_COUNT)))
      .andExpect(jsonPath("$.itemsPerPage", equalTo(1)))
      .andExpect(jsonPath("$.startIndex", equalTo(1)))
      .andExpect(jsonPath("$.schemas", contains(ScimListResponse.SCHEMA)))
      .andExpect(jsonPath("$.Resources", hasSize(equalTo(1))));
  }

  @Test
  public void testCountShouldBeLimitedToOneHundred() throws Exception {

    restUtils.getUsers(ParamsBuilder.builder().count(1000).build())
      .andExpect(jsonPath("$.totalResults", equalTo(TOTAL_USERS_COUNT)))
      .andExpect(jsonPath("$.itemsPerPage", equalTo(100)))
      .andExpect(jsonPath("$.startIndex", equalTo(1)))
      .andExpect(jsonPath("$.schemas", contains(ScimListResponse.SCHEMA)))
      .andExpect(jsonPath("$.Resources", hasSize(equalTo(100))));
  }

  @Test
  public void testNegativeCountBecomesZero() throws Exception {

    restUtils.getUsers(ParamsBuilder.builder().count(-10).build())
      .andExpect(jsonPath("$.totalResults", equalTo(TOTAL_USERS_COUNT)))
      .andExpect(jsonPath("$.itemsPerPage", equalTo(0)))
      .andExpect(jsonPath("$.startIndex", equalTo(1)))
      .andExpect(jsonPath("$.schemas", contains(ScimListResponse.SCHEMA)))
      .andExpect(jsonPath("$.Resources", hasSize(equalTo(0))));
  }

  @Test
  public void testInvalidStartIndex() throws Exception {

    restUtils.getUsers(ParamsBuilder.builder().startIndex(TOTAL_USERS_COUNT + 1).build())
      .andExpect(jsonPath("$.totalResults", equalTo(TOTAL_USERS_COUNT)))
      .andExpect(jsonPath("$.itemsPerPage", equalTo(0)))
      .andExpect(jsonPath("$.startIndex", equalTo(TOTAL_USERS_COUNT + 1)))
      .andExpect(jsonPath("$.schemas", contains(SCHEMA)))
      .andExpect(jsonPath("$.Resources", hasSize(equalTo(0))));
  }

  @Test
  public void testRightEndPagination() throws Exception {

    restUtils.getUsers(ParamsBuilder.builder().startIndex(TOTAL_USERS_COUNT - 5).count(10).build())
      .andExpect(jsonPath("$.totalResults", equalTo(TOTAL_USERS_COUNT)))
      .andExpect(jsonPath("$.itemsPerPage", equalTo(6)))
      .andExpect(jsonPath("$.startIndex", equalTo(TOTAL_USERS_COUNT - 5)))
      .andExpect(jsonPath("$.schemas", contains(SCHEMA)))
      .andExpect(jsonPath("$.Resources", hasSize(equalTo(6))));
  }

  @Test
  public void testLastElementPagination() throws Exception {

    restUtils.getUsers(ParamsBuilder.builder().startIndex(TOTAL_USERS_COUNT).count(2).build())
      .andExpect(jsonPath("$.totalResults", equalTo(TOTAL_USERS_COUNT)))
      .andExpect(jsonPath("$.itemsPerPage", equalTo(1)))
      .andExpect(jsonPath("$.startIndex", equalTo(TOTAL_USERS_COUNT)))
      .andExpect(jsonPath("$.schemas", contains(SCHEMA)))
      .andExpect(jsonPath("$.Resources", hasSize(equalTo(1))));
  }

  @Test
  public void testFirstElementPagination() throws Exception {

    restUtils.getUsers(ParamsBuilder.builder().startIndex(1).count(5).build())
      .andExpect(jsonPath("$.totalResults", equalTo(TOTAL_USERS_COUNT)))
      .andExpect(jsonPath("$.itemsPerPage", equalTo(5)))
      .andExpect(jsonPath("$.startIndex", equalTo(1)))
      .andExpect(jsonPath("$.schemas", contains(SCHEMA)))
      .andExpect(jsonPath("$.Resources", hasSize(equalTo(5))));
  }
}
