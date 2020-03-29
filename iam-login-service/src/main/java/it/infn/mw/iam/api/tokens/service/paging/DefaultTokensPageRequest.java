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
package it.infn.mw.iam.api.tokens.service.paging;

public class DefaultTokensPageRequest implements TokensPageRequest {

  private final int count;
  private final int startIndex;

  private DefaultTokensPageRequest(Builder b) {
    this.count = b.count;
    this.startIndex = b.startIndex;
  }

  @Override
  public int getCount() {

    return count;
  }

  @Override
  public int getStartIndex() {

    return startIndex;
  }

  public static class Builder {

    private int count;
    private int startIndex;

    public Builder count(int count) {

      this.count = count;
      return this;
    }

    public Builder startIndex(int startIndex) {

      this.startIndex = startIndex;
      return this;
    }

    public DefaultTokensPageRequest build() {

      return new DefaultTokensPageRequest(this);
    }
  }
}
