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
package it.infn.mw.iam.api.account.group;

import static it.infn.mw.iam.api.account.group.ErrorSuppliers.noSuchAccount;
import static it.infn.mw.iam.api.account.group.ErrorSuppliers.noSuchGroup;
import static java.lang.String.format;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import it.infn.mw.iam.api.common.ErrorDTO;
import it.infn.mw.iam.api.common.NoSuchAccountError;
import it.infn.mw.iam.core.group.error.NoSuchGroupError;
import it.infn.mw.iam.persistence.model.IamAccount;
import it.infn.mw.iam.persistence.model.IamGroup;
import it.infn.mw.iam.persistence.repository.IamAccountRepository;
import it.infn.mw.iam.persistence.repository.IamGroupRepository;

@RestController
public class AccountGroupController {

  final IamAccountRepository accountRepo;
  final IamGroupRepository groupRepo;

  @Autowired
  public AccountGroupController(IamAccountRepository accountRepo, IamGroupRepository groupRepo) {
    this.accountRepo = accountRepo;
    this.groupRepo = groupRepo;
  }


  @RequestMapping(value = "/iam/account/{accountUuid}/groups/{groupUuid}", method = POST)
  @ResponseStatus(value = HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN') or #iam.isGroupManager(#groupUuid)")
  public void addAccountToGroup(@PathVariable String accountUuid, @PathVariable String groupUuid) {
    IamGroup group = groupRepo.findByUuid(groupUuid).orElseThrow(noSuchGroup(groupUuid));

    IamAccount account =
        accountRepo.findByUuid(accountUuid).orElseThrow(noSuchAccount(accountUuid));

    if (group.getAccounts().contains(account)) {
      throw new AlreadyMemberError(
          format("Account %s is already a member of group %s", account.getUuid(), group.getUuid()));
    }

    account.getGroups().add(group);
    group.getAccounts().add(account);

    accountRepo.save(account);
    groupRepo.save(group);
  }

  @RequestMapping(value = "/iam/account/{accountUuid}/groups/{groupUuid}", method = DELETE)
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN') or #iam.isGroupManager(#groupUuid)")
  public void removeAccountFromGroup(@PathVariable String accountUuid, @PathVariable String groupUuid) {
    IamGroup group = groupRepo.findByUuid(groupUuid).orElseThrow(noSuchGroup(groupUuid));

    IamAccount account =
        accountRepo.findByUuid(accountUuid).orElseThrow(noSuchAccount(accountUuid));

    if (!group.getAccounts().contains(account)) {
      throw new NotAMemberError(
          format("Account %s is not a member of group %s", account.getUuid(), group.getUuid()));
    }

    account.getGroups().remove(group);
    group.getAccounts().remove(account);

    accountRepo.save(account);
    groupRepo.save(group);
  }
  
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  @ExceptionHandler(NoSuchAccountError.class)
  public ErrorDTO noSuchAccountError(HttpServletRequest req, Exception ex) {
    return ErrorDTO.fromString(ex.getMessage());
  }

  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  @ExceptionHandler(NoSuchGroupError.class)
  public ErrorDTO noSuchGroupError(HttpServletRequest req, Exception ex) {
    return ErrorDTO.fromString(ex.getMessage());
  }
  
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  @ExceptionHandler(AlreadyMemberError.class)
  public ErrorDTO alreadyMemberError(HttpServletRequest req, Exception ex) {
    return ErrorDTO.fromString(ex.getMessage());
  }
  
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  @ExceptionHandler(NotAMemberError.class)
  public ErrorDTO notAMemberError(HttpServletRequest req, Exception ex) {
    return ErrorDTO.fromString(ex.getMessage());
  }
}
