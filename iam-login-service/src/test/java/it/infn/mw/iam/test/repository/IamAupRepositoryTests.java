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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Date;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.persistence.model.IamAup;
import it.infn.mw.iam.persistence.repository.IamAupRepository;
import it.infn.mw.iam.test.api.aup.AupTestSupport;
import it.infn.mw.iam.test.util.DateEqualModulo1Second;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class})
@Transactional
public class IamAupRepositoryTests extends AupTestSupport{

  @Autowired
  EntityManager em;

  @Autowired
  IamAupRepository aupRepo;
  
  @Test
  public void defaultAupIsNotDefinedAtStartup() {

    Optional<IamAup> aup = aupRepo.findByName(DEFAULT_AUP_NAME);
    assertThat(aup.isPresent(), is(false));
  }

  @Test
  public void aupCreationWorks() {
  
    IamAup aup = buildDefaultAup();
    Date creationTime = aup.getCreationTime();    
    aupRepo.save(aup);

    aup = aupRepo.findByName(DEFAULT_AUP_NAME)
      .orElseThrow(() -> new AssertionError("Expected aup not found in repository"));
    
    assertThat(aup.getName(), equalTo(DEFAULT_AUP_NAME));
    assertThat(aup.getText(), equalTo(DEFAULT_AUP_TEXT));
    assertThat(aup.getDescription(), equalTo(DEFAULT_AUP_DESC));
    assertThat(aup.getCreationTime(), new DateEqualModulo1Second(creationTime));
    assertThat(aup.getLastUpdateTime(), new DateEqualModulo1Second(creationTime));
    assertThat(aup.getSignatureValidityInDays(), equalTo(365L));
    
  }
  
  @Test
  public void aupRemovalWorks() {
    
    IamAup aup = buildDefaultAup();
    
    aupRepo.save(aup);

    aup = aupRepo.findByName(DEFAULT_AUP_NAME)
      .orElseThrow(() -> new AssertionError("Expected aup not found in repository"));
    
    aupRepo.delete(aup);
    
    assertThat(aupRepo.findByName(DEFAULT_AUP_NAME).isPresent(), is(false)); 
  }



}
