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
package it.infn.mw.iam.test.ext_authn.saml;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

import javax.xml.xpath.XPathExpressionException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.test.MockMvcTestSupport;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class, CoreControllerTestSupport.class})
@WebAppConfiguration
@TestPropertySource(properties = {"saml.local-metadata.generated=false",
    "saml.local-metadata.locationUrl=classpath:/saml/metadata/iam-md.xml"})
@Transactional
public class LocalMetadataLoadingTests extends MockMvcTestSupport {

  @Before
  public void setup() {
    initMockMvc();
  }

  @Test
  public void testLocalMetadataLoading() throws XPathExpressionException, Exception {
    mvc.perform(get("/saml/metadata"))
      .andExpect(status().isOk())
      .andExpect(xpath("/EntityDescriptor/@entityID").string(is("urn:iam:iam-local-devel")));
  }
}
