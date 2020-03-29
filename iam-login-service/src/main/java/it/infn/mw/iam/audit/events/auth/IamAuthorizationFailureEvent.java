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
package it.infn.mw.iam.audit.events.auth;

import org.springframework.security.access.event.AuthorizationFailureEvent;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import it.infn.mw.iam.audit.events.IamAuditApplicationEvent;
import it.infn.mw.iam.audit.utils.IamAuthorizationFailureEventSerializer;

public class IamAuthorizationFailureEvent extends IamAuditApplicationEvent {

  private static final long serialVersionUID = 1L;
  
  @JsonSerialize(using=IamAuthorizationFailureEventSerializer.class)
  final AuthorizationFailureEvent sourceAuthzEvent;

  public IamAuthorizationFailureEvent(AuthorizationFailureEvent event) {
    super(IamEventCategory.AUTHORIZATION,event.getSource(), 
        event.getAccessDeniedException().getMessage());
    this.sourceAuthzEvent = event;
  }

  public AuthorizationFailureEvent getSourceAuthzEvent() {
    return sourceAuthzEvent;
  }
}
