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
package it.infn.mw.iam.api.tokens.service.paging;

import java.util.Optional;

public class DefaultTokensPageRequest implements TokensPageRequest {

  private final int count;
  private final int startIndex;
  private final String sortBy;
  private final String sortDirection;
  private final Optional<String> userId;
  private final Optional<String> clientId;

  private DefaultTokensPageRequest(Builder b) {
    this.count = b.count;
    this.startIndex = b.startIndex;
    this.sortBy = b.sortBy;
    this.sortDirection = b.sortDirection;
    this.userId = b.userId;
    this.clientId = b.clientId;
  }

  @Override
  public int getCount() {

    return count;
  }

  @Override
  public int getStartIndex() {

    return startIndex;
  }

  @Override
  public String getSortBy() {

    return sortBy;
  }

  @Override
  public String getSortDirection() {

    return sortDirection;
  }

  @Override
  public Optional<String> getUserId() {

    return userId;
  }

  @Override
  public Optional<String> getClientId() {

    return clientId;
  }

  public static class Builder {

    private int count;
    private int startIndex;
    private String sortBy;
    private String sortDirection;
    private Optional<String> userId;
    private Optional<String> clientId;

    public Builder count(int count) {

      this.count = count;
      return this;
    }

    public Builder startIndex(int startIndex) {

      this.startIndex = startIndex;
      return this;
    }

    public Builder sortBy(String sortBy) {

      this.sortBy = sortBy;
      return this;
    }

    public Builder sortDirection(String sortDirection) {

      this.sortDirection = sortDirection;
      return this;
    }

    public Builder userId(String userId) {

      this.userId = Optional.ofNullable(userId);
      return this;
    }

    public Builder clientId(String clientId) {

      this.clientId = Optional.ofNullable(clientId);
      return this;
    }

    public DefaultTokensPageRequest build() {

      return new DefaultTokensPageRequest(this);
    }
  }
}
