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

import it.infn.mw.iam.persistence.model.IamAccount;
import it.infn.mw.iam.persistence.model.IamGroup;
import it.infn.mw.iam.persistence.model.IamScopePolicy;

public interface IamScopePolicyRepository
    extends PagingAndSortingRepository<IamScopePolicy, Long>, IamScopePolicyRepositoryCustom {

  @Query("select s from IamScopePolicy s where s.group is null and s.account is null")
  List<IamScopePolicy> findDefaultPolicies();

  @Query("select s from IamScopePolicy s where s.group is null and s.account is null and s.rule = :rule")
  List<IamScopePolicy> findDefaultPoliciesByRule(@Param("rule") IamScopePolicy.Rule rule);

  @Query("select s from IamScopePolicy s join s.scopes ss where ss = :scope")
  List<IamScopePolicy> findByScope(@Param("scope") String scope);

  List<IamScopePolicy> findByGroup(IamGroup group);

  List<IamScopePolicy> findByGroupAndRule(IamGroup group, IamScopePolicy.Rule rule);

  List<IamScopePolicy> findByAccount(IamAccount account);

  List<IamScopePolicy> findByAccountAndRule(IamAccount account, IamScopePolicy.Rule rule);

  Optional<IamScopePolicy> findById(Long id);
}
