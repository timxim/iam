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
package it.infn.mw.iam.core;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.mitre.oauth2.model.OAuth2AccessTokenEntity;
import org.mitre.oauth2.service.impl.DefaultIntrospectionResultAssembler;
import org.mitre.openid.connect.model.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

import it.infn.mw.iam.persistence.model.IamGroup;
import it.infn.mw.iam.persistence.model.IamUserInfo;
import it.infn.mw.iam.persistence.repository.UserInfoAdapter;

public class IamIntrospectionResultAssembler extends DefaultIntrospectionResultAssembler {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(IamIntrospectionResultAssembler.class);

  public static final String NAME = "name";
  public static final String PREFERRED_USERNAME = "preferred_username";
  public static final String EMAIL = "email";
  public static final String GROUPS = "groups";
  public static final String ORGANISATION_NAME = "organisation_name";
  public static final String ISSUER = "iss";
  
  @Value("${iam.issuer}")
  private String oidcIssuer;
  
  @Value("${iam.organisation.name}")
  private String organisationName;

  @Override
  public Map<String, Object> assembleFrom(OAuth2AccessTokenEntity accessToken, UserInfo userInfo,
      Set<String> authScopes) {

    Map<String, Object> result = super.assembleFrom(accessToken, userInfo, authScopes);
    
    String trailingSlashIssuer = oidcIssuer.endsWith("/") ? oidcIssuer : oidcIssuer + "/";
    
    result.put(ISSUER, trailingSlashIssuer);
    
    try {

      List<String> audience = accessToken.getJwt().getJWTClaimsSet().getAudience();

      if (audience != null && !audience.isEmpty()) {
        result.put("aud", Joiner.on(' ').join(audience));
      }

    } catch (ParseException e) {
      LOGGER.error("Error getting audience out of access token: {}", e.getMessage(), e);
    }

    // Intersection of scopes authorized for the client and scopes linked to the
    // access token
    Set<String> scopes = Sets.intersection(authScopes, accessToken.getScope());

    if (userInfo != null) {
      if (scopes.contains("profile")) {

        IamUserInfo iamUserInfo = ((UserInfoAdapter) userInfo).getUserinfo();

        if (!iamUserInfo.getGroups().isEmpty()) {

          result.put(GROUPS,
              iamUserInfo.getGroups().stream().map(IamGroup::getName).collect(Collectors.toList()));
        }

        result.put(NAME, iamUserInfo.getName());
        result.put(PREFERRED_USERNAME, iamUserInfo.getPreferredUsername());
        result.put(ORGANISATION_NAME, organisationName);
      }

      if (scopes.contains("email")) {
        result.put(EMAIL, userInfo.getEmail());
      }
    }

    return result;
  }

}
