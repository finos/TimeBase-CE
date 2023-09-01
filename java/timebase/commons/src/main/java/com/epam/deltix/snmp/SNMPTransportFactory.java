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
package com.epam.deltix.snmp;

import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.snmp.agent.EmbeddedAgent;
import com.epam.deltix.snmp.pub.SNMP;
import com.epam.deltix.snmp.s4jrt.ExternalSocketTransportMapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Alexei Osipov
 */
public class SNMPTransportFactory {
    protected static final Logger LOGGER = Logger.getLogger ("deltix.util.tomcat");

    private static final Level LEVEL_STARTUP = new Level ("STARTUP", Level.SEVERE.intValue() - 10) { };
    public static final String TOP_SNMP_DATA_OBJECT_CLASS_NAME = "com.epam.deltix.qsrv.snmp.modimpl.QuantServerImpl";

    public static ExternalSocketTransportMapping initializeSNMP(int port, QuantServerSnmpObjectContainer objectContainer) {
        try {
            long enterTime = System.currentTimeMillis();
            Object QSMIB = createTopLevelSnmpInfoObject(objectContainer, port);

            //QSMIB.setPort (port);

            EmbeddedAgent SNMPAgent =
                    new EmbeddedAgent(
                            QSHome.getFile("logs"),
                            SNMP.getMIB(QSMIB)
                    );

            SNMPAgent.addUdpPort (null, port);

            ExternalSocketTransportMapping SNMP_TM = new ExternalSocketTransportMapping(null, port);

            SNMPAgent.addTransportMapping (SNMP_TM);

            SNMPAgent.loadDefaultProperties ();

            SNMPAgent.run ();
            long exitTime = System.currentTimeMillis();
            LOGGER.info("Initialized SNMP (" + (exitTime - enterTime)/1000 + " seconds)");

            return SNMP_TM;
        } catch (Exception x) {
            LOGGER.log (
                    LEVEL_STARTUP,
                    "SNMP initialization failed. Continuing without SNMP support.",
                    x
            );
        }
        return null;
    }


    private static Class<?> loadTopLevelSnmpInfoClass() throws RuntimeException {
        String cname = TOP_SNMP_DATA_OBJECT_CLASS_NAME;
        try {
            return SNMPTransportFactory.class.getClassLoader().loadClass(cname);
        } catch (ClassNotFoundException x) {
            throw new RuntimeException("Unable to load top level SNMP object class '" + cname + "'", x);
        }
    }


    private static Object createTopLevelSnmpInfoObject(QuantServerSnmpObjectContainer objectContainer, int port) {
        Class<?> clazz = loadTopLevelSnmpInfoClass();
        try {
            Constructor<?> constructor = clazz.getConstructor(QuantServerSnmpObjectContainer.class, Integer.TYPE);
            return constructor.newInstance(objectContainer, port);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
        }
    }
}