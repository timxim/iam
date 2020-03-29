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

import static it.infn.mw.iam.authn.ExternalAuthenticationHandlerSupport.EXT_AUTHN_UNREGISTERED_USER_AUTH;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import it.infn.mw.iam.authn.oidc.OidcAccessDeniedHandler;
import it.infn.mw.iam.config.cern.CernProperties;

@Profile("cern")
@Order(99)
@Configuration
public class CernRegistrationSecurityConfig extends WebSecurityConfigurerAdapter {

  @Autowired
  CernProperties properties;

  AuthenticationEntryPoint entryPoint() {
    String discoveryId = String.format("/saml/login?idp=%s", properties.getSsoEntityId());
    return new LoginUrlAuthenticationEntryPoint(discoveryId);
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {

    // @formatter:off
    http.requestMatchers()
      .antMatchers("/cern-registration")
      .and()
      .sessionManagement()
        .enableSessionUrlRewriting(false)
      .and()
        .authorizeRequests()
          .antMatchers("/cern-registration")
            .hasAuthority(EXT_AUTHN_UNREGISTERED_USER_AUTH.getAuthority())
      .and()
        .exceptionHandling()
          .accessDeniedHandler(new OidcAccessDeniedHandler())
          .authenticationEntryPoint(entryPoint());
      // @formatter:on
  }
}
