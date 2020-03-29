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
package it.infn.mw.iam.test.scim;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class RestUtils {

  protected final MockMvc mvc;
  protected final ObjectMapper mapper;

  @Autowired
  public RestUtils(WebApplicationContext context, ObjectMapper mapper) {

    mvc = MockMvcBuilders.webAppContextSetup(context)
      .apply(springSecurity())
      .alwaysDo(log())
      .build();
    this.mapper = mapper;
  }

  public <B extends Object> ResultActions doPost(String location, B contentObj, String contentType,
      HttpStatus expectedStatus) throws Exception {

    String contentJson = mapper.writeValueAsString(contentObj);

    return mvc.perform(post(location).contentType(contentType).content(contentJson))
      .andExpect(status().is(expectedStatus.value()))
      .andExpect(content().contentType(contentType));
  }

  public <B extends Object> ResultActions doPost(String location,
      MultiValueMap<String, String> formParams, String expectedContentType,
      HttpStatus expectedStatus) throws Exception {

    return mvc.perform(post(location).contentType(APPLICATION_FORM_URLENCODED).params(formParams))
      .andExpect(status().is(expectedStatus.value()))
      .andExpect(content().contentType(expectedContentType));
  }

  public ResultActions doGet(String location, String expectedContentType, HttpStatus expectedStatus)
      throws Exception {

    return mvc.perform(get(location)).andExpect(status().is(expectedStatus.value()));
  }

  public ResultActions doGet(String location, MultiValueMap<String, String> params,
      String expectedContentType, HttpStatus expectedStatus) throws Exception {

    return mvc.perform(get(location).params(params)).andExpect(status().is(expectedStatus.value()));
  }

  public ResultActions doDelete(String location, HttpStatus expectedStatus) throws Exception {

    return mvc.perform(delete(location)).andExpect(status().is(expectedStatus.value()));
  }

  public <B> ResultActions doPut(String location, B contentObj, String expectedContentType,
      HttpStatus expectedStatus) throws Exception {

    String contentJson = mapper.writeValueAsString(contentObj);

    return mvc.perform(put(location).contentType(expectedContentType).content(contentJson))
      .andExpect(status().is(expectedStatus.value()))
      .andExpect(content().contentType(expectedContentType));
  }

  public <T> ResultActions doPatch(String location, T contentObj, String expectedContentType,
      HttpStatus expectedStatus) throws Exception {

    String contentJson = mapper.writeValueAsString(contentObj);

    return mvc.perform(patch(location).contentType(expectedContentType).content(contentJson))
      .andExpect(status().is(expectedStatus.value()));
  }

}
