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

import static org.springframework.security.config.http.SessionCreationPolicy.NEVER;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import org.mitre.oauth2.web.CorsFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationProcessingFilter;
import org.springframework.security.oauth2.provider.client.ClientCredentialsTokenEndpointFilter;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;

import it.infn.mw.iam.config.client_registration.ClientRegistrationProperties;

@Configuration
public class MitreSecurityConfig {

  @Configuration
  @Order(10)
  public static class MitreApisEndpointAuthorizationConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    @Qualifier("resourceServerFilter")
    private OAuth2AuthenticationProcessingFilter resourceFilter;

    @Autowired
    private OAuth2AuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private CorsFilter corsFilter;

    @Override
    public void configure(final HttpSecurity http) throws Exception {

      // @formatter:off
      http.antMatcher("/api/**")
          .addFilterAfter(resourceFilter, SecurityContextPersistenceFilter.class)
          .addFilterBefore(corsFilter, WebAsyncManagerIntegrationFilter.class)
          .exceptionHandling()
            .authenticationEntryPoint(authenticationEntryPoint)
          .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.NEVER)
          .and()
            .csrf().disable();
      // @formatter:on

    }
  }

  @Configuration
  @Order(11)
  public static class ResourceEndpointAuthorizationConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private OAuth2AuthenticationProcessingFilter resourceFilter;

    @Autowired
    private OAuth2AuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private CorsFilter corsFilter;

    @Override

    public void configure(final HttpSecurity http) throws Exception {

      // @formatter:off
      http.antMatcher("/resource/**")
        .exceptionHandling()
          .authenticationEntryPoint(authenticationEntryPoint).and()
        .addFilterAfter(resourceFilter, SecurityContextPersistenceFilter.class)
        .addFilterBefore(corsFilter, WebAsyncManagerIntegrationFilter.class)
        .sessionManagement()
          .sessionCreationPolicy(STATELESS)
        .and()
          .authorizeRequests()
            .antMatchers("/resource/**").permitAll()
        .and()
          .csrf().disable();
      // @formatter:on
    }
  }

  @Configuration
  @Order(12)
  public static class RegisterEndpointAuthorizationConfig extends WebSecurityConfigurerAdapter {

    public static final String REGISTER_ENDPOINT_PATTERN = "/register/**";
    
    @Autowired
    private OAuth2AuthenticationProcessingFilter resourceFilter;

    @Autowired
    private OAuth2AuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private CorsFilter corsFilter;

    @Autowired
    private ClientRegistrationProperties clientRegProps;

    @Override
    public void configure(final HttpSecurity http) throws Exception {

      HttpSecurity registerEndpoint = http.antMatcher(REGISTER_ENDPOINT_PATTERN);

      registerEndpoint.exceptionHandling()
        .authenticationEntryPoint(authenticationEntryPoint)
        .and()
        .addFilterAfter(resourceFilter, SecurityContextPersistenceFilter.class)
        .addFilterBefore(corsFilter, WebAsyncManagerIntegrationFilter.class)
        .sessionManagement()
        .sessionCreationPolicy(STATELESS);

      if (ClientRegistrationProperties.AccessPolicy.ANYONE.equals(clientRegProps.getAllowFor())) {
        registerEndpoint.authorizeRequests().antMatchers(REGISTER_ENDPOINT_PATTERN).permitAll();
      } else {
        registerEndpoint.authorizeRequests()
          .antMatchers(REGISTER_ENDPOINT_PATTERN)
          .hasAnyRole("USER", "ADMIN")
          .and().sessionManagement().sessionCreationPolicy(NEVER);
      }
      
      registerEndpoint.csrf().disable();
    }
  }

  @Configuration
  @Order(13)
  public static class UserInfoEndpointAuthorizationConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private OAuth2AuthenticationProcessingFilter resourceFilter;

    @Autowired
    private OAuth2AuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private CorsFilter corsFilter;

    @Override
    public void configure(final HttpSecurity http) throws Exception {

      // @formatter:off
      http.antMatcher("/userinfo**")
        .exceptionHandling()
          .authenticationEntryPoint(authenticationEntryPoint)
        .and()
          .addFilterAfter(resourceFilter, SecurityContextPersistenceFilter.class)
          .addFilterBefore(corsFilter, WebAsyncManagerIntegrationFilter.class)
        .sessionManagement()
          .sessionCreationPolicy(STATELESS)
        .and()
          .csrf().disable();
      // @formatter:on
    }
  }

  @Configuration
  @Order(15)
  public static class IntrospectEndpointAuthorizationConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private OAuth2AuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    @Qualifier("clientUserDetailsService")
    private UserDetailsService userDetailsService;

    @Autowired
    private CorsFilter corsFilter;

    @Override
    protected void configure(final AuthenticationManagerBuilder auth) throws Exception {

      auth.userDetailsService(userDetailsService);
    }

    @Override
    protected void configure(final HttpSecurity http) throws Exception {

      // @formatter:off
      http.antMatcher("/introspect/**")
        .httpBasic()
          .authenticationEntryPoint(authenticationEntryPoint)
        .and()
          .addFilterBefore(corsFilter, SecurityContextPersistenceFilter.class)
            .exceptionHandling()
          .authenticationEntryPoint(authenticationEntryPoint)
        .and()
          .sessionManagement()
            .sessionCreationPolicy(STATELESS).and()
        .csrf()
          .disable()
        .authorizeRequests()
          .anyRequest()
            .fullyAuthenticated();
      // @formatter:on
    }
  }

  @Configuration
  @Order(16)
  public static class RevokeEndpointAuthorizationConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private OAuth2AuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    @Qualifier("clientUserDetailsService")
    private UserDetailsService userDetailsService;

    @Autowired
    private CorsFilter corsFilter;

    @Override
    protected void configure(final AuthenticationManagerBuilder auth) throws Exception {

      auth.userDetailsService(userDetailsService);
    }

    private ClientCredentialsTokenEndpointFilter clientCredentialsEndpointFilter()
        throws Exception {

      ClientCredentialsTokenEndpointFilter filter =
          new ClientCredentialsTokenEndpointFilter("/revoke");
      filter.setAuthenticationManager(authenticationManager());
      return filter;
    }

    @Override
    protected void configure(final HttpSecurity http) throws Exception {

      // @formatter:off
      http.antMatcher("/revoke**")
        .httpBasic()
          .authenticationEntryPoint(authenticationEntryPoint)
        .and()
          .addFilterBefore(corsFilter, SecurityContextPersistenceFilter.class)
          .addFilterBefore(clientCredentialsEndpointFilter(), BasicAuthenticationFilter.class)
        .exceptionHandling()
          .authenticationEntryPoint(authenticationEntryPoint)
        .and()
          .csrf().disable()
          .sessionManagement().sessionCreationPolicy(STATELESS);
      // @formatter:on
    }
  }

  @Configuration
  @Order(17)
  public static class JwkEndpointAuthorizationConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private Http403ForbiddenEntryPoint http403ForbiddenEntryPoint;

    @Override
    protected void configure(final HttpSecurity http) throws Exception {

      // @formatter:off
      http.antMatcher("/jwk**")
        .exceptionHandling()
          .authenticationEntryPoint(http403ForbiddenEntryPoint)
        .and()
          .sessionManagement()
          .sessionCreationPolicy(STATELESS)
        .and()
          .authorizeRequests()
            .antMatchers("/**").permitAll();
      // @formatter:on
    }
  }

  @Configuration
  @Order(27)
  public static class DeviceCodeEnpointConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    @Qualifier("clientUserDetailsService")
    private UserDetailsService userDetailsService;

    @Autowired
    private CorsFilter corsFilter;

    @Autowired
    private OAuth2AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void configure(final AuthenticationManagerBuilder auth) throws Exception {

      auth.userDetailsService(userDetailsService);
    }

    @Override
    protected void configure(final HttpSecurity http) throws Exception {

      // @formatter:off
      http.antMatcher("/devicecode/**")
        .httpBasic()
          .authenticationEntryPoint(authenticationEntryPoint)
        .and()
          .addFilterBefore(corsFilter, SecurityContextPersistenceFilter.class)
          .sessionManagement()
            .sessionCreationPolicy(STATELESS).and()
        .csrf()
          .disable()
        .authorizeRequests()
          .anyRequest()
            .fullyAuthenticated();
      // @formatter:on
    }
  }
}
