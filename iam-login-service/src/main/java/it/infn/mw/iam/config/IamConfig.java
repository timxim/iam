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

import java.time.Clock;
import java.util.Arrays;

import org.h2.server.web.WebServlet;
import org.mitre.oauth2.service.IntrospectionResultAssembler;
import org.mitre.oauth2.service.impl.DefaultOAuth2AuthorizationCodeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;

import it.infn.mw.iam.api.account.AccountUtils;
import it.infn.mw.iam.api.aup.AUPSignatureCheckService;
import it.infn.mw.iam.authn.oidc.OidcTokenEnhancer;
import it.infn.mw.iam.core.IamIntrospectionResultAssembler;
import it.infn.mw.iam.core.web.EnforceAupFilter;
import it.infn.mw.iam.notification.NotificationProperties;
import it.infn.mw.iam.notification.service.resolver.AddressResolutionService;
import it.infn.mw.iam.notification.service.resolver.AdminNotificationDeliveryStrategy;
import it.infn.mw.iam.notification.service.resolver.CompositeAdminsNotificationDelivery;
import it.infn.mw.iam.notification.service.resolver.GroupManagerNotificationDeliveryStrategy;
import it.infn.mw.iam.notification.service.resolver.NotifyAdminAddressStrategy;
import it.infn.mw.iam.notification.service.resolver.NotifyAdminsStrategy;
import it.infn.mw.iam.notification.service.resolver.NotifyGmStrategy;
import it.infn.mw.iam.notification.service.resolver.NotifyGmsAndAdminsStrategy;
import it.infn.mw.iam.persistence.repository.IamAupRepository;

@Configuration
public class IamConfig {

  @Value("${iam.organisation.name}")
  private String iamOrganisationName;

  @Bean
  GroupManagerNotificationDeliveryStrategy gmDeliveryStrategy(
      AdminNotificationDeliveryStrategy ands, AddressResolutionService ars,
      NotificationProperties props) {
    switch (props.getGroupManagerNotificationPolicy()) {
      case NOTIFY_GMS:
        return new NotifyGmStrategy(ars);
      case NOTIFY_GMS_AND_ADMINS:
        return new NotifyGmsAndAdminsStrategy(ands, ars);
      default:
        throw new IllegalArgumentException("Unhandled group manager notification policy: "
            + props.getGroupManagerNotificationPolicy());
    }
  }

  @Bean
  AdminNotificationDeliveryStrategy adminNotificationDeliveryStrategy(AddressResolutionService ars,
      NotificationProperties props) {

    switch (props.getAdminNotificationPolicy()) {
      case NOTIFY_ADDRESS:
        return new NotifyAdminAddressStrategy(props);
      case NOTIFY_ADMINS:
        return new NotifyAdminsStrategy(ars);
      case NOTIFY_ADDRESS_AND_ADMINS:
        return new CompositeAdminsNotificationDelivery(
            Arrays.asList(new NotifyAdminsStrategy(ars),
                new NotifyAdminsStrategy(ars)));

      default:
        throw new IllegalArgumentException(
            "Unhandled admin notification policy: " + props.getAdminNotificationPolicy());
    }
  }

  @Bean
  Clock defaultClock() {
    return Clock.systemDefaultZone();
  }

  @Bean
  AuthorizationCodeServices authorizationCodeServices() {

    return new DefaultOAuth2AuthorizationCodeService();
  }

  @Bean
  @Primary
  TokenEnhancer iamTokenEnhancer() {

    return new OidcTokenEnhancer();
  }

  @Bean
  IntrospectionResultAssembler defaultIntrospectionResultAssembler() {

    return new IamIntrospectionResultAssembler();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  FilterRegistrationBean aupSignatureCheckFilter(AUPSignatureCheckService service,
      AccountUtils utils, IamAupRepository repo) {
    EnforceAupFilter aupFilter = new EnforceAupFilter(service, utils, repo);
    FilterRegistrationBean frb = new FilterRegistrationBean(aupFilter);
    frb.setOrder(Ordered.LOWEST_PRECEDENCE);
    return frb;
  }

  @Bean
  @Profile("dev")
  ServletRegistrationBean h2Console() {

    WebServlet h2Servlet = new WebServlet();
    return new ServletRegistrationBean(h2Servlet, "/h2-console/*");
  }
}
