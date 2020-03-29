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
package it.infn.mw.iam.api.scim.provisioning;

import static java.lang.String.format;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;

import it.infn.mw.iam.api.common.OffsetPageable;
import it.infn.mw.iam.api.scim.converter.GroupConverter;
import it.infn.mw.iam.api.scim.exception.IllegalArgumentException;
import it.infn.mw.iam.api.scim.exception.ScimPatchOperationNotSupported;
import it.infn.mw.iam.api.scim.exception.ScimResourceExistsException;
import it.infn.mw.iam.api.scim.exception.ScimResourceNotFoundException;
import it.infn.mw.iam.api.scim.model.ScimGroup;
import it.infn.mw.iam.api.scim.model.ScimListResponse;
import it.infn.mw.iam.api.scim.model.ScimListResponse.ScimListResponseBuilder;
import it.infn.mw.iam.api.scim.model.ScimMemberRef;
import it.infn.mw.iam.api.scim.model.ScimPatchOperation;
import it.infn.mw.iam.api.scim.provisioning.paging.ScimPageRequest;
import it.infn.mw.iam.api.scim.updater.AccountUpdater;
import it.infn.mw.iam.api.scim.updater.factory.DefaultGroupMembershipUpdaterFactory;
import it.infn.mw.iam.core.group.IamGroupService;
import it.infn.mw.iam.persistence.model.IamAccount;
import it.infn.mw.iam.persistence.model.IamGroup;
import it.infn.mw.iam.persistence.repository.IamAccountRepository;

@Service
public class ScimGroupProvisioning
    implements ScimProvisioning<ScimGroup, List<ScimMemberRef>>, ApplicationEventPublisherAware {

  private static final int GROUP_NAME_MAX_LENGTH = 50;
  private static final int GROUP_FULLNAME_MAX_LENGTH = 512;

  private final IamGroupService groupService;
  private final IamAccountRepository accountRepository;
  private final Clock clock;

  private final GroupConverter converter;

  private final DefaultGroupMembershipUpdaterFactory groupUpdaterFactory;
  private ApplicationEventPublisher eventPublisher;

  @Autowired
  public ScimGroupProvisioning(IamGroupService groupService, IamAccountRepository accountRepository,
      GroupConverter converter, Clock clock) {

    this.accountRepository = accountRepository;
    this.groupService = groupService;
    this.converter = converter;
    this.clock = clock;

    this.groupUpdaterFactory = new DefaultGroupMembershipUpdaterFactory(accountRepository);

  }

  private void checkUnsupportedPath(ScimPatchOperation<List<ScimMemberRef>> op) {

    if (op.getPath() == null || op.getPath().isEmpty()) {
      throw new ScimPatchOperationNotSupported("empty path value is not currently supported");
    }
    if ("members".equals(op.getPath())) {
      return;
    }
    throw new ScimPatchOperationNotSupported(
        "path value " + op.getPath() + " is not currently supported");
  }

  @Override
  public ScimGroup create(ScimGroup group) {

    displayNameSanityChecks(group.getDisplayName());

    IamGroup iamGroup = new IamGroup();
    String uuid = UUID.randomUUID().toString();

    iamGroup.setUuid(uuid);
    iamGroup.setName(group.getDisplayName());

    IamGroup iamParentGroup = null;

    if (group.getIndigoGroup().getParentGroup() != null) {
      String parentGroupUuid = group.getIndigoGroup().getParentGroup().getValue();
      String parentGroupName = group.getIndigoGroup().getParentGroup().getDisplay();

      iamParentGroup = groupService.findByUuid(parentGroupUuid)
        .orElseThrow(() -> new ScimResourceNotFoundException(
            String.format("Parent group '%s' not found", parentGroupUuid)));

      String fullName = String.format("%s/%s", parentGroupName, group.getDisplayName());
      fullNameSanityChecks(fullName);

      iamGroup.setParentGroup(iamParentGroup);
      iamGroup.setName(fullName);

      Set<IamGroup> children = iamParentGroup.getChildrenGroups();
      children.add(iamGroup);
    }

    groupService.createGroup(iamGroup);

    return converter.dtoFromEntity(iamGroup);
  }

  @Override
  public void delete(String id) {
    idSanityChecks(id);
    groupService.deleteGroupByUuid(id);
  }

  private void displayNameSanityChecks(String displayName) {
    if (Strings.isNullOrEmpty(displayName)) {
      throw new IllegalArgumentException("Group displayName cannot be empty");
    }

    if (displayName.contains("/")) {
      throw new IllegalArgumentException("Group displayName cannot contain a slash character");
    }

    if (displayName.length() > GROUP_NAME_MAX_LENGTH) {
      throw new IllegalArgumentException(
          format("Group name length cannot exceed %d characters", GROUP_NAME_MAX_LENGTH));
    }
  }

  private void executePatchOperation(IamGroup group, ScimPatchOperation<List<ScimMemberRef>> op) {

    checkUnsupportedPath(op);

    List<AccountUpdater> updaters = groupUpdaterFactory.getUpdatersForPatchOperation(group, op);
    List<AccountUpdater> updatesToPublish = new ArrayList<>();

    boolean hasChanged = false;

    for (AccountUpdater u : updaters) {
      if (u.update()) {
        IamAccount a = u.getAccount();
        a.touch();
        accountRepository.save(a);
        hasChanged = true;
        updatesToPublish.add(u);
      }
    }

    if (hasChanged) {

      group.touch(clock);
      groupService.save(group);
      for (AccountUpdater u : updatesToPublish) {
        u.publishUpdateEvent(this, eventPublisher);
      }
    }
  }



  private void fullNameSanityChecks(String displayName) {
    if (displayName.length() > GROUP_FULLNAME_MAX_LENGTH) {
      throw new IllegalArgumentException(format(
          "Group displayName length cannot exceed %d characters", GROUP_FULLNAME_MAX_LENGTH));
    }

    if (groupService.findByName(displayName).isPresent()) {
      throw new ScimResourceExistsException(format("Duplicated group '%s'", displayName));
    }
  }

  @Override
  public ScimGroup getById(String id) {

    idSanityChecks(id);

    IamGroup group = groupService.findByUuid(id).orElseThrow(() -> noGroupMappedToId(id));

    return converter.dtoFromEntity(group);
  }

  private void idSanityChecks(String id) {

    if (id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }

    if (id.trim().isEmpty()) {
      throw new IllegalArgumentException("id cannot be the empty string");
    }
  }

  private boolean isGroupNameAvailable(String displayName, String id) {

    return !groupService.findByNameWithDifferentId(displayName, id).isPresent();
  }

  @Override
  public ScimListResponse<ScimGroup> list(ScimPageRequest params) {

    ScimListResponseBuilder<ScimGroup> builder = ScimListResponse.builder();

    if (params.getCount() == 0) {

      long totalResults = groupService.countAllGroups();
      builder.totalResults(totalResults);

    } else {

      OffsetPageable op = new OffsetPageable(params.getStartIndex(), params.getCount());

      Page<IamGroup> results = groupService.findAll(op);

      List<ScimGroup> resources = new ArrayList<>();

      results.getContent().forEach(g -> resources.add(converter.dtoFromEntity(g)));

      builder.resources(resources);
      builder.fromPage(results, op);

    }

    return builder.build();
  }

  private ScimResourceNotFoundException noGroupMappedToId(String id) {
    return new ScimResourceNotFoundException(String.format("No group mapped to id '%s'", id));
  }

  @Override
  public ScimGroup replace(String id, ScimGroup scimItemToBeReplaced) {

    IamGroup oldGroup = groupService.findByUuid(id).orElseThrow(() -> noGroupMappedToId(id));

    String displayName = scimItemToBeReplaced.getDisplayName();
    displayNameSanityChecks(displayName);

    if (!isGroupNameAvailable(displayName, id)) {
      throw new ScimResourceExistsException(displayName + " is already mapped to another group");
    }

    IamGroup newGroup = converter.entityFromDto(scimItemToBeReplaced);
    groupService.updateGroup(oldGroup, newGroup);

    return converter.dtoFromEntity(newGroup);
  }

  public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
    this.eventPublisher = publisher;
  }

  @Override
  public void update(String id, List<ScimPatchOperation<List<ScimMemberRef>>> operations) {

    IamGroup iamGroup = groupService.findByUuid(id).orElseThrow(() -> noGroupMappedToId(id));

    operations.forEach(op -> executePatchOperation(iamGroup, op));
  }

}
