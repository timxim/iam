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
package it.infn.mw.iam.api.tokens.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import it.infn.mw.iam.api.common.ListResponseDTO;
import it.infn.mw.iam.api.common.OffsetPageable;
import it.infn.mw.iam.api.tokens.service.paging.TokensPageRequest;

public abstract class AbstractTokenService<T> implements TokenService<T> {

  public static final Logger log = LoggerFactory.getLogger(AbstractTokenService.class);

  protected OffsetPageable getOffsetPageable(TokensPageRequest pageRequest) {

    return new OffsetPageable(pageRequest.getStartIndex() - 1, pageRequest.getCount(), getSort(pageRequest));
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
        sort = new Sort(new Order(direction, "authenticationHolder.clientId"), expirationDesc, idDesc);
        break;
      case "user":
        sort = new Sort(new Order(direction, "authenticationHolder.userAuth.name"), expirationDesc, idDesc);
        break;
      default:
        sort = new Sort(new Order(direction, "expiration"), idDesc);
    }

    log.info("Sort: {}", sort);
    return sort;
  }

  protected boolean isCountRequest(TokensPageRequest pageRequest) {

    return pageRequest.getCount() == 0;
  }

  protected ListResponseDTO<T> buildListResponse(List<T> resources, OffsetPageable op, long totalElements) {
    
    ListResponseDTO.Builder<T> builder = ListResponseDTO.builder();
    builder.itemsPerPage(resources.size());
    builder.startIndex(op.getOffset() + 1);
    builder.resources(resources);
    builder.totalResults(totalElements);
    return builder.build();
  }
}
