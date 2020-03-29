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

    function UserGroupsController(toaster, $uibModal, ModalService, scimFactory) {
        var self = this;

        self.$onInit = function() {
            console.log('UserGroupsController onInit');
            self.enabled = true;
        };

        self.isVoAdmin = function() { return self.userCtrl.isVoAdmin(); };

        self.handleSuccess = function(msg) {
            self.enabled = true;
            self.userCtrl.loadUser().then(function() {
                toaster.pop({
                    type: 'success',
                    body: msg
                });
            });
        };

        self.openAddGroupDialog = function() {
            var modalInstance = $uibModal.open({
                templateUrl: '/resources/iam/apps/dashboard-app/templates/user/addusergroup.html',
                controller: 'AddUserGroupController',
                controllerAs: 'addGroupCtrl',
                resolve: { user: function() { return self.user; } }
            });

            modalInstance.result.then(self.handleSuccess);

        };

        self.openRemoveGroupDialog = function(group) {
            var modalOptions = {
                closeButtonText: 'Cancel',
                actionButtonText: 'Remove user from group',
                headerText: `Remove  '${self.user.name.formatted}' from group '${group.display}'`,
                bodyText: `Are you sure you want to remove '${self.user.name.formatted}' from group '${group.display}'?`
            };

            ModalService.showModal({}, modalOptions).then(function() {
                scimFactory
                    .removeUserFromGroup(
                        group.value, self.user.id, self.user.meta.location,
                        self.user.name.formatted)
                    .then(function() {
                        self.handleSuccess(`User removed from group '${group.display}'`);
                    })
                    .catch(self.userCtrl.handleError);
            });

        };
    }

    angular.module('dashboardApp').component('userGroups', {
        require: { userCtrl: '^user' },
        bindings: { user: '=' },
        templateUrl: '/resources/iam/apps/dashboard-app/components/user/groups/user.groups.component.html',
        controller: [
            'toaster', '$uibModal', 'ModalService', 'scimFactory',
            UserGroupsController
        ]
    });
})();