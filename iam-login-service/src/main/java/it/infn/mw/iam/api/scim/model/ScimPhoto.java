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
package it.infn.mw.iam.api.scim.model;

import javax.annotation.Generated;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ScimPhoto {

  @NotEmpty
  @NotNull
  private final String value;

  @NotEmpty
  @NotNull
  private final ScimPhotoType type;

  public enum ScimPhotoType {
    thumbnail, photo;
  }

  @JsonCreator
  private ScimPhoto(@JsonProperty("value") String value, @JsonProperty("type") ScimPhotoType type) {

    this.value = value;
    this.type = type;
  }

  public String getValue() {

    return this.value;
  }

  public ScimPhotoType getType() {

    return this.type;
  }

  private ScimPhoto(Builder b) {

    this.value = b.value;
    this.type = b.type;
  }

  @Generated("eclipse")
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    return result;
  }

  @Generated("eclipse")
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ScimPhoto other = (ScimPhoto) obj;
    if (value == null) {
      if (other.value != null)
        return false;
    } else if (!value.equals(other.value))
      return false;
    return true;
  }

  public static Builder builder() {

    return new Builder();
  }

  public static class Builder {

    private String value;
    private ScimPhotoType type;

    public Builder() {

      this.type = ScimPhotoType.photo;
    }

    public Builder value(String value) {

      this.value = value;
      return this;
    }

    public ScimPhoto build() {

      return new ScimPhoto(this);
    }
  }
}
