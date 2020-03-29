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
(function() {
  'use strict';

  angular.module('dashboardApp')
      .controller('EditUserController', EditUserController);

  EditUserController.$inject = [
    '$scope', '$rootScope', '$state', '$uibModalInstance', 'Utils',
    'scimFactory', 'user'
  ];

  function EditUserController(
      $scope, $rootScope, $state, $uibModalInstance, Utils, scimFactory, user) {
    var editUserCtrl = this;

    editUserCtrl.pattern = "^[^<>%$]*$";

    editUserCtrl.oUser = user;
    editUserCtrl.id = user.id;

    editUserCtrl.submit = submit;
    editUserCtrl.reset = reset;
    editUserCtrl.dismiss = dismiss;
    editUserCtrl.isSubmitDisabled = isSubmitDisabled;

    editUserCtrl.reset();

    function submit() {
      editUserCtrl.enabled = false;

      var operations = [];

      // remove picture if it's dirty and empty
      if ($scope.userUpdateForm.picture.$dirty && !editUserCtrl.eUser.picture) {
        operations.push(
            {op: 'remove', value: {photos: editUserCtrl.oUser.photos}});
      }

      if ($scope.userUpdateForm.name.$dirty ||
          $scope.userUpdateForm.surname.$dirty) {
        operations.push({
          op: 'replace',
          value: {
            displayName:
                editUserCtrl.eUser.name + ' ' + editUserCtrl.eUser.surname,
            name: {
              givenName: editUserCtrl.eUser.name,
              familyName: editUserCtrl.eUser.surname,
              middleName: ''
            }
          }
        });
      }
      if ($scope.userUpdateForm.email.$dirty) {
        operations.push({
          op: 'replace',
          value: {
            emails: [{
              type: 'work',
              value: editUserCtrl.eUser.email,
              primary: true
            }]
          }
        });
      }
      if ($scope.userUpdateForm.username.$dirty) {
        operations.push(
            {op: 'replace', value: {userName: editUserCtrl.eUser.username}});
      }
      if ($scope.userUpdateForm.picture.$dirty) {
        operations.push({
          op: 'replace',
          value:
              {photos: [{type: 'photo', value: editUserCtrl.eUser.picture}]}
        });
      }

      console.info('Operations ... ', operations);

      var promise = null;

      if (Utils.isMe(user.id)) {
        promise = scimFactory.updateMe(operations);
      } else {
        promise = scimFactory.updateUser(user.id, operations);
      }

      promise
          .then(function(response) {
            if (Utils.isMe(user.id)) {
              $rootScope.reloadInfo();
            }

            $uibModalInstance.close(response);
            editUserCtrl.enabled = true;

          })
          .catch(function(error) {
            $scope.operationResult = Utils.buildErrorOperationResult(error);
            editUserCtrl.enabled = true;
          });
    }

    function reset() {
      editUserCtrl.eUser = {
        name: editUserCtrl.oUser.name.givenName,
        surname: editUserCtrl.oUser.name.familyName,
        picture:
            editUserCtrl.oUser.photos ? editUserCtrl.oUser.photos[0].value : '',
        email: editUserCtrl.oUser.emails[0].value,
        username: editUserCtrl.oUser.userName
      };
      if ($scope.userUpdateForm) {
        $scope.userUpdateForm.$setPristine();
      }
      editUserCtrl.enabled = true;
    }

    function dismiss() { $uibModalInstance.dismiss('Cancel'); }

    function isSubmitDisabled() {
      return !editUserCtrl.enabled || !$scope.userUpdateForm.$dirty ||
          $scope.userUpdateForm.name.$invalid ||
          $scope.userUpdateForm.surname.$invalid ||
          $scope.userUpdateForm.email.$invalid ||
          $scope.userUpdateForm.username.$invalid ||
          $scope.userUpdateForm.picture.$invalid;
    }
  }
})();