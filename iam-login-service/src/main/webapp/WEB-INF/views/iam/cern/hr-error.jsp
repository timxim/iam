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
<%@ taglib prefix="t" tagdir="/WEB-INF/tags/iam"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<t:page title="HR DB API error">
  <h2 class="text-center text-danger">${hrError.message}</h2>
  <div class="text-center" id="exception-message">
    <p>${hrError.message}</p>
  </div>
  <div id="register-confirm-back-btn" class="row text-center">
    <a class="btn btn-primary" href='/reset-session'>Back to Login Page</a>
  </div>
</t:page>