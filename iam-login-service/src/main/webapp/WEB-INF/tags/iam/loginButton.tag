
<%@attribute name="href" required="true"%>
<%@attribute name="id" required="true"%>
<%@attribute name="cssClass" required="true"%>
<%@attribute name="btn" required="true" type="it.infn.mw.iam.config.login.LoginButtonProperties"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<div id="${id}" class="${cssClass}">
  <a class="btn btn-block ${btn.style}" href="${href}" title="${btn.title}">
    <c:if test="${not empty btn.text}">
      <c:choose>
        <c:when test="${not empty btn.image.url}">
          <span style="vertical-align: middle">${btn.text}</span>
        </c:when>
        <c:otherwise>
          ${btn.text}
        </c:otherwise>
      </c:choose>
    </c:if>
    <c:if test="${not empty btn.image}">
      <c:choose>
        <c:when test="${not empty btn.image.faIcon }">
          <i class="fa fa-${btn.image.faIcon}"></i>
        </c:when>
        <c:when test="${not empty btn.image.url }">
          <img src="${btn.image.url}" class="login-image-size-${btn.image.size}" />
        </c:when>
      </c:choose>
    </c:if>
  </a>
</div>
