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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class ScimIndigoGroup {

  private ScimGroupRef parentGroup;
  
  private String description;

  @JsonCreator
  private ScimIndigoGroup(@JsonProperty("parentGroup") ScimGroupRef parentGroup,
      @JsonProperty("description") String description) {
    this.parentGroup = parentGroup;
    this.description = description;
  }

  private ScimIndigoGroup(Builder builder) {
    this.parentGroup = builder.parentGroup;
    this.description = builder.description;
  }

  public ScimGroupRef getParentGroup() {
    return parentGroup;
  }

  public String getDescription() {
    return description;
  }
  
  public static Builder getBuilder() {
    return new Builder();
  }

  public static class Builder {
    private ScimGroupRef parentGroup = null;
    
    private String description;

    public Builder parentGroup(ScimGroupRef parentGroup) {
      this.parentGroup = parentGroup;
      return this;
    }
    
    public Builder description(String desc) {
      this.description = desc;
      return this;
    }

    public ScimIndigoGroup build() {

      return new ScimIndigoGroup(this);
    }
  }

}
