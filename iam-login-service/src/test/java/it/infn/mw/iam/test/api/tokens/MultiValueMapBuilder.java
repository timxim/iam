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
package it.infn.mw.iam.test.api.tokens;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import com.google.common.collect.Lists;

public class MultiValueMapBuilder {

  private MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();

  public static MultiValueMapBuilder builder() {

    return new MultiValueMapBuilder();
  }

  MultiValueMapBuilder count(long count) {

    params.put("count", Lists.newArrayList(String.valueOf(count)));
    return this;
  }

  MultiValueMapBuilder startIndex(long startIndex) {

    params.put("startIndex", Lists.newArrayList(String.valueOf(startIndex)));
    return this;
  }

  MultiValueMapBuilder userId(String userId) {

    params.put("userId", Lists.newArrayList(userId));
    return this;
  }

  MultiValueMapBuilder clientId(String clientId) {

    params.put("clientId", Lists.newArrayList(clientId));
    return this;
  }

  MultiValueMapBuilder attributes(String value) {

    params.put("attributes", Lists.newArrayList(value));
    return this;
  }

  MultiValueMapBuilder sortByClient() {

    return sortBy("client");
  }

  MultiValueMapBuilder sortByUser() {

    return sortBy("user");
  }

  MultiValueMapBuilder sortByExpiration() {

    return sortBy("expiration");
  }

  MultiValueMapBuilder sortBy(String sortBy) {

    params.put("sortBy", Lists.newArrayList(sortBy));
    return this;
  }

  MultiValueMapBuilder sortDirectionAsc() {

    return sortDirection("asc");
  }

  MultiValueMapBuilder sortDirectionDesc() {

    return sortDirection("desc");
  }

  MultiValueMapBuilder sortDirection(String direction) {

    params.put("sortDirection", Lists.newArrayList(direction));
    return this;
  }

  MultiValueMap<String, String> build() {

    return params;
  }
}
