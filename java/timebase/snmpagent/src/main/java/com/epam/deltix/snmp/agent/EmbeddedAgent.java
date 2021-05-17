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
package com.epam.deltix.snmp.agent;

import com.epam.deltix.snmp.s4jrt.S4JUtils;
import com.epam.deltix.snmp.s4jrt.VariableProviderFromMOServer;
import com.epam.deltix.util.lang.*;
import java.io.*;
import java.net.InetAddress;
import java.util.*;

import org.snmp4j.*;
import org.snmp4j.agent.*;
import org.snmp4j.agent.cfg.*;
import org.snmp4j.agent.io.*;
import org.snmp4j.agent.io.prop.*;
import org.snmp4j.mp.*;
import org.snmp4j.smi.*;
import org.snmp4j.transport.*;
import org.snmp4j.util.*;
import org.snmp4j.agent.mo.*;
import org.snmp4j.security.SecurityProtocols;

public class EmbeddedAgent implements Disposable {
    static {
        S4JUtils.setUpLogging ();
    }

    private File                        workArea;
    private AgentConfigManager          agent;
    private ThreadPool                  threadPool;
    private final MOServer              server = new DefaultMOServer ();
    private final MessageDispatcher     md = new MessageDispatcherImpl ();
    private final Properties            props = new Properties ();
    private int                         numHandlerThreads = 2;
    private final MOInputFactory        configurationFactory = 
        new MOInputFactory() {
            @Override
            public MOInput createMOInput() {
                return new PropertyMOInput (props, new VariableProviderFromMOServer (server));
            }
        };
    // supported MIBs
    protected final MOGroup           g;

    //protected Properties tableSizeLimits;

    static {
        // Add all available security protocols (e.g. SHA,MD5,DES,AES,3DES,..)
        SecurityProtocols.getInstance().addDefaultProtocols();
    }

    public EmbeddedAgent (File workArea, MOGroup group) {
        this.workArea = workArea;
        this.g = group;
//    String tlsVersions = (String)((List)args.get("tls-version")).get(0);
//    if (tlsVersions != null) {
//      System.setProperty(SnmpConfigurator.P_TLS_VERSION, tlsVersions);
//    }
    }
    
    public void         loadDefaultProperties () throws IOException {
        InputStream         is =
            EmbeddedAgent.class.getResourceAsStream ("TestAgentConfig.properties");
        
        try {
            props.load (is);
        } finally {
            Util.close (is);
        }
    }            
    
//    InputStream tableSizeLimitsInputStream =
//        TestAgent.class.getResourceAsStream("TestAgentTableSizeLimits.properties");
//    
//    tableSizeLimits = new Properties();
//    try {
//      tableSizeLimits.load(tableSizeLimitsInputStream);
//    }
//    catch (IOException ex) {
//      ex.printStackTrace();
//    }        
            
        
 

    public void         addTransportMapping (TransportMapping tm) {        
        md.addTransportMapping(tm);
    }
    
    public void         addUdpPort (InetAddress ip, int port) {
        if (ip == null)
            ip = IpAddress.ANY_IPADDRESS;
        
        Address             address = new UdpAddress (ip, port); 
        
        addTransportMapping (TransportMappings.getInstance().createTransportMapping(address));
    }
    
    public void         run() {   
        threadPool = ThreadPool.create ("SNMP Request Handler", numHandlerThreads);
        
        agent = 
            new AgentConfigManager (
                new OctetString (MPv3.createLocalEngineID ()),
                md,
                null,
                new MOServer[] { server },
                threadPool,
                configurationFactory,
                null,
                new EngineBootsCounterFile (new File (workArea, "snmp.bc"))
            );
        
        // initialize agent before registering our own modules
        agent.initialize();
        // switch logging of notifications to log sent notifications instead
        // of logging the original internal notification event:
        //agent.getNotificationLogMIB().setLoggerMode(
        //  NotificationLogMib.Snmp4jNotificationLogModeEnum.sent);
        // this requires sysUpTime to be available.
        registerMIBs();
        // add proxy forwarder
        agent.setupProxyForwarder();
        // apply table size limits
        //agent.setTableSizeLimits(tableSizeLimits);

        // now continue agent setup and launch it.
        agent.run();
    }

    @Override
    public void         close () {
        threadPool.stop ();
        agent.shutdown ();
    }
    
    /**
    * Get the {@link MOFactory} that creates the various MOs (MIB Objects).
    * @return
    *    a {@link DefaultMOFactory} instance by default.
    * @since 1.3.2
    */
    protected MOFactory getFactory() {
        return DefaultMOFactory.getInstance();
    }

    /**
     * Register your own MIB modules in the specified context of the agent.
     * The {@link MOFactory} provided to the <code>Modules</code> constructor
     * is returned by {@link #getFactory()}.
     */
    protected void registerMIBs () {
        try {
            g.registerMOs (server, null);
        } catch (DuplicateRegistrationException drex) {
            throw new IllegalStateException (drex);
        }
    }
}
