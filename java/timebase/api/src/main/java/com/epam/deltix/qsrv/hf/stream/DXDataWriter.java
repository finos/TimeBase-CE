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
package com.epam.deltix.qsrv.hf.stream;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.qsrv.util.archive.DXDataEntry;
import com.epam.deltix.qsrv.util.archive.DXDataOutputStream;
import com.epam.deltix.qsrv.util.archive.DXHeaderEntry;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.DataExchangeUtils;
import com.epam.deltix.util.memory.MemoryDataOutput;
import org.apache.commons.compress.archivers.ArchiveEntry;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class DXDataWriter extends AbstractMessageWriter implements MessageChannel<InstrumentMessage>  {
    
    private DXDataOutputStream  out;
    private final DXHeaderEntry header = new DXHeaderEntry("header", new byte[16]);
    private GZIPOutputStream    gzip;

    protected final MemoryDataOutput    buffer = new MemoryDataOutput (4096);
    long startTime = Long.MIN_VALUE;
    long endTime = Long.MIN_VALUE;

    public DXDataWriter(File file) throws IOException {
        out = new DXDataOutputStream(file);
        out.putHeaderEntry(header);
    }

    public void         startBlock(String key) throws IOException {
        
        final ArchiveEntry meta = new DXDataEntry(key + ".xml");
        out.putArchiveEntry(meta);
        Protocol.writeTypes(new DataOutputStream(out), getTypes());
        out.closeArchiveEntry();

        out.putArchiveEntry(new DXDataEntry(key));

        gzip = new GZIPOutputStream(out, 8192);
    }

    public void         startBlock(String key, RecordClassDescriptor ... descriptors)
        throws IOException
    {
        // clear previous types
        numTypes = 0;

        for (RecordClassDescriptor type : descriptors)
            addNew(type, null, null);

        startBlock(key);
    }

    public void         endBlock() throws IOException {
        if (gzip != null) {
            gzip.finish();

            DataExchangeUtils.writeLong(header.data, 0, startTime);
            DataExchangeUtils.writeLong(header.data, 8, endTime);
            out.putHeaderEntry(header);

            out.closeArchiveEntry();
            out.flush();
            gzip = null;
        }
    }

//    @Override
//    protected int       getTypeIndex(RecordClassDescriptor type) {
//        int index = super.getTypeIndex(type);
//        return index == -1 ? addNew(type, null, null) : index;
//    }

    @Override
    public void                     send (InstrumentMessage msg) {
        long nanoTime = msg.getNanoTime();
        
        if (startTime == Long.MIN_VALUE)
            startTime = nanoTime;

        if (endTime < nanoTime)
            endTime = nanoTime;
        
        try {
            encode(msg, buffer);
            
            final int   size = buffer.getSize ();
            MessageSizeCodec.write(size, gzip);
            gzip.write(buffer.getBuffer(), 0, size);
            
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    public long                     count() {
        return out.count();
    }

    @Override
    public void close() {
        Util.close(out);
    }
}