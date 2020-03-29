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
package it.infn.mw.iam.test.oauth;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.UnsupportedEncodingException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.repository.OAuth2ClientRepository;
import org.mitre.oauth2.service.ClientDetailsEntityService;
import org.mitre.openid.connect.ClientDetailsEntityJsonProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.core.oauth.IamDeviceCodeTokenGranter;
import it.infn.mw.iam.core.oauth.TokenExchangeTokenGranter;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = IamLoginService.class)
@WebAppConfiguration
@Transactional
public class ClientRegistrationTests extends ClientRegistrationTestSupport {

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private ClientDetailsEntityService clientService;

  @Autowired
  private OAuth2ClientRepository clientRepo;

  private MockMvc mvc;

  @Before
  public void setup() throws Exception {
    mvc =
        MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).alwaysDo(log()).build();
  }

  @Test
  public void testClientRegistrationAccessTokenWorks() throws Exception {

    String jsonInString = ClientJsonStringBuilder.builder().scopes("test").build();

    // @formatter:off
    String response =
        mvc.perform(post(REGISTER_ENDPOINT)
            .contentType(APPLICATION_JSON)
            .content(jsonInString))
          .andExpect(status().isCreated())
          .andExpect(content().contentType(APPLICATION_JSON))
          .andReturn()
          .getResponse()
          .getContentAsString();
    // @formatter:on

    JsonNode jsonNode = mapper.readTree(response);

    String rat = jsonNode.get("registration_access_token").asText();
    String registrationUri = jsonNode.get("registration_client_uri").asText();

    assertThat(rat, notNullValue());
    assertThat(registrationUri, notNullValue());

    // @formatter:off
    mvc.perform(get(registrationUri)
        .contentType(APPLICATION_JSON)
        .header("Authorization", "Bearer " + rat))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON));
    
    mvc.perform(get(registrationUri)
        .contentType(APPLICATION_JSON)
        .header("Authorization", "Bearer " + rat))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON));
    // @formatter:on
  }

  @Test
  public void testCreateClientWithRegistrationReservedScopes() throws Exception {

    String[] scopes =
        {"registration:read", "registration:write", "scim:read", "scim:write", "proxy:generate"};

    String jsonInString = ClientJsonStringBuilder.builder().scopes(scopes).build();


    // @formatter:off
    String response =
        mvc.perform(post(REGISTER_ENDPOINT)
            .contentType(APPLICATION_JSON)
            .content(jsonInString))
          .andExpect(status().isCreated())
          .andExpect(content().contentType(APPLICATION_JSON))
          .andReturn()
          .getResponse()
          .getContentAsString();
    // @formatter:on

    ClientDetailsEntity saved = ClientDetailsEntityJsonProcessor.parse(response);

    assertNotNull(saved);
    for (String reservedScope : scopes) {
      assertThat(saved.getScope(), not(hasItem(reservedScope)));
    }
  }

  @Test
  public void testGetTokenWithScimReservedScopesFailure() throws Exception {

    String[] scopes = {"scim:read", "scim:write", "registration:read", "registration:write"};

    String jsonInString = ClientJsonStringBuilder.builder().scopes(scopes).build();

    // @formatter:off
    String response =
        mvc.perform(post(REGISTER_ENDPOINT)
            .contentType(APPLICATION_JSON)
            .content(jsonInString))
          .andExpect(status().isCreated())
          .andExpect(content().contentType(APPLICATION_JSON))
          .andReturn()
          .getResponse()
          .getContentAsString();
    // @formatter:on

    ClientDetailsEntity saved = ClientDetailsEntityJsonProcessor.parse(response);

    assertNotNull(saved);

    // @formatter:off
    mvc.perform(post("/token")
        .param("grant_type", "client_credentials")
        .param("client_id", saved.getClientId())
        .param("client_secret", saved.getClientSecret())
        .param("scope", ClientJsonStringBuilder.JOINER.join(scopes)))
      .andExpect(status().isBadRequest());
    // @formatter:on
  }

  @Test
  public void passwordGrantTypeNotAllowedWhenRegisteringNewClient() throws Exception {

    String jsonInString =
        ClientJsonStringBuilder.builder().grantTypes("authorization_code", "password").build();

    mvc.perform(post(REGISTER_ENDPOINT).contentType(APPLICATION_JSON).content(jsonInString))
      .andExpect(status().isCreated())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.grant_types", not(hasItem("password"))));

  }

  @Test
  public void tokenExchangeGrantTypeNotAllowedWhenRegisteringNewClient() throws Exception {

    String jsonInString = ClientJsonStringBuilder.builder()
      .grantTypes("authorization_code", TokenExchangeTokenGranter.GRANT_TYPE)
      .build();

    mvc.perform(post(REGISTER_ENDPOINT).contentType(APPLICATION_JSON).content(jsonInString))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.grant_types", not(hasItem(TokenExchangeTokenGranter.GRANT_TYPE))));

  }

  @Test
  public void additionalGrantTypesAreNotLostWhenUpdatingClient() throws Exception {

    String jsonInString =
        ClientJsonStringBuilder.builder().grantTypes("authorization_code").build();

    String clientJson =
        mvc.perform(post(REGISTER_ENDPOINT).contentType(APPLICATION_JSON).content(jsonInString))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.registration_access_token").exists())
          .andExpect(jsonPath("$.registration_client_uri").exists())
          .andReturn()
          .getResponse()
          .getContentAsString();

    JsonNode jsonNode = mapper.readTree(clientJson);

    String rat = jsonNode.get("registration_access_token").asText();
    String registrationUri = jsonNode.get("registration_client_uri").asText();

    String clientId = jsonNode.get("client_id").asText();

    ClientDetailsEntity clientModel = clientService.loadClientByClientId(clientId);
    clientModel.getGrantTypes().add("password");
    clientModel.getGrantTypes().add(TokenExchangeTokenGranter.GRANT_TYPE);

    clientRepo.saveClient(clientModel);

    clientJson = mvc
      .perform(get(registrationUri).contentType(APPLICATION_JSON)
        .header("Authorization", "Bearer " + rat))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(
          jsonPath("$.grant_types", hasItems("password", TokenExchangeTokenGranter.GRANT_TYPE)))
      .andReturn()
      .getResponse()
      .getContentAsString();

    mvc
      .perform(put(registrationUri).contentType(APPLICATION_JSON)
        .header("Authorization", "Bearer " + rat)
        .content(clientJson))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.grant_types", hasItem("password")))
      .andExpect(jsonPath("$.grant_types", hasItem(TokenExchangeTokenGranter.GRANT_TYPE)));


    clientModel = clientService.loadClientByClientId(clientId);
    clientModel.getGrantTypes().remove("password");
    clientModel.getGrantTypes().remove(TokenExchangeTokenGranter.GRANT_TYPE);
    clientRepo.saveClient(clientModel);

    mvc
      .perform(get(registrationUri).contentType(APPLICATION_JSON)
        .header("Authorization", "Bearer " + rat))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.grant_types", not(hasItem("password"))))
      .andExpect(jsonPath("$.grant_types", not(hasItem(TokenExchangeTokenGranter.GRANT_TYPE))))
      .andReturn()
      .getResponse()
      .getContentAsString();

    mvc
      .perform(put(registrationUri).contentType(APPLICATION_JSON)
        .header("Authorization", "Bearer " + rat)
        .content(clientJson))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.grant_types", not(hasItem("password"))))
      .andExpect(jsonPath("$.grant_types", not(hasItem(TokenExchangeTokenGranter.GRANT_TYPE))));

  }

  @Test
  public void deviceCodeTimeoutNotAffectedWhenCreatingAndUpdatingClient()
      throws UnsupportedEncodingException, Exception {
    String jsonInString =
        ClientJsonStringBuilder.builder().grantTypes("authorization_code").build();

    String clientJson =
        mvc.perform(post(REGISTER_ENDPOINT).contentType(APPLICATION_JSON).content(jsonInString))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.registration_access_token").exists())
          .andExpect(jsonPath("$.registration_client_uri").exists())
          .andReturn()
          .getResponse()
          .getContentAsString();

    JsonNode jsonNode = mapper.readTree(clientJson);

    String rat = jsonNode.get("registration_access_token").asText();
    String registrationUri = jsonNode.get("registration_client_uri").asText();

    String clientId = jsonNode.get("client_id").asText();

    ClientDetailsEntity clientModel = clientService.loadClientByClientId(clientId);
    assertThat(clientModel.getGrantTypes(), not(contains(IamDeviceCodeTokenGranter.GRANT_TYPE)));
    assertThat(clientModel.getDeviceCodeValiditySeconds(), greaterThan(0));

    mvc
      .perform(put(registrationUri).contentType(APPLICATION_JSON)
        .header("Authorization", "Bearer " + rat)
        .content(clientJson))
      .andExpect(status().isOk());

    clientModel = clientService.loadClientByClientId(clientId);
    assertThat(clientModel.getGrantTypes(), not(contains(IamDeviceCodeTokenGranter.GRANT_TYPE)));
    assertThat(clientModel.getDeviceCodeValiditySeconds(), greaterThan(0));

  }



}
