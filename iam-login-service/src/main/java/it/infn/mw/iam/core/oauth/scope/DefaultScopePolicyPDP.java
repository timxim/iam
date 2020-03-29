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
package it.infn.mw.iam.core.oauth.scope;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import it.infn.mw.iam.persistence.model.IamAccount;
import it.infn.mw.iam.persistence.model.IamGroup;
import it.infn.mw.iam.persistence.model.IamScopePolicy;
import it.infn.mw.iam.persistence.repository.IamScopePolicyRepository;

@Component
public class DefaultScopePolicyPDP implements ScopePolicyPDP {

  public static final Logger LOG = LoggerFactory.getLogger(DefaultScopePolicyPDP.class);

  public static class DecisionContext {

    public static final Logger LOG = LoggerFactory.getLogger(DecisionContext.class);

    enum ScopeStatus {
      PERMIT,
      DENY,
      UNPROCESSED
    }

    private final Map<String, ScopeStatus> scopeStatus = Maps.newHashMap();

    public DecisionContext(Set<String> requestedScopes) {
      LOG.debug("Decision context created for scopes '{}'", requestedScopes);
      requestedScopes.forEach(s -> scopeStatus.put(s, ScopeStatus.UNPROCESSED));
    }

    protected void permitScope(String scope) {
      if (!scopeStatus.get(scope).equals(ScopeStatus.DENY)) {
        scopeStatus.put(scope, ScopeStatus.PERMIT);
      } else {
        LOG.debug("Permit on scope {} ignored. Former DENY overrides", scope);
      }
    }

    protected void denyScope(String scope) {
      scopeStatus.put(scope, ScopeStatus.DENY);
    }

    protected boolean entryIsUnprocessed(Map.Entry<String, ScopeStatus> e) {
      return e.getValue().equals(ScopeStatus.UNPROCESSED);
    }

    protected boolean entryIsProcessed(Map.Entry<String, ScopeStatus> e) {
      return !e.getValue().equals(ScopeStatus.UNPROCESSED);
    }


    protected void applyScopePolicy(String scope, IamScopePolicy p, IamAccount a) {
      LOG.debug("Evaluating {} policy #{} ('{}') against scope '{}' for account '{}'",
          p.getPolicyType(), p.getId(), p.getDescription(), scope, a.getUsername());

      if (!p.appliesToScope(scope)) {
        LOG.debug("{} policy #{} ('{}') NOT APPLICABLE to scope '{}' for account '{}'",
            p.getPolicyType(), p.getId(), p.getDescription(), scope,
                a.getUsername());
        return;
      }

      if (p.isPermit()) {
        LOG.debug("{} policy #{} ('{}') PERMITS scope '{}' for account '{}'", 
            p.getPolicyType(), p.getId(), p.getDescription(), scope, a.getUsername());
        permitScope(scope);

      } else {
        LOG.debug("{} policy #{} ('{}') DENIES scope '{}' for account '{}'",
            p.getPolicyType(), p.getId(), p.getDescription(), scope, a.getUsername());
        denyScope(scope);
      }
    }

    public void applyPolicy(IamScopePolicy p, IamAccount a) {
      scopeStatus.keySet().forEach(s -> applyScopePolicy(s, p, a));
    }

    public boolean hasUnprocessedScopes() {
      return scopeStatus.entrySet().stream().anyMatch(this::entryIsUnprocessed);
    }

    public Set<String> getAllowedScopes() {
      return scopeStatus.entrySet()
        .stream()
        .filter(e -> ScopeStatus.PERMIT.equals(e.getValue()))
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
    }

    public void forgetProcessedEntries() {
      Set<String> processedKeys = scopeStatus.entrySet()
        .stream()
        .filter(this::entryIsProcessed)
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());

      processedKeys.forEach(scopeStatus::remove);
    }

    @Override
    public String toString() {
      return "DecisionContext [" + scopeStatus + "]";
    }
  }

  private final IamScopePolicyRepository policyRepo;

  @Autowired
  public DefaultScopePolicyPDP(IamScopePolicyRepository policyRepo) {
    this.policyRepo = policyRepo;
  }

  protected Set<IamScopePolicy> resolveGroupScopePolicies(IamAccount account) {

    Set<IamScopePolicy> groupPolicies = Sets.newHashSet();

    Set<IamGroup> groups = account.getGroups();
    for (IamGroup g : groups) {
      groupPolicies.addAll(g.getScopePolicies());
    }

    return groupPolicies;
  }

  @Override
  public Set<String> filterScopes(Set<String> requestedScopes, IamAccount account) {

    DecisionContext dc = new DecisionContext(requestedScopes);

    // Apply user policies
    for (IamScopePolicy p : account.getScopePolicies()) {
      dc.applyPolicy(p, account);
    }

    Set<String> allowedScopes = dc.getAllowedScopes();

    if (!dc.hasUnprocessedScopes()) {
      return allowedScopes;
    }

    Set<IamScopePolicy> groupPolicies = resolveGroupScopePolicies(account);

    // Apply group policies only on unprocessed scopes
    dc.forgetProcessedEntries();

    // Group policies are naturally composed with the deny overrides behaviour
    for (IamScopePolicy p : groupPolicies) {
      dc.applyPolicy(p, account);
    }

    allowedScopes.addAll(dc.getAllowedScopes());

    if (!dc.hasUnprocessedScopes()) {
      return allowedScopes;
    }

    dc.forgetProcessedEntries();

    List<IamScopePolicy> defaultPolicies = policyRepo.findDefaultPolicies();

    for (IamScopePolicy p : defaultPolicies) {
      dc.applyPolicy(p, account);
    }

    allowedScopes.addAll(dc.getAllowedScopes());
    return allowedScopes;
  }

}
