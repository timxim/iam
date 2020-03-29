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
package it.infn.mw.iam.api.scim.model;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import it.infn.mw.iam.api.scim.model.ScimUser.NewUserValidation;

@JsonInclude(Include.NON_EMPTY)
public class ScimSshKey {

  @Length(max = 36)
  @NotBlank(groups = {NewUserValidation.class})
  private final String display;

  private final Boolean primary;

  @Length(max = 48)
  private final String fingerprint;

  @NotBlank(groups = {NewUserValidation.class})
  private final String value;

  @JsonCreator
  private ScimSshKey(@JsonProperty("display") String display,
      @JsonProperty("primary") Boolean primary, @JsonProperty("value") String value,
      @JsonProperty("fingerprint") String fingerprint) {

    this.display = display;
    this.value = value;
    this.primary = primary;
    this.fingerprint = fingerprint;
  }

  public String getDisplay() {

    return display;
  }

  public String getFingerprint() {

    return fingerprint;
  }

  public String getValue() {

    return value;
  }

  public Boolean isPrimary() {

    return primary;
  }

  private ScimSshKey(Builder b) {

    this.display = b.display;
    this.primary = b.primary;
    this.value = b.value;
    this.fingerprint = b.fingerprint;
  }

  public static Builder builder() {

    return new Builder();
  }

  public static class Builder {

    private String display;
    private String value;
    private Boolean primary;
    private String fingerprint;

    public Builder display(String display) {

      this.display = display;
      return this;
    }

    public Builder value(String value) {

      this.value = value;
      return this;
    }

    public Builder fingerprint(String fingerprint) {

      this.fingerprint = fingerprint;
      return this;
    }

    public Builder primary(Boolean primary) {

      this.primary = primary;
      return this;
    }

    public ScimSshKey build() {

      return new ScimSshKey(this);
    }
  }

  @Override
  public String toString() {
    return "ScimSshKey [display=" + display + ", primary=" + primary + ", fingerprint="
        + fingerprint + ", value=" + value + "]";
  }
}
