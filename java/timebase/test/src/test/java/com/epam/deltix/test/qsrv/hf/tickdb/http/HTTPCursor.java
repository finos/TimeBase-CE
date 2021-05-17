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
package com.epam.deltix.test.qsrv.hf.tickdb.http;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;

import com.sun.xml.bind.IDResolver;
import static com.epam.deltix.qsrv.hf.pub.util.SerializationUtils.readIdentityKey;

import com.epam.deltix.qsrv.hf.tickdb.http.HTTPProtocol;
import com.epam.deltix.qsrv.hf.tickdb.http.SelectRequest;
import com.epam.deltix.qsrv.hf.tickdb.http.TypeTransmission;
import com.epam.deltix.qsrv.hf.tickdb.http.UncheckedException;
import org.xml.sax.SAXException;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptorArray;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.pub.md.UHFJAXBContext;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.io.LittleEndianDataInputStream;
import com.epam.deltix.util.lang.Util;

/**
 *
 */
public class HTTPCursor implements MessageSource<RawMessage> {
    private final DataInput din;
    private final InputStream is;

    private final TypeTransmission typeTransmission;
    private RecordClassSet          recordClassSet;
    private final MyIDResolver idResolver;

    private final ObjectArrayList<RecordClassDescriptor> concreteTypes = new ObjectArrayList<>();
    private final ObjectArrayList<ConstantIdentityKey> entities = new ObjectArrayList<ConstantIdentityKey>();
    private final ArrayList<String> streams = new ArrayList<String>();

    private byte[] streamMsgBuffer = new byte[256];
    private final RawMessage raw = new RawMessage();
    private boolean isWithinMessageBlock = false;

    private boolean isAtEnd = false;
    private long    id; // server cursor id
    private long    commandSerial;
    private long    nextSerial;

    public HTTPCursor(InputStream is, SelectRequest request) {
        this(is, request.isBigEndian, request.typeTransmission, null);
    }

    public void setRecordClassSet(RecordClassSet recordClassSet) {
        this.recordClassSet = recordClassSet;
    }

    public HTTPCursor(InputStream is, SelectRequest request, RecordClassSet recordClassSet) {
        this(is, request.isBigEndian, request.typeTransmission, recordClassSet);
    }

    public HTTPCursor(InputStream is, boolean isBigEndian, TypeTransmission typeTransmission, RecordClassSet recordClassSet) {
        this.is = is;
        this.din = isBigEndian ? new DataInputStream(is) : new LittleEndianDataInputStream(is);
        this.typeTransmission = typeTransmission;
        this.recordClassSet = recordClassSet;
        idResolver = typeTransmission == TypeTransmission.DEFINITION ? new MyIDResolver() : null;
    }

    @Override
    public RawMessage       getMessage() {
        return raw;
    }

    public long             getId() {
        return id;
    }

    @Override
    public boolean          next() {
        if (isAtEnd)
            return (false);

        try {
            if (!isWithinMessageBlock)
                return readNext();
            else
                return readMessage() || readNext();

        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    @Override
    public boolean      isAtEnd() {
        return isAtEnd;
    }

    @Override
    public void         close() {
        Util.close(is);
    }

//    public void         reset(long time) {
//        ResetRequest reset = new ResetRequest(id);
//        reset.time = time;
//    }

    private boolean readNext() throws IOException {
        int command;

        while ((command = is.read()) != -1) {

            switch (command) {
                case HTTPProtocol.CURSOR_BLOCK_ID:
                    this.id = din.readLong();
                    break;

                case HTTPProtocol.COMMAND_BLOCK_ID:
                    this.commandSerial = din.readLong();
                    System.out.println("Command recieved: " + commandSerial);
                    break;

                case HTTPProtocol.TYPE_BLOCK_ID:
                    readType();
                    break;

                case HTTPProtocol.STREAM_BLOCK_ID:
                    readStream();
                    break;

                case HTTPProtocol.INSTRUMENT_BLOCK_ID:
                    int index = din.readShort();
                    entities.add(index, readIdentityKey(din));
                    break;

                case HTTPProtocol.MESSAGE_BLOCK_ID:
                    isWithinMessageBlock = true;
                    if (readMessage())
                        return true;
                    else
                        break;

                case HTTPProtocol.ERROR_BLOCK_ID:
                    final byte code = din.readByte();
                    final String text = din.readUTF();
                    throw new RuntimeException("err=" + code + " " + text);

                case HTTPProtocol.TERMINATOR_BLOCK_ID:
                    return false;

                case HTTPProtocol.PING_BLOCK_ID:
                    break; // just ignore it
                default:
                    throw new IllegalStateException("invalid command=" + command);
            }
        }

        throw new IllegalStateException();
    }

    private boolean     readMessage() throws IOException {
        int size = din.readInt();

        if (size == HTTPProtocol.TERMINATOR_RECORD) {
            isWithinMessageBlock = false;
            return false;
        }

        size -= HTTPProtocol.CURSOR_MESSAGE_HEADER_SIZE;
        if (size < 0)
            throw new IllegalStateException("size=" + size);

        // read: timeStampMs instrument_index type_index body
        raw.setNanoTime(din.readLong());
        final ConstantIdentityKey id = entities.get(din.readShort());
        raw.setSymbol(id.symbol);
        byte typeIndex = din.readByte();
        raw.type = concreteTypes.get(typeIndex);

        byte streamIndex = din.readByte();

        if (streamMsgBuffer.length < size)
            streamMsgBuffer = new byte[Util.doubleUntilAtLeast(streamMsgBuffer.length, size)];

        din.readFully(streamMsgBuffer, 0, size);
        raw.setBytes(streamMsgBuffer, 0, size);

        return true;
    }

    private void readType() throws IOException {
        int type_index = din.readShort();
        if (type_index != concreteTypes.size())
            throw new IOException(
                    "Out-of-order index: " + type_index + "; expected: " +
                            concreteTypes.size()
            );

        final RecordClassDescriptor rcd;
        switch (typeTransmission) {
            case GUID:
                final String guid = din.readUTF();
                rcd = recordClassSet.getContentClass(guid);
                if (rcd == null)
                    throw new IllegalStateException("Type is not found: guid=" + guid);
                break;
            case NAME:
                final String name = din.readUTF();
                rcd = (RecordClassDescriptor) recordClassSet.getClassDescriptor(name);
                if (rcd == null)
                    throw new IllegalStateException("Type is not found: name=" + name);
                break;
            case DEFINITION:
                final String xml = din.readUTF();
                final ClassDescriptorArray cda = unmarshall(xml);
                rcd = (RecordClassDescriptor)cda.getDescriptors()[0];
                break;
            default:
                throw new IllegalStateException("invalid typeTransmission=" + typeTransmission);
        }

        concreteTypes.add(rcd);
    }

    private void readStream() throws IOException {
        int index = din.readByte();
        if (index != -1)
            streams.add(index, din.readUTF());
    }

    private ClassDescriptorArray unmarshall(String xml) {
        try {
            Unmarshaller u = UHFJAXBContext.createUnmarshaller();
            idResolver.delegate = (IDResolver) u.getProperty(IDResolver.class.getName());
            u.setProperty(IDResolver.class.getName(), idResolver);
            return (ClassDescriptorArray)u.unmarshal(new StringReader(xml));
        } catch (JAXBException x) {
            throw new UncheckedException(x);
        }
    }

    public void  waitCommand(long serial) {
        nextSerial = serial;
    }

    private class MyIDResolver extends IDResolver {
        private final HashMap<String, ClassDescriptor> idmap = new HashMap<>();
        private IDResolver delegate;

        @Override
        public void bind(String s, Object o) throws SAXException {
            idmap.put(s, (ClassDescriptor) o);
        }

        @Override
        public Callable<?> resolve(final String s, final Class aClass) throws SAXException {
            return new Callable() {
                public Object call() {
                    final Object o = idmap.get(s);
                    if (o != null)
                        return o;
                    else
                        try {
                            return delegate.resolve(s, aClass).call();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                }
            };
        }
    }
}
