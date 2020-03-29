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

var app = angular.module('registrationApp', ['ui.bootstrap', 'ngCookies']);

// app.config(function simulateNetworkLatency($httpProvider) {
//     $httpProvider.interceptors.push(httpDelay);

//     function httpDelay($timeout, $q) {
//         var delayInMilliseconds = 2850;

//         // Return our interceptor configuration.
//         return ({
//             response: response,
//             responseError: responseError
//         });

//         function response(response) {
//             var deferred = $q.defer();
//             $timeout(
//                 function () {
//                     deferred.resolve(response);
//                 },
//                 delayInMilliseconds,
//                 false
//             );
//             return (deferred.promise);
//         }

//         function responseError(response) {
//             var deferred = $q.defer();
//             $timeout(
//                 function () {
//                     deferred.reject(response);
//                 },
//                 delayInMilliseconds,
//                 false
//             );
//             return (deferred.promise);
//         }
//     }
// });