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

import static com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter.filterOutAllExcept;
import static com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter.serializeAllExcept;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.converter.json.MappingJacksonValue;

import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import it.infn.mw.iam.api.common.Converter;
import it.infn.mw.iam.api.common.ListResponseDTO;
import it.infn.mw.iam.api.common.OffsetPageable;
import it.infn.mw.iam.api.tokens.service.TokenService;
import it.infn.mw.iam.api.tokens.service.paging.DefaultTokensPageRequest;
import it.infn.mw.iam.api.tokens.service.paging.TokensPageRequest;

public abstract class AbstractTokensController<T, E> {

  public static final Logger log = LoggerFactory.getLogger(AbstractTokensController.class);

  public static final String APPLICATION_JSON_CONTENT_TYPE = "application/json";
  public static final int TOKENS_MAX_PAGE_SIZE = 20;

  private static final String FILTER_NAME = "attributeFilter";
  private static final String ID_FIELD = "id";
  private static final String VALUE_FIELD = "value";

  @Autowired
  private Converter<T, E> converter;

  @Autowired
  private TokenService<E> tokenService;

  public TokensPageRequest buildTokensPageRequest(Integer startIndex, Integer count,
      String clientId, String userId, String sortBy, String sortDirection) {

    return buildPageRequest(startIndex, count, TOKENS_MAX_PAGE_SIZE, clientId, userId, sortBy,
        sortDirection);
  }

  private TokensPageRequest buildPageRequest(Integer startIndex, Integer count, int maxPageSize,
      String clientId, String userId, String sortBy, String sortDirection) {

    DefaultTokensPageRequest.Builder builder = new DefaultTokensPageRequest.Builder();
    builder.startIndex(startIndex);
    builder.count(count > maxPageSize ? maxPageSize : count);
    builder.sortBy(sortBy);
    builder.sortDirection(sortDirection);
    builder.clientId(clientId);
    builder.userId(userId);
    return builder.build();
  }

  private Set<String> parseAttributes(final String attributesParameter) {

    Set<String> result = new HashSet<>();
    if (!Strings.isNullOrEmpty(attributesParameter)) {
      result = Sets
        .newHashSet(Splitter.on(CharMatcher.anyOf(".,")).trimResults().omitEmptyStrings().split(
            attributesParameter));
    }
    return result;
  }

  public MappingJacksonValue filterTokensResponse(ListResponseDTO<T> result, String attributes) {

    MappingJacksonValue wrapper = new MappingJacksonValue(result);
    SimpleFilterProvider filterProvider = new SimpleFilterProvider();

    if (!Strings.isNullOrEmpty(attributes)) {

      Set<String> filteredAttributes = parseAttributes(attributes);
      filteredAttributes.add(ID_FIELD);
      filteredAttributes.remove(VALUE_FIELD);
      filterProvider.addFilter(FILTER_NAME, filterOutAllExcept(filteredAttributes));

    } else {

      filterProvider.addFilter(FILTER_NAME, serializeAllExcept(VALUE_FIELD));
    }

    wrapper.setFilters(filterProvider);
    return wrapper;
  }

  public MappingJacksonValue filterTokenResponse(T result) {

    MappingJacksonValue wrapper = new MappingJacksonValue(result);
    wrapper.setFilters(
        new SimpleFilterProvider().addFilter(FILTER_NAME, serializeAllExcept(VALUE_FIELD)));
    return wrapper;
  }

  private ListResponseDTO<T> buildCountResponse(long countResponse) {

    return new ListResponseDTO.Builder<T>().totalResults(countResponse).build();
  }

  private ListResponseDTO<T> buildListResponse(Page<E> p, OffsetPageable op) {

    ListResponseDTO.Builder<T> builder = ListResponseDTO.builder();
    builder.itemsPerPage(p.getContent().size());
    builder.startIndex(op.getOffset() + 1);

    List<T> resources = Lists.newArrayList();
    p.getContent().forEach(e -> resources.add(converter.dtoFromEntity(e)));
    builder.resources(resources);

    builder.totalResults(p.getTotalElements());
    return builder.build();
  }

  public T getTokenResponse(E entity) {

    return converter.dtoFromEntity(entity);
  }

  private OffsetPageable getOffsetPageable(TokensPageRequest pageRequest) {

    return new OffsetPageable(pageRequest.getStartIndex() - 1, pageRequest.getCount(),
        getSort(pageRequest));
  }

  private Sort getSort(TokensPageRequest pageRequest) {

    Sort.Direction direction;
    try {
      direction = Sort.Direction.fromString(pageRequest.getSortDirection());
    } catch (IllegalArgumentException e) {
      log.error(e.getMessage(), e);
      direction = Sort.Direction.DESC;
    }

    Sort sort;
    Sort.Order expirationDesc = new Order(Sort.Direction.DESC, "expiration");
    Sort.Order idDesc = new Order(Sort.Direction.DESC, "id");

    String sortBy = pageRequest.getSortBy().toLowerCase();
    switch (sortBy) {
      case "client":
        sort =
            new Sort(new Order(direction, "authenticationHolder.clientId"), expirationDesc, idDesc);
        break;
      case "user":
        sort = new Sort(new Order(direction, "authenticationHolder.userAuth.name"), expirationDesc,
            idDesc);
        break;
      default:
        sort = new Sort(new Order(direction, "expiration"), idDesc);
    }

    return sort;
  }

  public ListResponseDTO<T> getTokensResponse(TokensPageRequest pageRequest) {

    if (pageRequest.getCount() == 0) {
      return getCountResponse(pageRequest);
    }
    return getListResponse(pageRequest);
  }

  private ListResponseDTO<T> getCountResponse(TokensPageRequest pageRequest) {

    Optional<String> userId = pageRequest.getUserId();
    Optional<String> clientId = pageRequest.getClientId();

    Long countResponse;

    if (userId.isPresent()) {
      if (clientId.isPresent()) {
        /* filter user and client */
        countResponse = tokenService.countTokensForUserAndClient(userId.get(), clientId.get());
      } else {
        /* filter user */
        countResponse = tokenService.countTokensForUser(userId.get());
      }
    } else if (clientId.isPresent()) {
      /* filter client */
      countResponse = tokenService.countTokensForClient(clientId.get());
    } else {
      /* no filter */
      countResponse = tokenService.countAllTokens();
    }

    return buildCountResponse(countResponse);
  }

  private ListResponseDTO<T> getListResponse(TokensPageRequest pageRequest) {

    Optional<String> userId = pageRequest.getUserId();
    Optional<String> clientId = pageRequest.getClientId();

    Page<E> page;

    OffsetPageable op = getOffsetPageable(pageRequest);
    if (userId.isPresent()) {
      if (clientId.isPresent()) {
        /* filter user and client */
        page = tokenService.getTokensForClientAndUser(userId.get(), clientId.get(), op);
      } else {
        /* filter user */
        page = tokenService.getTokensForUser(userId.get(), op);
      }
    } else if (clientId.isPresent()) {
      /* filter client */
      page = tokenService.getTokensForClient(clientId.get(), op);
    } else {
      /* no filter */
      page = tokenService.getAllTokens(op);
    }

    return buildListResponse(page, op);
  }
}
