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

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import it.infn.mw.iam.persistence.model.IamAccount;


public interface IamAccountRepository
    extends PagingAndSortingRepository<IamAccount, Long>, IamAccountRepositoryCustom {

  Optional<IamAccount> findByUuid(@Param("uuid") String uuid);

  Optional<IamAccount> findByUsername(@Param("username") String username);

  @Query("select a from IamAccount a join a.samlIds si where si.idpId = :idpId "
      + "and si.attributeId = :attributeId and si.userId = :userId")
  Optional<IamAccount> findBySamlId(@Param("idpId") String idpId,
      @Param("attributeId") String attributeId, @Param("userId") String userId);

  @Query("select a from IamAccount a join a.oidcIds oi where oi.issuer = :issuer and oi.subject = :subject")
  Optional<IamAccount> findByOidcId(@Param("issuer") String issuer,
      @Param("subject") String subject);

  @Query("select a from IamAccount a join a.sshKeys sk where sk.fingerprint = :fingerprint")
  Optional<IamAccount> findBySshKeyFingerprint(@Param("fingerprint") String fingerprint);

  @Query("select a from IamAccount a join a.sshKeys sk where sk.label = :label")
  Optional<IamAccount> findBySshKeyLabel(@Param("label") String label);

  @Query("select a from IamAccount a join a.sshKeys sk where sk.value = :value")
  Optional<IamAccount> findBySshKeyValue(@Param("value") String value);

  @Query("select a from IamAccount a join a.userInfo ui where ui.email = :emailAddress")
  Optional<IamAccount> findByEmail(@Param("emailAddress") String emailAddress);

  @Query("select a from IamAccount a where a.username = :username and a.uuid != :uuid")
  Optional<IamAccount> findByUsernameWithDifferentUUID(@Param("username") String username,
      @Param("uuid") String uuid);

  @Query("select a from IamAccount a join a.userInfo ui where ui.email = :emailAddress and a.uuid != :uuid")
  Optional<IamAccount> findByEmailWithDifferentUUID(@Param("emailAddress") String emailAddress,
      @Param("uuid") String uuid);

  @Query("select a from IamAccount a join a.x509Certificates c where c.subjectDn = :subject")
  Optional<IamAccount> findByCertificateSubject(@Param("subject") String subject);

  @Query("select a from IamAccount a join a.x509Certificates c where c.certificate = :certificate")
  Optional<IamAccount> findByCertificate(@Param("certificate") String certificate);

  @Query("select a from IamAccount a join a.groups ag where ag.id = :groupId")
  List<IamAccount> findByGroupId(@Param("groupId") Long groupId);
  
  Optional<IamAccount> findByConfirmationKey(@Param("confirmationKey") String confirmationKey);

  Optional<IamAccount> findByResetKey(@Param("resetKey") String resetKey);

  @Query("select a from IamAccount a join a.authorities auth where auth.authority = :authority")
  List<IamAccount> findByAuthority(@Param("authority") String authority);

  @Query("select a from IamAccount a where a.provisioned = true and a.lastLoginTime < :timestamp")
  List<IamAccount> findProvisionedAccountsWithLastLoginTimeBeforeTimestamp(
      @Param("timestamp") Date timestamp);

  @Query("select a from IamAccount a join a.userInfo ui where lower(ui.email) LIKE lower(concat('%', :filter, '%')) or lower(a.username) LIKE lower(concat('%', :filter, '%')) or lower(a.uuid) LIKE lower(concat('%', :filter, '%')) or lower(concat(ui.givenName, ' ', ui.familyName)) LIKE lower(concat('%', :filter, '%'))")
  Page<IamAccount> findByFilter(@Param("filter") String filter, Pageable op);

  @Query("select count(a) from IamAccount a join a.userInfo ui where lower(ui.email) LIKE lower(concat('%', :filter, '%')) or lower(a.username) LIKE lower(concat('%', :filter, '%')) or lower(a.uuid) LIKE lower(concat('%', :filter, '%')) or lower(concat(ui.givenName, ' ', ui.familyName)) LIKE lower(concat('%', :filter, '%'))")
  long countByFilter(@Param("filter") String filter);
}
