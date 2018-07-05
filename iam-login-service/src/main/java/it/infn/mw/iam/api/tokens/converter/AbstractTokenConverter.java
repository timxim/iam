/**
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2016-2018
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
package it.infn.mw.iam.api.tokens.converter;

import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.model.SavedUserAuthentication;
import org.mitre.oauth2.service.ClientDetailsEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import it.infn.mw.iam.api.common.Converter;
import it.infn.mw.iam.api.scim.converter.ScimResourceLocationProvider;
import it.infn.mw.iam.api.tokens.model.ClientRef;
import it.infn.mw.iam.api.tokens.model.UserRef;
import it.infn.mw.iam.core.user.exception.IamAccountException;
import it.infn.mw.iam.persistence.model.IamAccount;
import it.infn.mw.iam.persistence.repository.IamAccountRepository;

@Component
public abstract class AbstractTokenConverter<T, E> implements Converter<T, E> {

  @Autowired
  private ClientDetailsEntityService clientDetailsService;

  @Autowired
  private IamAccountRepository accountRepository;

  @Autowired
  private ScimResourceLocationProvider scimResourceLocationProvider;

  protected ClientRef buildClientRef(String clientId) {

    ClientDetailsEntity cd = clientDetailsService.loadClientByClientId(clientId);

    return ClientRef.builder()
        .id(cd.getId())
        .clientId(cd.getClientId())
        .contacts(cd.getContacts())
        .ref(cd.getClientUri())
        .build();
  }

  protected UserRef buildUserRef(SavedUserAuthentication userAuth) {

    String username = userAuth.getPrincipal().toString();

    IamAccount account = accountRepository.findByUsername(username)
        .orElseThrow(() -> new IamAccountException("Account for " + username + " not found"));

    return UserRef.builder()
        .id(account.getUuid())
        .userName(account.getUsername())
        .ref(scimResourceLocationProvider.userLocation(account.getUuid()))
        .build();
  }
}
