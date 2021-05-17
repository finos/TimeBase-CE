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
package com.epam.deltix.snmp.s4jrt;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.snmp.smi.SMIOID;
import com.epam.deltix.util.net.IPEndpoint;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.snmp4j.*;
import org.snmp4j.log.*;
import org.snmp4j.smi.*;

/**
 *
 */
public abstract class S4JUtils {
    static final Log LOGGER = com.epam.deltix.gflog.api.LogFactory.getLog("deltix.snmp");

    //public static final Logger  LOGGER = Logger.getLogger ("deltix.snmp");
    
    public static void      setUpLogging () {
        LogFactory.setLogFactory (new JavaLogFactory ());
    }
    
    public static OID       oidToS4J (SMIOID in) {
        return (new OID (in.getInternalBuffer (), in.getOffset (), in.getLength ()));
    }
    
    public static SMIOID    oidToDeltix (OID in) {
        return (new SMIOID (in.toIntArray ()));
    }
    
    public static Address   ipEndpointToAddress (IPEndpoint ep) 
        throws UnknownHostException 
    {
        InetAddress     ia = InetAddress.getByName (ep.getHost ());
        
        switch (ep.getProtocol ()) {
            case TCP:   return (new TcpAddress (ia, ep.getPort ()));                              
            case UDP:   return (new UdpAddress (ia, ep.getPort ()));                
            default: throw new UnsupportedOperationException (ep.toString ());
        }
    }
    
    public static void      dumpPDU (String prompt, PDU resp) {
        System.out.println ("PDU '" + prompt + "'" + " = " + resp.getType ());
        
        for (VariableBinding vb : resp.getVariableBindings ()) {
            System.out.println (
                vb.getOid () + " = " + vb.getVariable ()
            );
        }
    }
}
