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
package com.epam.deltix.qsrv.dtb.store.tools;

import com.epam.deltix.qsrv.dtb.fs.pub.*;
import com.epam.deltix.qsrv.dtb.store.impl.PDSFactory;
import com.epam.deltix.qsrv.dtb.store.pub.*;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickDBImpl;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.util.collections.*;
import com.epam.deltix.util.io.*;
import com.epam.deltix.util.lang.Disposable;
import com.epam.deltix.util.memory.*;
import java.io.*;

public class OfflineDBCreator implements Disposable {    
    static class RawMessageAdapter implements TSMessageProducer {
        RawMessage          msg;
        
        @Override
        public void         writeBody (MemoryDataOutput out) {
            out.write (msg.data, msg.offset, msg.length);
        }                
    }
    
    public static final int                     NTSFS = 10 << 20;
    
    private final AbstractFileSystem            fs;
    private TSRoot                              root;
    private final PersistentDataStore           cache = PDSFactory.create ();
    
    private CharSequenceToIntegerMap            entityMap = 
        new CharSequenceToIntegerMap ();
        
    private DataWriter                        acc;
    
    private RecordTypeMap<RecordClassDescriptor> typeMap;
    private final RawMessageAdapter             rma = new RawMessageAdapter ();
    
    public OfflineDBCreator (AbstractFileSystem fs) {
        this.fs = fs;
    }
    
    public void         prepare (
        RecordClassDescriptor []        types,
        String                          outPath
    )
    {
        entityMap.clear ();
        
        typeMap = new RecordTypeMap<RecordClassDescriptor> (types);
        
        root = cache.createRoot (null, fs, outPath);
    }
    
    public void         send (RawMessage msg) throws InterruptedException {
        int                 eid = entityMap.get (msg.getSymbol(), -1);
        
        if (eid == -1) {
            eid = entityMap.size ();
            
            entityMap.put (msg.getSymbol(), eid);
        }
        
        if (acc == null) {
            acc = cache.createWriter ();
            
            acc.associate (root);
            
            acc.open (msg.getNanoTime (), null);            
        }
        
        rma.msg = msg;
        
        int         typeCode = typeMap.getCode (msg.type);        
        
        acc.insertMessage (eid, msg.getNanoTime (), typeCode, rma);
    }
    
    @Override
    public void         close () {
        acc.close ();
        root.close ();
    }
    
    public void         copyFromTimeBaseStream (
        String              path,
        String              streamKey,
        String              outPath
    )
        throws InterruptedException 
    {
        try (DXTickDB db = new TickDBImpl(new File(path))) {
            db.open(false);
            DXTickStream    stream = db.getStream (streamKey);
            
            prepare (stream.getPolymorphicDescriptors (), outPath);
            
            try (InstrumentMessageSource cur = stream.select (-1, new SelectionOptions (true, false))) {
                while (cur.next ())
                    send ((RawMessage) cur.getMessage ());
            }
        }
    }
    
    public static void main (String [] args) throws Exception {
        String      path = "o:\\dtb";
        
        IOUtil.removeRecursive (new File (path), null, false);
                
        try (OfflineDBCreator dbc = new OfflineDBCreator (FSFactory.getLocalFS())) {
            dbc.copyFromTimeBaseStream ("O:\\qshomes\\QSH_SandBox\\tickdb", "ticks", path);
        }
    }
}