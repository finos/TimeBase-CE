**Known Vulnerabilities:**

Scanned artifact: `org.apache.tomcat.embed:tomcat-embed-core` 8.0.53 (scan of 2026-11-22).

Descriptions below are quoted from the Apache Tomcat security pages
([Tomcat 9](https://tomcat.apache.org/security-9.html) for the 2026 issues, [Tomcat 8](https://tomcat.apache.org/security-8.html) for the older ones),
except where another source is stated. Apache severity is shown next to the scanner severity because they differ.

TimeBase Server embedded Tomcat setup (see `DXTomcat`, `TomcatRunner`, `AuthenticationRealm`):
 - only HTTP/1.1 connectors (plus optional SSL connector) are created; no AJP connector, no HTTP/2 (`UpgradeProtocol`) configured;
 - BASIC authentication only through a custom realm (`AuthenticationRealm` implements `Realm` directly; no FORM, no DIGEST, no `LockOutRealm`);
 - security constraints are created programmatically without HTTP method restrictions, and `web.xml` descriptors define no `<http-method>`;
 - CGI, SSI and WebDAV servlets are disabled (commented out in `config/tomcat/web.xml` and in the webmonitor `WEB-INF/web.xml`);
 - Tomcat WebSocket (server or client) is not used;
 - JMX Remote Lifecycle Listener is not configured;
 - no `PersistentManager` / `FileStore` for sessions.

Summary:

| CVE | Severity (scan) | Severity (Apache) | Affects TimeBase Server |
|---|---|---|---|
| CVE-2020-1938 | Critical | Important | No |
| CVE-2026-43512 | Critical | Moderate | No |
| CVE-2026-43515 | Critical | Moderate | No |
| CVE-2019-0199 | High | Important | No |
| CVE-2019-0232 | High | Important | No |
| CVE-2019-12418 | High | Moderate | No |
| CVE-2019-17563 | High | Low | No |
| CVE-2020-9484 | High | Important | No |
| CVE-2021-25329 | High | Low | No |
| CVE-2026-24880 | High | Low | No (only possible behind a reverse proxy, which is not recommended) |
| CVE-2026-41284 | High | Low | No |
| CVE-2026-42498 | High | Low | No |
| CVE-2026-43513 | High | Low | No |
| CVE-2020-1935 | Medium | Low | No |
| CVE-2026-43514 | Low | Low | No |

Recommended remediation for all of the above: upgrade embedded Tomcat to 9.0.118 or later (the `javax.servlet` namespace is kept in 9.0.x).
Tomcat 8.x is end-of-life, so the 2026 fixes are not available for it, and the Apache advisories for the 2026 issues list only 9.0.x, 10.1.x and 11.0.x.


1. [CVE-2020-1938](https://www.cvedetails.com/cve/CVE-2020-1938)
```
Severity: Critical (scan) / Important (Apache)
Fixed in: 8.5.51, 9.0.31 (Affects 8.5.0 to 8.5.50)

AJP Request Injection and potential Remote Code Execution

When using the Apache JServ Protocol (AJP), care must be taken when trusting incoming connections to Apache Tomcat. 
Tomcat treats AJP connections as having higher trust than, for example, a similar HTTP connection. If such connections 
are available to an attacker, they can be exploited in ways that may be surprising. Prior to Tomcat 8.5.51, Tomcat shipped 
with an AJP Connector enabled by default that listened on all configured IP addresses. It was expected (and recommended 
in the security guide) that this Connector would be disabled if not required.

This vulnerability report identified a mechanism that allowed the following:
 - returning arbitrary files from anywhere in the web application including under the WEB-INF and META-INF directories 
   or any other location reachable via ServletContext.getResourceAsStream()
 - processing any file in the web application as a JSP

It is important to note that mitigation is only required if an AJP port is accessible to untrusted users.
```
TimeBase Server with embedded tomcat setup is not affected because AJP protocol is disabled (no AJP connector is created).

2. [CVE-2026-43512](https://avd.aquasec.com/nvd/cve-2026-43512)
```
Severity: Critical (scan) / Moderate (Apache)
Fixed in: 9.0.118 (Affects 9.0.0.M1 to 9.0.117)

Digest authenticator will authenticate any unknown user

When DIGEST authentication was configured, any user not known to the configured Realm would be authenticated 
if they presented the password "null".
```
TimeBase Server with embedded tomcat setup is not affected: the issue requires DIGEST authentication, 
while TimeBase uses only BASIC authentication (`AuthenticationRealm.getLoginConfig()`); the digest `authenticate` method 
of the TimeBase realm always rejects.

3. [CVE-2026-43515](https://avd.aquasec.com/nvd/cve-2026-43515)
```
Severity: Critical (scan) / Moderate (Apache)
Fixed in: 9.0.118 (Affects 9.0.0.M1 to 9.0.117)

Security constraints not correctly applied

When multiple security constraints defined an HTTP method constraint for the same extension pattern, 
only the first method constraint was applied.
```
TimeBase Server with embedded tomcat setup is not affected:
 - TimeBase does not define HTTP method constraints (no `<http-method>` in `web.xml`, `addMethod` is not used in `AuthenticationRealm.createSecurityConstraint`);
 - TimeBase uses its own realm (`AuthenticationRealm`) with custom security constraint matching (`findSecurityConstraints`) 
   instead of the standard Tomcat `RealmBase` implementation.

4. [CVE-2019-0199](https://www.cvedetails.com/cve/CVE-2019-0199)
```
Severity: High (scan) / Important (Apache)
Fixed in: 8.5.38, 9.0.16 (Affects 8.5.0 to 8.5.37)

Denial of Service

The HTTP/2 implementation accepted streams with excessive numbers of SETTINGS frames and also permitted clients 
to keep streams open without reading/writing request/response data. By keeping streams open for requests that 
utilised the Servlet API's blocking I/O, clients were able to cause server-side threads to block eventually 
leading to thread exhaustion and a DoS.
```
TimeBase Server with embedded tomcat setup is not affected because HTTP/2 is not supported in 8.0.53 version
(and is not configured in TimeBase when running on 8.5.x).

5. [CVE-2019-0232](https://www.cvedetails.com/cve/CVE-2019-0232)
```
Severity: High (scan) / Important (Apache)
Fixed in: 8.5.40, 9.0.17 (Affects 8.5.0 to 8.5.39)

Remote Code Execution on Windows

When running on Windows with enableCmdLineArguments enabled, the CGI Servlet is vulnerable to Remote Code Execution 
due to a bug in the way the JRE passes command line arguments to Windows. The CGI Servlet is disabled by default.
```
TimeBase Server with embedded tomcat setup is not affected, because we have disabled CGI Servlet by default.

6. [CVE-2019-12418](https://www.cvedetails.com/cve/CVE-2019-12418)
```
Severity: High (scan) / Moderate (Apache)
Fixed in: 8.5.49, 9.0.29 (Affects 8.5.0 to 8.5.47)

Local Privilege Escalation

When Tomcat is configured with the JMX Remote Lifecycle Listener, a local attacker without access to the Tomcat 
process or configuration files is able to manipulate the RMI registry to perform a man-in-the-middle attack to 
capture user names and passwords used to access the JMX interface. The attacker can then use these credentials 
to access the JMX interface and gain complete control over the Tomcat instance.
```
TimeBase Server with embedded tomcat setup is not affected, because the JMX Remote Lifecycle Listener is not configured 
and in default configuration JMX endpoints are not exposed.

7. [CVE-2019-17563](https://www.cvedetails.com/cve/CVE-2019-17563)
```
Severity: High (scan) / Low (Apache)
Fixed in: 8.5.50, 9.0.30 (Affects 8.5.0 to 8.5.49)

Session fixation

When using FORM authentication there was a narrow window where an attacker could perform a session fixation attack. 
The window was considered too narrow for an exploit to be practical but, erring on the side of caution, 
this issue has been treated as a security vulnerability.
```
TimeBase Server with embedded tomcat setup is not affected, because only BASIC authentication is used (FORM authentication is not configured).

8. [CVE-2020-9484](https://www.cvedetails.com/cve/CVE-2020-9484)
```
Severity: High (scan) / Important (Apache)
Fixed in: 8.5.55, 9.0.35 (Affects 8.5.0 to 8.5.54)

Remote Code Execution via session persistence

If:
 - an attacker is able to control the contents and name of a file on the server; and
 - the server is configured to use the PersistenceManager with a FileStore; and
 - the PersistenceManager is configured with sessionAttributeValueClassNameFilter="null" (the default unless a 
   SecurityManager is used) or a sufficiently lax filter to allow the attacker provided object to be deserialized; and
 - the attacker knows the relative file path from the storage location used by FileStore to the file the attacker has control over;

then, using a specifically crafted request, the attacker will be able to trigger remote code execution via 
deserialization of the file under their control. Note: All of conditions above must be true for the attack to succeed.
```
TimeBase Server with embedded tomcat setup is not affected, because the issue requires the Tomcat Session persistence 
(PersistenceManager with FileStore), which is not used by TimeBase Server.

9. [CVE-2021-25329](https://www.cvedetails.com/cve/CVE-2021-25329)
```
Severity: High (scan) / Low (Apache)
Fixed in: 8.5.63, 9.0.41 (Affects 8.5.0 to 8.5.61)

Fix for CVE-2020-9484 was incomplete

The fix for CVE-2020-9484 was incomplete. When using a highly unlikely configuration edge case, the Tomcat instance 
was still vulnerable to CVE-2020-9484. Note that both the previously published prerequisites for CVE-2020-9484 and 
the previously published non-upgrade mitigations for CVE-2020-9484 also apply to this issue.
```
TimeBase Server with embedded tomcat setup is not affected, because issue related to the Tomcat Session persistence, which is not used by TimeBase Server.

10. [CVE-2026-24880](https://avd.aquasec.com/nvd/cve-2026-24880)
```
Severity: High (scan) / Low (Apache)
Fixed in: 9.0.116 (Affects 9.0.0.M1 to 9.0.115)

Request smuggling via invalid chunk extension

Tomcat did not validate that contents of HTTP/1.1 chunk extensions. This enabled a request smuggling attack 
if a reverse proxy in front of Tomcat allowed CRLF sequences in an otherwise valid chunk extension.
```
TimeBase Server with embedded tomcat setup is not affected in the supported deployment: the attack requires a reverse proxy 
in front of Tomcat that allows CRLF sequences in a chunk extension, and using a reverse proxy is not recommended for TimeBase.
If a reverse proxy is used, make sure it rejects CRLF in chunk extensions or upgrade Tomcat.

11. [CVE-2026-41284](https://avd.aquasec.com/nvd/cve-2026-41284)
```
Severity: High (scan) / Low (Apache)
Fixed in: 9.0.118 (Affects 9.0.0.M1 to 9.0.117)

Unbounded read in WebDAV LOCK and PROPFIND handling

No limit was enforced on the request body for WebDAV LOCK or PROPFIND requests which were available 
to unauthenticated users.
```
TimeBase Server with embedded tomcat setup is not affected, because the issue requires the WebDAV servlet, which is not used by TimeBase 
(it is not defined in the default `web.xml`, and the sample `webdav` servlet in the webmonitor `WEB-INF/web.xml` is commented out).

12. [CVE-2026-42498](https://avd.aquasec.com/nvd/cve-2026-42498)
```
Severity: High (scan) / Low (Apache)
Fixed in: 9.0.118 (Affects 9.0.2 to 9.0.117)

WebSocket authentication header exposure

If a WebSocket request was redirected after authentication, Tomcat's WebSocket client would present 
the most recent authentication header to the redirect target host.
```
TimeBase Server with embedded tomcat setup is not affected, because the issue is in the Tomcat WebSocket client, 
which is not used by TimeBase Server (no WebSocket code, `tomcat-embed-websocket` is not a dependency).

13. [CVE-2026-43513](https://avd.aquasec.com/nvd/cve-2026-43513)
```
Severity: High (scan) / Low (Apache)
Fixed in: 9.0.118 (Affects 9.0.0.M1 to 9.0.117)

LockOutRealm treats user names as case-sensitive

The LockOut Realm treated user names as case sensitive meaning that, for Realms where the user name was case insensitive, 
the LockOut Realm was not as effective at blocking brute force attacks against a user's password.
```
TimeBase Server with embedded tomcat setup is not affected, because `LockOutRealm` is not used 
(TimeBase uses its own `AuthenticationRealm`).

14. [CVE-2020-1935](https://www.cvedetails.com/cve/CVE-2020-1935)
```
Severity: Medium (scan) / Low (Apache)
Fixed in: 8.5.51, 9.0.31 (Affects 8.5.0 to 8.5.50)

HTTP Request Smuggling

The HTTP header parsing code used an approach to end-of-line (EOL) parsing that allowed some invalid HTTP headers 
to be parsed as valid. This led to a possibility of HTTP Request Smuggling if Tomcat was located behind a reverse 
proxy that incorrectly handled the invalid Transfer-Encoding header in a particular manner. 
Such a reverse proxy is considered unlikely.
```
TimeBase Server with embedded tomcat setup is not affected in usual configurations. Using reverse proxy is not recommended. 

15. [CVE-2026-43514](https://avd.aquasec.com/nvd/cve-2026-43514)
```
Severity: Low (scan) / Low (Apache)
Fixed in: 9.0.118 (Affects 9.0.0.M1 to 9.0.117)

AJP secret compared in non-constant time

The AJP secret was compared in non-constant time allowing an attacker on the local network to mount 
a timing attack to determine the AJP secret.
```
TimeBase Server with embedded tomcat setup is not affected because AJP protocol is disabled.


**Entries not reported by the latest scan** (kept for reference):

16. [CVE-2021-24122](https://www.cvedetails.com/cve/CVE-2021-24122)
```
Severity: Important (Apache)
Fixed in: 8.5.60 (Affects 8.5.0 to 8.5.59)

Information disclosure

When serving resources from a network location using the NTFS file system it was possible to bypass security 
constraints and/or view the source code for JSPs in some configurations. The root cause was the unexpected 
behaviour of the JRE API File.getCanonicalPath() which in turn was caused by the inconsistent behaviour 
of the Windows API (FindFirstFileW) in some circumstances. 
```

TimeBase Server with embedded tomcat setup is not affected:
 - we distribute web resources locally only without source code.
 - it's not recommended to use symlinks for TimeBase setup.  

17. [CVE-2021-25122](https://www.cvedetails.com/cve/CVE-2021-25122)
```
Severity: Important (Apache)
Fixed in: 8.5.63 (Affects 8.5.0 to 8.5.61)

Request mix-up with h2c

When responding to new h2c connection requests, Apache Tomcat could duplicate request headers 
and a limited amount of request body from one request to another meaning user A and user B 
could both see the results of user A's request.
```
TimeBase Server with embedded tomcat setup is not affected because 8.0.53 doesn't support h2c connections
(and h2c is not configured in TimeBase when running on 8.5.x).

18. [CVE-2016-5388](https://www.cvedetails.com/cve/CVE-2016-5388) (description from NVD)
```
Apache Tomcat 7.x through 7.0.70 and 8.x through 8.5.4, when the CGI Servlet is enabled, follows RFC 3875 section
4.1.18 and therefore does not protect applications from the presence of untrusted client data in the HTTP_PROXY 
environment variable, which might allow remote attackers to redirect an application's outbound HTTP traffic to an 
arbitrary proxy server via a crafted Proxy header in an HTTP request, aka an "httpoxy" issue.
```
TimeBase Server with embedded tomcat setup is not affected, because we have disabled CGI Servlet by default.

19. [CVE-2019-0221](https://www.cvedetails.com/cve/CVE-2019-0221)
```
Severity: Low (Apache)
Fixed in: 8.5.40 (Affects 8.5.0 to 8.5.39)

XSS in SSI printenv

The SSI printenv command echoes user provided data without escaping and is, therefore, vulnerable to XSS. 
SSI is disabled by default. The printenv command is intended for debugging and is unlikely to be present in a production website.
```
TimeBase Server with embedded tomcat setup is not affected, because SSI is disabled.
