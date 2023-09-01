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
    "com/epam/deltix/snmp/mibs-v2/SNMPv2-SMI.txt",
    "com/epam/deltix/snmp/mibs-v2/SNMPv2-TC.txt",
    "com/epam/deltix/snmp/mibs-v2/SNMPv2-CONF.txt",
    "com/epam/deltix/snmp/mibs-v2/SNMPv2-MIB.txt",
    "com/epam/deltix/snmp/mibs-v2/IANAifType-MIB.txt",
    "com/epam/deltix/snmp/mibs-v2/IF-MIB.txt"
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
            rd = IOUtil.openResourceAsReader ("com/epam/deltix/snmp/mibs-v2/" + name);
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