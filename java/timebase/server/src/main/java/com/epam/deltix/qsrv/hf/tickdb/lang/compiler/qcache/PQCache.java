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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.qcache;

import com.epam.deltix.qsrv.hf.tickdb.lang.pub.CompilerUtil;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.ParamSignature;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.SelectExpression;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.PreparedQuery;
import com.epam.deltix.util.collections.QuickList;
import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.util.parsers.Element;
import com.epam.deltix.util.time.TimeKeeper;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class PQCache {
    private final Map <PQKey, PQEntry>              map =
        new HashMap <PQKey, PQEntry> ();
    
    private final QuickList <PQEntry>               lifo =
        new QuickList <PQEntry> ();
    
    private int                                     count = 0;
    private final DXTickDB                          db;
    private int                                     maxCount = 1000;
    private long                                    maxAge = 3600000;
    
    public PQCache (DXTickDB db) {
        this.db = db;
    }
        
    private boolean                     shouldCache (Element qql) {
        return (qql instanceof SelectExpression);
    }

    public synchronized PreparedQuery prepareQuery(Element qql, ParamSignature[] paramSignature, long endTimestamp) {
        if (!shouldCache(qql))
            return (CompilerUtil.prepareQuery(db, qql, endTimestamp, paramSignature));
        PQKey key = new PQKey(qql, paramSignature, endTimestamp);
        PQEntry e = map.get(key);
        if (e == null) {
            PreparedQuery pq = CompilerUtil.prepareQuery(db, qql, endTimestamp, paramSignature);
            e = new PQEntry(key, pq);
            count++;
            lifo.linkLast(e);
            map.put(key, e);
            syncCleanup();
        } else {
            e.unlink();
            lifo.linkLast(e);
            e.timestamp = TimeKeeper.currentTime;
        }
        return (e.query);
    }
    
    public synchronized PreparedQuery   prepareQuery (
        Element                             qql, 
        ParamSignature []                   paramSignature
    )
        throws CompilationException
    {
        if (!shouldCache (qql))
            return (CompilerUtil.prepareQuery (db, qql, paramSignature));
        
        PQKey           key = new PQKey (qql, paramSignature, Long.MIN_VALUE);
        PQEntry         e = map.get (key);
        
        if (e == null) {
            PreparedQuery   pq = CompilerUtil.prepareQuery (db, qql, paramSignature);
                        
            e = new PQEntry (key, pq);
            count++;
            lifo.linkLast (e);
            map.put (key, e);
            
            syncCleanup ();            
        }
        else {
            e.unlink ();
            lifo.linkLast (e);
            e.timestamp = TimeKeeper.currentTime;
        }
        
        return (e.query);
    }
    
    private void                        syncCleanup () {
        while (count > maxCount) 
            remove (lifo.getFirst ());        
        
        long            minTimestamp = TimeKeeper.currentTime - maxAge;
        
        for (;;) {
            PQEntry     e = lifo.getFirst ();
            
            if (e == null || e.timestamp >= minTimestamp)
                break;
            
            remove (e);
        }
    }

    private void                        remove (PQEntry e) {
        e.unlink ();
        
        PQEntry     check = map.remove (e.key);
        
        assert check == e;
        
        count--;
    }
    
    public synchronized void            clear () {
        count = 0;
        lifo.clear ();
        map.clear ();
    }
}