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
      .controller('EditPasswordController', EditPasswordController);

  EditPasswordController.$inject = [
    '$scope', '$state', '$uibModalInstance', 'Utils', 'ResetPasswordService',
    'user'
  ];

  function EditPasswordController(
      $scope, $state, $uibModalInstance, Utils, ResetPasswordService, user) {
    var editPasswordCtrl = this;

    editPasswordCtrl.passwordMinlength = 6;
    editPasswordCtrl.userToEdit = user;

    editPasswordCtrl.dismiss = dismiss;
    editPasswordCtrl.reset = reset;

    function reset() {
      console.log('reset form');

      editPasswordCtrl.enabled = true;

      editPasswordCtrl.user = {
        currentPassword: '',
        password: '',
        confirmPassword: ''
      };

      if ($scope.editPasswordForm) {
        $scope.editPasswordForm.$setPristine();
      }
    }

    editPasswordCtrl.reset();

    function dismiss() { return $uibModalInstance.dismiss('Cancel'); }

    editPasswordCtrl.message = '';

    editPasswordCtrl.submit = function() {
      ResetPasswordService
          .updatePassword(
              editPasswordCtrl.user.currentPassword,
              editPasswordCtrl.user.password)
          .then(function() { return $uibModalInstance.close('Password updated'); })
          .catch(function(error) {
            console.error(error);
            $scope.operationResult = Utils.buildErrorResult(error.data);
          });
    };
  }

  var compareTo = function() {
    return {
      require: 'ngModel',
      scope: {otherModelValue: '=compareTo'},
      link: function(scope, element, attributes, ngModel) {

        ngModel.$validators.compareTo = function(modelValue) {
          return modelValue == scope.otherModelValue;
        };

        scope.$watch('otherModelValue', function() { ngModel.$validate(); });
      }
    };
  };

  angular.module('dashboardApp').directive('compareTo', compareTo);
})();