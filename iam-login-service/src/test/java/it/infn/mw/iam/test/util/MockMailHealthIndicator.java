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
package it.infn.mw.iam.test.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health.Builder;

import it.infn.mw.iam.actuator.endpoint.IamMailHealthIndicator;

public class MockMailHealthIndicator extends IamMailHealthIndicator {

  boolean active = false;
  String mailhost;
  int mailPort;

  @Autowired
  public MockMailHealthIndicator() {
    super(null);
  }


  public boolean isActive() {
    return active;
  }


  public void setActive(boolean active) {
    this.active = active;
  }



  public String getMailhost() {
    return mailhost;
  }


  public void setMailhost(String mailhost) {
    this.mailhost = mailhost;
  }


  public int getMailPort() {
    return mailPort;
  }


  public void setMailPort(int mailPort) {
    this.mailPort = mailPort;
  }


  @Override
  protected void doHealthCheck(Builder builder) throws Exception {

    builder.withDetail("location", String.format("%s:%d", mailhost, mailPort));
    
    if (isActive()) {
      builder.up();
    } else {
      builder.down();
    }
  }
}
