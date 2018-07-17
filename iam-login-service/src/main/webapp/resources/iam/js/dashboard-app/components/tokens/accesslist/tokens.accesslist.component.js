/*
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2016-2018
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
(function () {
  'use strict';

  function RevokeAccessTokenController($rootScope, $uibModalInstance, TokensService, token) {
    var self = this;

    self.token = token;
    self.enabled = true;
    self.error = undefined;

    self.doRevoke = function (token) {
      self.error = undefined;
      self.enabled = false;
      TokensService.revokeAccessToken(token.id).then(function (response) {
        $uibModalInstance.close(token);
        $rootScope.accessTokensCount--;
        self.enabled = true;
      }).catch(function (error) {
        console.error(error);
        self.error = error;
        self.enabled = true;
      });
    };

    self.cancel = function () {
      $uibModalInstance.dismiss('Dismissed');
    };
  }

  function AccessTokensListController($q, $scope, $rootScope, $uibModal, ModalService,
    TokensService, scimFactory, Utils, toaster) {

    var self = this;

    // pagination controls
    self.currentPage = 1;
    self.currentOffset = 1;
    self.itemsPerPage = 10;
    self.totalResults = self.total;
    self.sortByValue = "expiration";
    self.sortDirection = "desc";

    self.$onInit = function () {
      console.debug("init AccessTokensListController", self.tokens, self.currentPage, self.currentOffset, self.totalResults);
    };

    $scope.$on('refreshAccessTokensList', function (e) {
      console.debug("received refreshAccessTokensList event");
      self.searchTokens(1);
    });

    self.updateAccessTokenCount = function (responseValue) {
      if (self.clientSelected || self.userSelected) {
        if (responseValue > $rootScope.accessTokensCount) {
          $rootScope.accessTokensCount = responseValue;
        }
      } else {
        $rootScope.accessTokensCount = responseValue;
      }
    };

    self.searchTokens = function (page) {

      console.debug("page = ", page);
      $rootScope.pageLoadingProgress = 0;
      self.loaded = false;

      self.tokens = [];
      self.currentPage = page;
      self.currentOffset = (page - 1) * self.itemsPerPage + 1;

      var handleResponse = function (response) {
        self.totalResults = response.data.totalResults;
        angular.forEach(response.data.Resources, function (token) {
          self.tokens.push(token);
        });
        $rootScope.pageLoadingProgress = 100;
        self.updateAccessTokenCount(response.data.totalResults);
        self.loaded = true;
        self.loadingModal.dismiss("Cancel");
      };

      var handleError = function (error) {
        self.loadingModal.dismiss("Error");
        toaster.pop({ type: 'error', body: error });
      };

      self.loadingModal = $uibModal.open({
        animation: false,
        templateUrl: '/resources/iam/template/dashboard/loading-modal.html'
      });

      self.loadingModal.opened.then(function () {
        self.getAccessTokenList(self.currentOffset, self.itemsPerPage).then(handleResponse, handleError);
      });
    }

    self.getAccessTokenList = function (startIndex, count) {
      if (self.clientSelected && self.userSelected) {
        return TokensService.getAccessTokensFilteredByUserAndClient(startIndex, count, self.userSelected.userName, self.clientSelected.clientId, self.sortByValue, self.sortDirection);
      }
      if (self.clientSelected) {
        return TokensService.getAccessTokensFilteredByClient(startIndex, count, self.clientSelected.clientId, self.sortByValue, self.sortDirection);
      }
      if (self.userSelected) {
        return TokensService.getAccessTokensFilteredByUser(startIndex, count, self.userSelected.userName, self.sortByValue, self.sortDirection);
      }
      return TokensService.getAccessTokens(startIndex, count, self.sortByValue, self.sortDirection);
    }

    self.sortBy = function (sortByValue, sortDirection) {
      self.sortByValue = sortByValue;
      self.sortDirection = sortDirection;
      self.searchTokens(self.currentPage);
    }

    self.handleRevokeSuccess = function (token) {
      self.enabled = true;
      toaster.pop({
        type: 'success',
        body: 'Token Revoked'
      });
      self.totalResults--;
      if (self.currentOffset > self.totalResults) {
        if (self.currentPage > 1) {
          self.currentPage--;
        }
      }
      self.searchTokens(self.currentPage);
    };

    self.openRevokeAccessTokenDialog = function (token) {

      var modalInstance = $uibModal.open({
        templateUrl: '/resources/iam/js/dashboard-app/components/tokens/accesslist/token.revoke.dialog.html',
        controller: RevokeAccessTokenController,
        controllerAs: '$ctrl',
        resolve: {
          token: token
        }
      });

      modalInstance.result.then(self.handleRevokeSuccess);
    };
  }

  angular
    .module('dashboardApp')
    .component(
      'tokensAccesslist',
      {
        require: {
          $parent: '^tokens'
        },
        bindings: {
          clients: '=',
          users: '=',
          tokens: '<',
          total: '<'
        },
        templateUrl: '/resources/iam/js/dashboard-app/components/tokens/accesslist/tokens.accesslist.component.html',
        controller: ['$q', '$scope', '$rootScope', '$uibModal', 'ModalService',
          'TokensService', 'scimFactory', 'Utils', 'toaster', AccessTokensListController]
      });
})();