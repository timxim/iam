<%--

    Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2016-2019

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

--%>
<%@ taglib prefix="authz" uri="http://www.springframework.org/security/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags/iam"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<t:page title="Register">
  <h2 class="text-center">No HR record found</h2>
  <p class="text-center"> 
    No valid ${experiment} membership record found for user ${user.CERN_FIRST_NAME} ${user.CERN_LAST_NAME} 
    (PersonID: ${user.CERN_PERSON_ID}).
  </p>
  <div id="register-confirm-back-btn" class="row text-center">
    <a class="btn btn-primary" href='/reset-session'>Back to Login Page</a>
  </div>
</t:page>