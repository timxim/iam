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
package it.infn.mw.iam.core.userinfo;

import static it.infn.mw.iam.authn.util.AuthenticationUtils.isSupportedExternalAuthenticationToken;

import java.util.Map;

import org.mitre.oauth2.service.SystemScopeService;
import org.mitre.openid.connect.model.UserInfo;
import org.mitre.openid.connect.service.UserInfoService;
import org.mitre.openid.connect.view.HttpCodeView;
import org.mitre.openid.connect.view.UserInfoView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Strings;

import it.infn.mw.iam.authn.ExternalAuthenticationInfoProcessor;

@Controller
@RequestMapping("/userinfo")
public class IamUserInfoEndpoint {

  @Autowired
  private UserInfoService userInfoService;

  @Autowired
  private ExternalAuthenticationInfoProcessor extAuthnInfoProcessor;

  @Value("${iam.organisation.name}")
  String organisationName;

  private static final Logger LOG = LoggerFactory.getLogger(IamUserInfoEndpoint.class);

  private void processExternalAuthenticationInfo(OAuth2Authentication auth, DecoratedUserInfo dui) {

    if (isSupportedExternalAuthenticationToken(auth.getUserAuthentication())) {

      Map<String, String> processedAuthInfo = extAuthnInfoProcessor.process(auth);

      if (!processedAuthInfo.isEmpty()) {
        dui.setAuthenticationInfo(processedAuthInfo);
      }
    }
  }


  @PreAuthorize("hasRole('ROLE_USER') and #oauth2.hasScope('" + SystemScopeService.OPENID_SCOPE
      + "')")
  @RequestMapping(method = RequestMethod.GET,
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public String getInfo(
      @RequestParam(value = "claims", required = false) String claimsRequestJsonString,
      @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String acceptHeader,
      OAuth2Authentication auth, Model model) {


    String username = auth.getName();


    UserInfo userInfo =
        userInfoService.getByUsernameAndClientId(username, auth.getOAuth2Request().getClientId());

    if (userInfo == null) {
      LOG.error("user not found: {}", username);
      model.addAttribute(HttpCodeView.CODE, HttpStatus.NOT_FOUND);
      return HttpCodeView.VIEWNAME;
    }

    IamDecoratedUserInfo dui = IamDecoratedUserInfo.forUser(userInfo);
    dui.setOrganisationName(organisationName);

    model.addAttribute(UserInfoView.SCOPE, auth.getOAuth2Request().getScope());

    model.addAttribute(UserInfoView.AUTHORIZED_CLAIMS,
        auth.getOAuth2Request().getExtensions().get("claims"));

    if (!Strings.isNullOrEmpty(claimsRequestJsonString)) {
      model.addAttribute(UserInfoView.REQUESTED_CLAIMS, claimsRequestJsonString);
    }

    processExternalAuthenticationInfo(auth, dui);
    
    model.addAttribute(UserInfoView.USER_INFO, dui);
    
    return UserInfoView.VIEWNAME;
  }


}
