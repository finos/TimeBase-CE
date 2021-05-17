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

import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.test.qsrv.hf.tickdb.http.BaseTest;
import com.epam.deltix.test.qsrv.hf.tickdb.http.HTTPCursor;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.FixedBoundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.http.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamScope;
import com.epam.deltix.util.memory.MemoryDataOutput;
import com.epam.deltix.util.time.GMT;
import com.epam.deltix.util.time.TimeConstants;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.net.HttpURLConnection;
import java.util.zip.GZIPInputStream;

/**
 *
 */
public class TestRESTLoad extends BaseTest {

    private static final String[] SYMBOLS = {"AAPL-HTTP"};

    @Test
    public void loadRestBars() throws Exception {
        loadRestBars(true, false, null, null);
    }

    @Test
    public void loadOutBars() throws Exception {
        loadOutOfOrder(false, false, null, null);
    }

    private static void loadRestBars(boolean useCompression, boolean isBigEndian, String user, String password) throws Exception {
        String streamName = "1min";

        StreamOptions options = StreamOptions.fixedType(StreamScope.DURABLE, streamName, null, 0, StreamConfigurationHelper.mkUniversalBarMessageDescriptor());
        createStream(streamName, options);

        final BarMessage bar = new BarMessage();
        bar.setTimeStampMs(GMT.parseDateTime("2013-10-08 09:00:00").getTime());
        bar.setOpen(1.0);
        bar.setClose(1.0);
        bar.setHigh(1.0);
        bar.setLow(1.0);
        bar.setVolume(1.0);

        final RecordClassSet rcs = Utils.requestSchema(getPath("tb/xml"), streamName, user, password);
        final RecordClassDescriptor type = rcs.getTopType(0);
        final FixedBoundEncoder encoder = CodecFactory.COMPILED.createFixedBoundEncoder(TypeLoaderImpl.DEFAULT_INSTANCE, type);
        final MemoryDataOutput mdo = new MemoryDataOutput();

        final MessageChannel<RawMessage> loader = Utils.getRestLoader(
            TB_HOST, runner.getPort(), useCompression, isBigEndian, streamName, rcs.getTopTypes(), user, password);
        RawMessage raw = new RawMessage();
        raw.type = type;

        for (int i = 0; i < 5000000; i++) {
            for (String symbol : SYMBOLS) {
                bar.setSymbol(symbol);

                mdo.reset();
                encoder.encode(bar, mdo);
                raw.setSymbol(bar.getSymbol());
                raw.setTimeStampMs(bar.getTimeStampMs());
                raw.setBytes(mdo);
                loader.send(raw);

                bar.setOpen(bar.getOpen() + 1);
            }

            bar.setTimeStampMs(bar.getTimeStampMs() + TimeConstants.MINUTE);
            bar.setHigh(bar.getHigh() + 1);
            bar.setLow(bar.getLow() + 0.1);
        }

        loader.close();

        //assert selectCount("1min") == 5000000;
    }

    private static void loadOutOfOrder(boolean useCompression, boolean isBigEndian, String user, String password) throws Exception {
        String streamName = "1minOutOfOrder";

        StreamOptions options = StreamOptions.fixedType(StreamScope.DURABLE, streamName, null, 0, StreamConfigurationHelper.mkUniversalBarMessageDescriptor());
        createStream(streamName, options);

        final RecordClassSet rcs = Utils.requestSchema(getPath("tb/xml"), streamName, user, password);
        final BarMessage bar = new BarMessage();
        bar.setSymbol(SYMBOLS[0]);
        bar.setTimeStampMs(GMT.parseDateTime("2013-10-08 09:00:00").getTime());
        bar.setOpen(1.0);
        bar.setClose(1.0);
        bar.setHigh(1.0);
        bar.setLow(1.0);
        bar.setVolume(1.0);
        final RecordClassDescriptor type = rcs.getTopType(0);
        final FixedBoundEncoder encoder = CodecFactory.COMPILED.createFixedBoundEncoder(TypeLoaderImpl.DEFAULT_INSTANCE, type);
        final MemoryDataOutput mdo = new MemoryDataOutput();

        final MessageChannel<RawMessage> loader = Utils.getRestLoader(
            TB_HOST, runner.getPort(), useCompression, isBigEndian, streamName, rcs.getTopTypes(), user, password);
        RawMessage raw = new RawMessage();
        raw.type = type;

        for (int i = 0; i < 5; i++) {
            mdo.reset();
            encoder.encode(bar, mdo);
            raw.setSymbol(bar.getSymbol());
            raw.setTimeStampMs(bar.getTimeStampMs());
            raw.setBytes(mdo);
            loader.send(raw);

            bar.setOpen(bar.getOpen() + 1);
            bar.setTimeStampMs(bar.getTimeStampMs() - TimeConstants.MINUTE);
            bar.setHigh(bar.getHigh() + 1);
            bar.setLow(bar.getLow() + 0.1);
        }

        loader.close();
    }

    //data selection
    public static int selectCount(String key) throws JAXBException, IOException {
        SelectRequest sr = new SelectRequest();
        sr.streams = new String[] { key };
        sr.reverse = true;
        sr.symbols = null;
        sr.isBigEndian = false;
        sr.useCompression = false;
        sr.typeTransmission = TypeTransmission.DEFINITION;
        sr.from = Long.MIN_VALUE;
        sr.to = Long.MAX_VALUE;

        final long ts0 = System.currentTimeMillis();
        RecordClassSet set = runner.getTickDb().getStream(key).getStreamOptions().getMetaData();
        //int count = selectCount(sr, );

        int count = 0;
        try (HTTPCursor cursor = select(sr)) {
            cursor.setRecordClassSet(set);

            while (cursor.next()) {
                count++;
            }
            cursor.close();
        }

        return count;
    }

    private static HTTPCursor select(SelectRequest select) throws IOException, JAXBException {

        final HttpURLConnection conn = (HttpURLConnection) URL.openConnection();

        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();

        marshaller.marshal(select, os);
        int rc = conn.getResponseCode();
        if (rc != 200) {
            System.out.println("HTTP rc=" + rc + " " + conn.getResponseMessage());
            return null;
        }

        // Content-Encoding [gzip]
        final InputStream is = HTTPProtocol.GZIP.equals(conn.getHeaderField(HTTPProtocol.CONTENT_ENCODING)) ?
            new GZIPInputStream(conn.getInputStream()) : conn.getInputStream();

        return new HTTPCursor(is, select);
    }
}
