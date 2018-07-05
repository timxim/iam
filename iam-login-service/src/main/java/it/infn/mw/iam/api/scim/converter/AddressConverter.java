/**
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2016-2018
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
package it.infn.mw.iam.api.scim.converter;

import org.mitre.openid.connect.model.Address;
import org.mitre.openid.connect.model.DefaultAddress;
import org.springframework.stereotype.Service;
import it.infn.mw.iam.api.common.Converter;
import it.infn.mw.iam.api.scim.model.ScimAddress;

@Service
public class AddressConverter implements Converter<ScimAddress, Address> {

  @Override
  public Address entityFromDto(ScimAddress scim) {

    Address address = new DefaultAddress();

    address.setCountry(scim.getCountry());
    address.setFormatted(scim.getFormatted());
    address.setLocality(scim.getLocality());
    address.setCountry(scim.getCountry());
    address.setPostalCode(scim.getPostalCode());
    address.setRegion(scim.getRegion());
    address.setStreetAddress(scim.getStreetAddress());

    return address;
  }

  @Override
  public ScimAddress dtoFromEntity(Address entity) {

    return ScimAddress.builder()
      .country(entity.getCountry())
      .formatted(entity.getFormatted())
      .locality(entity.getLocality())
      .postalCode(entity.getPostalCode())
      .region(entity.getRegion())
      .streetAddress(entity.getStreetAddress())
      .build();
  }

}
