package it.infn.mw.iam.api.requests;

import javax.validation.Valid;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import it.infn.mw.iam.api.common.ListResponseDTO;
import it.infn.mw.iam.api.common.OffsetPageable;
import it.infn.mw.iam.api.common.PagingUtils;
import it.infn.mw.iam.api.requests.model.GroupRequestDto;
import it.infn.mw.iam.api.requests.service.GroupRequestsService;

@RestController
@RequestMapping("/iam/group_requests")
@Validated
public class GroupRequestsController {

  private static final Integer GROUP_REQUEST_MAX_PAGE_SIZE = 10;

  @Autowired
  private GroupRequestsService groupRequestService;

  @RequestMapping(method = RequestMethod.POST, value = {"", "/"})
  @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
  public GroupRequestDto createGroupRequest(@RequestBody @Valid GroupRequestDto groupRequest) {
    return groupRequestService.createGroupRequest(groupRequest);
  }

  @RequestMapping(method = RequestMethod.GET, value = "/")
  @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
  public ListResponseDTO<GroupRequestDto> listGroupRequest(
      @RequestParam(required = false) String username,
      @RequestParam(required = false) String groupName,
      @RequestParam(required = false) String status, @RequestParam(required = false) Integer count,
      @RequestParam(required = false) Integer startIndex) {

    OffsetPageable pageRequest =
        PagingUtils.buildPageRequest(count, startIndex, GROUP_REQUEST_MAX_PAGE_SIZE);
    return groupRequestService.listGroupRequests(username, groupName, status, pageRequest);
  }

  @RequestMapping(method = RequestMethod.GET, value = "/{requestId}")
  @PreAuthorize("hasRole('ADMIN') or #iam.userOwnsGroupRequest(#requestId)")
  public GroupRequestDto getGroupRequestDetails(
      @Valid @PathVariable("requestId") String requestId) {
    return groupRequestService.getGroupRequestDetails(requestId);
  }

  @RequestMapping(method = RequestMethod.DELETE, value = "/{requestId}")
  @PreAuthorize("hasRole('ADMIN') or #iam.userCanDeleteGroupRequest(#requestId)")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteGroupRequest(@Valid @PathVariable("requestId") String requestId) {
    groupRequestService.deleteGroupRequest(requestId);
  }

  @RequestMapping(method = RequestMethod.POST, value = "/{requestId}/approve")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.OK)
  public GroupRequestDto approveGroupRequest(@Valid @PathVariable("requestId") String requestId) {
    return groupRequestService.approveGroupRequest(requestId);
  }

  @RequestMapping(method = RequestMethod.POST, value = "/{requestId}/reject")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.OK)
  public GroupRequestDto rejectGroupRequest(@Valid @PathVariable("requestId") String requestId,
      @RequestParam @NotEmpty String motivation) {
    return groupRequestService.rejectGroupRequest(requestId, motivation);
  }

}
