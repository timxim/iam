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

angular.module('dashboardApp').controller('AddSshKeyController',
		AddSshKeyController);

AddSshKeyController.$inject = [ '$scope', '$uibModalInstance', 'scimFactory', 'Utils',
		'$state', 'user' ];

function AddSshKeyController($scope, $uibModalInstance, scimFactory, Utils, $state,
		user) {

	var addSshKeyCtrl = this;
	addSshKeyCtrl.user = user;
	addSshKeyCtrl.cancel = cancel;

	addSshKeyCtrl.addSshKey = addSshKey;
	addSshKeyCtrl.reset = reset;

	addSshKeyCtrl.reset();

	function reset() {

		addSshKeyCtrl.label = "";
		addSshKeyCtrl.value = "";
		addSshKeyCtrl.enabled = true;
	}

	function checkBase64Encoding() {

		var base64Matcher = new RegExp(
				"^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{4})$");
		return (base64Matcher.test(addSshKeyCtrl.value));
	}

	function addSshKey() {

		addSshKeyCtrl.enabled = false;

		if (!checkBase64Encoding()) {
			$scope.operationResult = Utils.buildErrorOperationResult({
				data: {
					detail: "Key is not a base64 encoded string!"
				}
			});
			return;
		}

		scimFactory.addSshKey(addSshKeyCtrl.user.id, addSshKeyCtrl.label,
				false, addSshKeyCtrl.value).then(
				function(response) {
					console.log("Added SSH-key ", addSshKeyCtrl.label,
							addSshKeyCtrl.value);
					$uibModalInstance.close(response.data);
					addSshKeyCtrl.enabled = true;
				}, function(error) {
					console.error('Error creating ssh key', error);
					$scope.operationResult = Utils.buildErrorOperationResult(error);
					addSshKeyCtrl.enabled = true;
				});
	}

	function cancel() {
		$uibModalInstance.dismiss("cancel");
	}
}