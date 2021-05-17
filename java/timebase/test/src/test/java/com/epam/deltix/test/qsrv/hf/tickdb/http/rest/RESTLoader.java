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
package com.epam.deltix.test.qsrv.hf.tickdb.http.rest;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.qsrv.hf.blocks.InstrumentToObjectMap;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.codec.RecordTypeMap;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.http.HTTPProtocol;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingOptions;
import com.epam.deltix.util.io.LittleEndianDataOutputStream;

import java.io.*;
import java.lang.reflect.Constructor;
import java.net.Socket;
import java.util.zip.GZIPOutputStream;

import static com.epam.deltix.qsrv.hf.pub.util.SerializationUtils.writeIdentityKey;

/**
 *
 */
public class RESTLoader implements MessageChannel<RawMessage> {
    private final Socket                        socket;
    private final DataInput                     din;
    private final DataOutput                    dout;
    private final GZIPOutputStream              gzip_out;
    private boolean                             isWithinMessageBlock = false;
    private final InstrumentToObjectMap<Integer> instrumentMap = new InstrumentToObjectMap<>();
    private final RecordTypeMap<RecordClassDescriptor> typeMap;

    private final Object                        writeLock = new Object();

    public RESTLoader(Socket socket, boolean useCompression, boolean isBigEndian,
                      String streamName, RecordClassDescriptor[] rcds) throws IOException
    {
        this.socket = socket;
        InputStream is = socket.getInputStream();
        OutputStream os = socket.getOutputStream();

        os.write(HTTPProtocol.REQ_UPLOAD_DATA);
        os.write(useCompression ? 1 : 0);

        if (useCompression) {
            os = gzip_out = new GZIPOutputStream(os, 0x1000, true);
        } else {
            gzip_out = null;
        }

        dout = isBigEndian ? new DataOutputStream(os) : new LittleEndianDataOutputStream(os);

        // endianness version stream write_mode allowed_errors
        synchronized (writeLock) {
            dout.writeByte(isBigEndian ? 1 : 0);
            dout.writeUTF(streamName);
            dout.write(LoadingOptions.WriteMode.REWRITE.ordinal());
        }

        din = new DataInputStream(is);
        typeMap = new RecordTypeMap<>(rcds);
    }

    @Override
    public void send(RawMessage msg) {
        final Integer boxedIndex = instrumentMap.get(msg);
        final int index;
        try {
            if (boxedIndex == null) {
                writeInsrumentBlock(msg);
                index = instrumentMap.size();
                instrumentMap.put(msg, instrumentMap.size());
            } else
                index = boxedIndex;

            beginMessageBlock();
            writeMessageRecord(msg, typeMap.getCode(msg.type), index);
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        try {
            endMessageBlock();
            writeTerminatorBlock();

            // must flush GZIP buffer here to fix EOFException: Unexpected end of ZLIB input stream
            if(gzip_out != null)
                gzip_out.finish();

            for (;;) {
                byte respId = din.readByte();
                if (respId == HTTPProtocol.ERROR_BLOCK_ID) {
                    HTTPProtocol.LOGGER.warning("Loading error: " + din.readInt() + " (" + din.readUTF() + ")");
                } else if (respId == HTTPProtocol.KEEP_ALIVE_ID) {
                    //keep alive packet
                } else if (respId == HTTPProtocol.RESPONSE_BLOCK_ID) {
                    int resp = din.readInt();
                    if (resp == HTTPProtocol.RESP_ERROR) {
                        String clsName = din.readUTF();
                        String message = din.readUTF();

                        Exception ex;
                        try {
                            Constructor<?> constructor = Class.forName(clsName).getConstructor(String.class);
                            ex = (Exception) constructor.newInstance(message);
                            throw ex;
                        } catch (Exception e) {
                            throw new com.epam.deltix.util.io.UncheckedIOException(clsName + ": " + message);
                        }
                    }

                    break;
                } else {
                    throw new com.epam.deltix.util.io.UncheckedIOException("Unknown response code.");
                }
            }
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                throw new com.epam.deltix.util.io.UncheckedIOException(e);
            }
        }
    }

    private void beginMessageBlock() throws IOException {
        if (!isWithinMessageBlock) {
            synchronized (writeLock) {
                dout.write(HTTPProtocol.MESSAGE_BLOCK_ID);
            }
            isWithinMessageBlock = true;
        }
    }

    private void endMessageBlock() throws IOException {
        if (isWithinMessageBlock) {
            synchronized (writeLock) {
                dout.writeInt(HTTPProtocol.TERMINATOR_RECORD);
            }
            isWithinMessageBlock = false;
        }
    }

    private void writeMessageRecord(RawMessage raw, int typeIndex, int entityIndex) throws IOException {
        int msg_size = raw.length;
        if (msg_size >= HTTPProtocol.MAX_MESSAGE_SIZE)
            throw new IllegalStateException("invalid message size " + msg_size);

        // write: message_size timeStampMs instrument_index type_index body

        msg_size += (8 + 2 + 1);
        synchronized (writeLock) {
            dout.writeInt(msg_size);
            dout.writeLong(raw.getNanoTime());
            dout.writeShort(entityIndex);
            dout.write(typeIndex);
            dout.write(raw.getData(), raw.offset, raw.length);
        }
    }

    private void writeInsrumentBlock(RawMessage raw) throws IOException {
        endMessageBlock();

        synchronized (writeLock) {
            dout.write(HTTPProtocol.INSTRUMENT_BLOCK_ID);
            writeIdentityKey(raw, dout);
        }
    }

    private void writeTerminatorBlock() throws IOException {
        synchronized (writeLock) {
            dout.write(HTTPProtocol.TERMINATOR_BLOCK_ID);
        }
    }
}