<%@ page import="java.lang.*"%>
<%@ page import="java.lang.management.RuntimeMXBean"%>
<%@ page import="java.lang.management.ManagementFactory"%>
<%@ page import="java.util.*"%>

<%!
    private static Map<String, String> getSortedProperties() {
        Properties properties = System.getProperties();
        TreeMap<String, String> result = new TreeMap<String, String>();
        for (String key : properties.stringPropertyNames()) {
            if (key.toLowerCase().contains("pass"))
                continue;
            result.put(key, properties.getProperty(key));
        }
        return result;
    }
%>

<%
Runtime rt = Runtime.getRuntime ();
rt.gc ();
long    maxMem = rt.maxMemory ();
long    freeMem = rt.freeMemory ();
long    currentMem = rt.totalMemory ();
long    usedMem = currentMem - freeMem;
long    availMem = maxMem - usedMem;
Date    now = new Date ();
%>

<html>
<head>
    <title>System Status at <%=now%></title>
    <link rel='stylesheet' type='text/css' href='../style.css'>	
</head>
<body>

<h1>System Status at <%=now%></h1>

<h2>JVM Status</h2>

<table border="0" cellpadding="4" cellspacing="1">
    <tr><th align=left># CPUs:</th> <td align=right><%=rt.availableProcessors ()%></td></tr>
    <tr><th align=left>Max Memory (MB):</th> <td align=right><%=maxMem >> 20%></td></tr>
    <tr><th align=left>Used Memory (MB):</th> <td align=right><%=usedMem >> 20%></td></tr>
    <tr><th align=left>Current Memory (MB):</th> <td align=right><%=currentMem >> 20%></td></tr>
    <tr><th align=left>Available Memory (MB):</th> <td align=right><%=availMem >> 20%></td></tr>
    <tr><th align="left">JVM Arguments:</th><td>
    <%
        List<String> arguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String key : arguments) {
    %>
    <%= key %> <BR/>
    <%
        }
    %>
    </td></tr>
</table>

<h2>System Properties</h2>

<table cellpadding="4" class="compact">
    <%
        int count = 0;
        Map<String, String> properties = getSortedProperties();
        for (String key : properties.keySet()) {
    %>
    <tr><th align="left"><%= key %></th> <td class="<%= count++ % 2 == 0 ? "even" : "odd" %>"><%= properties.get(key) %></td></tr>
    <%
        }
    %>
</table>

</body>
</html>
