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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.TimeCodec;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.io.UncheckedIOException;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Date: Feb 1, 2010
 */
class UniqueMessageContainer extends MessageContainer {

    private final UnboundDecoder decoder;

    private final Comparator<Object>        pkComparator =
        new Comparator <Object> () {
            private final MemoryDataInput in1 = new MemoryDataInput ();
            private final MemoryDataInput in2 = new MemoryDataInput ();

            private StringBuilder         symbol1 = new StringBuilder();
            private StringBuilder         symbol2 = new StringBuilder();

            public int      compare (Object o1, Object o2) {

                if (o1 instanceof DataContainer) {
                    DataContainer c = (DataContainer) o1;
                    in1.setBytes(c.getBytes(), 0, c.size());
                } else if (o1 instanceof MemoryDataOutput) {
                    MemoryDataOutput out = (MemoryDataOutput) o1;
                    in1.setBytes(out.getBuffer(), 0, out.getSize());
                }
                int type1 = skipHeader(in1, symbol1);

                if (o2 instanceof DataContainer) {
                    DataContainer c = (DataContainer) o2;
                    in2.setBytes(c.getBytes(), 0, c.size());
                } else if (o2 instanceof MemoryDataOutput) {
                    MemoryDataOutput out = (MemoryDataOutput) o2;
                    in2.setBytes(out.getBuffer(), 0, out.getSize());
                }
                int type2 = skipHeader(in2, symbol2);

                int code = decoder.comparePrimaryKeys(in1, in2);
                if (code == -2) { // no primary keys defined
                    if (type1 == type2)
                        return Util.compare(symbol1, symbol2, true);

                    return Util.compare(type1, type2);
                }

               return code;
            }

            public int skipHeader(MemoryDataInput in, StringBuilder sb) {
               sb.setLength(0);
               try {
                   TimeCodec.skipTime(in); // time
                   int type = in.readByte(); // message type
                   IOUtil.readUTF(in, sb);
                   return type;
               } catch (IOException e) {
                   throw new UncheckedIOException(e);
               }
            }
        };

    UniqueMessageContainer(CodecFactory factory, RecordClassDescriptor... descriptors) {
        super(factory, descriptors);
        this.decoder = factory.createPolyUnboundDecoder(descriptors);
    }

    public DataContainer           getMessageData(int index) {
        return data.get(index);
    }

    public boolean                  hasData(int index) {
        synchronized (data) {
            return index < data.size();
        }
    }

    public synchronized boolean     contains(InstrumentMessage msg) {
        try {
            encode(msg, buffer);

            final int   size = buffer.getSize ();

            synchronized (data) {
                int pos =
                        Arrays.binarySearch(
                                data.getInternalBuffer(),
                                0,
                                data.size(),
                                buffer,
                                pkComparator
                        );

                if (pos >= 0) {
                    DataContainer existing = data.get(pos);

                    if (Util.arrayequals(existing.getBytes(), 0, size, buffer.getBuffer(), 0, size))
                        return true;
                }

                return false;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    public synchronized boolean     add(InstrumentMessage msg, boolean ignoreDublicates) {
        try {
            encode(msg, buffer);

            final int   size = buffer.getSize ();
            synchronized (data) {
                int                 pos =
                    Arrays.binarySearch (
                        data.getInternalBuffer (),
                        0,
                        data.size (),
                        buffer,
                        pkComparator
                    );

                if (pos >= 0) {
                    DataContainer existing = data.get(pos);

                    if (ignoreDublicates) {
                        if (Util.arrayequals(existing.getBytes(), 0, size, buffer.getBuffer(), 0, size))
                            return false;
                    }

                    existing.put(buffer.getBuffer(), 0, size);
                } else {
                    data.add (-pos - 1, DataContainer.createFrom(buffer));
                }
            }

            dirty = true;
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean                  add(InstrumentMessage msg) {
        return add(msg, true);
    }

    public synchronized boolean     remove(InstrumentMessage msg) {
        try {
            encode(msg, buffer);

            synchronized (data) {
                int                 pos =
                        Arrays.binarySearch (
                                data.getInternalBuffer (),
                                0,
                                data.size (),
                                buffer,
                                pkComparator
                        );

                if (pos >= 0) {
                    data.remove(pos);
                    setDirty(true);
                    return true;
                }

            }

            return false;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}