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
