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
package it.infn.mw.iam.registration;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import it.infn.mw.iam.api.scim.exception.ScimResourceNotFoundException;
import it.infn.mw.iam.authn.AbstractExternalAuthenticationToken;
import it.infn.mw.iam.authn.ExternalAuthenticationRegistrationInfo;
import it.infn.mw.iam.core.IamRegistrationRequestStatus;

@RestController
@Transactional
@Profile("registration")
public class RegistrationApiController {

  public static final Logger LOG = LoggerFactory.getLogger(RegistrationApiController.class);
  private RegistrationRequestService service;

  private Optional<ExternalAuthenticationRegistrationInfo> getExternalAuthenticationInfo() {

    Authentication authn = SecurityContextHolder.getContext().getAuthentication();

    if (authn == null) {
      return Optional.empty();
    }

    if (authn instanceof AbstractExternalAuthenticationToken<?>) {

      return Optional.of(((AbstractExternalAuthenticationToken<?>) authn)
        .toExernalAuthenticationRegistrationInfo());
    }

    return Optional.empty();
  }

  @Autowired
  public RegistrationApiController(RegistrationRequestService registrationService) {
    service = registrationService;
  }

  @RequestMapping(value = "/registration/username-available/{username:.+}",
      method = RequestMethod.GET)
  public Boolean usernameAvailable(@PathVariable("username") String username) {
    return service.usernameAvailable(username);
  }

  @RequestMapping(value = "/registration/email-available/{email:.+}", method = RequestMethod.GET)
  public Boolean emailAvailable(@PathVariable("email") String email) {
    return service.emailAvailable(email);
  }

  @PreAuthorize("#oauth2.hasScope('registration:read') or hasRole('ADMIN')")
  @RequestMapping(value = "/registration/list", method = RequestMethod.GET)
  @ResponseBody
  public List<RegistrationRequestDto> listRequests(
      @RequestParam(value = "status", required = false) IamRegistrationRequestStatus status) {

    return service.listRequests(status);
  }

  @PreAuthorize("#oauth2.hasScope('registration:read') or hasRole('ADMIN')")
  @RequestMapping(value = "/registration/list/pending", method = RequestMethod.GET)
  @ResponseBody
  public List<RegistrationRequestDto> listPendingRequests() {

    return service.listPendingRequests();
  }

  @RequestMapping(value = "/registration/create", method = RequestMethod.POST,
      consumes = "application/json")
  public RegistrationRequestDto createRegistrationRequest(
      @RequestBody RegistrationRequestDto request) {

    return service.createRequest(request, getExternalAuthenticationInfo());

  }

  @PreAuthorize("#oauth2.hasScope('registration:write') or hasRole('ADMIN')")
  @RequestMapping(value = "/registration/approve/{uuid}", method = RequestMethod.POST)
  public RegistrationRequestDto approveRequest(@PathVariable("uuid") String uuid) {
    return service.approveRequest(uuid);
  }

  @PreAuthorize("#oauth2.hasScope('registration:write') or hasRole('ADMIN')")
  @RequestMapping(value = "/registration/reject/{uuid}", method = RequestMethod.POST)
  public RegistrationRequestDto rejectRequest(@PathVariable("uuid") String uuid,
      @RequestParam(required = false) String motivation) {

    return service.rejectRequest(uuid, Optional.ofNullable(motivation));
  }


  @RequestMapping(value = "/registration/confirm/{token}", method = RequestMethod.GET)
  public RegistrationRequestDto confirmRequest(@PathVariable("token") String token) {

    return service.confirmRequest(token);
  }

  @RequestMapping(value = "/registration/verify/{token}", method = RequestMethod.GET)
  public ModelAndView verify(final Model model, @PathVariable("token") String token) {
    try {
      service.confirmRequest(token);
      model.addAttribute("verificationSuccess", true);
      SecurityContextHolder.clearContext();
    } catch (ScimResourceNotFoundException e) {
      LOG.warn(e.getMessage(), e);
      String message = "Activation failed: " + e.getMessage();
      model.addAttribute("verificationMessage", message);
      model.addAttribute("verificationFailure", true);
    }

    return new ModelAndView("iam/requestVerified");
  }

  @RequestMapping(value = "/registration/submitted", method = RequestMethod.GET)
  public ModelAndView submissionSuccess() {
    SecurityContextHolder.clearContext();
    return new ModelAndView("iam/requestSubmitted");
  }

}
