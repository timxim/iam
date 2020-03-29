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
package it.infn.mw.iam.audit.events.account;

import it.infn.mw.iam.authn.x509.IamX509AuthenticationCredential;
import it.infn.mw.iam.persistence.model.IamAccount;

public class X509CertificateUpdatedEvent extends X509CertificateLinkedEvent {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public X509CertificateUpdatedEvent(Object source, IamAccount account, String message,
      IamX509AuthenticationCredential cred) {
    super(source, account, message, cred);
  }

}
