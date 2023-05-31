F**Known Vulnerabilities:**


1. [CVE-2021-24122](https://www.cvedetails.com/cve/CVE-2021-24122)
```
Severity: Low
When serving resources from a network location using the NTFS file system it was possible to bypass security 
constraints and/or view the source code for JSPs in some configurations. The root cause was the unexpected 
behavior of the JRE API File.getCanonicalPath() which in turn was caused by the inconsistent behavior 
of the Windows API (FindFirstFileW) in some circumstances. 
```

TimeBase Server with embedded tomcat setup is not affected:
 - we distribute web resources locally only without source code.
 - it's not recommended to use symlinks for TimeBase setup.  

2. [CVE-2021-25329](https://www.cvedetails.com/cve/CVE-2021-25329)

```
Severity: Low
Very specific conditions to be reproducible.
```
TimeBase Server with embedded tomcat setup is not affected, because issue related to the Tomcat Session persistence, which is not used by TimeBase Server.


3. [CVE-2021-25122](https://www.cvedetails.com/cve/CVE-2021-25122)
```
Important: Request mix-up with h2c CVE-2021-25122

When responding to new h2c connection requests, Apache Tomcat could duplicate request headers 
and a limited amount of request body from one request to another meaning user A and user B 
could both see the results of user A's request.
```
TimeBase Server with embedded tomcat setup is not affected because 8.0.53 doesn't support h2c connections.

4. [CVE-2016-5388](https://www.cvedetails.com/cve/CVE-2016-5388)
```

Apache Tomcat 7.x through 7.0.70 and 8.x through 8.5.4, when the CGI Servlet is enabled, follows RFC 3875 section
4.1.18 and therefore does not protect applications from the presence of untrusted client data in the HTTP_PROXY 
environment variable, which might allow remote attackers to redirect an application's outbound HTTP traffic to an 
arbitrary proxy server via a crafted Proxy header in an HTTP request, aka an "httpoxy" issue.
```
TimeBase Server with embedded tomcat setup is not affected, because we have disabled CGI Servlet by default.


5. [CVE-2020-1935](https://www.cvedetails.com/cve/CVE-2020-1935)
```
Low: HTTP Request Smuggling CVE-2020-1935

The HTTP header parsing code used an approach to end-of-line (EOL) parsing that allowed some invalid HTTP headers
to be parsed as valid. This led to a possibility of HTTP Request Smuggling if Tomcat was located behind a reverse 
proxy that incorrectly handled the invalid Transfer-Encoding header in a particular manner. 
Such a reverse proxy is considered unlikely.

```
TimeBase Server with embedded tomcat setup is not affected in usual configurations. Using reverse proxy is not recommended. 


6. [CVE-2020-1938](https://www.cvedetails.com/cve/CVE-2020-1938)
```
When using the Apache JServ Protocol (AJP), care must be taken when trusting incoming connections to Apache Tomcat. 
Tomcat treats AJP connections as having higher trust than, for example, a similar HTTP connection. If such connections 
  are available to an attacker, they can be exploited in ways that may be surprising.
```
TimeBase Server with embedded tomcat setup is not affected because AJP protocol is disabled.

7. [CVE-2019-0199](https://www.cvedetails.com/cve/CVE-2019-0199)
```
The HTTP/2 implementation in Apache Tomcat 9.0.0.M1 to 9.0.14 and 8.5.0 to 8.5.37 accepted streams with excessive 
numbers of SETTINGS frames and also permitted clients to keep streams open without reading/writing request/response data. 
By keeping streams open for requests that utilised the Servlet API's blocking I/O, 
clients were able to cause server-side threads to block eventually leading to thread exhaustion and a DoS. 
```
TimeBase Server with embedded tomcat setup is not affected because HTTP/2 is not supported in 8.0.53 version

8. [CVE-2019-0232](https://www.cvedetails.com/cve/CVE-2019-0232)
```
When running on Windows with enableCmdLineArguments enabled, the CGI Servlet in 
Apache Tomcat 9.0.0.M1 to 9.0.17, 8.5.0 to 8.5.39 and 7.0.0 to 7.0.93 is vulnerable to Remote Code Execution 
due to a bug in the way the JRE passes command line arguments to Windows. 
```
TimeBase Server with embedded tomcat setup is not affected, because we have disabled CGI Servlet by default.

9. [CVE-2019-12418](https://www.cvedetails.com/cve/CVE-2019-12418)
```
When Tomcat is configured with the JMX Remote Lifecycle Listener, a local attacker without 
 access to the Tomcat process or configuration files is able to manipulate the RMI registry to perform 
 a man-in-the-middle attack to capture user names and passwords used to access the JMX interface. 
 The attacker can then use these credentials to access the JMX interface and gain complete control over the Tomcat instance.
```
TimeBase Server with embedded tomcat setup is not affected, because in default configuration JMX endpoints is not exposed

10. [CVE-2019-17563](https://www.cvedetails.com/cve/CVE-2019-17563)
```
When using FORM authentication there was a narrow window where an attacker could perform a session fixation attack.
The window was considered too narrow for an exploit to be practical but, erring on the side of caution, 
this issue has been treated as a security vulnerability.
```
Fix going to be integrated into Timebase codebase.

11. [CVE-2019-0221](https://www.cvedetails.com/cve/CVE-2019-0221)
```
The SSI printenv command echoes user provided data without escaping and is, therefore, vulnerable to XSS. 
SSI is disabled by default. The printenv command is intended for debugging and is unlikely to be present in a production website.
```
TimeBase Server with embedded tomcat setup is not affected, because SSI is disabled.