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
package it.infn.mw.iam.api.tokens.service;

import java.util.Date;
import java.util.Optional;
import org.mitre.oauth2.model.OAuth2AccessTokenEntity;
import org.mitre.oauth2.service.impl.DefaultOAuth2ProviderTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import it.infn.mw.iam.api.common.OffsetPageable;
import it.infn.mw.iam.api.tokens.exception.TokenNotFoundException;
import it.infn.mw.iam.persistence.repository.IamOAuthAccessTokenRepository;

@Service
public class DefaultAccessTokenService implements TokenService<OAuth2AccessTokenEntity> {

  @Autowired
  private DefaultOAuth2ProviderTokenService tokenService;

  @Autowired
  private IamOAuthAccessTokenRepository tokenRepository;

  @Override
  public Long countAllTokens() {

    return tokenRepository.countValidAccessTokens(new Date());
  }

  @Override
  public Page<OAuth2AccessTokenEntity> getAllTokens(OffsetPageable op) {

    return tokenRepository.findAllValidAccessTokens(new Date(), op);
  }

  @Override
  public Long countTokensForUser(String userId) {

    return tokenRepository.countValidAccessTokensForUser(userId, new Date());
  }

  @Override
  public Page<OAuth2AccessTokenEntity> getTokensForUser(String userId, OffsetPageable op) {

    return tokenRepository.findValidAccessTokensForUser(userId, new Date(), op);
  }

  @Override
  public Long countTokensForClient(String clientId) {

    return tokenRepository.countValidAccessTokensForClient(clientId, new Date());
  }

  @Override
  public Page<OAuth2AccessTokenEntity> getTokensForClient(String clientId, OffsetPageable op) {

    return tokenRepository.findValidAccessTokensForClient(clientId, new Date(), op);
  }

  @Override
  public Long countTokensForUserAndClient(String userId, String clientId) {

    return tokenRepository.countValidAccessTokensForUserAndClient(userId, clientId, new Date());
  }

  @Override
  public Page<OAuth2AccessTokenEntity> getTokensForClientAndUser(String userId, String clientId,
      OffsetPageable op) {

    return tokenRepository.findValidAccessTokensForUserAndClient(userId, clientId, new Date(), op);
  }

  @Override
  public Optional<OAuth2AccessTokenEntity> getTokenById(Long id) {

    return Optional.ofNullable(tokenService.getAccessTokenById(id));
  }

  @Override
  public void revokeTokenById(Long id) {

    OAuth2AccessTokenEntity at = getTokenById(id).orElseThrow(() -> new TokenNotFoundException(id));
    tokenService.revokeAccessToken(at);
  }

  @Override
  public void deleteAllTokens() {

    tokenRepository.deleteAll();
  }
}
