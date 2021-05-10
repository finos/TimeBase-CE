<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%@page import="java.util.Collections" %>
<%@ page import="java.util.Comparator" %>
<%@ page import="java.util.*" %>
<%@ page import="java.util.logging.Logger" %>
<%@ page import="com.epam.deltix.gflog.api.*" %>
<%@ page import="com.epam.deltix.qsrv.util.log.ServerLoggingConfigurer" %>

<%
    List<Log> loggers = new ArrayList<>(LogFactory.getLogs());
            Collections.sort(
                    loggers,
                    new Comparator<Log>() {
                        public int compare(Log a, Log b) {
                            return a.getName().compareTo(b.getName());
                        }
                    }
            );

            for (Log logger : loggers) {
                String loggerName = logger.getName();
                String loggerLevelParameterKey = loggerName + ".level";
                String loggerLevelParameterValue = request.getParameter(loggerLevelParameterKey);
                if (loggerLevelParameterValue != null) {
                    LogLevel level = LogLevel.valueOf(loggerLevelParameterValue);
                    if (logger.getLevel() != level) {
                        logger.setLevel(level);
                        Logger.getLogger(loggerName).setLevel(ServerLoggingConfigurer.getJULLevel(level));
                    }
                }
            }

    pageContext.setAttribute("loggers", loggers);
    pageContext.setAttribute("levels", LogLevel.values());
%>

<html>
<head>
    <title>Logging Control Panel</title>
    <link rel='stylesheet' type='text/css' href='../style.css'/>
</head>
<body>
<h1>Logging Control Panel</h1>

<form method="post">
    <table border="1">
        <tr>
            <th>Name</th>
            <th>Level</th>
        </tr>
        <c:forEach items="${loggers}" var="logger">
            <tr>
                <td>
                    <c:choose>
                        <c:when test="${empty logger.name}">
                            ROOT
                        </c:when>
                        <c:when test="${fn:startsWith(logger.name, 'deltix')}">
                            <b>
                                <span style="font-size:larger">
                                        ${logger.name}
                                </span>
                            </b>
                        </c:when>
                        <c:otherwise>
                            ${logger.name}
                        </c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <select name="${logger.name}.level">
                        <c:forEach items="${levels}" var="level">
                            <option ${logger.level eq level ? "selected" :""}/>
                            ${level}</option> <%--TODO: remove parameter if loggerLevel == newLevel --%>
                        </c:forEach>
                    </select>
                </td>
            </tr>
        </c:forEach>
    </table>
    <input type="submit" value="Apply Changes"/>
</form>
</body>
</html>
