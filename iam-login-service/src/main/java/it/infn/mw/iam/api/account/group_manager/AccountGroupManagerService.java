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
package it.infn.mw.iam.api.account.group_manager;

import java.util.List;

import it.infn.mw.iam.api.account.group_manager.model.AccountManagedGroupsDTO;
import it.infn.mw.iam.persistence.model.IamAccount;
import it.infn.mw.iam.persistence.model.IamGroup;

public interface AccountGroupManagerService {

  AccountManagedGroupsDTO getManagedGroupInfoForAccount(IamAccount account);
  
  void addManagedGroupForAccount(IamAccount account, IamGroup group);
  void removeManagedGroupForAccount(IamAccount account, IamGroup group);
  
  List<IamAccount> getGroupManagersForGroup(IamGroup group);
  
}
