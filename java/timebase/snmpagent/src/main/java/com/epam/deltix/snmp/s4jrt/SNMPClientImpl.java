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
package com.epam.deltix.snmp.s4jrt;

import com.epam.deltix.snmp.mtree.SNMPSPI;
import com.epam.deltix.snmp.smi.SMIOID;
import com.epam.deltix.util.concurrent.UncheckedInterruptedException;
import com.epam.deltix.util.lang.Disposable;

import com.epam.deltix.util.net.IPEndpoint;
import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.*;
import org.snmp4j.security.*;
import org.snmp4j.smi.*;
import org.snmp4j.transport.*;

import java.io.*;
import java.util.Vector;
import java.util.logging.Level;

/**
 *
 */
public class SNMPClientImpl implements SNMPSPI, Disposable {
    static {
        S4JUtils.setUpLogging ();
    }

    private class Walker implements ResponseListener {
        private final Handler   handler;
        private final OID       rootOid;
        private final PDU       request = new PDU ();
        private boolean         finished = false;
        private RuntimeException  error = null;
        private int             maxReps;
        
        Walker (OID oid, Handler handler, int maxReps) {
            this.rootOid = oid;
            this.handler = handler;
            this.maxReps = maxReps;
            
            request.add (new VariableBinding (oid));

            request.setType (PDU.GETBULK);
            request.setMaxRepetitions (maxReps);
            request.setNonRepeaters (0);

            //S4JUtils.dumpPDU ("REQ", request);
        }
        
        public void         go () {
            try {
                snmp.send (request, target, null, this);
            } catch (Exception x) {
                setFinishedWithError (x);
            }
        }
        
        private synchronized void   setFinishedWithError (Exception x) {
            if (x instanceof RuntimeException)
                error = (RuntimeException) x;
            else
                error = new RuntimeException ("SNMP request failed", x);
            
            finished = true;
            notify ();
        }
        
        private synchronized void   setFinishedWalk () {
            finished = true;
            notify ();
        }
        
        public synchronized void    waitUntilFinished () {
            try {
                while (!finished) 
                    wait ();
            } catch (InterruptedException x) {
                throw new UncheckedInterruptedException ();
            }
            
            if (error != null)
                throw error;
        }
        
        @Override
        public void                 onResponse (ResponseEvent event) {
            // Always cancel async request when response has been received
            // otherwise a memory leak is created! Not canceling a request
            // immediately can be useful when sending a request to a broadcast
            // address.
            ((Snmp) event.getSource ()).cancel (event.getRequest (), this);

            PDU     resp = event.getResponse ();

            //showResponse ("RESP", resp);
            // see TreeUtils:257 for possible scenarios

            // for now
            if (resp == null) {
                setFinishedWithError (new RuntimeException ("No response"));
                return;
            }
                        
            OID     oid = null;
            
            Vector <? extends VariableBinding>  vbs = resp.getVariableBindings ();
            
            for (VariableBinding vb : vbs) {
                oid = vb.getOid ();
                
                if (!oid.startsWith (rootOid)) {
                    setFinishedWalk ();
                    return;
                }
                    
                Variable    vari = vb.getVariable ();
                
                try {
                    handler.set (S4JUtils.oidToDeltix (oid), vari.toString ());
                } catch (Exception x) {
                    S4JUtils.LOGGER.warn ("Error handling SNMP data: %s").with(x);
                }
            }
            
            if (oid == null) {
                setFinishedWalk ();
                return;
            }
            
            request.clear ();
                    
            request.add (new VariableBinding (oid));
                    
            go ();
        }
          
    }
    
    private final TransportMapping  transport;
    private final Snmp              snmp;
    private final CommunityTarget   target = new CommunityTarget ();
    
    public SNMPClientImpl (IPEndpoint ep) throws IOException {
        Address             targetAddress = S4JUtils.ipEndpointToAddress (ep);
        
        transport = new DefaultUdpTransportMapping ();
        
        snmp = new Snmp (transport);
        
        //snmp.addTransportMapping (new DefaultTcpTransportMapping ());
        
        USM                 usm = 
            new USM (
                SecurityProtocols.getInstance(),
                new OctetString (MPv3.createLocalEngineID ()), 
                0
            );
        
        SecurityModels.getInstance ().addSecurityModel (usm);
        
        transport.listen ();
                        
        // setting up target
        
        target.setCommunity (new OctetString ("public"));
        target.setAddress (targetAddress);
        target.setRetries (2);
        target.setTimeout (1500);
        target.setVersion (SnmpConstants.version2c);
    }
    
    @Override
    public void     close () {
        try {
            snmp.close ();
        } catch (IOException iox) {
            S4JUtils.LOGGER.error ("Failed to close session: %s").with(iox);
        }
    }
    
    static final class SingleValueHandler implements Handler {
        Object          value;

        @Override
        public void     set (SMIOID oid, Object value) {
            this.value = value;
        }                
    }
    
    @Override
    public Object   get (SMIOID oid, SMIOID index) {
        SingleValueHandler  handler = new SingleValueHandler ();
        final Walker        walker = new Walker (S4JUtils.oidToS4J (oid), handler, 1);
                
        walker.go ();
        
        walker.waitUntilFinished ();
        
        return (handler.value);
    }

    @Override
    public void     walk (SMIOID root, Handler handler) {
        final Walker        walker = 
            new Walker (S4JUtils.oidToS4J (root), handler, 50);
                
        walker.go ();
        
        walker.waitUntilFinished ();        
    }        
}