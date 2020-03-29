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
package it.infn.mw.iam.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import it.infn.mw.iam.core.IamRegistrationRequestStatus;
import it.infn.mw.iam.persistence.model.IamRegistrationRequest;

public interface IamRegistrationRequestRepository
    extends PagingAndSortingRepository<IamRegistrationRequest, Long> {

  Optional<List<IamRegistrationRequest>> findByStatus(
      @Param("status") IamRegistrationRequestStatus status);

  Optional<IamRegistrationRequest> findByUuid(@Param("uuid") String uuid);

  @Query("select r from IamRegistrationRequest r join r.account a where a.confirmationKey = :confirmationKey and r.status = it.infn.mw.iam.core.IamRegistrationRequestStatus.NEW")
  Optional<IamRegistrationRequest> findByAccountConfirmationKey(
      @Param("confirmationKey") String confirmationKey);

  @Query("select r from IamRegistrationRequest r where r.status = it.infn.mw.iam.core.IamRegistrationRequestStatus.NEW or r.status = it.infn.mw.iam.core.IamRegistrationRequestStatus.CONFIRMED")
  List<IamRegistrationRequest> findPendingRequests();
}
