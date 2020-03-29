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
package it.infn.mw.iam.config;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.mitre.jwt.assertion.impl.SelfAssertionValidator;
import org.mitre.jwt.signer.service.impl.ClientKeyCacheService;
import org.mitre.jwt.signer.service.impl.JWKSetCacheService;
import org.mitre.jwt.signer.service.impl.SymmetricKeyJWTValidatorCacheService;
import org.mitre.oauth2.service.ClientDetailsEntityService;
import org.mitre.oauth2.service.DeviceCodeService;
import org.mitre.oauth2.service.OAuth2TokenEntityService;
import org.mitre.oauth2.service.SystemScopeService;
import org.mitre.oauth2.service.impl.BlacklistAwareRedirectResolver;
import org.mitre.oauth2.service.impl.DefaultClientUserDetailsService;
import org.mitre.oauth2.service.impl.DefaultDeviceCodeService;
import org.mitre.oauth2.service.impl.DefaultOAuth2ClientDetailsEntityService;
import org.mitre.oauth2.service.impl.DefaultOAuth2ProviderTokenService;
import org.mitre.oauth2.service.impl.DefaultSystemScopeService;
import org.mitre.oauth2.token.ScopeServiceAwareOAuth2RequestValidator;
import org.mitre.oauth2.web.CorsFilter;
import org.mitre.openid.connect.config.ConfigurationPropertiesBean;
import org.mitre.openid.connect.config.UIConfiguration;
import org.mitre.openid.connect.filter.AuthorizationRequestFilter;
import org.mitre.openid.connect.service.ApprovedSiteService;
import org.mitre.openid.connect.service.BlacklistedSiteService;
import org.mitre.openid.connect.service.ClientLogoLoadingService;
import org.mitre.openid.connect.service.LoginHintExtracter;
import org.mitre.openid.connect.service.OIDCTokenService;
import org.mitre.openid.connect.service.PairwiseIdentiferService;
import org.mitre.openid.connect.service.StatsService;
import org.mitre.openid.connect.service.UserInfoService;
import org.mitre.openid.connect.service.WhitelistedSiteService;
import org.mitre.openid.connect.service.impl.DefaultApprovedSiteService;
import org.mitre.openid.connect.service.impl.DefaultBlacklistedSiteService;
import org.mitre.openid.connect.service.impl.DefaultOIDCTokenService;
import org.mitre.openid.connect.service.impl.DefaultStatsService;
import org.mitre.openid.connect.service.impl.DefaultUserInfoService;
import org.mitre.openid.connect.service.impl.DefaultWhitelistedSiteService;
import org.mitre.openid.connect.service.impl.DummyResourceSetService;
import org.mitre.openid.connect.service.impl.InMemoryClientLogoLoadingService;
import org.mitre.openid.connect.service.impl.MITREidDataService_1_0;
import org.mitre.openid.connect.service.impl.MITREidDataService_1_1;
import org.mitre.openid.connect.service.impl.MITREidDataService_1_2;
import org.mitre.openid.connect.service.impl.MatchLoginHintsAgainstUsers;
import org.mitre.openid.connect.service.impl.UUIDPairwiseIdentiferService;
import org.mitre.openid.connect.token.ConnectTokenEnhancer;
import org.mitre.openid.connect.token.TofuUserApprovalHandler;
import org.mitre.openid.connect.web.AuthenticationTimeStamper;
import org.mitre.openid.connect.web.ServerConfigInterceptor;
import org.mitre.openid.connect.web.UserInfoInterceptor;
import org.mitre.uma.service.ResourceSetService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.OAuth2RequestValidator;
import org.springframework.security.oauth2.provider.approval.UserApprovalHandler;
import org.springframework.security.oauth2.provider.endpoint.RedirectResolver;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import com.google.common.collect.Sets;

import it.infn.mw.iam.authn.oidc.RestTemplateFactory;
import it.infn.mw.iam.core.IamOAuth2RequestFactory;
import it.infn.mw.iam.core.oauth.IamJWKSetCacheService;
import it.infn.mw.iam.core.oauth.scope.IamScopeFilter;

@Configuration
public class MitreServicesConfig {

  @Value("${iam.issuer}")
  private String issuer;

  @Value("${iam.baseUrl}")
  private String baseUrl;

  @Value("${iam.token.lifetime}")
  private Long tokenLifeTime;

  @Value("${iam.topbarTitle}")
  private String topbarTitle;

  @Bean
  public ConfigurationPropertiesBean config(IamProperties properties) {

    ConfigurationPropertiesBean config = new ConfigurationPropertiesBean();

    config.setLogoImageUrl(properties.getLogo().getUrl());
    config.setTopbarTitle(topbarTitle);

    if (!issuer.endsWith("/")) {
      issuer = issuer + "/";
    }

    config.setIssuer(issuer);

    if (tokenLifeTime <= 0L) {
      config.setRegTokenLifeTime(null);
    } else {
      config.setRegTokenLifeTime(tokenLifeTime);
    }

    config.setForceHttps(false);
    config.setLocale(Locale.ENGLISH);

    return config;
  }


  @Bean
  public UIConfiguration uiConfiguration() {

    Set<String> jsFiles =
        Sets.newHashSet("resources/js/client.js", "resources/js/grant.js", "resources/js/scope.js",
            "resources/js/whitelist.js", "resources/js/dynreg.js", "resources/js/rsreg.js",
            "resources/js/token.js", "resources/js/blacklist.js", "resources/js/profile.js");

    UIConfiguration config = new UIConfiguration();
    config.setJsFiles(jsFiles);
    return config;

  }

  @Bean
  RedirectResolver blacklistAwareRedirectResolver() {

    return new BlacklistAwareRedirectResolver();
  }

  @Bean
  OAuth2RequestValidator requestValidator() {

    return new ScopeServiceAwareOAuth2RequestValidator();
  }

  @Bean
  UserApprovalHandler tofuApprovalHandler() {

    return new TofuUserApprovalHandler();
  }

  @Bean
  OAuth2RequestFactory requestFactory(IamScopeFilter scopeFilter) {
    return new IamOAuth2RequestFactory(clientDetailsEntityService(), scopeFilter);
  }

  @Bean
  @Qualifier("iamClientDetailsEntityService")
  ClientDetailsEntityService clientDetailsEntityService() {

    return new DefaultOAuth2ClientDetailsEntityService();
  }

  @Bean
  OAuth2TokenEntityService tokenServices() {

    return new DefaultOAuth2ProviderTokenService();
  }


  @Bean(name = "mitreUserInfoInterceptor")
  public AsyncHandlerInterceptor userInfoInterceptor() {

    return new UserInfoInterceptor();
  }

  @Bean(name = "mitreServerConfigInterceptor")
  public AsyncHandlerInterceptor serverConfigInterceptor() {

    return new ServerConfigInterceptor();
  }

  @Bean
  public FilterRegistrationBean disabledMitreFilterRegistration(AuthorizationRequestFilter f) {

    FilterRegistrationBean b = new FilterRegistrationBean(f);
    b.setEnabled(false);
    return b;
  }

  @Bean(name = "mitreAuthzRequestFilter")
  public AuthorizationRequestFilter authorizationRequestFilter() {

    return new AuthorizationRequestFilter();
  }

  @Bean
  public AuthenticationTimeStamper timestamper() {

    return new AuthenticationTimeStamper();
  }

  @Bean
  public Http403ForbiddenEntryPoint http403ForbiddenEntryPoint() {

    return new Http403ForbiddenEntryPoint();
  }

  @Bean
  public FilterRegistrationBean disabledCorsFilterRegistration(CorsFilter c) {

    FilterRegistrationBean b = new FilterRegistrationBean(c);
    b.setEnabled(false);
    return b;
  }

  @Primary
  @Bean
  public CorsFilter corsFilter() {

    return new CorsFilter();
  }

  @Bean
  public OAuth2AuthenticationEntryPoint oauth2AuthenticationEntryPoint() {

    OAuth2AuthenticationEntryPoint entryPoint = new OAuth2AuthenticationEntryPoint();
    entryPoint.setRealmName("openidconnect");
    return entryPoint;
  }

  @Bean
  public TokenEnhancer defaultTokenEnhancer() {

    return new ConnectTokenEnhancer();
  }

  @Bean(name = "clientUserDetailsService")
  public UserDetailsService defaultClientUserDetailsService() {

    return new DefaultClientUserDetailsService();
  }

  @Bean
  MITREidDataService_1_0 mitreDataService1_0() {

    return new MITREidDataService_1_0();
  }

  @Bean
  MITREidDataService_1_1 mitreDataService1_1() {

    return new MITREidDataService_1_1();
  }

  @Bean
  MITREidDataService_1_2 mitreDataService1_2() {

    return new MITREidDataService_1_2();
  }

  @Bean
  LoginHintExtracter defaultLoginHintExtracter() {

    return new MatchLoginHintsAgainstUsers();
  }

  @Bean
  ClientLogoLoadingService defaultClientLogoLoadingService() {

    return new InMemoryClientLogoLoadingService();
  }



  @Bean
  SymmetricKeyJWTValidatorCacheService defaultSimmetricKeyJWTValidatorCacheService() {

    return new SymmetricKeyJWTValidatorCacheService();
  }

  @Bean
  JWKSetCacheService defaultCacheService(RestTemplateFactory rtf) {

    return new IamJWKSetCacheService(rtf, 100, 1, TimeUnit.HOURS);
  }

  @Bean
  OIDCTokenService defaultOIDCTokenService() {

    return new DefaultOIDCTokenService();
  }

  @Bean
  PairwiseIdentiferService defaultPairwiseIdentifierService() {

    return new UUIDPairwiseIdentiferService();
  }

  @Bean
  UserInfoService defaultUserInfoService() {

    return new DefaultUserInfoService();
  }

  @Bean
  ApprovedSiteService defaultApprovedSiteService() {

    return new DefaultApprovedSiteService();
  }

  @Bean
  StatsService defaultStatsService() {

    return new DefaultStatsService();
  }

  @Bean
  WhitelistedSiteService defaultWhitelistedSiteService() {

    return new DefaultWhitelistedSiteService();
  }

  @Bean
  BlacklistedSiteService defaultBlacklistedSiteService() {

    return new DefaultBlacklistedSiteService();
  }

  @Bean
  SystemScopeService defaultSystemScopeService() {

    return new DefaultSystemScopeService();
  }

  @Bean
  ResourceSetService defaultResourceSetService() {

    return new DummyResourceSetService();
  }

  @Bean
  ClientKeyCacheService defaultClientKeyCacheService() {

    return new ClientKeyCacheService();
  }

  @Bean
  DeviceCodeService defaultDeviceCodeService() {
    return new DefaultDeviceCodeService();
  }

  @Bean
  SelfAssertionValidator selfAssertionValidator() {
    return new SelfAssertionValidator();
  }
}
