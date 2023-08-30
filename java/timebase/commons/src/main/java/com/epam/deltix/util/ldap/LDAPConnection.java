/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.util.ldap;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import java.net.SocketException;
import java.security.AccessControlException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.lang.Util;

public class LDAPConnection {
    public static final Logger LOGGER = Logger.getLogger("deltix.uac");
    public static final String LOGPREFIX = "[UAC] ";

    public static enum Vendor {
        ActiveDirectory,
        ApacheDS
    }

    public static String toString(String host, int port) {
        return "ldap://" + host + ":" + port;
    }

    public static boolean authenticate(LDAPContext ctx, String username, String password) {
        int retries = 2;

        while (retries > 0) {
            try {
                if (retries < 2)
                    ctx.reconnect();

                ctx.context.addToEnvironment(Context.SECURITY_PRINCIPAL, username);
                ctx.context.addToEnvironment(Context.SECURITY_CREDENTIALS, password);
                ctx.context.getAttributes("", null);
                return true;
            } catch (javax.naming.AuthenticationException e) {
                LOGGER.log(Level.FINE, LOGPREFIX + "Authentication failure: " + e.getMessage(), e);
                return false;
            } catch (CommunicationException e) {
                LOGGER.log(Level.WARNING, LOGPREFIX + "Disconnected error during \"" + username + "\" authentication. Reconnecting.", e);
                retries--;
            } catch (NamingException e) {
                LOGGER.log(Level.WARNING, LOGPREFIX + "Unexpected error during \"" + username + "\" authentication: " + e.getMessage(), e);
                return false;
            }  catch (Throwable e) { // unknown case - just try to reconnect
                LOGGER.log(Level.WARNING, LOGPREFIX + "Unexpected error during \"" + username + "\" authentication: " + e.getMessage(), e);
                retries--;
            }
        }

        return false;
    }

    public static DirContext connect(List<String> url, String username, String pass) throws NamingException {
        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        props.put(Context.PROVIDER_URL, StringUtils.join(" ", Arrays.copyOf(url.toArray(), url.size(), String[].class)));
        props.put(Context.REFERRAL, "ignore");
        //props.put(Context.REFERRAL, "follow");
        props.put(Context.SECURITY_AUTHENTICATION, "simple");
        props.put(Context.SECURITY_PRINCIPAL, username);
        props.put(Context.SECURITY_CREDENTIALS, pass);

        return new InitialDirContext(props);
    }

    public static DirContext connect(List<String> url) throws NamingException {
        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        props.put(Context.PROVIDER_URL, StringUtils.join(" ", Arrays.copyOf(url.toArray(), url.size(), String[].class)));
        props.put(Context.REFERRAL, "ignore");

        return new InitialDirContext(props);
    }


    public static void changePassword(LDAPContext ctx, String adminUser, String adminPass,
                                      String user, String oldPassword, String newPassword) {
        try {
            authenticate(ctx, user, oldPassword);

            ctx.context.addToEnvironment(Context.SECURITY_PRINCIPAL, adminUser);
            ctx.context.addToEnvironment(Context.SECURITY_CREDENTIALS, adminPass);

            // modify user password on behalf of admin
            ModificationItem[] mods = new ModificationItem[1];
            Attribute mod0 = new BasicAttribute("userPassword", newPassword);
            mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, mod0);
            ctx.context.modifyAttributes(user, mods);

            ctx.context.getAttributes("", null);
        } catch (javax.naming.AuthenticationException e) {
            throw new AccessControlException("Not authorized.");
        } catch (NamingException e) {
            throw Util.asRuntimeException(e);
        }
    }
}