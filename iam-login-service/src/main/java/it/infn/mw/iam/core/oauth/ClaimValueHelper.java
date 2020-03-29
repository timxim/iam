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
package it.infn.mw.iam.core.oauth;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;

import it.infn.mw.iam.persistence.model.IamGroup;
import it.infn.mw.iam.persistence.model.IamUserInfo;

@Component
public class ClaimValueHelper {

  public static final Set<String> ADDITIONAL_CLAIMS =
      ImmutableSet.of("name", "email", "preferred_username", "organisation_name", "groups");

  @Value("${iam.organisation.name}")
  String organisationName;

  public Object getClaimValueFromUserInfo(String claim, IamUserInfo info) {

    switch (claim) {

      case "name":
        return info.getName();

      case "email":
        return info.getEmail();

      case "preferred_username":
        return info.getPreferredUsername();

      case "organisation_name":
        return organisationName;

      case "groups":
        return info.getGroups().stream().map(IamGroup::getName).toArray(String[]::new);

      default:
        return null;
    }
  }

}
