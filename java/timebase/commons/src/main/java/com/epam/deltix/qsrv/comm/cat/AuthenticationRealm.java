/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.qsrv.comm.cat;

import com.epam.deltix.util.security.AuthenticationController;
import org.apache.catalina.*;
import org.apache.catalina.realm.Constants;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.ietf.jgss.GSSContext;

import javax.servlet.http.HttpServletRequest;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.security.AccessControlException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Authentication realm for Tomcat based on AuthenticationController.
 * Enables basic authentication for all resources.
 */
public class AuthenticationRealm implements Realm {
    protected static final Logger LOGGER = Logger.getLogger ("deltix.util.tomcat");

    private AuthenticationController controller;
    private Container container;
    private CredentialHandler credentialHandler;

    public AuthenticationRealm(AuthenticationController controller) {
        this.controller = controller;
    }

    @Override
    public Container getContainer() {
        return container;
    }

    @Override
    public void setContainer(Container container) {
        this.container = container;
    }

    @Override
    public CredentialHandler getCredentialHandler() {
        return credentialHandler;
    }

    @Override
    public void setCredentialHandler(CredentialHandler credentialHandler) {
        this.credentialHandler = credentialHandler;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    //todo: review this with Andy
    @Override
    public Principal authenticate(String username) {
        return controller.getUser(username);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Principal authenticate(String username, String credentials) {
        try {
            // null password is not allowed
            if (credentials == null)
                return null;

            return controller.authenticate(username, credentials);
        } catch (AccessControlException e) {
            LOGGER.log(Level.WARNING, "Cannot authenticate user \"" + username +"\"");
            return null;
        }
    }

    @Override
    public Principal authenticate(String s, String s1, String s2, String s3, String s4, String s5, String s6, String s7) {
        return null;
    }

    @Override
    public Principal authenticate(GSSContext gssContext, boolean storeCreds) {
        return null;
    }

    @Override
    public Principal authenticate(X509Certificate[] x509Certificates) {
        try {
            throw new UnsupportedOperationException();
        } catch (AccessControlException e) {
            LOGGER.log(Level.WARNING, "Authenticate error.", e);
            return null;
        }
    }

    @Override
    public void backgroundProcess() {
        // do nothing
    }

    @Override
    public SecurityConstraint[] findSecurityConstraints(Request request, Context context)
    {
        ArrayList<SecurityConstraint> results = new ArrayList<SecurityConstraint>();

        SecurityConstraint[] constraints = context.findConstraints();
        if ((constraints == null) || (constraints.length == 0)) {
//            if (log.isDebugEnabled())
//                log.debug("  No applicable constraints defined");
            return null;
        }

        String uri = request.getRequestPathMB().toString();

        String method = request.getMethod();

        boolean found = false;
        // start from the end, because rule is to define more common constrains at the top
        for (int i = constraints.length - 1; i >= 0; i--) {
            SecurityCollection[] collection = constraints[i].findCollections();

            if (collection == null)
                continue;
//            if (log.isDebugEnabled()) {
//                log.debug("  Checking constraint '" + constraints[i] + "' against " + method + " " + uri + " --> " + constraints[i].included(uri, method));
//            }

            for (int j = 0; j < collection.length; j++) {
                String[] patterns = collection[j].findPatterns();

                if (patterns == null)
                    continue;

                for (int k = 0; k < patterns.length; k++) {
                    if (uri.equals(patterns[k])) {
                        found = true;
                        if (collection[j].findMethod(method))
                            results.add(constraints[i]);
                    }
                }
            }
        }

        if (found)
            return toArray(results);

        int longest = -1;

        // start from the end, because rule is to define more common constrains at the top
        for (int i = constraints.length - 1; i >= 0; i--) {
            SecurityCollection[] collection = constraints[i].findCollections();

            if (collection == null)
            {
                continue;
            }
//            if (log.isDebugEnabled()) {
//                log.debug("  Checking constraint '" + constraints[i] + "' against " + method + " " + uri + " --> " + constraints[i].included(uri, method));
//            }

            for (int j = 0; j < collection.length; j++) {
                String[] patterns = collection[j].findPatterns();

                if (patterns == null)
                {
                    continue;
                }
                boolean matched = false;
                int length = -1;
                for (int k = 0; k < patterns.length; k++) {
                    String pattern = patterns[k];
                    if ((!pattern.startsWith("/")) || (!pattern.endsWith("/*")) || (pattern.length() < longest)) {
                        continue;
                    }
                    if (pattern.length() == 2) {
                        matched = true;
                        length = pattern.length(); } else {
                        if ((!pattern.regionMatches(0, uri, 0, pattern.length() - 1)) && ((pattern.length() - 2 != uri.length()) || (!pattern.regionMatches(0, uri, 0, pattern.length() - 2))))
                        {
                            continue;
                        }

                        matched = true;
                        length = pattern.length();
                    }
                }

                if (matched) {
                    found = true;
                    if (length > longest) {
                        if (results != null) {
                            results.clear();
                        }
                        longest = length;
                    }

                    if (collection[j].findMethod(method))
                        results.add(constraints[i]);

                }
            }
        }

        if (found)
            return toArray(results);

        // start from the end, because rule is to define more common constrains at the top
        for (int i = constraints.length - 1; i >= 0; i--) {
            SecurityCollection[] collection = constraints[i].findCollections();

            if (collection == null)
            {
                continue;
            }
//            if (log.isDebugEnabled()) {
//                log.debug("  Checking constraint '" + constraints[i] + "' against " + method + " " + uri + " --> " + constraints[i].included(uri, method));
//            }

            boolean matched = false;
            int pos = -1;
            for (int j = 0; j < collection.length; j++) {
                String[] patterns = collection[j].findPatterns();

                if (patterns == null)
                {
                    continue;
                }
                for (int k = 0; (k < patterns.length) && (!matched); k++) {
                    String pattern = patterns[k];
                    if (pattern.startsWith("*.")) {
                        int slash = uri.lastIndexOf("/");
                        int dot = uri.lastIndexOf(".");
                        if ((slash < 0) || (dot <= slash) || (dot == uri.length() - 1) || (uri.length() - dot != pattern.length() - 1)) {
                            continue;
                        }
                        if (pattern.regionMatches(1, uri, dot, uri.length() - dot)) {
                            matched = true;
                            pos = j;
                        }
                    }
                }
            }

            if (matched) {
                found = true;
                if (collection[pos].findMethod(method))
                    results.add(constraints[i]);
            }
        }

        if (found)
            return toArray(results);

        // start from the end, because rule is to define more common constrains at the top
        for (int i = constraints.length - 1; i >= 0; i--) {
            SecurityCollection[] collection = constraints[i].findCollections();

            if (collection == null)
                continue;
//            if (log.isDebugEnabled()) {
//                log.debug("  Checking constraint '" + constraints[i] + "' against " + method + " " + uri + " --> " + constraints[i].included(uri, method));
//            }

            for (int j = 0; j < collection.length; j++) {
                String[] patterns = collection[j].findPatterns();

                if (patterns == null)
                {
                    continue;
                }
                boolean matched = false;
                for (int k = 0; (k < patterns.length) && (!matched); k++) {
                    String pattern = patterns[k];
                    if (pattern.equals("/")) {
                        matched = true;
                    }
                }

                if (matched)
                    results.add(constraints[i]);

            }
        }

//        if (results == null)
//        {
////            if (log.isDebugEnabled())
////                log.debug("  No applicable constraint located");
//        }

        return toArray(results);
    }

    private SecurityConstraint[] toArray(ArrayList<SecurityConstraint> results)
    {
        if (results == null || results.size() == 0)
            return null;

        return results.toArray(new SecurityConstraint[results.size()]);
    }

    public boolean hasResourcePermission(Request request, Response response, SecurityConstraint[] constraints, Context context)
            throws IOException
    {
        if ((constraints == null) || (constraints.length == 0)) {
            return true;
        }

        LoginConfig config = context.getLoginConfig();
        if ((config != null) && (Constants.FORM_METHOD.equals(config.getAuthMethod())))
        {
            String requestURI = request.getRequestPathMB().toString();
            String loginPage = config.getLoginPage();
            if (loginPage.equals(requestURI))
                return true;

            String errorPage = config.getErrorPage();
            if (errorPage.equals(requestURI)) {
                return true;
            }
            if (requestURI.endsWith(Constants.FORM_ACTION)) {
                return true;
            }
        }

        Principal principal = request.getPrincipal();
        boolean status = false;

        for (int i = 0; i < constraints.length; i++) {
            SecurityConstraint constraint = constraints[i];

            String[] roles;
            if (constraint.getAllRoles()) {
                roles = request.getContext().findSecurityRoles();
            }
            else {
                roles = constraint.findAuthRoles();
            }

            if (roles == null)
                roles = new String[0];

            if ((roles.length == 0)) {
                if (constraint.getAuthConstraint()) {
                    status = principal != null;
                } else {
                    return true;
                }
            } else if (principal == null) {
                status = false;
            } else {
                for (int j = 0; j < roles.length; j++) {
                    if (hasRole(request.getWrapper(), principal, roles[j]))
                        status = true;
                }
            }
        }

        if (!status) {
            response.sendError(403, "Access forbidden");
        }

        return status;
    }

    @Override
    public boolean      hasRole(Wrapper wrapper, Principal principal, String role) {
        return !(principal == null || role == null);
    }

    public boolean hasUserDataPermission(Request request, Response response, SecurityConstraint[] constraints)
            throws IOException
    {
        if ((constraints == null) || (constraints.length == 0)) {
            return true;
        }

        for (int i = 0; i < constraints.length; i++) {
            String userConstraint = constraints[i].getUserConstraint();

            if (userConstraint == null)
                return true;

            if (userConstraint.equals(Constants.NONE_TRANSPORT))
                return true;

        }

        if (request.getRequest().isSecure())
            return true;

        int redirectPort = request.getConnector().getRedirectPort();

        if (redirectPort <= 0) {
            response.sendError(403, request.getRequestURI());
            return false;
        }

        StringBuffer file = new StringBuffer();
        String protocol = "https";
        String host = request.getServerName();

        file.append(protocol).append("://").append(host);

        if (redirectPort != 443) {
            file.append(":").append(redirectPort);
        }

        file.append(request.getRequestURI());
        String requestedSessionId = request.getRequestedSessionId();
        if ((requestedSessionId != null) && (request.isRequestedSessionIdFromURL()))
        {
            file.append(";jsessionid=");
            file.append(requestedSessionId);
        }
        String queryString = request.getQueryString();
        if (queryString != null) {
            file.append('?');
            file.append(queryString);
        }
        response.sendRedirect(file.toString());
        return false;
    }

    public LoginConfig                  getLoginConfig() {
        LoginConfig config = new LoginConfig();
        config.setAuthMethod(HttpServletRequest.BASIC_AUTH);
        return config;
    }

    /**
     * This method is used to enable SSL and/or authentication in Tomcat web app
     * @param confidential
     * @param requiresAuthentication
     * @param uriPatterns URI pattern to apply security/confidentiality to (e.g. "/*" or "/shopping_cart/*")
     */
    public static SecurityConstraint    createSecurityConstraint(boolean confidential, boolean requiresAuthentication, String... uriPatterns) {
        //    <security-constraint>
        //    <web-resource-collection>
        //    <web-resource-name>Secure Content</web-resource-name>
        //    <url-pattern>/*</url-pattern>
        //        </web-resource-collection>
        //        <user-data-constraint>
        //            <transport-guarantee>CONFIDENTIAL</transport-guarantee>
        //        </user-data-constraint>
        //    </security-constraint>
        SecurityConstraint constraint = new SecurityConstraint();

        if (requiresAuthentication) {
            constraint.setAuthConstraint(true);
            constraint.addAuthRole("*"); // all
        }

        if (confidential)
            constraint.setUserConstraint(Constants.CONFIDENTIAL_TRANSPORT);

        SecurityCollection securityCollection = new SecurityCollection();
        securityCollection.setName("Protected");

        if (uriPatterns == null || uriPatterns.length == 0)
            uriPatterns = new String [] { "/*" }; // any URL

        for (String uriPattern : uriPatterns)
            securityCollection.addPattern(uriPattern);

        // unspecified means "all" ?
//        securityCollection.addMethod("POST");
//        securityCollection.addMethod("GET");
//        securityCollection.addMethod("PUT");
//        securityCollection.addMethod("DELETE");

        constraint.addCollection(securityCollection);

        return constraint;
    }

}