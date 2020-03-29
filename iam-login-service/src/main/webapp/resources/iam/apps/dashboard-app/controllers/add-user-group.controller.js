/*
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
'use strict';

angular.module('dashboardApp').controller('AddUserGroupController',
		AddUserGroupController);

AddUserGroupController.$inject = [ '$scope', '$state', '$filter', 'Utils', '$q',
		'$uibModalInstance', '$sanitize', 'scimFactory', 'user' ];

function AddUserGroupController($scope, $state, $filter, Utils, $q, $uibModalInstance,
		$sanitize, scimFactory, user) {

	var addGroupCtrl = this;

	// methods
	addGroupCtrl.cancel = cancel;
	addGroupCtrl.lookupGroups = lookupGroups;
	addGroupCtrl.addGroup = addGroup;
	addGroupCtrl.getAllGroups = getAllGroups;
	addGroupCtrl.getNotMemberGroups = getNotMemberGroups;
	addGroupCtrl.loadGroups = loadGroups;

	// params
	addGroupCtrl.user = user;

	// fields
	addGroupCtrl.groupsSelected = null;
	addGroupCtrl.groups = [];
	addGroupCtrl.oGroups = [];
	addGroupCtrl.enabled = true;

	addGroupCtrl.loadGroups();

	function lookupGroups() {

		return addGroupCtrl.oGroups;
	}

	function loadGroups() {

		addGroupCtrl.loadingGroupsProgress = 30;
		addGroupCtrl.getAllGroups(1, 10);
	}

	function cancel() {

		$uibModalInstance.dismiss('Cancel');
	}

	function addGroup() {

		addGroupCtrl.enabled = false;
		var requests = [];
		var groupNames = [];
		angular.forEach(addGroupCtrl.groupsSelected, function(groupToAdd) {
			groupNames.push(groupToAdd.displayName);
			requests.push(scimFactory.addUserToGroup(groupToAdd.id,
					addGroupCtrl.user));
		});

		$q.all(requests).then(function(response) {
			console.log("Added ", addGroupCtrl.groupsSelected);
			$uibModalInstance.close(`User added to groups: '${groupNames.join(",")}'`);
			addGroupCtrl.enabled = true;
		}, function(error) {
			console.error(error);
			$scope.operationResult = Utils.buildErrorOperationResult(error);
			addGroupCtrl.enabled = true;
		});
	}
	
	function getAllGroups(startIndex, count) {

		scimFactory
			.getGroups(startIndex, count)
				.then(function(response) {

					angular.forEach(response.data.Resources, function(group) {
						addGroupCtrl.groups.push(group);
					});
					addGroupCtrl.groups = $filter('orderBy')(addGroupCtrl.groups, "displayName", false);

					if (response.data.totalResults > (response.data.startIndex + response.data.itemsPerPage)) {

						addGroupCtrl.loadingGroupsProgress = Math.floor((startIndex + count) * 100 / response.data.totalResults);
						addGroupCtrl.getAllGroups(startIndex + count, count);

					} else {

						addGroupCtrl.loadingGroupsProgress = 100;
						addGroupCtrl.oGroups = addGroupCtrl.getNotMemberGroups();
					}
				}, function(error) {

					console.error(error);
					$scope.operationResult = Utils.buildErrorOperationResult(error);
				});
	}

	function getNotMemberGroups() {
	
		return addGroupCtrl.groups.filter(function(group) {
			for ( var i in addGroupCtrl.user.groups) {
				if (group.id === addGroupCtrl.user.groups[i].value) {
					return false;
				}
			}
			return true;
		});
	}

}