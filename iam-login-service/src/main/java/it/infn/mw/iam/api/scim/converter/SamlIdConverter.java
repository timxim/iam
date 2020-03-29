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
package it.infn.mw.iam.api.scim.converter;

import org.springframework.stereotype.Service;

import it.infn.mw.iam.api.scim.model.ScimSamlId;
import it.infn.mw.iam.persistence.model.IamSamlId;

@Service
public class SamlIdConverter implements Converter<ScimSamlId, IamSamlId> {

  @Override
  public IamSamlId entityFromDto(ScimSamlId scim) {

    IamSamlId samlId = new IamSamlId();
    samlId.setIdpId(scim.getIdpId());
    samlId.setUserId(scim.getUserId());
    samlId.setAttributeId(scim.getAttributeId());
    samlId.setAccount(null);

    return samlId;
  }

  @Override
  public ScimSamlId dtoFromEntity(IamSamlId entity) {

    return ScimSamlId.builder().idpId(entity.getIdpId()).
        userId(entity.getUserId())
        .attributeId(entity.getAttributeId())
        .build();
  }
}
