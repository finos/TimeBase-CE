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
package com.epam.deltix.qsrv.dtb.store.raw;

import com.epam.deltix.qsrv.dtb.store.codecs.TSFFormat;
import com.epam.deltix.qsrv.dtb.store.codecs.TSNames;
import com.epam.deltix.qsrv.dtb.fs.pub.*;
import com.epam.deltix.util.collections.*;
import com.epam.deltix.util.collections.generated.IntegerToObjectHashMap;
import java.io.*;

/**
 *
 */
public class RawFolder extends RawNode {
    private static final Comparator2 <RawEntityIndexEntry, Integer> REIE_COMP2 = 
        new Comparator2 <RawEntityIndexEntry, Integer> () {
            @Override
            public int compare (RawEntityIndexEntry e, Integer id) {
                return (e.getEntity () - id);
            }
        };
    
    private AbstractPath                                indexPath;
    private int                                         nextChildId;
    private int                                         numChildren;
    private RawFolderEntry []                           children;
    private IntegerToObjectHashMap <RawFolderEntry>     idToChild;
    private RawEntityIndexEntry []                      entityIndex;
    
    public static boolean       isTSFolder (AbstractPath path) {
        return (path.append (TSNames.INDEX_NAME).exists ());
    }
    
    @Override
    public void                 setPath (AbstractPath path) {
        this.path = path;                        
        indexPath = path.append (TSNames.INDEX_NAME);           
    }

    @Override
    public void                 readIndex (
        long                        startTimestamp,
        long                        limitTimestamp,
        DiagListener                dlnr
    )
        throws IOException 
    {
        try (DataInputStream dis = 
                new DataInputStream (
                    new BufferedInputStream (indexPath.openInput (0))
                )
            )
        {   
            formatVersion = dis.readUnsignedShort ();
            
            if (formatVersion > TSFFormat.INDEX_FORMAT_VERSION) {
                dlnr.unknownFormat (this, formatVersion);
                return;
            }
            
            version = formatVersion >= 2 ? dis.readLong () : dis.readInt();
            
            if (version < -1) {
                dlnr.illegalVersionInHeader (this, version);
                return;
            }
            
            nextChildId = dis.readUnsignedShort ();
            numChildren = dis.readUnsignedShort ();
                        
            children = new RawFolderEntry [numChildren];
            
            idToChild = new IntegerToObjectHashMap <> (numChildren);
            
            for (int ii = 0; ii < numChildren; ii++) {
                boolean         isFile = dis.readBoolean ();
                int             eid = dis.readUnsignedShort ();
                long            ts = dis.readLong ();
                long            version = formatVersion >= 2 ? dis.readLong() : 0;
                
                RawFolderEntry  rfe = new RawFolderEntry (ii, isFile, eid, ts);
                
                children [ii] = rfe;
                
                RawFolderEntry  dup = idToChild.get (eid, null);
                
                if (dup == null)
                    idToChild.put (eid, rfe);
                else
                    dlnr.duplicateChildId (this, dup, ii, rfe); 
                
                if (ii == 0) {
                    if (startTimestamp != Verifier.TS_UNKNOWN && ts != startTimestamp)
                        dlnr.firstChildHasWrongTimestamp (
                            this, rfe, startTimestamp
                        );
                }
                else {
                    RawFolderEntry  prev = children [ii - 1];
                    
                    if (ts < children [ii - 1].getStartTimestamp ())
                        dlnr.badChildTimeOrder (this, prev, rfe);
                    
                    if (limitTimestamp != Verifier.TS_UNKNOWN && ts > limitTimestamp)
                        dlnr.childTimeBeyondLimit (this, rfe, limitTimestamp);
                }
            }
                        
            numEntities = dis.readInt ();
            
            entityIndex = new RawEntityIndexEntry [numEntities];
        
            int             prevEntity = -1;
            
            for (int ii = 0; ii < numEntities; ii++) {
                int                 entity = dis.readInt ();
                
                if (entity <= prevEntity)
                    dlnr.badEntityOrder (this, prevEntity, ii, entity);
                        
                prevEntity = entity;
                
                int                 firstChildIdx, lastChildIdx;
                int                 firstId = dis.readUnsignedShort ();
                RawFolderEntry      first = idToChild.get (firstId, null);
                
                if (first == null) {
                    firstChildIdx = -1;
                    dlnr.badFirstChildIdInIndex (this, ii, firstId);
                }
                else
                    firstChildIdx = first.getIdxInParent ();
                
                int                 lastId = dis.readUnsignedShort ();                
                RawFolderEntry      last = idToChild.get (lastId, null);
                
                if (last == null) {
                    lastChildIdx = -1;                
                    dlnr.badLastChildIdInIndex (this, ii, lastId);
                }
                else
                    lastChildIdx = last.getIdxInParent ();
                
                entityIndex [ii] = new RawEntityIndexEntry (entity, firstChildIdx, lastChildIdx);
                
                if (first != null && last != null && firstChildIdx > lastChildIdx)
                    dlnr.badFolderIndexRange (this, ii, firstChildIdx, lastChildIdx);
                
            }
        }         
    }     

    public AbstractPath         getIndexPath () {
        return indexPath;
    }

    public int                  getNextChildId () {
        return nextChildId;
    }

    public int                  getNumChildren () {
        return numChildren;
    }

    public RawFolderEntry       getChild (int idx) {
        return children [idx];
    }

    public AbstractPath         getChildPath (int idx) {
        return (path.append (children [idx].getName ()));
    }
    
    @Override
    public int                  getEntity (int idx) {
        return entityIndex [idx].getEntity ();
    }
    
    public RawEntityIndexEntry  getEntityIndexEntry (int idx) {
        return entityIndex [idx];
    }

    public RawEntityIndexEntry  findEntityIndexEntry (int entity) {
        int     pos = BinarySearch2.binarySearch (entityIndex, entity, REIE_COMP2);
        
        if (pos < 0)
            return (null);
        
        return (getEntityIndexEntry (pos));
    }
    
    public void                 verify (
        DiagListener                dlnr,
        long                        startTimestamp,
        long                        limitTimestamp
    )
        throws IOException
    {
        dlnr.processingFolder (this);
        
        readIndex (startTimestamp, limitTimestamp, dlnr);
        
        IntegerToObjectHashMap <RawEntityIndexEntry>   observedIndex =
            new IntegerToObjectHashMap <> (numEntities);
        
        for (int childIdx = 0; childIdx < numChildren; childIdx++) {
            RawFolderEntry          child = getChild (childIdx);
            AbstractPath            childPath = path.append (child.getName ());
            
            RawNode                 node;
            
            if (child.isFile ()) 
                node = new RawTSF ();
            else
                node = new RawFolder ();
            
            node.setPath (childPath);
                        
            int                     nextIdx = childIdx + 1;
            
            node.readIndex (
                child.getStartTimestamp (), 
                nextIdx == numChildren ? 
                    limitTimestamp : 
                    getChild (nextIdx).getStartTimestamp (),
                dlnr
            );
            
            int                     numEntitiesThisNode = node.getNumEntities ();
            
            for (int ient = 0; ient < numEntitiesThisNode; ient++) {
                int                 entity = node.getEntity (ient);                
                RawEntityIndexEntry entry = findEntityIndexEntry (entity);
                
                if (entry == null) {
                    dlnr.unindexedEntity (this, child, entity);
                    continue;
                }
                
                if (entry.getFirstChildIdx () > childIdx ||
                    entry.getLastChildIdx () < childIdx)
                    dlnr.entityOutOfIndexedRange (this, entry, child);                             
                //
                //  Keep track of observed range, to make sure at the end that
                //  the stored index is not wider than observed.
                //
                RawEntityIndexEntry observed = observedIndex.get (entity, null);
                
                if (observed == null) 
                    observedIndex.put (entity, new RawEntityIndexEntry (entity, childIdx, childIdx));                
                else {
                    if (childIdx < observed.firstChildIdx)
                        observed.firstChildIdx = childIdx;
                    
                    if (childIdx > observed.lastChildIdx)
                        observed.lastChildIdx = childIdx;
                }
            }
        }
        
        for (int ient = 0; ient < numEntities; ient++) {
            RawEntityIndexEntry     readFromIndex = getEntityIndexEntry (ient);            
            RawEntityIndexEntry     observed = observedIndex.get (readFromIndex.getEntity (), null);
            
            if (observed == null) {
                dlnr.indexedEntityNotPresent (this, readFromIndex);
                continue;
            }
            //
            //  We just checked for observed wider than readFromIndex
            //
            assert observed.firstChildIdx >= readFromIndex.firstChildIdx;
            assert observed.lastChildIdx <= readFromIndex.lastChildIdx;
            
            if (observed.firstChildIdx != readFromIndex.firstChildIdx ||
                observed.lastChildIdx != readFromIndex.lastChildIdx)
                dlnr.entityRangeDiscrepancy (this, observed, readFromIndex);
        }        
    }
    
}