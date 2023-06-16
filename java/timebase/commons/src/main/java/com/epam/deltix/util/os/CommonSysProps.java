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
package com.epam.deltix.util.os;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.lang.Util;

import java.io.File;
import java.io.FileReader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Properties;

class ProxyAuthenticator extends Authenticator {
    private static final Log LOG = LogFactory.getLog(ProxyAuthenticator.class);
    @Override
    protected PasswordAuthentication    getPasswordAuthentication () {
                
        URL             url = getRequestingURL ();

        LOG.info("Authenticating request to %s").with(url.getHost());

        String          protocol = url.getProtocol ();
        RequestorType   type = getRequestorType ();
        String          userKey, passwordKey;

        switch (type) {
            case PROXY: 
                userKey = ".proxyUser";
                passwordKey = ".proxyPassword";
                break;

            case SERVER:
                userKey = ".user";
                passwordKey = ".password";
                break;

            default:
                LOG.error ("Unknown requestor type: %s").with(type);
                return (null);
        }

        String          user = System.getProperty (protocol + userKey);
        String          password = System.getProperty (protocol + passwordKey);

        return new PasswordAuthentication (user, password.toCharArray ());
    }
}
/**
 *
 */
public class CommonSysProps {
    private static final Log LOG = LogFactory.getLog(CommonSysProps.class);
    public static final String      FNAME = "sys.properties";
    public static final String      QO_RELPATH = "Config/" + FNAME;

    private static File             propsFile = null;
    private static boolean          mergedOnce = false;

    static {
        if (propsFile == null && Home.isSet ()) {
            propsFile = Home.getFile (FNAME);
            
            if (!propsFile.isFile ())
                propsFile = null;
        }

        Authenticator.setDefault (new ProxyAuthenticator ());
    }

    public static synchronized void     setPropsFile (File f) {
        propsFile = f;
    }

    public static synchronized void     mergePropsOnceIfFileExists () {
        if (!mergedOnce) {
            mergePropsIfFileExists ();
            mergedOnce = true;
        }
    }

    public static synchronized void     mergePropsIfFileExists () {
        if (propsFile != null && propsFile.isFile ()) {
            try {
                mergeProps ();
            } catch (Exception x) {
                LOG.error("Error merging propertie: %s").with(x);
            }
        }
    }

    public static synchronized void     mergeProps () throws Exception {
        Properties      props = new Properties ();
        FileReader      fr = new FileReader (propsFile);

        try {
            props.load (fr);
        } finally {
            Util.close (fr);
        }

        LOG.info ("Merging properties from %s").with(propsFile);

        for (String name : props.stringPropertyNames ()) {
            String      value = props.getProperty (name);

            if (LOG.isTraceEnabled())
                LOG.trace("%s=%s").with(name).with(value);
            
            System.setProperty (name, value);
        }
    }
}