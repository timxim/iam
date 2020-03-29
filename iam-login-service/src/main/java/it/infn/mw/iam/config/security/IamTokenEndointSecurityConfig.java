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
package it.infn.mw.iam.config.security;

import static org.springframework.http.HttpMethod.OPTIONS;

import org.mitre.oauth2.web.CorsFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.provider.client.ClientCredentialsTokenEndpointFilter;
import org.springframework.security.oauth2.provider.error.OAuth2AccessDeniedHandler;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;

@Configuration
@Order(-1)
public class IamTokenEndointSecurityConfig extends WebSecurityConfigurerAdapter {

  public static final String TOKEN_ENDPOINT = "/token";
  
  @Autowired
  private CorsFilter corsFilter;

  @Autowired
  private OAuth2AuthenticationEntryPoint authenticationEntryPoint;

  @Autowired
  @Qualifier("clientUserDetailsService")
  private UserDetailsService userDetailsService;

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth.userDetailsService(userDetailsService);
  }

  @Bean
  public ClientCredentialsTokenEndpointFilter ccFilter() throws Exception {
    ClientCredentialsTokenEndpointFilter filter =
        new ClientCredentialsTokenEndpointFilter(TOKEN_ENDPOINT);
    filter.setAuthenticationManager(authenticationManager());
    return filter;
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {

    // @formatter:off
    http
        .requestMatchers()
            .antMatchers(TOKEN_ENDPOINT)
            .and()
        .httpBasic()
            .authenticationEntryPoint(authenticationEntryPoint)
            .and()
        .authorizeRequests()
            .antMatchers(OPTIONS, TOKEN_ENDPOINT).permitAll()
            .antMatchers(TOKEN_ENDPOINT).authenticated()
            .and()
        .addFilterBefore(ccFilter(), AbstractPreAuthenticatedProcessingFilter.class)   
        .addFilterBefore(corsFilter, SecurityContextPersistenceFilter.class)
        .exceptionHandling()
            .authenticationEntryPoint(authenticationEntryPoint)
            .accessDeniedHandler(new OAuth2AccessDeniedHandler())
            .and()
        .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
          .csrf()
            .disable();
    // @formatter:on

  }
}
