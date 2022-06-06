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
package com.epam.deltix.qsrv.hf.security;

import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.hf.security.rules.xml.FileBasedAccessControlProvider;
import com.epam.deltix.qsrv.hf.security.simple.SimpleUserDirectoryFactory;
import com.epam.deltix.util.io.UncheckedIOException;
import com.epam.deltix.util.lang.Factory;
import com.epam.deltix.util.ldap.security.LdapUserDirectoryFactory;
import com.epam.deltix.util.security.AccessControlRulesFactory;
import com.epam.deltix.util.security.AuthenticatingUserDirectory;
import com.epam.deltix.util.time.Interval;

import java.io.File;
import java.util.logging.Logger;

public class SecurityControllerFactory {
    protected static final Logger LOGGER = Logger.getLogger ("deltix.util.tomcat");
    public static final String LDAP_SECURITY_FILE_NAME = "uac-ldap-security.xml";
    public static final String SIMPLE_SECURITY_FILE_NAME = "uac-file-security.xml";
    public static final String OAUTH_SECURITY_FILE_NAME = "uac-oauth-security.xml";
    public static final String ACCESS_RULES_FILE_NAME = "uac-access-rules.xml";

    public static DefaultSecurityController createLDAP(Interval updateInterval) throws Exception {
        File configFile = getConfigFile(LDAP_SECURITY_FILE_NAME);
        LOGGER.info("[UAC] Initializing LDAP security from file: " + configFile.getAbsolutePath());
        Factory<AuthenticatingUserDirectory> userDirectory = new LdapUserDirectoryFactory(configFile);
        return getDefaultSecurityController(updateInterval, userDirectory);
    }

    public static DefaultSecurityController createFile(Interval updateInterval) throws Exception {
        File configFile = getConfigFile(SIMPLE_SECURITY_FILE_NAME);
        LOGGER.info("[UAC] Initializing FILE security from file: " + configFile.getAbsolutePath());
        Factory<AuthenticatingUserDirectory> userDirectory = new SimpleUserDirectoryFactory(configFile);
        return getDefaultSecurityController(updateInterval, userDirectory);
    }

//    public static DefaultSecurityController createOAuth(Interval updateInterval) throws Exception {
//        File configFile = getConfigFile(OAUTH_SECURITY_FILE_NAME);
//        LOGGER.info("[UAC] Initializing OAuth security from file: " + configFile.getAbsolutePath());
//        Factory<AuthenticatingUserDirectory> userDirectory = new ExternalDirectoryFactory(configFile);
//        return getDefaultSecurityController(updateInterval, userDirectory);
//    }

    private static File getConfigFile(String fileName) {
        File configFolder = QSHome.getFile("config");
        File configFile = new File(configFolder, fileName);
        if ( ! configFile.exists())
            throw new UncheckedIOException("Security configuration file is not found: " + configFile.getAbsolutePath());
        return configFile;
    }

    private static DefaultSecurityController getDefaultSecurityController(Interval updateInterval, Factory<AuthenticatingUserDirectory> userDirectory) {
        File rulesFile = new File (QSHome.getFile("config"), ACCESS_RULES_FILE_NAME);
        if ( ! rulesFile.exists())
            throw new UncheckedIOException("Access Rules configuration file is not found: " + rulesFile.getAbsolutePath());
        AccessControlRulesFactory rulesFactory = new FileBasedAccessControlProvider(rulesFile);
        return new DefaultSecurityController(userDirectory, rulesFactory, updateInterval);
    }

}