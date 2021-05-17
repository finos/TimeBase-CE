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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.FixedBoundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.stream.AbstractMessageReader;
import com.epam.deltix.qsrv.hf.stream.AbstractMessageWriter;
import com.epam.deltix.util.collections.generated.ByteArrayList;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.io.RandomAccessFileToInputStreamAdapter;
import com.epam.deltix.util.io.RandomAccessFileToOutputStreamAdapter;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.DataExchangeUtils;
import com.epam.deltix.util.memory.MemoryDataOutput;

import java.io.*;

class MessageContainer extends AbstractMessageWriter {

    final static class DataContainer {
        final ByteArrayList data;

        public DataContainer(int capacity) {
            data = new ByteArrayList(capacity);
        }

        public byte[]   getBytes() {
            return data.getInternalBuffer();
        }

        public void     writeTo(OutputStream out) throws IOException {
            out.write(data.getInternalBuffer(), 0, data.size());
        }

        public boolean  readFrom(InputStream in, int length) throws IOException {
            int count = in.read(data.getInternalBuffer(), 0, length);
            data.setSize(count);
            return count == size();
        }

        public static DataContainer createFrom(MemoryDataOutput out) {
            DataContainer c = new DataContainer(out.getSize());
            c.data.addAll(out.getBuffer(), 0, out.getSize());
            return c;
        }

        public void     put(byte[] buffer, int offset, int length) {
            data.clear();
            data.addAll(buffer, offset, length);
        }

        public int      size() {
            return data.size();
        }
    }


    final ObjectArrayList<DataContainer> data = new ObjectArrayList<DataContainer>();
    final MemoryDataOutput buffer = new MemoryDataOutput(8192);
    final CodecFactory factory;
    final RecordClassDescriptor[] descriptors;
    boolean dirty = false;

    public MessageContainer(CodecFactory factory, Class[] classes, RecordClassDescriptor[] descriptors) {
        this.factory = factory;
        this.descriptors = descriptors;

        for (int i = 0; i < classes.length; i++) {
            FixedBoundEncoder encoder = factory.createFixedBoundEncoder(TypeLoaderImpl.DEFAULT_INSTANCE, descriptors[i]);
            addNew(descriptors[i], classes[i], encoder);
        }

        for (RecordClassDescriptor rcd : descriptors)
            addNew(rcd, null, null);
    }

    public MessageContainer(CodecFactory factory, RecordClassDescriptor ... descriptors) {
        this.factory = factory;
        this.descriptors = descriptors;

        for (RecordClassDescriptor rcd : descriptors)
            addNew(rcd, null, null);
    }

    public synchronized boolean add(InstrumentMessage msg) {
        try {
            encode(msg, buffer);

            DataContainer container = DataContainer.createFrom(buffer);
            synchronized (data) {
               data.add(container);
            }

            return (dirty = true);
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    @Override
    protected int           getTypeIndex(RecordClassDescriptor type) {
        int index = super.getTypeIndex(type);
        if (index == -1)
            return (addNew(type, null, null));

        return index;
    }

    @Override
    protected int           getTypeIndex(Class<?> cls) {
        int index = super.getTypeIndex(cls);

        if (index == -1) {
            TypeLoaderImpl loader = new TypeLoaderImpl(cls.getClassLoader());

            // search for class
            for (int i = 0; i < descriptors.length; i++) {
                RecordClassDescriptor cd = descriptors[i];

                try {
                    if (cls.equals(loader.load(cd))) {
                        FixedBoundEncoder encoder = factory.createFixedBoundEncoder(loader, cd);
                        set(i, cd, cls, encoder);
                        return i;
                    }
                } catch (ClassNotFoundException e) {
                }
            }
        }

        return index;
    }

    public synchronized boolean     isDirty() {
        return dirty;
    }

    protected synchronized void     setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public void write(File f) throws IOException {
        byte[] tmp = new byte[8];

        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(f, "rw");
            raf.write(tmp);
            OutputStream out = new RandomAccessFileToOutputStreamAdapter(raf);
            long size = write(out);
            DataExchangeUtils.writeLong(tmp, 0, size);
            raf.seek(0);
            raf.write(tmp);
            raf.close();
        } finally {
            setDirty(false);
            Util.close(raf);
        }
    }

    public void read(File f) throws IOException {
        byte[] tmp = new byte[8];

        if (!f.exists())
            return;

        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(f, "r");
            raf.read(tmp);
            read(new RandomAccessFileToInputStreamAdapter(raf),
                    DataExchangeUtils.readLong(tmp, 0));
            raf.close();
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        } finally {
            Util.close(raf);
        }
    }

    protected long write(OutputStream out) throws IOException {
        UniqueMessageContainer.writeHeader(out, null, descriptors);

        int total = 0;

        synchronized (data) {
            for (DataContainer bytes : data) {
                final int size = bytes.size();

                MessageSizeCodec.write(size, out);
                bytes.writeTo(out);
                total += size;
            }
        }

        setDirty(false);

        return total;
    }

    protected void read(InputStream in, long size) throws IOException {

        synchronized (data) {
            data.clear();

            AbstractMessageReader.readHeader(in);
            int total = 0;
            while (total < size) {
                int length = MessageSizeCodec.read(in);

                DataContainer c = new DataContainer(length);
                if (c.readFrom(in, length)) {
                    data.add(c);
                    total += length;
                }
            }
        }

    }
}