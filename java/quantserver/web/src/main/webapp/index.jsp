<html>
<head>
    <title>Deltix QuantServer - Version <%=com.epam.deltix.util.Version.VERSION_STRING%>
    </title>
    <link rel='stylesheet' type='text/css' href='style.css' />
</head>

<body bgcolor="#E0E0E0">
<h1>Deltix QuantServer - Version <%=com.epam.deltix.util.Version.VERSION_STRING%>
</h1>

<h2>Debugging Tools</h2>
<ul>
    <li><a class="action" href="tools/threads.jsp">Thread Management</a>

        <div class="help">
            Display active JVM threads. Allows to interrupt threads.
        </div>

    <li><a class="action" href="tools/sysprops.jsp">System Status</a>

        <div class="help">
            Display various JVM properties, including System Properties.
        </div>
</ul>
<h2>Server Control</h2>

<ul>
    <li><a class="action" href="logging/">Log Levels</a>

        <div class="help">Change logging levels.</div>

    <li><a class="action" href="getlogs">Log Download</a>

        <div class="help">Download all server log files in a zip format.</div>
        
    <li><a class="action" href="heapdump">Heap Dump Download</a>

        <div class="help">Download heap dump in a zip format.</div>

    <li><a class="action" href="QuantServer.mib">MIB definition</a>

        <div class="help">View the MIB definition file, used by SNMP clients.</div>


</ul>

</body>
</html>
