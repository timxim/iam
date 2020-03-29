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
package it.infn.mw.iam.authn.oidc;

import static it.infn.mw.iam.core.oauth.ClaimValueHelper.ADDITIONAL_CLAIMS;
import static java.util.stream.Collectors.joining;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.model.OAuth2AccessTokenEntity;
import org.mitre.oauth2.service.SystemScopeService;
import org.mitre.openid.connect.model.UserInfo;
import org.mitre.openid.connect.service.OIDCTokenService;
import org.mitre.openid.connect.service.UserInfoService;
import org.mitre.openid.connect.token.ConnectTokenEnhancer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTClaimsSet.Builder;
import com.nimbusds.jwt.SignedJWT;

import it.infn.mw.iam.core.oauth.ClaimValueHelper;
import it.infn.mw.iam.core.oauth.scope.IamScopeFilter;
import it.infn.mw.iam.core.userinfo.IamScopeClaimTranslationService;
import it.infn.mw.iam.persistence.repository.UserInfoAdapter;

public class OidcTokenEnhancer extends ConnectTokenEnhancer {

  @Autowired
  private UserInfoService userInfoService;

  @Autowired
  private OIDCTokenService connectTokenService;

  @Autowired
  private IamScopeFilter scopeFilter;

  @Autowired
  private IamScopeClaimTranslationService scopeClaimConverter;

  @Autowired
  private ClaimValueHelper claimValueHelper;

  @Value("${iam.access_token.include_authn_info}")
  private Boolean includeAuthnInfo;
  
  @Value("${iam.access_token.include_scope}")
  private Boolean includeScope;
  
  @Value("${iam.access_token.include_nbf}")
  private Boolean includeNbf;

  private static final String AUD_KEY = "aud";
  private static final String SCOPE_CLAIM_NAME = "scope";
  private static final String SPACE = " ";

  private SignedJWT signClaims(JWTClaimsSet claims) {
    JWSAlgorithm signingAlg = getJwtService().getDefaultSigningAlgorithm();

    JWSHeader header = new JWSHeader(signingAlg, null, null, null, null, null, null, null, null,
        null, getJwtService().getDefaultSignerKeyId(), null, null);
    SignedJWT signedJWT = new SignedJWT(header, claims);

    getJwtService().signJwt(signedJWT);
    return signedJWT;

  }

  protected OAuth2AccessTokenEntity buildAccessToken(OAuth2AccessToken accessToken,
      OAuth2Authentication authentication, UserInfo userInfo, Date issueTime) {

    OAuth2AccessTokenEntity token = (OAuth2AccessTokenEntity) accessToken;

    String subject = null;

    if (userInfo == null) {
      subject = authentication.getName();
    } else {
      subject = userInfo.getSub();
    }

    // @formatter:off
    Builder builder = 
        new JWTClaimsSet.Builder()
          .issuer(getConfigBean().getIssuer())
          .issueTime(issueTime)
          .expirationTime(token.getExpiration())
          .subject(subject)
          .jwtID(UUID.randomUUID().toString());
    // @formatter:on

    String audience = (String) authentication.getOAuth2Request().getExtensions().get(AUD_KEY);

    if (!Strings.isNullOrEmpty(audience)) {
      builder.audience(Lists.newArrayList(audience));
    }

    if (includeAuthnInfo && userInfo != null) {
      Set<String> requiredClaims = scopeClaimConverter.getClaimsForScopeSet(token.getScope());
      requiredClaims.stream().filter(ADDITIONAL_CLAIMS::contains).forEach(c -> builder.claim(c,
          claimValueHelper.getClaimValueFromUserInfo(c, ((UserInfoAdapter) userInfo).getUserinfo())));
    }
    
    if (includeScope && !accessToken.getScope().isEmpty()) {
      final String scopeString = accessToken.getScope().stream().collect(joining(SPACE));
      builder.claim(SCOPE_CLAIM_NAME, scopeString);
    }
    
    if (includeNbf) {
      builder.notBeforeTime(issueTime);
    }

    JWTClaimsSet claims = builder.build();
    token.setJwt(signClaims(claims));

    return token;

  }


  @Override
  public OAuth2AccessToken enhance(OAuth2AccessToken accessToken,
      OAuth2Authentication authentication) {

    OAuth2Request originalAuthRequest = authentication.getOAuth2Request();

    String username = authentication.getName();
    String clientId = originalAuthRequest.getClientId();

    UserInfo userInfo = userInfoService.getByUsernameAndClientId(username, clientId);

    scopeFilter.filterScopes(accessToken.getScope(), authentication);

    Date issueTime = new Date();
    OAuth2AccessTokenEntity accessTokenEntity =
        buildAccessToken(accessToken, authentication, userInfo, issueTime);

    /**
     * Authorization request scope MUST include "openid" in OIDC, but access token request may or
     * may not include the scope parameter. As long as the AuthorizationRequest has the proper
     * scope, we can consider this a valid OpenID Connect request. Otherwise, we consider it to be a
     * vanilla OAuth2 request.
     * 
     * Also, there must be a user authentication involved in the request for it to be considered
     * OIDC and not OAuth, so we check for that as well.
     */
    if (originalAuthRequest.getScope().contains(SystemScopeService.OPENID_SCOPE)
        && !authentication.isClientOnly()) {

      ClientDetailsEntity client = getClientService().loadClientByClientId(clientId);

      JWT idToken = connectTokenService.createIdToken(client, originalAuthRequest, issueTime,
          userInfo.getSub(), accessTokenEntity);

      accessTokenEntity.setIdToken(idToken);
    }

    return accessTokenEntity;
  }

}
