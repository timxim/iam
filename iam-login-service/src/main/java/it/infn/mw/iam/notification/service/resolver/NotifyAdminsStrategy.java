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
package it.infn.mw.iam.notification.service.resolver;

import static it.infn.mw.iam.notification.service.resolver.DefaultAddressResolutionService.VO_ADMINS;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

public class NotifyAdminsStrategy implements AdminNotificationDeliveryStrategy {

  final AddressResolutionService service;
  
  @Autowired
  public NotifyAdminsStrategy(AddressResolutionService service) {
    this.service = service;
  }
  @Override
  public List<String> resolveAdminEmailAddresses() {
    return service.resolveAddressesForAudience(VO_ADMINS);
  }

}
