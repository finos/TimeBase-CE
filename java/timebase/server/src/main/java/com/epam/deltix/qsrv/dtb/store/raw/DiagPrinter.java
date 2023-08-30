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
package com.epam.deltix.qsrv.dtb.store.raw;

/**
 *
 */
public class DiagPrinter extends DiagListener {
    private final Appendable        out;
    private boolean                 printProgress = true;
    
    public DiagPrinter () {
        this.out = System.out;
    }
    
    public DiagPrinter (Appendable out) {
        this.out = out;
    }

    public boolean      getPrintProgress () {
        return printProgress;
    }

    public void         setPrintProgress (boolean printProgress) {
        this.printProgress = printProgress;
    }
            
    public String       getPrefix (RawNode node) {
        return ("");
    }
    
    public void         append (CharSequence s) {
        try {
            out.append (s);
        } catch (Exception iox) {
            iox.printStackTrace ();
        }
    }
    
    public void         format (String fmt, Object ... args) {
        append (String.format (fmt, args));
    }
    
    public void         appendPrefix (RawNode node) {
        append (getPrefix (node));
    }
    
    @Override
    public void         unknownFormat (RawNode folder, int formatVersion) {
        appendPrefix (folder);
        
        format ("Unknown format version: %d\n", formatVersion);
    }

    @Override
    public void         illegalVersionInHeader (RawNode folder, long version) {
        appendPrefix (folder);
        
        format ("Illegal version number: %d\n", version);
    }

    @Override
    public void         duplicateChildId (RawFolder folder, RawFolderEntry prev, int nextIdx, RawFolderEntry next) {
        appendPrefix (folder);
        
        format ("Duplicate child id %d at position %d\n", next.getId (), nextIdx);
    }

    @Override
    public void         badEntityOrder (RawNode node, int prevEntity, int nextIdx, int nextEntity) {
        appendPrefix (node);
        
        format (
            "Entity id %d out of order at position %d, expected > %d\n", 
            nextEntity, nextIdx, prevEntity
        );
    }

    @Override
    public void         badFirstChildIdInIndex (RawFolder folder, int atIdx, int firstId) {
        appendPrefix (folder);
        
        format (
            "First child %d referenced by index entry at position %d was not found\n", 
            firstId, atIdx
        );
    }

    @Override
    public void         badLastChildIdInIndex (RawFolder folder, int atIdx, int lastId) {
        appendPrefix (folder);
        
        format (
            "Last child %d referenced by index entry at position %d was not found\n", 
            lastId, atIdx
        );
    }

    @Override
    public void         badFolderIndexRange (RawFolder folder, int atIdx, int fidx, int lidx) {
        appendPrefix (folder);
        
        format (
            "Index entry at position %d points to children in wrong order: %d .. %d\n", 
            atIdx, fidx, lidx
        );
    }
    
    @Override
    public void     fileTooShortForIndex (RawTSF tsf, int blockOffset, long physicalLength) {
        appendPrefix (tsf);
        
        format (
            "Physical length %,d is insufficient even for the index block (size=%,d)\n",
            physicalLength, blockOffset
        );
    }

    @Override
    public void     fileTooShortForBlock (RawTSF tsf, RawDataBlock rdb, long physicalLength) {
        appendPrefix (tsf);
        
        format (
            "Physical length %,d is insufficient at block #%,d\n",
            physicalLength, rdb.getIdxInFile ()
        );
    }

    @Override
    public void     badFileIndexRange (RawTSF tsf, RawDataBlock rdb) {
        appendPrefix (tsf);
        
        format (
            "Block #%,d has timestamps reversed: %d .. %d\n",
            rdb.getIdxInFile (), rdb.getFirstTimestamp (), rdb.getLastTimestamp ()
        );
    }

    @Override
    public void     fileLengthDiscrepancy (RawTSF tsf, int blockOffset, long physicalLength) {
        appendPrefix (tsf);
        
        format (
            "Computed file length %,d differs from phystical length %,d\n",
            blockOffset, physicalLength
        );
    }
    
    @Override
    public void     unindexedEntity (RawFolder folder, RawFolderEntry child, int entity) {
        appendPrefix (folder);
        
        format (
            "Child #%d '%s' has data for unindexed entity %d\n",
            child.getIdxInParent (), child.getName (), entity
        );
    }

    @Override
    public void     entityOutOfIndexedRange (RawFolder folder, RawEntityIndexEntry entry, RawFolderEntry child) {
        appendPrefix (folder);
        
        format (
            "Child #%d '%s' has data for entity %d\n"
            + "    This child's index is out of expected range %d .. %d\n",
            child.getIdxInParent (), 
            child.getName (), 
            entry.getEntity (), 
            entry.getFirstChildIdx (), 
            entry.getLastChildIdx ()            
        );
    }

    @Override
    public void     indexedEntityNotPresent (RawFolder folder, RawEntityIndexEntry entry) {
        appendPrefix (folder);
        
        format (
            "Entity %d, supposedly found in children %d .. %d, was not found at all\n",
            entry.getEntity (), 
            entry.getFirstChildIdx (), 
            entry.getLastChildIdx ()            
        );
    }

    @Override
    public void     entityRangeDiscrepancy (RawFolder folder, RawEntityIndexEntry observed, RawEntityIndexEntry readFromIndex) {
        appendPrefix (folder);
        
        format (
            "Incorrect index for entity %d: expected %d .. %d; observed: %d .. %d\n",
            observed.getEntity (), 
            readFromIndex.getFirstChildIdx (), 
            readFromIndex.getLastChildIdx (),
            observed.getFirstChildIdx (), 
            observed.getLastChildIdx ()
        );
    }

    @Override
    public void     firstChildHasWrongTimestamp (RawFolder folder, RawFolderEntry rfe, long startTimestamp) {
        appendPrefix (folder);
        
        format (
            "First folder entry %s has timestamp %d, different from folder's timestamp %d\n",
            rfe.getName (),
            rfe.getStartTimestamp (),
            startTimestamp
        );
    }

    @Override
    public void     badChildTimeOrder (RawFolder folder, RawFolderEntry prev, RawFolderEntry rfe) {
        appendPrefix (folder);
        
        format (
            "Timestamp order violation: entry #%d (%s) has ts=%d, followed by entry #%d (%s) with ts=%d\n",
            prev.getIdxInParent (),
            prev.getName (),
            prev.getStartTimestamp (),
            rfe.getIdxInParent (),
            rfe.getName (),
            rfe.getStartTimestamp ()
        );
    }

    @Override
    public void     childTimeBeyondLimit (RawFolder folder, RawFolderEntry rfe, long limitTimestamp) {
        appendPrefix (folder);
        
        format (
            "Folder entry #%d (%s) has ts=%d beyond expected limit ts=%d\n",
            rfe.getIdxInParent (),
            rfe.getName (),
            rfe.getStartTimestamp (),
            limitTimestamp
        );
    }

    @Override
    public void     processingFolder (RawFolder folder) {
        if (printProgress)
            format ("Processing folder %s ...\n", folder.getPath ().toString ());
    }  
    
    @Override
    public void     processingFile (RawTSF file) {
        if (printProgress)
            format ("Processing file %s ...\n", file.getPath ().toString ());
    } 

    @Override
    public void     blockOutOfTimeRange (RawTSF file, RawDataBlock rdb, long startTimestamp, long limitTimestamp) {
        appendPrefix (file);
        
        format (
            "Block #%d for entity %d has indexed time range:\n"
            + "    [%d .. %d] out of expected range\n"
            + "    [%d .. %d]\n",
            rdb.getIdxInFile (),
            rdb.getEntity (),
            rdb.getFirstTimestamp (),
            rdb.getLastTimestamp (),
            startTimestamp,
            limitTimestamp
        );
    }

    @Override
    public void     unreadableTimestamp (RawTSF file, RawDataBlock db, int msgOffset, int msgIdx, Throwable x) {
        appendPrefix (file);
        
        format (
            "In block #%d for entity %d, which starts at offset %d:\n"
            + "    Error reading timestamp in message #%d:\n"
            + "    %s\n",
            db.getIdxInFile (),
            db.getEntity (),
            db.getOffset (),
            msgIdx,
            x.toString ()
        );
    }

    @Override
    public void     firstTimestampMismatch (RawTSF file, RawDataBlock db, int msgOffset, int msgIdx, long ts) {
        appendPrefix (file);
        
        format (
            "In block #%d for entity %d, which starts at offset %d:\n"
            + "    First message timestamp %d does not match the indexed value %d\n",
            db.getIdxInFile (),
            db.getEntity (),
            db.getOffset (),
            ts,
            db.getFirstTimestamp ()
        );
    }

    @Override
    public void     endOfBlockReadingMessageType (RawTSF file, RawDataBlock db, int msgOffset, int msgIdx, long ts, int position) {
        appendPrefix (file);
        
        format (
            "In block #%d for entity %d, which starts at offset %d:\n"
            + "    In message #%d at offset %d, ts=%d:\n"
            + "    End of block while reading the message type field\n",
            db.getIdxInFile (), db.getEntity (), db.getOffset (),
            msgIdx, msgOffset, ts
        );
    }

    @Override
    public void     unreadableBodySize (RawTSF file, RawDataBlock db, int msgOffset, int msgIdx, long ts, boolean inHead, int sizeOffset, Throwable x) {
        appendPrefix (file);
        
        format (
            "In block #%d for entity %d, which starts at offset %d:\n"
            + "    In message #%d at offset %d, ts=%d:\n"
            + "    Error reading the message size field in %s:\n"
            + "    %s",
            db.getIdxInFile (), db.getEntity (), db.getOffset (),
            msgIdx, msgOffset, ts,
            inHead ? "head" : "tail",
            x.toString ()
        );
    }

    @Override
    public void             tailSizeMismatch (
        RawTSF file, RawDataBlock db, int msgOffset, int msgIdx, long ts, 
        int tailSizeOffset, int tailSize
    )
    {
        appendPrefix (file);
        
        format (
            "In block #%d for entity %d, which starts at offset %d:\n"
            + "    In message #%d at offset %d, ts=%d:\n"
            + "    Tail size=%d at offset %d (should have been equal)\n",
            db.getIdxInFile (), db.getEntity (), db.getOffset (),
            msgIdx, msgOffset, ts,
            tailSize, tailSizeOffset
        );
    }
    
    @Override
    public void     endOfBlockReadingBody (RawTSF file, RawDataBlock db, int msgOffset, int msgIdx, long ts, int bodyOffset, int size) {
        appendPrefix (file);
        
        format (
            "In block #%d for entity %d, which starts at offset %d:\n"
            + "    In message #%d at offset %d, ts=%d:\n"
            + "    End of block reading a %d byte body at offset %d\n",
            db.getIdxInFile (), db.getEntity (), db.getOffset (),
            msgIdx, msgOffset, ts,
            size, bodyOffset
        );
    }

    @Override
    public void     lastTimestampMismatch (RawTSF file, RawDataBlock db, int msgOffset, int msgIdx, long ts) {
        appendPrefix (file);
        
        format (
            "In block #%d for entity %d, which starts at offset %d:\n"
            + "    In last message #%d at offset %d:\n"
            + "    Timestamp %d does not match the indexed value %d\n",
            db.getIdxInFile (), db.getEntity (), db.getOffset (),
            msgIdx, msgOffset,
            ts, db.getLastTimestamp ()
        );
    }

    @Override
    public void     messageOutOfOrder (RawTSF file, RawDataBlock db, int msgOffset, int msgIdx, long prevTimestamp, long ts) {
        appendPrefix (file);
        
        format (
            "In block #%d for entity %d, which starts at offset %d:\n"
            + "    In message #%d at offset %d:\n"
            + "    Timestamp %d is less than previous timestamp %d\n",
            db.getIdxInFile (), db.getEntity (), db.getOffset (),
            msgIdx, msgOffset,
            ts, prevTimestamp
        );
    }        
}