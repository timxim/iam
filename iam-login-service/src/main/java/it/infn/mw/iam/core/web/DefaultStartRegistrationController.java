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
package it.infn.mw.iam.core.web;

import static it.infn.mw.iam.authn.ExternalAuthenticationHandlerSupport.EXT_AUTHN_UNREGISTERED_USER_AUTH;
import static java.util.Objects.isNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class DefaultStartRegistrationController {

  public static final String REGISTRATION_PROFILE = "registration";
  public static final String CERN_PROFILE = "cern";

  private boolean registrationProfileEnabled;
  private boolean cernProfileEnabled;

  @Autowired
  public DefaultStartRegistrationController(Environment env) {
    registrationProfileEnabled = cernProfileEnabled = false;

    for (String ap : env.getActiveProfiles()) {
      if (REGISTRATION_PROFILE.equals(ap)) {
        registrationProfileEnabled = true;
      }

      if (CERN_PROFILE.equals(ap)) {
        cernProfileEnabled = true;
      }
    }
  }

  @RequestMapping(method = RequestMethod.GET, path = "/start-registration")
  public String startRegistration(Authentication authentication) {

    if (!isNull(authentication) && authentication.isAuthenticated()
        && !authentication.getAuthorities().contains(EXT_AUTHN_UNREGISTERED_USER_AUTH)) {
      return "iam/dashboard";
    }

    if (registrationProfileEnabled) {
      if (cernProfileEnabled) {
        return "redirect:/cern-registration";
      } else {
        return "iam/register";
      }
    } else {
      return "iam/registrationDisabled";
    }
  }

}
