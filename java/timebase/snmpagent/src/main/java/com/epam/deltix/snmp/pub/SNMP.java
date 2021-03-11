package com.epam.deltix.snmp.pub;

import com.epam.deltix.snmp.s4jrt.*;
import org.snmp4j.agent.MOGroup;

/**
 *
 */
public abstract class SNMP {
    public static final long                MAX_UINT32 = 0xFFFFFFFFL;
    
    public static final String              INSTANCE_FIELD = "INSTANCE";
    public static final String              SUPPORT_SUFFIX = "_SnmpSupport";
    
    @SuppressWarnings ("unchecked")
    public static MOGroup                   getMIB (Object top) {
        Class <?>                               topClass = 
            top.getClass ().getInterfaces () [0];
        
        Class <? extends MOGroup>               supportClass = 
            (Class <? extends MOGroup>) loadSupportClass (topClass);
        
        try {
            return (supportClass.getConstructor (topClass).newInstance (top));
        } catch (Exception x) {
            throw new RuntimeException (x);
        }
    }
    
    public static <X> Table <X>             createTable (Class <X> entryClass) {
        return (new TableImpl <X> (getEntrySupport (entryClass)));
    }    
    
    @SuppressWarnings ("unchecked")
    private static <X> EntrySupport <X>     getEntrySupport (Class <X> entryClass) {
        Class <?>       supportClass = loadSupportClass (entryClass);
        
        try {
            return (
                (EntrySupport <X>) 
                    supportClass.getField (INSTANCE_FIELD).get (null)
            );
        } catch (Exception x) {
            throw new RuntimeException (x);
        }            
    }

    private static Class <?>                loadSupportClass (Class <?> entryClass) 
        throws RuntimeException 
    {
        String      cname = entryClass.getName ();
        
        cname += SUPPORT_SUFFIX;
        
        try {
            return (entryClass.getClassLoader ().loadClass (cname));
        } catch (ClassNotFoundException x) {
            throw new RuntimeException (
                "Unable to load SNMP Support class '" + cname + "'", 
                x
            );
        }
    }
}
