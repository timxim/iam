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
package it.infn.mw.iam.api.registration.cern.mock;

import static it.infn.mw.iam.authn.ExternalAuthenticationHandlerSupport.EXT_AUTHN_UNREGISTERED_USER_AUTH;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import javax.servlet.http.HttpSession;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import it.infn.mw.iam.util.test.saml.SamlSecurityContextBuilder;

@Controller
@Profile({"mock"})
public class MockCernAuthController {

  @RequestMapping(method = GET, path = "/mock-cern-auth")
  public String mockCernAuthentication(HttpSession session) {

    SamlSecurityContextBuilder builder = new SamlSecurityContextBuilder();
    builder.authorities(EXT_AUTHN_UNREGISTERED_USER_AUTH.getAuthority())
      .subject("123456789")
      .issuer("https://cern.ch/login")
      .email("test@example.org")
      .username("testone")
      .name("Test", "One");

    // CERN registration details
    builder.cernPersonId("123456789")
      .cernFirstName("Test")
      .cernLastName("One")
      .cernEmail("test@example.org")
      .cernHomeInstitute("Test institute");

    SecurityContext samlSecurityContext = builder.buildSecurityContext();
    SecurityContextHolder.clearContext();
    SecurityContextHolder.setContext(samlSecurityContext);
    
    session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, samlSecurityContext);
    
    return "redirect:/cern-registration";
  }
}
