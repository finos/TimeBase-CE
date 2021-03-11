<%@tag description="Overall Page template" pageEncoding="UTF-8"%>

<html>
    <%@include file="/WEB-INF/jsp/common/header.jsp"%>
    <body>
        <%@include file="/WEB-INF/jsp/common/menu.jsp"%>

        <div class="bodybox">
            <div class="container">
                <jsp:doBody/>
            </div>
        </div>

        <div class="footer navbar-fixed-bottom" id="footer">
            <%@include file="/WEB-INF/jsp/common/footer.jsp"%>
        </div>
    </body>
</html>