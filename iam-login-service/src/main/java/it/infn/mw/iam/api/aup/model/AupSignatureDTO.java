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
package it.infn.mw.iam.api.aup.model;

import java.util.Date;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import it.infn.mw.iam.api.scim.controller.utils.JsonDateSerializer;

public class AupSignatureDTO {

  AupDTO aup;
  
  AccountDTO account;
  
  @JsonSerialize(using = JsonDateSerializer.class)
  Date signatureTime;
  
  public AupSignatureDTO() {
    // empty constructor
  }


  public AupDTO getAup() {
    return aup;
  }


  public void setAup(AupDTO aup) {
    this.aup = aup;
  }


  public AccountDTO getAccount() {
    return account;
  }


  public void setAccount(AccountDTO account) {
    this.account = account;
  }
  
  public void setSignatureTime(Date signatureTime) {
    this.signatureTime = signatureTime;
  }


  public Date getSignatureTime() {
    return signatureTime;
  }
    
}
