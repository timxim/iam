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
package it.infn.mw.iam.authn.saml.validator;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.providers.ExpiringUsernameAuthenticationToken;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.stereotype.Service;

import it.infn.mw.iam.authn.common.ValidatorCheck;
import it.infn.mw.iam.authn.common.ValidatorError;
import it.infn.mw.iam.authn.common.ValidatorResolver;
import it.infn.mw.iam.authn.common.ValidatorResult;
import it.infn.mw.iam.authn.common.config.AuthenticationValidator;

@Service
public class DefaultSamlValidator
    implements AuthenticationValidator<ExpiringUsernameAuthenticationToken> {

  public static final Logger LOG = LoggerFactory.getLogger(DefaultSamlValidator.class);

  private final ValidatorResolver<SAMLCredential> resolver;

  @Autowired
  public DefaultSamlValidator(ValidatorResolver<SAMLCredential> resolver) {
    this.resolver = resolver;
  }

  @Override
  public void validateAuthentication(ExpiringUsernameAuthenticationToken token)
      throws ValidatorError {

    SAMLCredential samlCredentials = (SAMLCredential) token.getCredentials();

    Optional<ValidatorCheck<SAMLCredential>> checks =
        resolver.resolveChecks(samlCredentials.getRemoteEntityID());

    if (checks.isPresent()) {
      ValidatorResult result = checks.get().validate(samlCredentials);
      LOG.debug("{} validation result: {} {}", samlCredentials, result.getStatus(),
          result.getMessage());
      if (!result.isSuccess()) {
        throw new ValidatorError(result.getMessage());
      }
    } else {
      LOG.debug("No checks defined for entity: {}", samlCredentials.getRemoteEntityID());
    }
  }

}
