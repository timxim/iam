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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.data.repository.query.SecurityEvaluationContextExtension;
import org.springframework.security.oauth2.provider.expression.OAuth2WebSecurityExpressionHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.GenericFilterBean;

import it.infn.mw.iam.api.account.AccountUtils;
import it.infn.mw.iam.api.aup.AUPSignatureCheckService;
import it.infn.mw.iam.authn.EnforceAupSignatureSuccessHandler;
import it.infn.mw.iam.authn.ExternalAuthenticationHintService;
import it.infn.mw.iam.authn.HintAwareAuthenticationEntryPoint;
import it.infn.mw.iam.authn.RootIsDashboardSuccessHandler;
import it.infn.mw.iam.authn.oidc.OidcAccessDeniedHandler;
import it.infn.mw.iam.authn.oidc.OidcAuthenticationProvider;
import it.infn.mw.iam.authn.oidc.OidcClientFilter;
import it.infn.mw.iam.authn.x509.IamX509AuthenticationProvider;
import it.infn.mw.iam.authn.x509.IamX509AuthenticationUserDetailService;
import it.infn.mw.iam.authn.x509.IamX509PreauthenticationProcessingFilter;
import it.infn.mw.iam.authn.x509.X509AuthenticationCredentialExtractor;
import it.infn.mw.iam.persistence.repository.IamAccountRepository;

@Configuration
@EnableWebSecurity
public class IamWebSecurityConfig {
  
  @Bean
  public SecurityEvaluationContextExtension contextExtension() {
    return new SecurityEvaluationContextExtension();
  }

  @Configuration
  @Order(100)
  public static class UserLoginConfig extends WebSecurityConfigurerAdapter {

    @Value("${iam.baseUrl}")
    private String iamBaseUrl;

    @Autowired
    private OAuth2WebSecurityExpressionHandler oAuth2WebSecurityExpressionHandler;

    @Autowired
    @Qualifier("mitreAuthzRequestFilter")
    private GenericFilterBean authorizationRequestFilter;

    @Autowired
    @Qualifier("iamUserDetailsService")
    private UserDetailsService iamUserDetailsService;

    @Autowired
    private X509AuthenticationCredentialExtractor x509CredentialExtractor;

    @Autowired
    private IamX509AuthenticationUserDetailService x509UserDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private IamAccountRepository accountRepo;

    @Autowired
    private AUPSignatureCheckService aupSignatureCheckService;

    @Autowired
    private AccountUtils accountUtils;

    @Autowired
    private ExternalAuthenticationHintService hintService;

    @Autowired
    public void configureGlobal(final AuthenticationManagerBuilder auth) throws Exception {
      // @formatter:off
      auth
        .userDetailsService(iamUserDetailsService)
        .passwordEncoder(passwordEncoder);
      // @formatter:on
    }

    @Override
    public void configure(final WebSecurity web) throws Exception {
      web.expressionHandler(oAuth2WebSecurityExpressionHandler);
    }


    public IamX509AuthenticationProvider iamX509AuthenticationProvider() {
      IamX509AuthenticationProvider provider = new IamX509AuthenticationProvider();
      provider.setPreAuthenticatedUserDetailsService(x509UserDetailsService);
      return provider;
    }

    public IamX509PreauthenticationProcessingFilter iamX509Filter() {
      return new IamX509PreauthenticationProcessingFilter(x509CredentialExtractor,
          iamX509AuthenticationProvider(), successHandler());
    }

    protected AuthenticationEntryPoint entryPoint() {
      LoginUrlAuthenticationEntryPoint delegate = new LoginUrlAuthenticationEntryPoint("/login");
      return new HintAwareAuthenticationEntryPoint(delegate, hintService);
    }


    @Override
    protected void configure(final HttpSecurity http) throws Exception {

      // @formatter:off
      http.requestMatchers()
        .antMatchers("/", "/login**", "/logout", "/authorize", "/manage/**", "/dashboard**", "/start-registration",
            "/reset-session", "/device/**")
        .and()
        .sessionManagement()
          .enableSessionUrlRewriting(false)
        .and()
          .authorizeRequests()
            .antMatchers("/login**", "/webjars/**").permitAll()
            .antMatchers("/start-registration").permitAll()
            .antMatchers("/authorize**").permitAll()
            .antMatchers("/reset-session").permitAll()
            .antMatchers("/device/**").authenticated()
            .antMatchers("/").authenticated()
        .and()
          .formLogin()
            .loginPage("/login")
            .failureUrl("/login?error=failure")
            .successHandler(successHandler())
        .and()
          .exceptionHandling()
            .accessDeniedHandler(new OidcAccessDeniedHandler())
            .authenticationEntryPoint(entryPoint())
        .and()
          .addFilterBefore(authorizationRequestFilter, SecurityContextPersistenceFilter.class)
        .logout()
          .logoutUrl("/logout")
        .and().anonymous()
        .and()
          .csrf()
            .requireCsrfProtectionMatcher(new AntPathRequestMatcher("/authorize")).disable()
        .addFilter(iamX509Filter());
      // @formatter:on
    }

    @Bean
    public OAuth2WebSecurityExpressionHandler oAuth2WebSecurityExpressionHandler() {
      return new OAuth2WebSecurityExpressionHandler();
    }

    public AuthenticationSuccessHandler successHandler() {
      AuthenticationSuccessHandler delegate =
          new RootIsDashboardSuccessHandler(iamBaseUrl, new HttpSessionRequestCache());

      return new EnforceAupSignatureSuccessHandler(delegate, aupSignatureCheckService, accountUtils,
          accountRepo);
    }
  }

  @Configuration
  @Order(105)
  public static class ExternalOidcLogin extends WebSecurityConfigurerAdapter {

    @Autowired
    @Qualifier("OIDCAuthenticationManager")
    private AuthenticationManager oidcAuthManager;

    @Autowired
    OidcAuthenticationProvider authProvider;

    @Autowired
    @Qualifier("OIDCAuthenticationFilter")
    private OidcClientFilter oidcFilter;

    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {

      return oidcAuthManager;
    }

    public LoginUrlAuthenticationEntryPoint authenticationEntryPoint() {
      return new LoginUrlAuthenticationEntryPoint("/openid_connect_login");
    }

    @Override
    protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
      auth.authenticationProvider(authProvider);
    }

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
      // @formatter:off
      http
        .antMatcher("/openid_connect_login**")
          .exceptionHandling()
            .authenticationEntryPoint(authenticationEntryPoint())
          .accessDeniedHandler(new OidcAccessDeniedHandler())
        .and()
          .addFilterAfter(oidcFilter, SecurityContextPersistenceFilter.class).authorizeRequests()
        .antMatchers("/openid_connect_login**")
          .permitAll()
        .and()
          .sessionManagement()
            .enableSessionUrlRewriting(false)
            .sessionCreationPolicy(SessionCreationPolicy.ALWAYS);
      // @formatter:on
    }
  }

  @Configuration
  @Order(Ordered.HIGHEST_PRECEDENCE)
  @Profile("h2-console")
  public static class H2ConsoleEndpointAuthorizationConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(final HttpSecurity http) throws Exception {

      HttpSecurity h2Console = http.requestMatchers()
        .antMatchers("/h2-console", "/h2-console/**")
        .and()
        .csrf()
        .disable();

      h2Console.httpBasic();
      h2Console.headers().frameOptions().disable();

      h2Console.authorizeRequests().antMatchers("/h2-console/**", "/h2-console").permitAll();
    }

    @Override
    public void configure(final WebSecurity builder) throws Exception {
      builder.debug(true);
    }
  }
}
