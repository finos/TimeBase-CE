package com.epam.deltix.snmp.s4jrt;

import org.snmp4j.agent.*;
import org.snmp4j.agent.mo.util.VariableProvider;
import org.snmp4j.agent.request.*;
import org.snmp4j.smi.*;

/**
 *  Entirely generic utility, extracted from snmp4j:SampleAgent
 */
public class VariableProviderFromMOServer
    implements VariableProvider 
{
    private final MOServer          server;

    public VariableProviderFromMOServer (MOServer server) {
        this.server = server;
    }
        
    @Override
    public Variable                 getVariable (String name) {
        OID                             oid;
        OctetString                     context = null;
        int                             pos = name.indexOf (':');
        
        if (pos >= 0) {
            context = new OctetString (name.substring (0, pos));
            oid = new OID (name.substring (pos + 1, name.length ()));
        }
        else {
            oid = new OID (name);
        }
        
        final DefaultMOContextScope     scope =
            new DefaultMOContextScope (context, oid, true, oid, true);
        
        MOQuery                         query = 
            new MOQueryWithSource (scope, false, this);
        
        ManagedObject                   mo = server.lookup (query);
        
        if (mo != null) {
            final VariableBinding vb = new VariableBinding (oid);
            final RequestStatus status = new RequestStatus ();
            SubRequest req = new SubRequest () {

                private boolean completed;
                private MOQuery query;

                @Override
                public boolean hasError () {
                    return false;
                }

                @Override
                public void setErrorStatus (int errorStatus) {
                    status.setErrorStatus (errorStatus);
                }

                @Override
                public int getErrorStatus () {
                    return status.getErrorStatus ();
                }

                @Override
                public RequestStatus getStatus () {
                    return status;
                }

                @Override
                public MOScope getScope () {
                    return scope;
                }

                @Override
                public VariableBinding getVariableBinding () {
                    return vb;
                }

                @Override
                public Request getRequest () {
                    return null;
                }

                @Override
                public Object getUndoValue () {
                    return null;
                }

                @Override
                public void setUndoValue (Object undoInformation) {
                }

                @Override
                public void completed () {
                    completed = true;
                }

                @Override
                public boolean isComplete () {
                    return completed;
                }

                @Override
                public void setTargetMO (ManagedObject managedObject) {
                }

                @Override
                public ManagedObject getTargetMO () {
                    return null;
                }

                @Override
                public int getIndex () {
                    return 0;
                }

                @Override
                public void setQuery (MOQuery query) {
                    this.query = query;
                }

                @Override
                public MOQuery getQuery () {
                    return query;
                }

                @Override
                public SubRequestIterator repetitions () {
                    return null;
                }

                @Override
                public void updateNextRepetition () {
                }

                @Override
                public Object getUserObject () {
                    return null;
                }

                @Override
                public void setUserObject (Object userObject) {
                }
            };
            
            mo.get (req);
            
            return vb.getVariable ();
        }
        
        return null;
    }
}
