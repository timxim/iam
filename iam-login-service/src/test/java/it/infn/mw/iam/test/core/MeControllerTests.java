package it.infn.mw.iam.test.core;

import static it.infn.mw.iam.test.scim.ScimUtils.SCIM_CLIENT_ID;
import static it.infn.mw.iam.test.scim.ScimUtils.SCIM_READ_SCOPE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.test.scim.ScimRestUtilsMvc;
import it.infn.mw.iam.test.util.WithMockOAuthUser;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(
    classes = {IamLoginService.class, CoreControllerTestSupport.class, ScimRestUtilsMvc.class})
@WebAppConfiguration
public class MeControllerTests {

  @Autowired
  private ScimRestUtilsMvc restUtils;

  private final static String TESTUSER_USERNAME = "test_101";
  private final static String NOT_FOUND_USERNAME = "not_found";

  @Before
  public void setup() {}

  @Test
  @WithMockOAuthUser(user = TESTUSER_USERNAME, authorities = {})
  public void insufficientScopeUser() throws Exception {

    restUtils.getMe(FORBIDDEN);
  }

  @Test
  @WithMockOAuthUser(user = NOT_FOUND_USERNAME, authorities = {"ROLE_USER"})
  public void notFoundUser() throws Exception {

    restUtils.getMe(NOT_FOUND);
  }

  @Test
  @WithMockOAuthUser(user = TESTUSER_USERNAME, authorities = {"ROLE_USER"})
  public void authenticatedUser() throws Exception {

    assertThat(restUtils.getMe().getUserName(), equalTo(TESTUSER_USERNAME));
  }

  @Test
  @WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE})
  public void notAuthorizedClient() throws Exception {

    restUtils.getMe(BAD_REQUEST)
      .andExpect(jsonPath("$.detail", is("No user linked to the current OAuth token")));

  }
}
