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
import java.io.DataOutput;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.zip.GZIPOutputStream;

import static com.epam.deltix.qsrv.hf.pub.util.SerializationUtils.writeIdentityKey;

import com.epam.deltix.qsrv.hf.tickdb.http.HTTPProtocol;
import com.epam.deltix.qsrv.hf.tickdb.http.LoadResponse;
import com.epam.deltix.qsrv.hf.tickdb.http.TBJAXBContext;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.qsrv.hf.blocks.InstrumentToObjectMap;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.codec.RecordTypeMap;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

/**
 *
 */
public class HTTPLoader implements MessageChannel<RawMessage> {
    private final HttpURLConnection conn;
    private final DataOutput dout;
    private final GZIPOutputStream gzip_out;
    private boolean isWithinMessageBlock = false;
    private final InstrumentToObjectMap<Integer> instrumentMap = new InstrumentToObjectMap<>();
    private final RecordTypeMap<RecordClassDescriptor> typeMap;

    public HTTPLoader(HttpURLConnection conn, DataOutput dout, GZIPOutputStream gzip_out, RecordClassDescriptor[] rcds) {
        this.conn = conn;
        this.dout = dout;
        this.gzip_out = gzip_out;
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

            // take care about response
            int rc = conn.getResponseCode();
            if (rc != 200)
                throw new RuntimeException("HTTP rc=" + rc + " " + conn.getResponseMessage());

            try {
                final Unmarshaller um = TBJAXBContext.createUnmarshaller();
                LoadResponse lr = (LoadResponse) um.unmarshal(conn.getInputStream());
                System.out.println(lr.wasError + "," + lr.responseMessage + "\n" + lr.details);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    private void beginMessageBlock() throws IOException {
        if (!isWithinMessageBlock) {
            dout.write(HTTPProtocol.MESSAGE_BLOCK_ID);
            isWithinMessageBlock = true;
        }
    }

    private void endMessageBlock() throws IOException {
        if (isWithinMessageBlock) {
            dout.writeInt(HTTPProtocol.TERMINATOR_RECORD);
            isWithinMessageBlock = false;
        }
    }

    private void writeMessageRecord(RawMessage raw, int typeIndex, int entityIndex) throws IOException {
        int msg_size = raw.length;
        if (msg_size >= HTTPProtocol.MAX_MESSAGE_SIZE)
            throw new IllegalStateException("invalid message size " + msg_size);

        // write: message_size timeStampMs instrument_index type_index body

        msg_size += (8 + 2 + 1);
        dout.writeInt(msg_size);
        dout.writeLong(raw.getNanoTime());
        dout.writeShort(entityIndex);
        dout.write(typeIndex);
        dout.write(raw.getData(), raw.offset, raw.length);
    }

    private void writeInsrumentBlock(RawMessage raw) throws IOException {
        endMessageBlock();
        dout.write(HTTPProtocol.INSTRUMENT_BLOCK_ID);
        writeIdentityKey(raw, dout);
    }

    private void writeTerminatorBlock() throws IOException {
        dout.write(HTTPProtocol.TERMINATOR_BLOCK_ID);
    }
}
