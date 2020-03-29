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
package it.infn.mw.iam.test.registration.cern;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.registration.cern.CernHrDBApiService;
import it.infn.mw.iam.api.registration.cern.CernHrDbApiError;
import it.infn.mw.iam.authn.oidc.RestTemplateFactory;
import it.infn.mw.iam.test.util.oidc.MockRestTemplateFactory;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class, CernHrDbApiClientTests.class})
@WebAppConfiguration
@Transactional
@TestPropertySource(properties = {"cern.hr-api.username=" + CernTestSupport.HR_API_USERNAME,
    "cern.hr-api.password=" + CernTestSupport.HR_API_PASSWORD,
    "cern.hr-api.url=" + CernTestSupport.HR_API_URL,
    "cern.experiment-name=" + CernTestSupport.EXPERIMENT_NAME,
    "cern.sso-entity-id=" + CernTestSupport.SSO_ENTITY_ID})
@ActiveProfiles({"h2-test","cern"})
public class CernHrDbApiClientTests extends CernTestSupport {

  @Bean
  @Primary
  public RestTemplateFactory mockRestTemplateFactory() {
    return new MockRestTemplateFactory();
  }

  @Autowired
  RestTemplateFactory rtf;

  @Autowired
  ObjectMapper mapper;

  MockRestTemplateFactory mockRtf;

  @Autowired
  CernHrDBApiService hrDbService;

  @Before
  public void setup() {
    mockRtf = (MockRestTemplateFactory) rtf;
    mockRtf.resetTemplate();
  }

  @Test
  public void checkMembershipSuccess() {
    String personId = "12356789";
    String apiValidationUrl = apiValidationUrl(personId);
    mockRtf.getMockServer()
      .expect(requestTo(apiValidationUrl))
      .andExpect(method(GET))
      .andExpect(header("Authorization", BASIC_AUTH_HEADER_VALUE))
      .andRespond(withStatus(OK).contentType(APPLICATION_JSON_UTF8).body("true"));

    assertThat(hrDbService.hasValidExperimentParticipation(personId), is(true));
  }

  @Test
  public void checkMembershipFailure() {
    String personId = "12356789";
    String apiValidationUrl = apiValidationUrl(personId);
    mockRtf.getMockServer()
      .expect(requestTo(apiValidationUrl))
      .andExpect(method(GET))
      .andExpect(header("Authorization", BASIC_AUTH_HEADER_VALUE))
      .andRespond(withStatus(OK).contentType(APPLICATION_JSON_UTF8).body("false"));

    assertThat(hrDbService.hasValidExperimentParticipation(personId), is(false));
  }

  @Test(expected=CernHrDbApiError.class)
  public void checkAuthorizationError() {
    String personId = "12356789";
    String apiValidationUrl = apiValidationUrl(personId);
    mockRtf.getMockServer()
      .expect(requestTo(apiValidationUrl))
      .andExpect(method(GET))
      .andExpect(header("Authorization", BASIC_AUTH_HEADER_VALUE))
      .andRespond(withUnauthorizedRequest());
    try {
      hrDbService.hasValidExperimentParticipation(personId);
    } catch (CernHrDbApiError e) {
      assertThat(e.getMessage(), containsString("401"));
      throw e;
    }
  }
}
