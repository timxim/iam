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
package it.infn.mw.iam.test.repository;

import static org.hamcrest.Matchers.equalTo;

import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.authn.saml.util.Saml2Attribute;
import it.infn.mw.iam.persistence.model.IamAccount;
import it.infn.mw.iam.persistence.model.IamSamlId;
import it.infn.mw.iam.persistence.repository.IamAccountRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class})
public class IamAccountRepositoryTests {

  static final IamSamlId TEST_USER_ID = new IamSamlId("https://idptestbed/idp/shibboleth",
      Saml2Attribute.EPUID.getAttributeName(), "78901@idptestbed");

  @Autowired
  IamAccountRepository repo;

  @Autowired
  EntityManager em;

  @Test
  public void testSamlIdResolutionWorksAsExpected() {

    IamAccount testUserAccount = repo.findBySamlId(TEST_USER_ID)
      .orElseThrow(() -> new AssertionError("Could not lookup test user by SAML id"));

    Assert.assertThat(testUserAccount.getUsername(), equalTo("test"));
  }

}
