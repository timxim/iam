/**
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2016-2019
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
package it.infn.mw.iam.core.oauth;

import static it.infn.mw.iam.core.oauth.ClaimValueHelper.ADDITIONAL_CLAIMS;

import java.util.Set;

import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.model.OAuth2AccessTokenEntity;
import org.mitre.openid.connect.service.impl.DefaultOIDCTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.stereotype.Service;

import com.nimbusds.jwt.JWTClaimsSet.Builder;

import it.infn.mw.iam.api.account.password_reset.error.UserNotFoundError;
import it.infn.mw.iam.core.userinfo.IamScopeClaimTranslationService;
import it.infn.mw.iam.persistence.model.IamAccount;
import it.infn.mw.iam.persistence.model.IamUserInfo;
import it.infn.mw.iam.persistence.repository.IamAccountRepository;

@Service
@Primary
public class IamOIDCTokenService extends DefaultOIDCTokenService {

  @Autowired
  private IamAccountRepository iamAccountRepository;

  @Autowired
  private IamScopeClaimTranslationService scopeClaimConverter;

  @Autowired
  private ClaimValueHelper claimValueHelper;

  public IamOIDCTokenService() {
    // empty on purpose
  }

  @Override
  protected void addCustomIdTokenClaims(Builder idClaims, ClientDetailsEntity client,
      OAuth2Request request, String sub, OAuth2AccessTokenEntity accessToken) {
    IamAccount account = iamAccountRepository.findByUuid(sub)
      .orElseThrow(() -> new UserNotFoundError(String.format("No user found for uuid %s", sub)));
    IamUserInfo info = account.getUserInfo();

    Set<String> requiredClaims = scopeClaimConverter.getClaimsForScopeSet(request.getScope());

    requiredClaims.stream()
      .filter(ADDITIONAL_CLAIMS::contains)
      .forEach(c -> idClaims.claim(c, claimValueHelper.getClaimValueFromUserInfo(c, info)));

  }


}
