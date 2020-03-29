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
package it.infn.mw.iam.api.scim.controller;

import static it.infn.mw.iam.api.scim.controller.utils.ValidationHelper.handleValidationError;
import static it.infn.mw.iam.api.scim.updater.UpdaterType.ACCOUNT_REMOVE_OIDC_ID;
import static it.infn.mw.iam.api.scim.updater.UpdaterType.ACCOUNT_REMOVE_PICTURE;
import static it.infn.mw.iam.api.scim.updater.UpdaterType.ACCOUNT_REMOVE_SAML_ID;
import static it.infn.mw.iam.api.scim.updater.UpdaterType.ACCOUNT_REPLACE_EMAIL;
import static it.infn.mw.iam.api.scim.updater.UpdaterType.ACCOUNT_REPLACE_FAMILY_NAME;
import static it.infn.mw.iam.api.scim.updater.UpdaterType.ACCOUNT_REPLACE_GIVEN_NAME;
import static it.infn.mw.iam.api.scim.updater.UpdaterType.ACCOUNT_REPLACE_PICTURE;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import it.infn.mw.iam.api.scim.converter.OidcIdConverter;
import it.infn.mw.iam.api.scim.converter.SamlIdConverter;
import it.infn.mw.iam.api.scim.converter.SshKeyConverter;
import it.infn.mw.iam.api.scim.converter.UserConverter;
import it.infn.mw.iam.api.scim.converter.X509CertificateConverter;
import it.infn.mw.iam.api.scim.exception.ScimException;
import it.infn.mw.iam.api.scim.exception.ScimPatchOperationNotSupported;
import it.infn.mw.iam.api.scim.exception.ScimResourceNotFoundException;
import it.infn.mw.iam.api.scim.model.ScimConstants;
import it.infn.mw.iam.api.scim.model.ScimPatchOperation;
import it.infn.mw.iam.api.scim.model.ScimUser;
import it.infn.mw.iam.api.scim.model.ScimUserPatchRequest;
import it.infn.mw.iam.api.scim.updater.AccountUpdater;
import it.infn.mw.iam.api.scim.updater.UpdaterType;
import it.infn.mw.iam.api.scim.updater.factory.DefaultAccountUpdaterFactory;
import it.infn.mw.iam.persistence.model.IamAccount;
import it.infn.mw.iam.persistence.repository.IamAccountRepository;

@RestController
@RequestMapping("/scim/Me")
@Transactional
public class ScimMeController implements ApplicationEventPublisherAware {

  protected static final EnumSet<UpdaterType> SUPPORTED_UPDATER_TYPES =
      EnumSet.of(ACCOUNT_REMOVE_OIDC_ID, ACCOUNT_REMOVE_SAML_ID, ACCOUNT_REPLACE_EMAIL,
          ACCOUNT_REPLACE_FAMILY_NAME, ACCOUNT_REPLACE_GIVEN_NAME, ACCOUNT_REPLACE_PICTURE,
          ACCOUNT_REMOVE_PICTURE);

  private final IamAccountRepository iamAccountRepository;

  private final UserConverter userConverter;

  private final DefaultAccountUpdaterFactory updatersFactory;

  private ApplicationEventPublisher eventPublisher;

  @Autowired
  public ScimMeController(IamAccountRepository accountRepository, UserConverter userConverter,
      PasswordEncoder passwordEncoder, OidcIdConverter oidcIdConverter,
      SamlIdConverter samlIdConverter, SshKeyConverter sshKeyConverter,
      X509CertificateConverter x509CertificateConverter) {

    this.iamAccountRepository = accountRepository;
    this.userConverter = userConverter;
    this.updatersFactory = new DefaultAccountUpdaterFactory(passwordEncoder, accountRepository,
        oidcIdConverter, samlIdConverter, sshKeyConverter, x509CertificateConverter);
  }

  public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
    this.eventPublisher = publisher;
  }

  @PreAuthorize("#oauth2.hasScope('scim:read') or hasRole('USER')")
  @RequestMapping(method = RequestMethod.GET, produces = ScimConstants.SCIM_CONTENT_TYPE)
  public ScimUser whoami() {

    IamAccount account = getCurrentUserAccount();
    return userConverter.dtoFromEntity(account);

  }

  @PreAuthorize("#oauth2.hasScope('scim:write') or hasRole('USER')")
  @RequestMapping(method = RequestMethod.PATCH, consumes = ScimConstants.SCIM_CONTENT_TYPE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateUser(
      @RequestBody @Validated(ScimUser.UpdateUserValidation.class) final ScimUserPatchRequest patchRequest,
      final BindingResult validationResult) {

    handleValidationError("Invalid Scim Patch Request", validationResult);

    IamAccount account = getCurrentUserAccount();

    patchRequest.getOperations().forEach(op -> executePatchOperation(account, op));

  }

  private void executePatchOperation(IamAccount account, ScimPatchOperation<ScimUser> op) {

    List<AccountUpdater> updaters = updatersFactory.getUpdatersForPatchOperation(account, op);
    List<AccountUpdater> updatesToPublish = new ArrayList<>();

    boolean hasChanged = false;

    for (AccountUpdater u : updaters) {
      if (!SUPPORTED_UPDATER_TYPES.contains(u.getType())) {
        throw new ScimPatchOperationNotSupported(u.getType().getDescription() + " not supported");
      }
      if (u.update()) {
        hasChanged = true;
        updatesToPublish.add(u);
      }
    }

    if (hasChanged) {

      account.touch();
      iamAccountRepository.save(account);
      for (AccountUpdater u : updatesToPublish) {
        u.publishUpdateEvent(this, eventPublisher);
      }
    }
  }

  private IamAccount getCurrentUserAccount() throws ScimException, ScimResourceNotFoundException {

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth instanceof OAuth2Authentication) {
      OAuth2Authentication oauth = (OAuth2Authentication) auth;
      if (oauth.getUserAuthentication() == null) {
        throw new ScimException("No user linked to the current OAuth token");
      }
      auth = oauth.getUserAuthentication();
    }

    final String username = auth.getName();

    return iamAccountRepository.findByUsername(username).orElseThrow(
        () -> new ScimResourceNotFoundException("No user mapped to username '" + username + "'"));
  }
}
