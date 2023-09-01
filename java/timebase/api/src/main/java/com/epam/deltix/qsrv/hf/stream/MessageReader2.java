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

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

import com.epam.deltix.data.stream.ConsumableMessageSource;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.TypeSubscriptionController;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.EntitySubscriptionController;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.io.*;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.Interval;

/**
 *
 */
public class MessageReader2
        extends AbstractMessageReader
        implements ConsumableMessageSource<InstrumentMessage>,
        TypeSubscriptionController, EntitySubscriptionController
{
    private final InputStream                   in;
    private final ByteCountingInputStream       bcin;
    private final long                          fileSize;
    private boolean                             atEnd = false;
    protected Interval                          periodicity;
    protected IdentityKey                       currentEntity = new InstrumentKey();

    private Set<String>                         subscribedTypeNames = null;
    private Set<IdentityKey>                    subscribedEntities = null;

    public static MessageReader2    createRaw (File f) throws IOException {
        return create(f, 1 << 20, null);
    }

    public static MessageReader2    create (File f, TypeLoader loader) throws IOException {
        return create(f, 1 << 20, loader);
    }

    public static MessageReader2 create(File f, int bufferSize, TypeLoader loader) throws IOException {
        return new MessageReader2(
            new FileInputStream (f),
            f.length (),
            Protocol.isDeflatedMessageStream (f),
            bufferSize,
            loader
        );
    }

    public static MessageReader2 create(File f, RecordClassDescriptor[] types) throws IOException {
        MessageFileHeader header = readHeader(f);

        RecordClassDescriptor[] fileTypes = header.getTypes();
        if (!MessageProcessor.isCompatible(fileTypes, types))
            throw new IllegalArgumentException("Input class descriptors " + Arrays.toString(fileTypes)
                    + " is not compatible with output " + Arrays.toString(types));
        
        return new MessageReader2(
            new FileInputStream (f),
            f.length (),
            Protocol.isDeflatedMessageStream (f),
            1 << 20,
            null,
            MessageProcessor.sort(fileTypes, types));
            //MessageProcessor.findMatches(types, fileTypes));
    }

    public static byte readVersion(File f) throws IOException {
        InputStream in = null;

        try {
            in = new FileInputStream(f);

            if (Protocol.isDeflatedMessageStream (f))
                in = new GZIPInputStream (in, 100);

            in = new BufferedInputStream (in, 100);

            return readVersion(in);

        } finally {
            Util.close (in);
        }
    }
   
    protected RecordClassDescriptor[] readHeader()
            throws IOException
    {
        MessageFileHeader header = readHeader(this.in);
        this.periodicity = header.periodicity;
        return header.getTypes();
    }

    static MessageFileHeader readHeader(File f)
            throws IOException
    {
        InputStream in = null;

        try {
            in = new FileInputStream(f);

            if (Protocol.isDeflatedMessageStream (f))
                in = new GZIPInputStream (in, 100);
            in = new BufferedInputStream (in, 100);

            return readHeader(in);
        } finally {
            Util.close (in);
        }
    }

     public MessageReader2 (
        InputStream                 in,
        long                        inLength,
        boolean                     unzip,
        int                         bufferSize,
        TypeLoader                 bindLoader) throws IOException
     {
         this(in, inLength, unzip, bufferSize, bindLoader, null);
     }
    
    protected MessageReader2 (
        InputStream                 in,
        long                        inLength,
        boolean                     unzip,
        int                         bufferSize,
        TypeLoader                  bindLoader,
        RecordClassDescriptor[]     types
    )
        throws IOException
    {
        this.fileSize = inLength;
        
        in = bcin = new ByteCountingInputStream (in);

        try {
            if (unzip) {
                bufferSize /= 2;
                in = new GZIPInputStream (in, bufferSize);
            }

            if (bufferSize > 0)
                in = new BufferedInputStream (in, bufferSize);

            this.in = in;
            in = null;  // Signal ok
        } finally {
            Util.close (in);
        }

        // read type descriptors and header 
        RecordClassDescriptor[] rcd = readHeader();
        this.types = types == null ? rcd : types;

        if (bindLoader == null) {
            rawMsg = new RawMessage ();
            
            rawMsg.setSymbol(symbol);
            
            curMsg = rawMsg;

            decoders = null;
            messages = null;
        }
        else{
            rawMsg = null;

            final int       numTypes = this.types.length;

            decoders = new FixedExternalDecoder [numTypes];
            messages = new InstrumentMessage[numTypes];

            for (int ii = 0; ii < numTypes; ii++) {
                final FixedExternalDecoder  dec =
                    CodecFactory.COMPILED.createFixedExternalDecoder (bindLoader, this.types [ii]);

                final InstrumentMessage msg = (InstrumentMessage) dec.getClassInfo().newInstance ();

                dec.setStaticFields (msg);

                msg.setSymbol(symbol);

                decoders [ii] = dec;                    
                messages [ii] = msg;
            }
        }
    }

    protected MessageReader2 (
        InputStream             input,
        long                        inLength,
        RecordClassDescriptor[]     types
    )
        throws IOException
    {
        this.fileSize = inLength;
        this.types = types;

        in = bcin = new ByteCountingInputStream (input);

        curMsg = rawMsg = new RawMessage ();
        rawMsg.setSymbol(symbol);

        decoders = null;
        messages = null;
    }

    public boolean                      isAtEnd () {
        return (atEnd);
    }

    @Override
    public synchronized boolean         next () {
        try {
            for (;;) {
                readMessage();

                if (subscribedEntities != null && !subscribedEntities.contains (currentEntity))
                    continue;

                RecordClassDescriptor currentType = getCurrentType();

                if (subscribedTypeNames != null &&
                    !subscribedTypeNames.contains (currentType.getName ()))
                    continue;

                return (true);
            }
            
        } catch (EOFException x) {
            atEnd = true;
            return (false);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        }
    }

    public void                         close () {
        Util.close (in);
    }

    @Override
    public double                       getProgress () {
        if (bcin == null)
            return 1.0;
        
        return (((double) bcin.getNumBytesRead ()) / fileSize);
    }

    private void                        readMessage () throws IOException {
        final int length = MessageSizeCodec.read (in);

        if (length < 0)
            throw new EOFException ();

        checkBuffer(length);

        for (int pos = 0, remain = length; remain > 0;) {
            int num = in.read (bytes, pos, remain);

            if (num < 0)
                throw new EOFException ();

            pos += num;
            remain -= num;
        }

        buffer.setBytes (bytes, 0, length);

        final long              nanos = TimeCodec.readNanoTime(buffer);

        // deprecated code
        int code = buffer.readUnsignedByte();
        //final InstrumentType insType = ITYPE_TYPES [buffer.readUnsignedByte()];

        symbol.setLength (0);
        IOUtil.readUTF (buffer, symbol);

        curTypeCode = buffer.readUnsignedByte ();

        if (decoders == null) {
            rawMsg.type = types [curTypeCode];
            rawMsg.data = bytes;

            final int       offset = buffer.getPosition ();

            rawMsg.offset = offset;
            rawMsg.length = length - offset;
        }
        else {
            curMsg = messages [curTypeCode];
            decoders [curTypeCode].decode (buffer, curMsg);
        }

        curMsg.setNanoTime(nanos);
    }

    // TypeSubscriptionController

    @Override
    public synchronized void subscribeToAllTypes() {
        subscribedTypeNames = null;
    }

    @Override
    public synchronized void addTypes(String... names) {
        if (subscribedTypeNames == null)
            subscribedTypeNames = new HashSet <String> ();

        subscribedTypeNames.addAll(Arrays.asList(names));
    }

    @Override
    public void setTypes(String... names) {
        if (subscribedTypeNames == null)
            subscribedTypeNames = new HashSet <String> ();
        else
            subscribedTypeNames.clear();
        
        subscribedTypeNames.addAll(Arrays.asList(names));
    }

    @Override
    public synchronized void removeTypes(String... names) {
        if (subscribedTypeNames != null) {
            for (String s : names)
                subscribedTypeNames.remove (s);
        }
    }

    // EntitySubscriptionController

    @Override
    public synchronized void subscribeToAllEntities() {
        subscribedEntities = null;
    }

    @Override
    public synchronized void clearAllEntities() {
        if (subscribedEntities == null)
            subscribedEntities = new HashSet <IdentityKey> ();

        subscribedEntities.clear();

    }

    @Override
    public synchronized void addEntity(IdentityKey id) {
        if (subscribedEntities == null)
            subscribedEntities = new HashSet <IdentityKey> ();

        subscribedEntities.add(id);
    }

    @Override
    public void addEntities(IdentityKey[] ids, int offset, int length) {
        if (subscribedEntities == null)
            subscribedEntities = new HashSet <IdentityKey> ();

        subscribedEntities.addAll(Arrays.asList(ids));
    }

    @Override
    public void removeEntity(IdentityKey id) {
        if (subscribedEntities != null)
            subscribedEntities.remove(id);
    }

    @Override
    public void removeEntities(IdentityKey[] ids, int offset, int length) {
        if (subscribedEntities != null) {
            for (int i = offset; i < offset + length; i++)
                subscribedEntities.remove(ids[i]);
        }
    }

    public static void  main (String [] args) throws IOException {
        MessageReader2 rd = MessageReader2.createRaw(new File(args[0]));

        long t0 = System.currentTimeMillis();
        boolean timeit = args.length == 2 && args[1].equals("-t");
        long count = 0;

        while (rd.next()) {
            if (!timeit)
                System.out.println(rd.getMessage());
            count++;
        }

        if (timeit) {
            long t1 = System.currentTimeMillis();
            System.out.printf("%,d messages in %,d milliseconds\n", count, t1 - t0);
        }
    }
}