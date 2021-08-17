**Known Vulnerabilities:**


1. [CVE-2021-24122](https://www.cvedetails.com/cve/CVE-2021-24122)
```
Important: Information disclosure CVE-2021-24122

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
RCE via session persistence
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

TimeBase Server with embedded tomcat setup is not affected in usual configurations
