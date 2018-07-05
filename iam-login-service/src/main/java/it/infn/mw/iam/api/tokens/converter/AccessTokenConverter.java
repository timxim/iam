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
import org.mitre.oauth2.model.OAuth2AccessTokenEntity;
import org.mitre.oauth2.model.SavedUserAuthentication;
import org.springframework.stereotype.Component;
import it.infn.mw.iam.api.tokens.model.AccessToken;

@Component
public class AccessTokenConverter
    extends AbstractTokenConverter<AccessToken, OAuth2AccessTokenEntity> {

  @Override
  public OAuth2AccessTokenEntity entityFromDto(AccessToken dto) {

    return null;
  }

  @Override
  public AccessToken dtoFromEntity(OAuth2AccessTokenEntity entity) {

    AuthenticationHolderEntity ah = entity.getAuthenticationHolder();

    AccessToken.Builder builder = AccessToken.builder();
    builder.id(entity.getId());
    builder.expiration(entity.getExpiration());
    builder.scopes(entity.getScope());
    builder.value(entity.getValue());

    String clientId = ah.getClientId();
    if (clientId != null) {
      builder.client(buildClientRef(clientId));
    }

    SavedUserAuthentication userAuth = ah.getUserAuth();
    if (userAuth != null) {
      builder.user(buildUserRef(userAuth));
    }

    return builder.build();
  }

}
