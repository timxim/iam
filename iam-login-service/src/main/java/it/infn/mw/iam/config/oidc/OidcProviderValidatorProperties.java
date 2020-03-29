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
package it.infn.mw.iam.config.oidc;

import java.util.List;

public class OidcProviderValidatorProperties {

  public enum CombinationRule {
    and,
    or
  }

  CombinationRule combinationRule = CombinationRule.and;
  List<OidcProviderValidatorCheckProperties> checks;
  String message;

  public OidcProviderValidatorProperties() {}

  public CombinationRule getCombinationRule() {
    return combinationRule;
  }

  public void setCombinationRule(CombinationRule combinationRule) {
    this.combinationRule = combinationRule;
  }

  public List<OidcProviderValidatorCheckProperties> getChecks() {
    return checks;
  }

  public void setChecks(List<OidcProviderValidatorCheckProperties> checks) {
    this.checks = checks;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

}
