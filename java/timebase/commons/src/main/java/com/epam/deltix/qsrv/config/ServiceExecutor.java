package com.epam.deltix.qsrv.config;

import com.epam.deltix.snmp.QuantServerSnmpObjectContainer;
import org.apache.catalina.Context;

import java.io.Closeable;

public interface ServiceExecutor extends Closeable {

    /**
     * Service entry point
     * @param configs QuantService Configurations
     */
    void    run(QuantServiceConfig ... configs);

    /**
     * Web module configuration hook
     * @param context
     */
    void    configure(Context context);

    /**
     * Implementors for this method may put own SNMP data objects into provided {@code snmpContextHolder}.
     * @param snmpContextHolder container for SNMP data objects
     */
    default void registerSnmpObjects(QuantServerSnmpObjectContainer snmpContextHolder) {
    }
}
