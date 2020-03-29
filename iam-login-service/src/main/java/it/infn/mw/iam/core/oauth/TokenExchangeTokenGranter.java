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

import static java.lang.String.format;

import java.util.Optional;
import java.util.Set;

import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.model.OAuth2AccessTokenEntity;
import org.mitre.oauth2.service.ClientDetailsEntityService;
import org.mitre.oauth2.service.OAuth2TokenEntityService;
import org.mitre.oauth2.service.SystemScopeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AbstractTokenGranter;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import it.infn.mw.iam.api.account.AccountUtils;
import it.infn.mw.iam.api.aup.AUPSignatureCheckService;
import it.infn.mw.iam.persistence.model.IamAccount;

public class TokenExchangeTokenGranter extends AbstractTokenGranter {

  public static final Logger LOG = LoggerFactory.getLogger(TokenExchangeTokenGranter.class);

  public static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:token-exchange";
  private static final String TOKEN_TYPE = "urn:ietf:params:oauth:token-type:jwt";
  private static final String AUDIENCE_FIELD = "audience";

  private final OAuth2TokenEntityService tokenServices;
  private final SystemScopeService systemScopeService;

  private AccountUtils accountUtils;
  private AUPSignatureCheckService signatureCheckService;


  @Autowired
  public TokenExchangeTokenGranter(final OAuth2TokenEntityService tokenServices,
      final ClientDetailsEntityService clientDetailsService,
      final OAuth2RequestFactory requestFactory, final SystemScopeService systemScopeService) {
    super(tokenServices, clientDetailsService, requestFactory, GRANT_TYPE);
    this.tokenServices = tokenServices;
    this.systemScopeService = systemScopeService;
  }

  protected Set<String> loadSystemScopes() {
    final Set<String> systemScopes = Sets.newHashSet();

    systemScopes.addAll(systemScopeService.toStrings(systemScopeService.getUnrestricted()));
    systemScopes.addAll(systemScopeService.toStrings(systemScopeService.getRestricted()));

    return systemScopes;
  }

  protected void validateScopeExchange(final ClientDetails actorClient,
      final TokenRequest tokenRequest, OAuth2AccessTokenEntity subjectToken) {

    /**
     * These scopes, in order to be "exchanged" across services, need to be present in the set of
     * scopes linked to the subject token that is presented for the exchange.
     */
    final Set<String> systemScopes = loadSystemScopes();

    String audience = tokenRequest.getRequestParameters().get(AUDIENCE_FIELD);

    ClientDetailsEntity subjectClient = subjectToken.getClient();

    Set<String> requestedScopes = tokenRequest.getScope();
    Set<String> actorScopes = actorClient.getScope();
    Set<String> subjectTokenScopes = subjectToken.getScope();

    LOG.info(
        "Client '{}' requests token exchange from client '{}' to impersonate user '{}' on audience '{}' with scopes '{}'",
        actorClient.getClientId(), subjectClient.getClientId(),
        subjectToken.getAuthenticationHolder().getUserAuth().getName(), audience, requestedScopes);

    LOG.debug("System scopes: {}", systemScopes);
    LOG.debug("Requested scopes: {}", requestedScopes);
    LOG.debug("Actor scopes: {}", actorScopes);
    LOG.debug("Subject token scopes: {}", subjectTokenScopes);

    // Ensure that actor can only request scopes allowed by its configuration
    if (!actorScopes.containsAll(requestedScopes)) {
      String errorMsg =
          String.format("Client '%s' requested a scope that is not allowed to request",
              actorClient.getClientId());

      throw new InvalidScopeException(errorMsg, requestedScopes);
    }

    for (String rs : requestedScopes) {
      if (systemScopes.contains(rs) && !subjectTokenScopes.contains(rs)) {
        LOG.error(
            "Requested scope '{}' is an IAM system scope but not linked to subject token scopes",
            rs);
        throw new InvalidScopeException(String.format(
            "Requested scope '%s' is an IAM system scope but not linked to subject token", rs));
      }
    }
  }

  @Override
  protected OAuth2Authentication getOAuth2Authentication(final ClientDetails actorClient,
      final TokenRequest tokenRequest) {

    if (tokenRequest.getRequestParameters().get("actor_token") != null
        || tokenRequest.getRequestParameters().get("want_composite") != null) {
      throw new InvalidRequestException("Delegation not supported");
    }


    String subjectTokenValue = tokenRequest.getRequestParameters().get("subject_token");
    OAuth2AccessTokenEntity subjectToken = tokenServices.readAccessToken(subjectTokenValue);

    // Does token exchange among clients acting on behalf of themselves make sense?
    if (subjectToken.getAuthenticationHolder().getUserAuth() == null) {
      throw new InvalidRequestException("No user identity linked to subject token.");
    }

    Optional<IamAccount> account = accountUtils
      .getAuthenticatedUserAccount(subjectToken.getAuthenticationHolder().getUserAuth());
    
    if (account.isPresent() && signatureCheckService.needsAupSignature(account.get())) {
      throw new InvalidGrantException(format("User '%s' needs to sign AUP for this organization "
          + "in order to proceed.", account.get().getUsername()));
    }

    validateScopeExchange(actorClient, tokenRequest, subjectToken);

    OAuth2Authentication authentication =
        new OAuth2Authentication(getRequestFactory().createOAuth2Request(actorClient, tokenRequest),
            subjectToken.getAuthenticationHolder().getAuthentication().getUserAuthentication());

    String audience = tokenRequest.getRequestParameters().get(AUDIENCE_FIELD);
    if (!Strings.isNullOrEmpty(audience)) {
      authentication.getOAuth2Request().getExtensions().put("aud", audience);
    }

    return authentication;
  }

  @Override
  protected OAuth2AccessToken getAccessToken(final ClientDetails client,
      final TokenRequest tokenRequest) {

    OAuth2Authentication auth = getOAuth2Authentication(client, tokenRequest);
    OAuth2AccessToken accessToken = tokenServices.createAccessToken(auth);
    accessToken.getAdditionalInformation().put("issued_token_type", TOKEN_TYPE);

    return accessToken;
  }

  public void setAccountUtils(AccountUtils accountUtils) {
    this.accountUtils = accountUtils;
  }

  public void setSignatureCheckService(AUPSignatureCheckService signatureCheckService) {
    this.signatureCheckService = signatureCheckService;
  }



}
