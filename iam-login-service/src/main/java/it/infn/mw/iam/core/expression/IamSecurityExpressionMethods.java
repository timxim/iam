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
package it.infn.mw.iam.core.expression;

import static it.infn.mw.iam.authn.ExternalAuthenticationHandlerSupport.EXT_AUTHN_UNREGISTERED_USER_AUTH;

import java.util.Optional;

import org.springframework.security.core.Authentication;

import it.infn.mw.iam.api.account.AccountUtils;
import it.infn.mw.iam.api.requests.GroupRequestUtils;
import it.infn.mw.iam.authn.AbstractExternalAuthenticationToken;
import it.infn.mw.iam.core.IamGroupRequestStatus;
import it.infn.mw.iam.persistence.model.IamAccount;
import it.infn.mw.iam.persistence.model.IamGroupRequest;

public class IamSecurityExpressionMethods {

  private final Authentication authentication;
  private final AccountUtils accountUtils;
  private final GroupRequestUtils groupRequestUtils;


  public IamSecurityExpressionMethods(Authentication authentication, AccountUtils accountUtils,
      GroupRequestUtils groupRequestUtils) {
    this.authentication = authentication;
    this.accountUtils = accountUtils;
    this.groupRequestUtils = groupRequestUtils;
  }

  public boolean isExternallyAuthenticatedWithIssuer(String issuer) {
    if (authentication.getAuthorities().contains(EXT_AUTHN_UNREGISTERED_USER_AUTH)) {

      @SuppressWarnings("rawtypes")
      AbstractExternalAuthenticationToken token =
          (AbstractExternalAuthenticationToken) authentication;
      return token.toExernalAuthenticationRegistrationInfo().getIssuer().equals(issuer);
    }

    return false;
  }

  public boolean isAGroupManager() {
    return authentication.getAuthorities()
      .stream()
      .anyMatch(a -> a.getAuthority().startsWith("ROLE_GM:"));
  }

  public boolean isGroupManager(String groupUuid) {
    return authentication.getAuthorities()
      .stream()
      .anyMatch(a -> a.getAuthority().equals("ROLE_GM:" + groupUuid));
  }

  public boolean isUser(String userUuid) {
    Optional<IamAccount> account = accountUtils.getAuthenticatedUserAccount();
    return account.isPresent() && account.get().getUuid().equals(userUuid);
  }

  public boolean canManageGroupRequest(String requestId) {
    Optional<IamGroupRequest> groupRequest = groupRequestUtils.getOptionalGroupRequest(requestId);

    return groupRequest.isPresent() && isGroupManager(groupRequest.get().getGroup().getUuid());
  }

  public boolean canAccessGroupRequest(String requestId) {
    Optional<IamGroupRequest> groupRequest = groupRequestUtils.getOptionalGroupRequest(requestId);
    Optional<IamAccount> userAccount = accountUtils.getAuthenticatedUserAccount();


    return userAccount.isPresent() && groupRequest.isPresent()
        && (groupRequest.get().getAccount().getUuid().equals(userAccount.get().getUuid())
            || isGroupManager(groupRequest.get().getGroup().getUuid()));
  }

  public boolean userCanDeleteGroupRequest(String requestId) {
    Optional<IamGroupRequest> groupRequest = groupRequestUtils.getOptionalGroupRequest(requestId);

    return groupRequest.isPresent() && ((canAccessGroupRequest(requestId)
        && IamGroupRequestStatus.PENDING.equals(groupRequest.get().getStatus()))
        || canManageGroupRequest(requestId));
  }
}
