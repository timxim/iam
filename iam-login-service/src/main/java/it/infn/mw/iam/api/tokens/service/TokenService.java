/**
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2016-2018
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package it.infn.mw.iam.api.tokens.service;

import java.util.Optional;
import org.springframework.data.domain.Page;
import it.infn.mw.iam.api.common.OffsetPageable;

public interface TokenService<T> {

  Long countAllTokens();

  Page<T> getAllTokens(OffsetPageable op);

  Long countTokensForUser(String userId);

  Page<T> getTokensForUser(String userId, OffsetPageable op);

  Long countTokensForClient(String clientId);

  Page<T> getTokensForClient(String clientId, OffsetPageable op);

  Long countTokensForUserAndClient(String userId, String clientId);

  Page<T> getTokensForClientAndUser(String userId, String clientId, OffsetPageable op);

  Optional<T> getTokenById(Long id);

  void revokeTokenById(Long id);

  void deleteAllTokens();

}
