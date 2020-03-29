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

import java.text.ParseException;
import java.util.Date;

import org.mitre.openid.connect.client.OIDCAuthenticationProvider;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;

import it.infn.mw.iam.authn.common.config.AuthenticationValidator;
import it.infn.mw.iam.authn.oidc.service.OidcUserDetailsService;

public class OidcAuthenticationProvider extends OIDCAuthenticationProvider {

  public static final Logger LOG = LoggerFactory.getLogger(OidcAuthenticationProvider.class);

  private final OidcUserDetailsService userDetailsService;
  private final AuthenticationValidator<OIDCAuthenticationToken> tokenValidatorService;

  @Autowired
  public OidcAuthenticationProvider(OidcUserDetailsService userDetailsService,
      AuthenticationValidator<OIDCAuthenticationToken> tokenValidatorService) {

    this.userDetailsService = userDetailsService;
    this.tokenValidatorService = tokenValidatorService;
  }

  private Date getExpirationTimeFromOIDCAuthenticationToken(OIDCAuthenticationToken token) {
    try {
      return token.getIdToken().getJWTClaimsSet().getExpirationTime();
    } catch (ParseException e) {
      throw new BadCredentialsException("Could not extract expiration time from ID token", e);
    }
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {

    OIDCAuthenticationToken token = (OIDCAuthenticationToken) super.authenticate(authentication);

    if (token == null) {
      return null;
    }

    tokenValidatorService.validateAuthentication(token);

    User user = (User) userDetailsService.loadUserByOIDC(token);

    return new OidcExternalAuthenticationToken(token,
        getExpirationTimeFromOIDCAuthenticationToken(token), user.getUsername(), null,
        user.getAuthorities());
  }

}
