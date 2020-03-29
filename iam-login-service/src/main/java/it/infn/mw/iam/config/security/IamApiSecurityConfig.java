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

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import org.mitre.oauth2.web.CorsFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationProcessingFilter;
import org.springframework.security.oauth2.provider.error.OAuth2AccessDeniedHandler;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;

import it.infn.mw.iam.api.proxy.ProxyCertificatesApiController;
import it.infn.mw.iam.config.CustomAuthenticationEntryPoint;
import it.infn.mw.iam.core.oauth.FormClientCredentialsAuthenticationFilter;

@Configuration
public class IamApiSecurityConfig {

  @Configuration
  @Order(20)
  public static class IamProxyCertificateApiConfig extends WebSecurityConfigurerAdapter {
    private static final String PROXY_API_MATCHER =
        ProxyCertificatesApiController.PROXY_API_PATH + "/**";

    @Autowired
    private OAuth2AuthenticationProcessingFilter resourceFilter;

    @Autowired
    @Qualifier("clientUserDetailsService")
    private UserDetailsService userDetailsService;

    @Autowired
    private OAuth2AuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private CorsFilter corsFilter;

    @Override
    protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
      auth.userDetailsService(userDetailsService);
    }

    @Override
    protected void configure(final HttpSecurity http) throws Exception {

      FormClientCredentialsAuthenticationFilter ccFilter =
          new FormClientCredentialsAuthenticationFilter(PROXY_API_MATCHER,
              authenticationEntryPoint);
      
      ccFilter.setAuthenticationManager(authenticationManager());
      
      // @formatter:off
      http.antMatcher(PROXY_API_MATCHER)
          .exceptionHandling()
            .authenticationEntryPoint(authenticationEntryPoint)
            .accessDeniedHandler(new OAuth2AccessDeniedHandler())
        .and()
          .addFilterBefore(ccFilter, SecurityContextPersistenceFilter.class)
          .addFilterAfter(resourceFilter, SecurityContextPersistenceFilter.class)
          .addFilterBefore(corsFilter, WebAsyncManagerIntegrationFilter.class)
        .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
          .authorizeRequests()
            .anyRequest().fullyAuthenticated()            
        .and()
          .csrf().disable();
      // @formatter:on
    }
  }

  @Configuration
  @Order(21)
  public static class IamApiConfig extends WebSecurityConfigurerAdapter {

    private static final String AUP_PATH = "/iam/aup";

    @Autowired
    private OAuth2AuthenticationProcessingFilter resourceFilter;

    @Autowired
    private OAuth2AuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private CorsFilter corsFilter;

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
      // @formatter:off
      http.requestMatchers()
        .antMatchers("/scim/**", 
            "/registration/**", 
            "/iam/**")
        .and()
          .exceptionHandling()
            .authenticationEntryPoint(authenticationEntryPoint)
        .and()
          .addFilterAfter(resourceFilter, SecurityContextPersistenceFilter.class)
          .addFilterBefore(corsFilter, WebAsyncManagerIntegrationFilter.class)
        .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.NEVER)
        .and()
          .authorizeRequests()
            .antMatchers("/iam/password-reset/**").permitAll()
            .antMatchers(POST, "/registration/create").permitAll()
            .antMatchers(GET, "/registration/username-available/**").permitAll()
            .antMatchers(GET, "/registration/email-available/**").permitAll()
            .antMatchers(GET, "/registration/confirm/**").permitAll()
            .antMatchers(GET, "/registration/verify/**").permitAll()
            .antMatchers(GET, "/registration/submitted").permitAll()
            .antMatchers(GET, "/iam/config/**").permitAll()
            .antMatchers(GET, AUP_PATH).permitAll()
            .anyRequest().authenticated()            
        .and()
          .csrf().disable();
      // @formatter:on
    }
  }

  @Configuration
  @Order(25)
  public static class ActuatorEndpointsConfig extends WebSecurityConfigurerAdapter {

    @Value("${iam.superuser.username}")
    private String basicUsername;

    @Value("${iam.superuser.password}")
    private String basicPassword;

    @Autowired
    private OAuth2AuthenticationProcessingFilter resourceFilter;

    @Autowired
    private CustomAuthenticationEntryPoint customAuthenticationEntyPoint;

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
      // @formatter:off
      auth.inMemoryAuthentication()
        .withUser(basicUsername).password(basicPassword)
        .roles("SUPERUSER", "ADMIN");
      // @formatter:on
    }

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
      // @formatter:off
      http
        .requestMatchers()
          .antMatchers("/metrics", "/info", "/health", "/health/mail", "/health/external",
              "/configprops", "/env", "/mappings", "/flyway", "/autoconfig", "/beans", "/dump", "/trace")
        .and()
          .httpBasic()
          .authenticationEntryPoint(customAuthenticationEntyPoint)
        .and()
          .exceptionHandling()
            .accessDeniedHandler(new AccessDeniedHandlerImpl())
        .and()
          .addFilterAfter(resourceFilter, SecurityContextPersistenceFilter.class)
        .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.NEVER)
        .and()
          .authorizeRequests()
            .antMatchers(GET, "/info", "/health", "/health/mail", "/health/external").permitAll()
            .antMatchers(GET, "/metrics").hasRole("ADMIN")
            .antMatchers(GET, "/configprops", "/env", "/mappings", 
                "/flyway", "/autoconfig", "/beans", "/dump", "/trace").hasRole("SUPERUSER");
      // @formatter:on
    }
  }
}
