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

import java.util.function.Supplier;

import it.infn.mw.iam.api.common.NoSuchAccountError;
import it.infn.mw.iam.core.group.error.NoSuchGroupError;

public class ErrorSuppliers {

  private ErrorSuppliers() {
    // prevent instantiation
  }

  static Supplier<NoSuchGroupError> noSuchGroup(String groupUuid) {
    return () -> NoSuchGroupError.forUuid(groupUuid);
  }

  static Supplier<NoSuchAccountError> noSuchAccount(String accountUuid) {
    return () -> NoSuchAccountError.forUuid(accountUuid);
  }
}
