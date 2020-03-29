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
package it.infn.mw.iam.test.util.oidc;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import it.infn.mw.iam.test.util.WithMockOIDCUser;

public class WithMockOIDCUserSecurityContextFactory
    implements WithSecurityContextFactory<WithMockOIDCUser> {

  @Override
  public SecurityContext createSecurityContext(WithMockOIDCUser annotation) {

    OidcSecurityContextBuilder builder = new OidcSecurityContextBuilder();

    return builder.issuer(annotation.issuer())
      .subject(annotation.subject())
      .email(annotation.email())
      .name(annotation.givenName(), annotation.familyName())
      .username(annotation.username())
      .authorities(annotation.authorities())
      .expirationTime(annotation.expirationTime())
      .buildSecurityContext();
  }


}
