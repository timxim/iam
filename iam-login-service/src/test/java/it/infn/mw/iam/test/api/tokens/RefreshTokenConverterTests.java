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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.model.OAuth2RefreshTokenEntity;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.common.Converter;
import it.infn.mw.iam.api.scim.converter.ScimResourceLocationProvider;
import it.infn.mw.iam.api.tokens.model.RefreshToken;
import it.infn.mw.iam.persistence.model.IamAccount;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class, CoreControllerTestSupport.class})
@WebAppConfiguration
public class RefreshTokenConverterTests extends TestTokensUtils {

  public static final String[] SCOPES = {"openid", "profile", "offline_access"};

  public static final String TEST_CLIENT_ID = "token-exchange-subject";
  private static final String TESTUSER_USERNAME = "test_105";

  @Autowired
  private Converter<RefreshToken, OAuth2RefreshTokenEntity> converter;

  @Autowired
  private ScimResourceLocationProvider locationProvider;

  @Test
  public void coverNotImplementedMethod() {
    assertThat(converter.entityFromDto(Mockito.mock(RefreshToken.class)), equalTo(null));
  }

  @Test
  public void convertEntityToDTO() {

    ClientDetailsEntity client = loadTestClient(TEST_CLIENT_ID);
    IamAccount user = loadTestUser(TESTUSER_USERNAME);
    OAuth2RefreshTokenEntity entity =
        buildAccessToken(client, TESTUSER_USERNAME, SCOPES).getRefreshToken();

    RefreshToken rt = converter.dtoFromEntity(entity);

    assertThat(rt.getClient().getClientId(), equalTo(client.getClientId()));
    assertThat(rt.getClient().getId(), equalTo(client.getId()));
    assertThat(rt.getExpiration(), equalTo(entity.getExpiration()));
    assertThat(rt.getId(), equalTo(entity.getId()));
    assertThat(rt.getValue(), equalTo(entity.getValue()));
    assertThat(rt.getUser().getId(), equalTo(user.getUuid()));
    assertThat(rt.getUser().getUserName(), equalTo(user.getUsername()));
    assertThat(rt.getUser().getRef(), equalTo(locationProvider.userLocation(user.getUuid())));
  }
}
