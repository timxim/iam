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
package it.infn.mw.iam.api.tokens.converter;

import org.mitre.oauth2.model.AuthenticationHolderEntity;
import org.mitre.oauth2.model.OAuth2RefreshTokenEntity;
import org.springframework.stereotype.Component;
import it.infn.mw.iam.api.tokens.model.ClientRef;
import it.infn.mw.iam.api.tokens.model.RefreshToken;
import it.infn.mw.iam.api.tokens.model.UserRef;

@Component
public class RefreshTokenConverter
    extends AbstractTokenConverter<RefreshToken, OAuth2RefreshTokenEntity> {

  @Override
  public OAuth2RefreshTokenEntity entityFromDto(RefreshToken dto) {

    return null;
  }

  @Override
  public RefreshToken dtoFromEntity(OAuth2RefreshTokenEntity entity) {

    AuthenticationHolderEntity ah = entity.getAuthenticationHolder();

    ClientRef clientRef = buildClientRef(ah.getClientId());
    UserRef userRef = buildUserRef(ah.getUserAuth());

    // @formatter:off
    return RefreshToken.builder()
        .id(entity.getId())
        .client(clientRef)
        .expiration(entity.getExpiration()).user(userRef).value(entity.getValue()).build();
    // @formatter:on
  }

}
