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
package it.infn.mw.iam.api.tokens;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import it.infn.mw.iam.api.common.ListResponseDTO;
import it.infn.mw.iam.api.tokens.service.paging.DefaultTokensPageRequest;
import it.infn.mw.iam.api.tokens.service.paging.TokensPageRequest;

import org.springframework.http.converter.json.MappingJacksonValue;

import java.util.HashSet;
import java.util.Set;

public class TokensControllerSupport {

  public static final String APPLICATION_JSON_CONTENT_TYPE = "application/json";
  public static final int TOKENS_MAX_PAGE_SIZE = 20;

  protected TokensPageRequest buildTokensPageRequest(Integer startIndex, Integer count,
      String clientId, String userId, String sortBy, String sortDirection) {

    return buildPageRequest(count, startIndex, TOKENS_MAX_PAGE_SIZE, clientId, userId, sortBy,
        sortDirection);
  }

  private TokensPageRequest buildPageRequest(Integer startIndex, Integer count, int maxPageSize,
      String clientId, String userId, String sortBy, String sortDirection) {

    // @formatter:off
    return new DefaultTokensPageRequest.Builder()
        .startIndex(startIndex)
        .count(count > maxPageSize ? maxPageSize : count)
        .sortBy(sortBy)
        .sortDirection(sortDirection)
        .clientId(clientId)
        .userId(userId)
        .build();
    // @formatter:on
  }

  protected Set<String> parseAttributes(final String attributesParameter) {

    Set<String> result = new HashSet<>();
    if (!Strings.isNullOrEmpty(attributesParameter)) {
      result = Sets.newHashSet(Splitter.on(CharMatcher.anyOf(".,"))
          .trimResults()
          .omitEmptyStrings()
          .split(attributesParameter));
    }
    result.add("id");
    return result;
  }

  protected <T> MappingJacksonValue filterAttributes(ListResponseDTO<T> result,
      String attributes) {

    MappingJacksonValue wrapper = new MappingJacksonValue(result);

    if (attributes != null) {
      Set<String> includeAttributes = parseAttributes(attributes);

      FilterProvider filterProvider = new SimpleFilterProvider().addFilter("attributeFilter",
          SimpleBeanPropertyFilter.filterOutAllExcept(includeAttributes));

      wrapper.setFilters(filterProvider);
    }

    return wrapper;
  }
}
