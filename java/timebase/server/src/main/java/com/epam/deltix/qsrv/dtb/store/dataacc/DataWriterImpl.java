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
package com.epam.deltix.qsrv.dtb.store.dataacc;

import com.epam.deltix.qsrv.dtb.store.codecs.*;
import com.epam.deltix.qsrv.dtb.store.pub.*;
import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.util.concurrent.*;
import com.epam.deltix.util.memory.*;
import com.epam.deltix.util.time.GMT;

/**
 *  Lock ordering: this, then current time slice.
 */
public class DataWriterImpl extends BlockAccessorBase implements DataWriter {
    private static final int                    MAX_ATTEMPTS = 10;

    protected EntityFilter                      loadHint;

    private long                                lastWrittenNanos = Long.MAX_VALUE;

    private final MemoryDataOutput              buffer = new MemoryDataOutput ();

    public DataWriterImpl () {
    }
    //
    //  DataWriter IMPLEMENTATION
    //
    @Override
    public synchronized void    open (
        long                        nstime,
        EntityFilter                loadHint
    )
    {
        this.loadHint = loadHint;

        try {
            currentTimeSlice = store.checkOutTimeSliceForInsert (this, nstime);
        } catch (InterruptedException x) {
            throw new UncheckedInterruptedException (x);
        }

        lastWrittenNanos = nstime;
    }

    public synchronized void    open (TimeSlice slice, EntityFilter loadHint) {
        this.loadHint = loadHint;
        this.currentTimeSlice = slice;
    }

    @Override
    public synchronized void         close () {
        loadHint = null;
        lastWrittenNanos = Long.MAX_VALUE;

        super.close ();
    }

    private int                encode(long nstime, int typeCode, TSMessageProducer producer)
    {
        assert Thread.holdsLock(this);

        buffer.reset();

        TimeCodec.writeNanoTime (nstime, buffer);
        buffer.writeUnsignedByte(typeCode);

        buffer.skip(1); // reserve 1 byte to the body length (most common case)

        int         bodyPos = buffer.getPosition ();

        producer.writeBody (buffer);

        int         posBeforeTail = buffer.getPosition();
        int         bodyLength = posBeforeTail - bodyPos;

        //assert  bodyLength > 0;

        int         sizeFieldSize = SymmetricSizeCodec.requiredFieldSize (bodyLength);
        int         extend = sizeFieldSize - 1;

        SymmetricSizeCodec.write (posBeforeTail + extend, buffer);
        int         packetLength = buffer.getPosition () + extend;

        if (sizeFieldSize > 1)
            buffer.insertSpace (bodyPos - 1, extend);

        buffer.seek (bodyPos - 1);
        SymmetricSizeCodec.write (bodyLength, buffer);

        return packetLength;
    }
        
    @Override
    public synchronized void    insertMessage (
        int                         entity,
        long                        nstime,
        int                         typeCode,
        TSMessageProducer           producer
    ) 
    {     
        assertOpen ();

        int packetLength = encode(nstime, typeCode, producer);
        
        for (int attempt = 0; ; attempt++) {
            if (attempt == MAX_ATTEMPTS)
                throw new RuntimeException (
                        "MAX_ATTEMPTS to insert [" + GMT.formatNanos(nstime) + ", having " +  packetLength + " bytes] into " + currentTimeSlice + " denied; aborting."
                );

            try {
                currentTimeSlice.insertNotify (loadHint, this, nstime, packetLength);
            } catch (SwitchTimeSliceException x) {
                clearLinks ();
                currentTimeSlice = x.newTimeSlice;
                continue;
            }
            
            AccessorBlockLink   link = getBlockLink (entity, nstime);
            link.insertMessage (nstime, buffer.getBuffer (), 0, packetLength);
            break;
        }
    }

    @Override
    public synchronized void    appendMessage (
            int                         entity,
            long                        nstime,
            int                         typeCode,
            TSMessageProducer           producer,
            boolean                     truncate
    )
    {
        assertOpen ();

        int packetLength = encode(nstime, typeCode, producer);

        for (int attempt = 0; ; attempt++) {
            if (attempt == MAX_ATTEMPTS)
                throw new RuntimeException (
                        "MAX_ATTEMPTS to insert [" + GMT.formatNanos(nstime) + ", having " +  packetLength + " bytes] into " + currentTimeSlice + " denied; aborting."
                );

            try {
                currentTimeSlice.insertNotify (loadHint, this, nstime, packetLength);
            } catch (SwitchTimeSliceException x) {
                clearLinks ();
                currentTimeSlice = x.newTimeSlice;
                continue;
            }

            AccessorBlockLink link = getBlockLink (entity, nstime);
            if (link.getEndTime() > nstime) {
                if (truncate)
                    link.truncate(nstime + 1);
                else
                    throw new IllegalMessageAppend(link.getEndTime());
            }

            link.insertMessage (nstime, buffer.getBuffer (), 0, packetLength);
            lastWrittenNanos = nstime;

            break;
        }
    }

    //
    //  DAPrivate IMPLEMENTATION
    //
    @Override
    public long                 getCurrentTimestamp () {
        return (lastWrittenNanos);
    }

    @Override
    public synchronized void                 truncate(long nstime, int entity) {
        if (currentTimeSlice != null)
            currentTimeSlice.truncate(nstime, entity, this);
    }

}