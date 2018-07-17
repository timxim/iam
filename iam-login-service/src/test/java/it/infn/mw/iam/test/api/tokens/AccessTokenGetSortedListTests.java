/**
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2016-2018
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.infn.mw.iam.test.api.tokens;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.model.OAuth2AccessTokenEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.MultiValueMap;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.common.ListResponseDTO;
import it.infn.mw.iam.api.tokens.model.AccessToken;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.util.WithMockOAuthUser;
import it.infn.mw.iam.test.util.oauth.MockOAuth2Filter;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class, CoreControllerTestSupport.class})
@WebAppConfiguration
@WithMockOAuthUser(user = "admin", authorities = {"ROLE_ADMIN"})
public class AccessTokenGetSortedListTests extends TestTokensUtils {

  public static long id = 1L;

  public static final String[] SCOPES = {"openid", "profile"};

  public static final String TEST_CLIENT_ID = "token-lookup-client";
  public static final String TEST_CLIENT2_ID = "password-grant";

  private static final String TESTUSER_USERNAME = "test_102";
  private static final String TESTUSER_USERNAME2 = "test_103";

  @Autowired
  private MockOAuth2Filter mockOAuth2Filter;

  @Before
  public void setup() {
    clearAllTokens();
    mockOAuth2Filter.cleanupSecurityContext();
    initMvc();
  }

  @After
  public void teardown() {
    clearAllTokens();
    mockOAuth2Filter.cleanupSecurityContext();
  }

  @Test
  public void getAccessTokenListSortedByExpirationDesc() throws Exception {

    ClientDetailsEntity client = loadTestClient(TEST_CLIENT_ID);

    OAuth2AccessTokenEntity e0 = buildAccessToken(client, TESTUSER_USERNAME, SCOPES, getDateOffsetBy(1));
    OAuth2AccessTokenEntity e1 = buildAccessToken(client, TESTUSER_USERNAME, SCOPES, getDateOffsetBy(2));

    MultiValueMap<String, String> params =
        MultiValueMapBuilder.builder().sortByExpiration().sortDirectionDesc().build();

    ListResponseDTO<AccessToken> tList = getAccessTokenList(params);

    assertThat(tList.getResources().size(), equalTo(2));
    assertThat(tList.getResources().get(0).getId(), equalTo(e1.getId()));
    assertThat(tList.getResources().get(1).getId(), equalTo(e0.getId()));
  }

  @Test
  public void getAccessTokenListSortedByExpirationAsc() throws Exception {

    ClientDetailsEntity client = loadTestClient(TEST_CLIENT_ID);

    OAuth2AccessTokenEntity e0 = buildAccessToken(client, TESTUSER_USERNAME, SCOPES, getDateOffsetBy(1));
    OAuth2AccessTokenEntity e1 = buildAccessToken(client, TESTUSER_USERNAME, SCOPES, getDateOffsetBy(2));

    MultiValueMap<String, String> params =
        MultiValueMapBuilder.builder().sortByExpiration().sortDirectionAsc().build();

    ListResponseDTO<AccessToken> tList = getAccessTokenList(params);

    assertThat(tList.getResources().size(), equalTo(2));
    assertThat(tList.getResources().get(0).getId(), equalTo(e0.getId()));
    assertThat(tList.getResources().get(1).getId(), equalTo(e1.getId()));
  }

  @Test
  public void getAccessTokenListSortedByClientDesc() throws Exception {

    ClientDetailsEntity c0 = loadTestClient(TEST_CLIENT_ID);
    ClientDetailsEntity c1 = loadTestClient(TEST_CLIENT2_ID);

    OAuth2AccessTokenEntity e0 = buildAccessToken(c0, TESTUSER_USERNAME, SCOPES);
    OAuth2AccessTokenEntity e1 = buildAccessToken(c1, TESTUSER_USERNAME, SCOPES);

    MultiValueMap<String, String> params =
        MultiValueMapBuilder.builder().sortByClient().sortDirectionDesc().build();

    ListResponseDTO<AccessToken> tList = getAccessTokenList(params);

    assertThat(tList.getResources().size(), equalTo(2));
    assertThat(tList.getResources().get(0).getId(), equalTo(e0.getId()));
    assertThat(tList.getResources().get(1).getId(), equalTo(e1.getId()));
  }

  @Test
  public void getAccessTokenListSortedByClientAsc() throws Exception {

    ClientDetailsEntity c0 = loadTestClient(TEST_CLIENT_ID);
    ClientDetailsEntity c1 = loadTestClient(TEST_CLIENT2_ID);

    OAuth2AccessTokenEntity e0 = buildAccessToken(c0, TESTUSER_USERNAME, SCOPES);
    OAuth2AccessTokenEntity e1 = buildAccessToken(c1, TESTUSER_USERNAME, SCOPES);

    MultiValueMap<String, String> params =
        MultiValueMapBuilder.builder().sortByClient().sortDirectionAsc().build();

    ListResponseDTO<AccessToken> tList = getAccessTokenList(params);

    assertThat(tList.getResources().size(), equalTo(2));
    assertThat(tList.getResources().get(0).getId(), equalTo(e1.getId()));
    assertThat(tList.getResources().get(1).getId(), equalTo(e0.getId()));
  }

  @Test
  public void getAccessTokenListSortedByUserDesc() throws Exception {

    ClientDetailsEntity client = loadTestClient(TEST_CLIENT_ID);

    OAuth2AccessTokenEntity e0 = buildAccessToken(client, TESTUSER_USERNAME, SCOPES);
    OAuth2AccessTokenEntity e1 = buildAccessToken(client, TESTUSER_USERNAME2, SCOPES);

    MultiValueMap<String, String> params =
        MultiValueMapBuilder.builder().sortByUser().sortDirectionDesc().build();

    ListResponseDTO<AccessToken> tList = getAccessTokenList(params);

    assertThat(tList.getResources().size(), equalTo(2));
    assertThat(tList.getResources().get(0).getId(), equalTo(e1.getId()));
    assertThat(tList.getResources().get(1).getId(), equalTo(e0.getId()));
  }

  @Test
  public void getAccessTokenListSortedByUserAsc() throws Exception {

    ClientDetailsEntity client = loadTestClient(TEST_CLIENT_ID);

    OAuth2AccessTokenEntity e0 = buildAccessToken(client, TESTUSER_USERNAME, SCOPES);
    OAuth2AccessTokenEntity e1 = buildAccessToken(client, TESTUSER_USERNAME2, SCOPES);

    MultiValueMap<String, String> params =
        MultiValueMapBuilder.builder().sortByUser().sortDirectionAsc().build();

    ListResponseDTO<AccessToken> tList = getAccessTokenList(params);

    assertThat(tList.getResources().size(), equalTo(2));
    assertThat(tList.getResources().get(0).getId(), equalTo(e0.getId()));
    assertThat(tList.getResources().get(1).getId(), equalTo(e1.getId()));
  }
}
