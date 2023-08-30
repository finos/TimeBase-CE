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
package com.epam.deltix.qsrv.config;

import com.epam.deltix.qsrv.hf.security.DefaultSecurityController;
import com.epam.deltix.qsrv.hf.security.SecurityControllerFactory;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.security.SecurityController;
import com.epam.deltix.util.time.Interval;
import org.apache.catalina.Context;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QuantServerExecutor implements ServiceExecutor {

    private static final Logger     LOGGER = Logger.getLogger ("deltix.util.tomcat");
    private static final Level      LEVEL_STARTUP = new Level ("STARTUP", Level.SEVERE.intValue() - 10) { };

    private static final String FILE_SECURITY_ID = "FILE"; //FileSecurity.ID;
    private static final String LDAP_SECURITY_ID = "LDAP"; //LDAPSecurity.ID;

    //public static ConnectionHandshakeHandler    SNMP;
    public static SecurityController                SC;
    public static TConnectionHandler                HANDLER;

    @Override
    public void run(QuantServiceConfig ... config) {
        assert config.length == 1 && config[0].getType() == QuantServiceConfig.Type.QuantServer;

        SC = createSecurityController(config[0]);
        HANDLER = new TConnectionHandler();
    }

    private DefaultSecurityController createSecurityController(QuantServiceConfig config) {
        String securityMode = config.getString(QuantServiceConfig.Type.QuantServer, "security", null);
        boolean secured = !StringUtils.isEmpty(securityMode);

        if (!secured)
            return null;

        try {
            String updateDelay = StringUtils.trim(config.getString("security.updateInterval"));
            Interval updateInterval = updateDelay != null ? Interval.valueOf(updateDelay) : null;

            switch (securityMode) {
                case FILE_SECURITY_ID:
                    return SecurityControllerFactory.createFile(updateInterval);
                case LDAP_SECURITY_ID:
                    return SecurityControllerFactory.createLDAP(updateInterval);
                default:
                    throw new IllegalArgumentException("Unsupported security mode: " + securityMode);
            }
        } catch (Throwable e) {
            throw Util.asRuntimeException(e);
        }
    }

    @Override
    public void configure(Context context) {

    }

    @Override
    public void close() throws IOException {
        Util.close(HANDLER);
    }
}