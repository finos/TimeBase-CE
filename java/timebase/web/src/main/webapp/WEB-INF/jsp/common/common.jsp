
<%-- allerts dialog --%>
<c:if test="${not empty alert_type}">
    <div class="alert alert-${alert_type} alert-dismissible" role="alert">
        <button type="button" class="close" data-dismiss="alert" aria-label="Close">
            <span aria-hidden="true">x</span>
        </button>
        <strong>${alert_msg}</strong>
    </div>
</c:if>

<%-- images --%>
<c:url var="returnImgUrl" value="/resources/img/return.png" />