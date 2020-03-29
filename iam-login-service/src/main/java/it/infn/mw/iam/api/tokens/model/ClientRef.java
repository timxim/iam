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
package it.infn.mw.iam.api.tokens.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ClientRef {

  private Long id;
  private String clientId;
  private Set<String> contacts;
  private String ref = null;

  @JsonCreator
  public ClientRef(@JsonProperty("id") Long id, @JsonProperty("clientId") String clientId,
      @JsonProperty("contacts") Set<String> contacts, @JsonProperty("$ref") String ref) {

    this.id = id;
    this.clientId = clientId;
    this.contacts = contacts;
    this.ref = ref;
  }

  public ClientRef(Builder builder) {

    this.id = builder.id;
    this.clientId = builder.clientId;
    this.contacts = builder.contacts;
    this.ref = builder.ref;
  }

  @JsonProperty("id")
  public Long getId() {

    return id;
  }

  @JsonProperty("clientId")
  public String getClientId() {

    return clientId;
  }

  @JsonProperty("contacts")
  public Set<String> getContacts() {

    return contacts;
  }

  @JsonProperty("$ref")
  public String getRef() {

    return ref;
  }

  @Override
  public String toString() {

    return "Client [id=" + id + ", clientId=" + clientId + ", contacts=" + contacts
        + ", ref=" + ref + "]";
  }

  public static Builder builder() {

    return new Builder();
  }

  public static class Builder {

    private Long id;
    private String clientId;
    private Set<String> contacts;
    private String ref;

    public Builder id(Long id) {
      this.id = id;
      return this;
    }

    public Builder clientId(String clientId) {
      this.clientId = clientId;
      return this;
    }

    public Builder contacts(Set<String> contacts) {
      this.contacts = contacts;
      return this;
    }

    public Builder ref(String ref) {
      this.ref = ref;
      return this;
    }

    public ClientRef build() {
      return new ClientRef(this);
    }
  }
}
