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

import static it.infn.mw.iam.api.scim.updater.UpdaterType.ACCOUNT_REPLACE_ACTIVE;

import it.infn.mw.iam.api.scim.updater.UpdaterType;
import it.infn.mw.iam.persistence.model.IamAccount;

public class ActiveReplacedEvent extends AccountUpdatedEvent {

  private static final long serialVersionUID = 5681737929767602266L;

  private final Boolean active;

  public ActiveReplacedEvent(Object source, IamAccount account, Boolean active) {
    super(source, account, ACCOUNT_REPLACE_ACTIVE, buildMessage(ACCOUNT_REPLACE_ACTIVE, active));
    this.active = active;
  }

  public Boolean getActive() {
    return active;
  }

  protected static String buildMessage(UpdaterType t, Boolean active) {
    return String.format("%s: %s", t.getDescription(), active);
  }
}
