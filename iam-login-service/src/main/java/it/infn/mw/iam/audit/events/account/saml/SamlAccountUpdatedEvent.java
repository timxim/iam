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
package it.infn.mw.iam.audit.events.account.saml;

import java.util.Collection;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import it.infn.mw.iam.api.scim.updater.UpdaterType;
import it.infn.mw.iam.audit.events.account.AccountUpdatedEvent;
import it.infn.mw.iam.audit.utils.IamSamlSerializer;
import it.infn.mw.iam.persistence.model.IamAccount;
import it.infn.mw.iam.persistence.model.IamSamlId;

public abstract class SamlAccountUpdatedEvent extends AccountUpdatedEvent {

  private static final long serialVersionUID = 1L;

  @JsonSerialize(using = IamSamlSerializer.class)
  private Collection<IamSamlId> samlIds;

  public SamlAccountUpdatedEvent(Object source, IamAccount account, UpdaterType type,
      Collection<IamSamlId> samlIds) {
    super(source, account, type, buildMessage(type, account, samlIds));
    this.samlIds = samlIds;
  }

  protected Collection<IamSamlId> getSamlIds() {
    return samlIds;
  }

  protected static String buildMessage(UpdaterType t, IamAccount account,
      Collection<IamSamlId> samlIds) {
    return String.format("%s: username: '%s' values '%s'", t.getDescription(),
        account.getUsername(), samlIds);
  }

}
