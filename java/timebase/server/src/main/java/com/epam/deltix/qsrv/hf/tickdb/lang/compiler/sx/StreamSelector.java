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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.SelectionMode;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.GrammarUtil;
import com.epam.deltix.qsrv.hf.tickdb.pub.Messages;
import com.epam.deltix.qsrv.hf.tickdb.pub.Streams;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
import java.util.Set;

/**
 *
 */
public class StreamSelector extends CompiledQuery {
    
    public final TickStream []          streams;
    public final SelectionMode                   mode;
    
    public StreamSelector (TickStream ... streams) {
        super (
            new QueryDataType (
                false, 
                new ClassDataType (false, Streams.catTypes (streams))
            )
        );
        
        this.streams = streams;
        this.mode = SelectionMode.NORMAL;
    }

    private static QueryDataType    addRTM (QueryDataType base) {
        ClassDataType                   baseOutput = base.getOutputType ();
        RecordClassDescriptor []        baseRCDs = baseOutput.getDescriptors ();
        int                             n = baseRCDs.length;
        
        for (int ii = 0; ii < n; ii++)
            if (baseRCDs [ii].getGuid ().equals (Messages.REAL_TIME_START_MESSAGE_DESCRIPTOR.getGuid()))
                return (base);
        
        RecordClassDescriptor []    outRCDs = new RecordClassDescriptor [n + 1];
        
        System.arraycopy (baseRCDs, 0, outRCDs, 0, n);
        outRCDs [n] = Messages.REAL_TIME_START_MESSAGE_DESCRIPTOR;
        
        return (new QueryDataType (false, new ClassDataType (false, outRCDs)));
    }
    
    public StreamSelector (StreamSelector template, SelectionMode newMode) {
        super (
            newMode == SelectionMode.HYBRID ?
                addRTM (template.type) :
                template.type
        );
        
        this.streams = template.streams;
        this.mode = newMode;
    }
    
    @Override
    public boolean              isForward () {
        return (mode != SelectionMode.REVERSE);
    }
    
    @Override
    public void                 getAllTypes (Set <ClassDescriptor> out) {
        for (TickStream s : streams)
            for (ClassDescriptor cd : s.getAllDescriptors ())
                out.add (cd);
        
        if (mode == SelectionMode.HYBRID)
            out.add (Messages.REAL_TIME_START_MESSAGE_DESCRIPTOR);
    }
        
    @Override
    protected void              print (StringBuilder out) {
        int         n = streams.length;

        if (mode != SelectionMode.NORMAL) {
            out.append (mode.name ());
            out.append (' ');        
        }
        
        if (n > 1)
            out.append ("(");

        GrammarUtil.escapeVarId (streams [0].getKey (), out);

        for (int ii = 1; ii < streams.length; ii++) {
            out.append (" union ");
            GrammarUtil.escapeVarId (streams [ii].getKey (), out);
        }

        if (n > 1)
            out.append (")");
    }
}
