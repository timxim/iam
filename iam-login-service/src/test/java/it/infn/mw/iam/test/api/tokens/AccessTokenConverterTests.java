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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.model.OAuth2AccessTokenEntity;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.common.Converter;
import it.infn.mw.iam.api.scim.converter.ScimResourceLocationProvider;
import it.infn.mw.iam.api.tokens.model.AccessToken;
import it.infn.mw.iam.persistence.model.IamAccount;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class, CoreControllerTestSupport.class})
@WebAppConfiguration
public class AccessTokenConverterTests extends TestTokensUtils {

  public static final String[] SCOPES = {"openid", "profile"};

  public static final String TEST_CLIENT_ID = "token-lookup-client";
  private static final String TESTUSER_USERNAME = "test_105";

  @Autowired
  private Converter<AccessToken, OAuth2AccessTokenEntity> converter;

  @Autowired
  private ScimResourceLocationProvider locationProvider;

  @Test
  public void coverNotImplementedMethod() {
    assertThat(converter.entityFromDto(Mockito.mock(AccessToken.class)), equalTo(null));
  }

  @Test
  public void convertEntityToDTO() {

    ClientDetailsEntity client = loadTestClient(TEST_CLIENT_ID);
    IamAccount user = loadTestUser(TESTUSER_USERNAME);
    OAuth2AccessTokenEntity entity = buildAccessToken(client, TESTUSER_USERNAME, SCOPES);

    AccessToken at = converter.dtoFromEntity(entity);

    assertThat(at.getClient().getClientId(), equalTo(client.getClientId()));
    assertThat(at.getClient().getId(), equalTo(client.getId()));
    assertThat(at.getExpiration(), equalTo(entity.getExpiration()));
    assertThat(at.getId(), equalTo(entity.getId()));
    assertThat(at.getScopes(), contains(SCOPES));
    assertThat(at.getValue(), equalTo(entity.getValue()));
    assertThat(at.getUser().getId(), equalTo(user.getUuid()));
    assertThat(at.getUser().getUserName(), equalTo(user.getUsername()));
    assertThat(at.getUser().getRef(), equalTo(locationProvider.userLocation(user.getUuid())));
  }
}
