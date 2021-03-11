package com.epam.deltix.snmp.mibc;

import com.epam.deltix.snmp.parser.MIBParser;
import com.epam.deltix.snmp.parser.Module;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.lang.Depends;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.parsers.CompilationException;

import java.io.IOException;
import java.io.Reader;

/**
 *
 */
@Depends ({ 
    "deltix/snmp/mibs-v2/SNMPv2-SMI.txt",
    "deltix/snmp/mibs-v2/SNMPv2-TC.txt",
    "deltix/snmp/mibs-v2/SNMPv2-CONF.txt",
    "deltix/snmp/mibs-v2/SNMPv2-MIB.txt",
    "deltix/snmp/mibs-v2/IANAifType-MIB.txt",
    "deltix/snmp/mibs-v2/IF-MIB.txt"
})
public class StandardModules {
    private static ModuleRegistry       sysRegistry = new ModuleRegistry ();

    public static CompiledModule        SNMPv2_SMI =
            initStdModule ("SNMPv2-SMI.txt");

    public static CompiledModule        SNMPv2_TC =
            initStdModule ("SNMPv2-TC.txt");

    public static CompiledModule        SNMPv2_CONF =
            initStdModule ("SNMPv2-CONF.txt");

    public static CompiledModule        SNMPv2_MIB =
            initStdModule ("SNMPv2-MIB.txt");

    public static CompiledModule        IANAifType_MIB =
            initStdModule ("IANAifType-MIB.txt");

    public static CompiledModule        IF_MIB =
            initStdModule ("IF-MIB.txt");

    private static CompiledModule       initStdModule (String name) {        
        Module      pmod;
        Reader      rd = null;
            
        
        try {
            rd = IOUtil.openResourceAsReader ("deltix/snmp/mibs-v2/" + name);
            pmod = MIBParser.parse (rd);
        } catch (IOException iox) {
            throw new IllegalStateException ("Could not load " + name, iox);
        } catch (CompilationException x) {
            throw new IllegalStateException ("Error compiling " + name, x);
        } finally {
            Util.close (rd);
        }
        
        CompiledModuleImpl  cmod = new CompiledModuleImpl (sysRegistry, pmod);
        
        sysRegistry.register (cmod);
        
        return (cmod);
    }
    
}
