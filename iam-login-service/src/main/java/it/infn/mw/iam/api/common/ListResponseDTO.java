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
package it.infn.mw.iam.api.common;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListResponseDTO<T> {

  private final Long totalResults;
  private final Integer itemsPerPage;
  private final Integer startIndex;
  private final List<T> resources;

  @JsonCreator
  public ListResponseDTO(@JsonProperty("totalResults") Long totalResults,
      @JsonProperty("itemsPerPage") Integer itemsPerPage,
      @JsonProperty("startIndex") Integer startIndex,
      @JsonProperty("Resources") List<T> resources) {

    this.totalResults = totalResults;
    this.itemsPerPage = itemsPerPage;
    this.startIndex = startIndex;
    this.resources = resources;
  }

  protected ListResponseDTO(Builder<T> builder) {
    this.totalResults = builder.totalResults;
    this.startIndex = builder.startIndex;
    this.itemsPerPage = builder.itemsPerPage;
    this.resources = builder.resources;
  }

  public Long getTotalResults() {
    return totalResults;
  }

  public Integer getItemsPerPage() {
    return itemsPerPage;
  }

  public Integer getStartIndex() {
    return startIndex;
  }

  @JsonProperty("Resources")
  public List<T> getResources() {
    return resources;
  }

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }


  public static class Builder<T> {

    private Long totalResults = null;
    private Integer itemsPerPage = null;
    private Integer startIndex = null;
    private List<T> resources = null;

    public <S> Builder<T> fromPage(Page<S> page, Pageable op) {
      this.totalResults = page.getTotalElements();
      this.itemsPerPage = page.getNumberOfElements();
      this.startIndex = op.getOffset() + 1;
      return this;
    }

    public Builder<T> totalResults(Long totalResults) {
      this.totalResults = totalResults;
      return this;
    }

    public Builder<T> itemsPerPage(Integer itemsPerPage) {
      this.itemsPerPage = itemsPerPage;
      return this;
    }

    public Builder<T> startIndex(Integer startIndex) {
      this.startIndex = startIndex;
      return this;
    }

    public Builder<T> resources(List<T> resources) {
      this.resources = resources;
      return this;
    }

    public ListResponseDTO<T> build() {
      return new ListResponseDTO<>(this);
    }
  }
}
