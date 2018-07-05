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
'use strict'

angular.module('dashboardApp').factory('TokensService', TokensService);

TokensService.$inject = ['$q', '$rootScope', '$http', '$httpParamSerializer'];

function TokensService($q, $rootScope, $http, $httpParamSerializer) {
    var service = {
        getAccessTokens: getAccessTokens,
        getAccessToken: getAccessToken,
        getAccessTokensFilteredByUser: getAccessTokensFilteredByUser,
        getAccessTokensFilteredByClient: getAccessTokensFilteredByClient,
        getAccessTokensFilteredByUserAndClient: getAccessTokensFilteredByUserAndClient,
        revokeAccessToken: revokeAccessToken,
        getRefreshToken: getRefreshToken,
        getRefreshTokensFilteredByUser: getRefreshTokensFilteredByUser,
        getRefreshTokensFilteredByClient: getRefreshTokensFilteredByClient,
        getRefreshTokensFilteredByUserAndClient: getRefreshTokensFilteredByUserAndClient,
        getRefreshTokens: getRefreshTokens,
        revokeRefreshToken: revokeRefreshToken,
        getAccessTokensCount: getAccessTokensCount,
        getRefreshTokensCount: getRefreshTokensCount
    };

    var urlAccessTokens = "/iam/api/access-tokens";
    var urlRefreshTokens = "/iam/api/refresh-tokens";

    return service;

    function doGetAccessTokens(queryStr) {
        var url = urlAccessTokens + '?' + queryStr;
        return $http.get(url);
    }

    function doGetRefreshTokens(queryStr) {
        var url = urlRefreshTokens + '?' + queryStr;
        return $http.get(url);
    }

    function getAccessTokensFilteredByUser(startIndex, count, userId, sortBy, sortDirection) {
        console.debug("Getting access-tokens offset-limit: ", startIndex, count);
        console.debug("Filtered user: ", userId);
        console.debug("Sort: ", sortBy, sortDirection);
        return doGetAccessTokens($httpParamSerializer({
            'startIndex': startIndex,
            'count': count,
            'userId': userId,
            'sortBy': sortBy,
            'sortDirection': sortDirection
        }));
    }

    function getRefreshTokensFilteredByUser(startIndex, count, userId, sortBy, sortDirection) {
        console.debug("Getting refresh-tokens offset-limit: ", startIndex, count);
        console.debug("Filtered user: ", userId);
        console.debug("Sort: ", sortBy, sortDirection);
        return doGetRefreshTokens($httpParamSerializer({
            'startIndex': startIndex,
            'count': count,
            'userId': userId,
            'sortBy': sortBy,
            'sortDirection': sortDirection
        }));
    }

    function getAccessTokensFilteredByClient(startIndex, count, clientId, sortBy, sortDirection) {
        console.debug("Getting access-tokens offset-limit: ", startIndex, count);
        console.debug("Filtered client: ", clientId);
        console.debug("Sort: ", sortBy, sortDirection);
        return doGetAccessTokens($httpParamSerializer({
            'startIndex': startIndex,
            'count': count,
            'clientId': clientId,
            'sortBy': sortBy,
            'sortDirection': sortDirection
        }));
    }

    function getRefreshTokensFilteredByClient(startIndex, count, clientId, sortBy, sortDirection) {
        console.debug("Getting refresh-tokens offset-limit: ", startIndex, count);
        console.debug("Filtered client: ", clientId);
        console.debug("Sort: ", sortBy, sortDirection);
        return doGetRefreshTokens($httpParamSerializer({
            'startIndex': startIndex,
            'count': count,
            'clientId': clientId,
            'sortBy': sortBy,
            'sortDirection': sortDirection
        }));
    }

    function getAccessTokensFilteredByUserAndClient(startIndex, count, userId, clientId, sortBy, sortDirection) {
        console.debug("Getting access-tokens offset-limit: ", startIndex, count);
        console.debug("Filtered user and client: ", userId, clientId);
        console.debug("Sort: ", sortBy, sortDirection);
        return doGetAccessTokens($httpParamSerializer({
            'startIndex': startIndex,
            'count': count,
            'clientId': clientId,
            'userId': userId,
            'sortBy': sortBy,
            'sortDirection': sortDirection
        }));
    }

    function getRefreshTokensFilteredByUserAndClient(startIndex, count, userId, clientId, sortBy, sortDirection) {
        console.debug("Getting refresh-tokens offset-limit: ", startIndex, count);
        console.debug("Filtered user and client: ", userId, clientId);
        console.debug("Sort: ", sortBy, sortDirection);
        return doGetRefreshTokens($httpParamSerializer({
            'startIndex': startIndex,
            'count': count,
            'clientId': clientId,
            'userId': userName,
            'sortBy': sortBy,
            'sortDirection': sortDirection
        }));
    }

    function getAccessTokens(startIndex, count, sortBy, sortDirection) {
        console.debug("Getting access-tokens offset-limit: ", startIndex, count);
        console.debug("Sort: ", sortBy, sortDirection);
        return doGetAccessTokens($httpParamSerializer({
            'startIndex': startIndex,
            'count': count
        }));
    }

    function getRefreshTokens(startIndex, count, sortBy, sortDirection) {
        console.debug("Getting refresh-tokens offset-limit: ", startIndex, count);
        console.debug("Sort: ", sortBy, sortDirection);
        return doGetRefreshTokens($httpParamSerializer({
            'startIndex': startIndex,
            'count': count,
            'sortBy': sortBy,
            'sortDirection': sortDirection
        }));
    }

    function getAccessToken(id) {
        console.debug("Getting access-token with id: ", id);
        var url = urlAccessTokens + '/' + id;
        return $http.get(url);
    }

    function revokeAccessToken(id) {
        console.debug("Revoking access-token with id: ", id);
        var url = urlAccessTokens + '/' + id;
        return $http.delete(url);
    }

    function getRefreshToken(id) {
        console.debug("Getting refresh-token with id: ", id);
        var url = urlRefreshTokens + '/' + id;
        return $http.get(url);
    }

    function revokeRefreshToken(id) {
        console.debug("Revoking refresh-token with id: ", id);
        var url = urlRefreshTokens + '/' + id;
        return $http.delete(url);
    }

    function getAccessTokensCount() {
        return doGetAccessTokens($httpParamSerializer({
            'count': 0
        }));
    }

    function getRefreshTokensCount() {
        return doGetRefreshTokens($httpParamSerializer({
            'count': 0
        }));
    }

}