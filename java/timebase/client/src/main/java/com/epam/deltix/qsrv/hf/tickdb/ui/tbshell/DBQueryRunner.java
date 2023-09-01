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
package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell;

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.StandardTypes;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;
import com.epam.deltix.util.cmdline.DefaultApplication;
import java.io.*;
import java.util.*;

/**
 *
 */
public class DBQueryRunner {
    private final SelectionOptions          opts = new SelectionOptions ();   
    private final Map <String, Parameter>   params =
        new TreeMap <String, Parameter> ();
    
    public DBQueryRunner() {
        opts.raw = true;
        opts.channelQOS = ChannelQualityOfService.MIN_INIT_TIME;
    }                    
    
    public void         clearParams () {
        params.clear ();
    }
    
    public void         showParam (Parameter p, PrintWriter out) throws IOException {        
        out.printf ("%-15s ", StandardTypes.toSimpleName (p.type));
        //TODO: MODULARIZATION
        //GrammarUtil.escapeIdentifier (NamedObjectType.VARIABLE, p.name, out);
        out.print(p.name);
        out.print (" = ");

        if (p.value.isNull ())
            out.println ("<null>");
        else
            out.println (p.value);        
    }
    
    public void         showParams (PrintWriter out) throws IOException {
        for (Parameter p : params.values ()) 
            showParam (p, out);        
    }
    
    public Parameter    addParam (String type, String name) {
        DataType            t = StandardTypes.forName (type);
        
        if (t == null)
            throw new IllegalArgumentException ("Type not found: '" + type + "'");
        
        return (addParam (t, name));
    }
    
    public Parameter    addParam (DataType type, String name) {
        Parameter           p = new Parameter (name, type);
        
        params.put (name, p);
        
        return (p);
    }
    
    public Parameter    setParam (String name, String value) {
        Parameter           p = params.get (name);
        
        if (p == null)
            return (null);
        
        p.value.writeString (value);
        return (p);
    }
    
    public Parameter [] getParams () {
        return (params.values ().toArray (new Parameter [params.size ()]));
    }
    
//    public void         runQuery (
//        PrintWriter         out,
//        DXTickDB            db,
//        SelectionOptions    options,
//        String              query,
//        long                from,
//        int                 numResults
//    )
//        throws IOException
//    {
//        runQuery (out, db, options, CompilerUtil.parse (query), from, numResults);
//    }
    
    public void         runQuery (
        PrintWriter         out,
        DXTickDB            db,
        SelectionOptions    options,
        String              query,
        long                startTimestamp,
        long                endTimestamp,
        int                 numResults,
        boolean             printJson
    ) throws IOException
    {
        try {
            InstrumentMessageSource ims = db.executeQuery(query, opts, null, null, startTimestamp, endTimestamp, getParams());
            IMSPrinter imsp = printJson ? new JsonIMSPrinter(out) : new SimpleIMSPrinter(out);
            imsp.setMaxCount (numResults);
            imsp.setIMS (ims, true);
            imsp.printAll ();                
        } catch (CompilationException x) {
            DefaultApplication.printException (x, false, out);
        } finally {
            out.flush ();
        }
    }            
}