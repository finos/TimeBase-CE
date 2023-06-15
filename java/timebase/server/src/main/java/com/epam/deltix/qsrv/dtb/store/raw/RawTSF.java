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

import com.epam.deltix.qsrv.dtb.store.codecs.*;
import com.epam.deltix.qsrv.dtb.fs.pub.*;
import com.epam.deltix.qsrv.dtb.store.impl.Restorer;
import com.epam.deltix.qsrv.hf.pub.codec.TimeCodec;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.lang.*;
import com.epam.deltix.util.memory.*;
import java.io.*;
import java.util.ArrayList;

/**
 *
 */
public class RawTSF extends RawNode {
    protected RawDataBlock []     index;
    protected boolean             compressed;
    protected long                minTimestamp;
    protected long                maxTimestamp;
    protected long                physicalLength;
    protected byte                algorithm;

    protected long                actualStartTimestamp = Long.MAX_VALUE;

    private   DiagListener        dlnr;

    public static boolean       isTSFile (AbstractPath path) {
        return (TSNames.isTSFileName (path.getName ()) && path.isFile ());
    }
    
    @Override
    public void                 setPath (AbstractPath path) {
        this.path = path;                        
    }

    protected BufferedInputStream open () throws IOException {
        return (new BufferedInputStream (path.openInput (0)));
    }   
    
    @Override
    public void                 readIndex (
        long                        startTimestamp,
        long                        limitTimestamp,
        DiagListener                dlnr
    )
        throws IOException 
    {
        this.dlnr = dlnr;
        try (DataInputStream dis = new DataInputStream (open ())) {   
            readIndex (dis, startTimestamp, limitTimestamp);
        }
    }
    
    public boolean              isCompressed () {
        return (compressed);
    }
    
    protected void                readIndex (
        InputStream                 is,
        long                        startTimestamp,
        long                        limitTimestamp
    )
        throws IOException 
    {
        Restorer.LOGGER.debug("Read index from file: " + path);

        physicalLength = path.length ();
        minTimestamp = Long.MAX_VALUE;
        maxTimestamp = Long.MIN_VALUE;
        
        int             blockOffset;
        
        DataInputStream dis = new DataInputStream (is);
        formatVersion = dis.readShort ();

        if (formatVersion < 0 || formatVersion > 3) {
            dlnr.unknownFormat (this, formatVersion);
            return;
        }

        version = formatVersion >= 3 ? dis.readLong () : dis.readInt();

        if (version < -1) {
            dlnr.illegalVersionInHeader (this, version);
            return;
        }

        int             flags = dis.readInt ();
        
        numEntities = flags & TSFFormat.NUM_ENTS_MASK;
        compressed = (flags & TSFFormat.COMPRESSED_FLAG) != 0;
        algorithm = TSFFormat.getAlgorithmCode(flags);

        ArrayList<RawDataBlock> blocks = new ArrayList<RawDataBlock>();

        blockOffset = numEntities * (compressed ? 28 : 24) + (formatVersion >= 3 ? 14 : 10);

        if (blockOffset > physicalLength) {
            dlnr.fileTooShortForIndex (this, blockOffset, physicalLength);
            return;
        }

        int             prevEntity = -1;

        for (int ii = 0; ii < numEntities; ii++) {
            int         entity = dis.readInt ();

            if (entity <= prevEntity)
                dlnr.badEntityOrder (this, prevEntity, ii, entity);

            prevEntity = entity;

            int         dataLength = dis.readInt ();
            int         lengthOnDisk = compressed ? dis.readInt () : dataLength;            
            long        firstTimestamp = dis.readLong ();
            long        lastTimestamp = dis.readLong ();

            // skip blocks we size 0
            if (dataLength == 0) {
                continue;
            }

            RawDataBlock    rdb =
                new RawDataBlock (
                    ii,
                    entity, 
                    blockOffset, 
                    dataLength, lengthOnDisk,
                    firstTimestamp, lastTimestamp
                );

            if (firstTimestamp > lastTimestamp)
                dlnr.badFileIndexRange (this, rdb);

            blockOffset += lengthOnDisk;

            if (blockOffset > physicalLength) {
                dlnr.fileTooShortForBlock (this, rdb, physicalLength);
                return;
            }

            blocks.add(rdb);

            if (firstTimestamp < minTimestamp)
                minTimestamp = firstTimestamp;

            if (lastTimestamp > maxTimestamp)
                maxTimestamp = lastTimestamp;
            
            if (startTimestamp != Verifier.TS_UNKNOWN && firstTimestamp < startTimestamp ||
                limitTimestamp != Verifier.TS_UNKNOWN && lastTimestamp > limitTimestamp)
                dlnr.blockOutOfTimeRange (this, rdb, startTimestamp, limitTimestamp);
        }

        index = blocks.toArray(new RawDataBlock[blocks.size()]);
        numEntities = index.length;

        if (blockOffset != physicalLength)
            dlnr.fileLengthDiscrepancy (this, blockOffset, physicalLength);
    }     

    public RawDataBlock         getBlock (int idx) {
        return index [idx];
    }

    @Override
    public int                  getEntity (int idx) {
        return index [idx].getEntity ();
    }
    
    public long                 getMinTimestamp () {
        return minTimestamp;
    }

    public long                 getMaxTimestamp () {
        return maxTimestamp;
    }

    public long                 getPhysicalLength () {
        return physicalLength;
    }        

    public void                 verify (
            DiagListener                dlnr,
            long                        startTimestamp,
            long                        limitTimestamp
    )
            throws IOException
    {
        this.dlnr = dlnr;
        this.dlnr.processingFile(this);
        read(startTimestamp, limitTimestamp, true);

    }

    protected void                 read (
        long                        startTimestamp,
        long                        limitTimestamp,
        boolean                     isVerify
    )
        throws IOException
    {
        byte []                     buffer = new byte [1 << 20];
        MemoryDataInput             mdi = new MemoryDataInput ();

        Restorer.LOGGER.debug("Read data file: " + path);

        try (BufferedInputStream is = open ()) {
            readIndex (is, startTimestamp, limitTimestamp);

            BlockDecompressor decomp =
                    isCompressed () ? BlockCompressorFactory.createDecompressor(algorithm) : null;

            for (int ient = 0; ient < numEntities; ient++) {
                RawDataBlock            db = getBlock (ient);
                int                     length = db.getDataLength ();
                int                     readLength = 0;

                if (buffer.length < length)
                    buffer = new byte [Util.doubleUntilAtLeast (buffer.length, length)];

                if (decomp == null)
                    readLength = IOUtil.readFully (is, buffer, 0, length);
                else
                    decomp.inflate (is, db.getLengthOnDisk (), buffer, 0, length);

                mdi.setBytes (buffer, 0, length);
                if (isVerify)
                    verifyBlock (mdi, db);

                db.setData(buffer, 0 , length);
            }
        }
    }

    /**
     *  @return Returns start timestamp
     */
    protected boolean         verifyBlock (
        MemoryDataInput         mdi,
        RawDataBlock            db
    )
    {        
        long                prevTimestamp = Long.MIN_VALUE;
        int                 msgIdx = 0;
        
        for (;;) {
            long            ts;
            int             msgOffset = mdi.getPosition ();
                
            try {
                ts = TimeCodec.readNanoTime (mdi);
            } catch (Throwable x) {
                dlnr.unreadableTimestamp (this, db, msgOffset, msgIdx, x);
                return (false);
            }

            if (ts < actualStartTimestamp)
                actualStartTimestamp = ts;

            if (msgIdx == 0) {
                if (ts != db.getFirstTimestamp ())
                    dlnr.firstTimestampMismatch (this, db, msgOffset, msgIdx, ts);     
                
                //  Assume it's recoverable
            }            
            else if (ts < prevTimestamp)
                dlnr.messageOutOfOrder (this, db, msgOffset, msgIdx, prevTimestamp, ts);
            
            prevTimestamp = ts;
            
            if (!mdi.hasAvail ()) {
                dlnr.endOfBlockReadingMessageType (
                    this, db, msgOffset, msgIdx, ts, mdi.getPosition () - msgOffset
                );
                return (false);
            }
            
            int         type = mdi.readUnsignedByte ();
            
            int         size;
            
            int         sizeOffset = mdi.getPosition () - msgOffset;
                
            try {
                size = SymmetricSizeCodec.readForward (mdi);
            } catch (Throwable x) {
                dlnr.unreadableBodySize (
                    this, db, msgOffset, msgIdx, ts, true, sizeOffset, x
                );
                return (false);
            }
            
            int         bodyOffset = mdi.getPosition () - msgOffset;
            
            if (mdi.getAvail () < size) {
                dlnr.endOfBlockReadingBody (
                    this, db, msgOffset, msgIdx, ts, bodyOffset, size
                );
                
                return (false);
            }
            
            mdi.skipBytes (size);
            
            int         tailSizeValue;
            int         tailSizeOffset = mdi.getPosition () - msgOffset;
                            
            try {
                tailSizeValue = SymmetricSizeCodec.readForward (mdi);
            } catch (Throwable x) {
                dlnr.unreadableBodySize (
                    this, db, msgOffset, msgIdx, ts, false, tailSizeOffset, x
                );
                return (false);
            }
            
            if (tailSizeValue != tailSizeOffset) {
                dlnr.tailSizeMismatch (
                    this, db, msgOffset, msgIdx, ts, 
                    tailSizeOffset, tailSizeValue
                );
                return (false);
            }
                
            if (!mdi.hasAvail ()) {
                if (ts != db.getLastTimestamp ())
                    dlnr.lastTimestampMismatch (this, db, msgOffset, msgIdx, ts); 
                
                break;
            }
                        
            msgIdx++;
        }    
        
        return (true);
    }
}