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

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.infn.mw.iam.api.scim.model.ScimGroup;
import it.infn.mw.iam.api.scim.model.ScimGroupRef;
import it.infn.mw.iam.api.scim.model.ScimIndigoGroup;
import it.infn.mw.iam.api.scim.model.ScimMemberRef;
import it.infn.mw.iam.api.scim.model.ScimMeta;
import it.infn.mw.iam.persistence.model.IamAccount;
import it.infn.mw.iam.persistence.model.IamGroup;

@Service
public class GroupConverter implements Converter<ScimGroup, IamGroup> {

  private final ScimResourceLocationProvider resourceLocationProvider;

  @Autowired
  public GroupConverter(ScimResourceLocationProvider rlp) {

    this.resourceLocationProvider = rlp;
  }

  /**
   * <ul>
   * <li>Mutable fields: name</li>
   * <li>Immutable fields: id, uuid, creationtime</li>
   * <li>Read-only fields: lastupdatetime, accounts</li>
   * <li>Not managed via SCIM: description</li>
   * </ul>
   */
  @Override
  public IamGroup entityFromDto(ScimGroup scimGroup) {

    IamGroup group = new IamGroup();

    group.setName(scimGroup.getDisplayName());
    
    if (scimGroup.getIndigoGroup() != null && StringUtils.isNotBlank(scimGroup.getIndigoGroup().getDescription())) {
      group.setDescription(scimGroup.getIndigoGroup().getDescription());
    }

    return group;
  }

  @Override
  public ScimGroup dtoFromEntity(IamGroup entity) {

    ScimMeta meta = ScimMeta.builder(entity.getCreationTime(), entity.getLastUpdateTime())
      .location(resourceLocationProvider.groupLocation(entity.getUuid()))
      .resourceType(ScimGroup.RESOURCE_TYPE)
      .build();

    Set<ScimMemberRef> members = new HashSet<>();

    for (IamAccount account : entity.getAccounts()) {
      ScimMemberRef memberRef = new ScimMemberRef.Builder().value(account.getUuid())
        .display(account.getUserInfo().getName())
        .ref(resourceLocationProvider.userLocation(account.getUuid()))
        .build();
      members.add(memberRef);
    }

    for (IamGroup subgroup : entity.getChildrenGroups()) {
      ScimMemberRef memberRef = new ScimMemberRef.Builder().display(subgroup.getName())
        .value(subgroup.getUuid())
        .ref(resourceLocationProvider.groupLocation(subgroup.getUuid()))
        .build();
      members.add(memberRef);
    }

    IamGroup iamParentGroup = entity.getParentGroup();
    ScimIndigoGroup.Builder scimIndigoGroup = ScimIndigoGroup.getBuilder();

    if (iamParentGroup != null) {
      
      ScimGroupRef parentGroupRef = ScimGroupRef.builder()
        .display(iamParentGroup.getName())
        .value(iamParentGroup.getUuid())
        .ref(resourceLocationProvider.groupLocation(iamParentGroup.getUuid()))
        .build();
      
      scimIndigoGroup.parentGroup(parentGroupRef);
    } 
    
    if (isNotBlank(entity.getDescription())) {
      scimIndigoGroup.description(entity.getDescription());
    }
    return ScimGroup.builder(entity.getName())
      .id(entity.getUuid())
      .meta(meta)
      .setMembers(members)
      .indigoGroup(scimIndigoGroup.build())
      .build();
  }

}
