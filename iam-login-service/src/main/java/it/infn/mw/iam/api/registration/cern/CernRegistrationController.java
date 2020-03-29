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
package it.infn.mw.iam.api.registration.cern;

import static java.util.Objects.isNull;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import it.infn.mw.iam.authn.ExternalAuthenticationInfoBuilder;
import it.infn.mw.iam.authn.saml.SamlExternalAuthenticationToken;
import it.infn.mw.iam.authn.saml.util.Saml2Attribute;
import it.infn.mw.iam.config.cern.CernProperties;

@Profile("cern")
@Controller
public class CernRegistrationController {

  public static final String REGISTRATION_PROFILE = "registration";

  private boolean registrationProfileEnabled;

  private final CernProperties cernProperties;

  private final CernHrDBApiService hrService;
  private final ExternalAuthenticationInfoBuilder infoBuilder;

  @Autowired
  public CernRegistrationController(CernHrDBApiService hrService, CernProperties props,
      ExternalAuthenticationInfoBuilder infoBuilder, Environment env) {
    registrationProfileEnabled = false;

    for (String ap : env.getActiveProfiles()) {
      if (REGISTRATION_PROFILE.equals(ap)) {
        registrationProfileEnabled = true;
      }
    }

    this.infoBuilder = infoBuilder;
    this.cernProperties = props;
    this.hrService = hrService;
  }

  private void checkUnregisteredUserIsFromCernSSO(Authentication authentication) {
    if (authentication instanceof SamlExternalAuthenticationToken) {
      SamlExternalAuthenticationToken token = (SamlExternalAuthenticationToken) authentication;

      if (!cernProperties.getSsoEntityId().equals(token.getSamlId().getIdpId())) {
        throw new AccessDeniedException("CERN SSO authentication is required");
      }
    }
  }

  private String resolvePersonId(Authentication authentication) {
    SamlExternalAuthenticationToken token = (SamlExternalAuthenticationToken) authentication;
    SAMLCredential cred = (SAMLCredential) token.getCredentials();

    String personId = cred.getAttributeAsString(Saml2Attribute.CERN_PERSON_ID.getAttributeName());
    if (isNull(personId)) {
      throw new AccessDeniedException("CERN person id not found in SAML assertion");
    }

    return personId;
  }

  @RequestMapping(method = GET, path = "/cern-registration")
  public ModelAndView startRegistration(Authentication authentication) {

    ModelAndView mav = new ModelAndView();

    if (registrationProfileEnabled) {
      checkUnregisteredUserIsFromCernSSO(authentication);
      if (hrService.hasValidExperimentParticipation(resolvePersonId(authentication))) {
        mav.setViewName("iam/cern/register");
      } else {
        Map<String, String> userInfoMap =
            ((SamlExternalAuthenticationToken) authentication).buildAuthnInfoMap(infoBuilder);
        
        mav.addObject("experiment", cernProperties.getExperimentName());
        mav.addObject("user", userInfoMap);
        mav.setViewName("iam/cern/not-a-vo-member");
      }
    } else {
      mav.setViewName("iam/registrationDisabled");
    }

    return mav;
  }

  @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(CernHrDbApiError.class)
  public ModelAndView handleValidationError(CernHrDbApiError e) {
    ModelAndView mav = new ModelAndView("iam/cern/hr-error");
    mav.addObject("hrError", e);
    return mav;
  }
}
