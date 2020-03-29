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

    function GroupMembersController($rootScope, $filter, scimFactory, $uibModal, ModalService, toaster) {

        var self = this;
        self.members = [];

        self.deleteMember = deleteMember;

        initMembersDisplay(self.group);

        self.$onInit = function() {
            console.log('GroupMembersController onInit');
        };

        function initMembersDisplay(group) {
            if (group.members) {
                group.members = $filter('orderBy')(group.members, "display", false);
                self.members = group.members.filter(memberIsAnAccount);
            } else {
                self.members = [];
            }
        }

        function memberIsAnAccount(member) {
            return (member.$ref.indexOf('scim/Users') != -1);
        }

        function deleteMember(member) {

            var modalOptions = {
                closeButtonText: 'Cancel',
                actionButtonText: 'Remove membership',
                headerText: 'Remove «' + member.display + "» from «" + self.group.displayName + "»",
                bodyText: `Are you sure you want to remove '${member.display}' from '${self.group.displayName}'?`
            };

            ModalService.showModal({}, modalOptions).then(
                function() {
                    scimFactory.removeMemberFromGroup(self.group.id, member.value, member.$ref,
                            member.display)
                        .then(function(response) {
                            self.loadGroup().then(function(group) {
                                initMembersDisplay(group);
                                toaster.pop({
                                    type: 'success',
                                    body: `Member ${member.display} has been removed successfully`
                                });
                            });
                        }, function(error) {
                            toaster.pop({
                                type: 'error',
                                body: `${error.data.detail}`
                            });
                        });
                });
        }
    }

    angular.module('dashboardApp').component('groupMembers', {
        templateUrl: '/resources/iam/apps/dashboard-app/components/group/members/group.members.component.html',
        bindings: { group: '<', loadGroup: '&' },
        controller: [
            '$rootScope', '$filter', 'scimFactory', '$uibModal', 'ModalService', 'toaster', GroupMembersController
        ]
    });
})();