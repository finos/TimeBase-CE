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

import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.http.HTTPProtocol;
import com.epam.deltix.qsrv.hf.tickdb.http.TBJAXBContext;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.qsrv.test.messages.BestBidOfferMessage;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.streaming.MessageChannel;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.FixedBoundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.pub.md.UHFJAXBContext;
import com.epam.deltix.qsrv.hf.tickdb.http.stream.GetSchemaRequest;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingOptions.WriteMode;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamScope;
import com.epam.deltix.util.codec.Base64EncoderEx;
import com.epam.deltix.util.io.LittleEndianDataOutputStream;
import com.epam.deltix.util.memory.MemoryDataOutput;
import com.epam.deltix.util.time.GMT;
import com.epam.deltix.util.time.TimeConstants;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPOutputStream;

/**
 *
 */
public class TestHttpLoad  extends BaseTest {
    private final static int SIZE_1MB = 0x100000; // 1MB
    //private static final String[] SYMBOLS = {"AAPL-HTTP", "IBM-HTTP", "MSFT-HTTP"};
    private static final String[] SYMBOLS = {"AAPL-HTTP"};

//    public static void main(String[] args) throws Exception {
//        final long ts0 = System.currentTimeMillis();
//
//        try {
//            final boolean isBigEndian = args.length > 0 && "big".equals(args[0]);
//            final boolean useCompression = args.length > 1 && HTTPProtocol.GZIP.equals(args[1]);
//            final String user = args.length > 2 ? args[2] : null;
//            final String password = args.length > 3 ? args[3] : "";
//            loadBars(useCompression, isBigEndian, user, password);
//            //loadOutOfOrder();
//            //loadTicks();
//        } finally {
//            final long ts1 = System.currentTimeMillis();
//            System.out.println("Total time: " + (ts1 - ts0) + "ms");
//        }
//    }

    @Test
    public void loadBars() throws Exception {
        loadBars(false, false, null, null);
    }

    @Test
    public void loadOutBars() throws Exception {
        loadOutOfOrder(false, false, null, null);
    }

    private static void loadBars(boolean useCompression, boolean isBigEndian, String user, String password) throws Exception {
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

        final RecordClassSet rcs = requestSchema(streamName, user, password);
        final RecordClassDescriptor type = rcs.getTopType(0);
        final FixedBoundEncoder encoder = CodecFactory.COMPILED.createFixedBoundEncoder(TypeLoaderImpl.DEFAULT_INSTANCE, type);
        final MemoryDataOutput mdo = new MemoryDataOutput();

        final MessageChannel<RawMessage> loader = getLoader(useCompression, isBigEndian, streamName, rcs.getTopTypes(), (short)0, user, password);
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
    }

    private static void loadOutOfOrder(boolean useCompression, boolean isBigEndian, String user, String password) throws Exception {
        String streamName = "1min";
        final RecordClassSet rcs = requestSchema(streamName, user, password);
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

        final MessageChannel<RawMessage> loader = getLoader(useCompression, isBigEndian, streamName, rcs.getTopTypes(), (short)3, user, password);
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

    private static void loadTicks(boolean useCompression, boolean isBigEndian, String user, String password) throws Exception {
        String streamName = "sine_hyb";
        final RecordClassSet rcs = requestSchema(streamName, user, password);

        final TradeMessage trade = new TradeMessage();
        trade.setPrice(1.0);
        trade.setSize(100);

        final BestBidOfferMessage bbo = new BestBidOfferMessage();
        bbo.setOfferPrice(1.0);
        bbo.setBidPrice(1.0);
        bbo.setBidSize(100);
        bbo.setOfferSize(100);

        RecordClassDescriptor tradeType = (RecordClassDescriptor)rcs.getClassDescriptor(TradeMessage.CLASS_NAME);
        RecordClassDescriptor bboType = (RecordClassDescriptor)rcs.getClassDescriptor(BestBidOfferMessage.CLASS_NAME);
        final FixedBoundEncoder tradeEncoder = CodecFactory.COMPILED.createFixedBoundEncoder(TypeLoaderImpl.DEFAULT_INSTANCE, tradeType);
        final FixedBoundEncoder bboEncoder = CodecFactory.COMPILED.createFixedBoundEncoder(TypeLoaderImpl.DEFAULT_INSTANCE, bboType);
        final MemoryDataOutput mdo = new MemoryDataOutput();

        final MessageChannel<RawMessage> loader = getLoader(useCompression, isBigEndian, streamName, rcs.getTopTypes(), (short)0, user, password);
        RawMessage raw = new RawMessage();
        long timestamp = GMT.parseDateTime("2013-10-09 09:00:00").getTime();

        for (int i = 0; i < 5; i++) {
            for (String symbol : SYMBOLS) {
                raw.setSymbol(symbol);
                raw.type = tradeType;

                mdo.reset();
                tradeEncoder.encode(trade, mdo);
                raw.setTimeStampMs(timestamp);
                raw.setBytes(mdo);
                loader.send(raw);

                bbo.setSymbol(symbol);
                raw.type = bboType;

                mdo.reset();
                bboEncoder.encode(bbo, mdo);
                raw.setBytes(mdo);
                raw.setTimeStampMs(raw.getTimeStampMs() + 1);
                loader.send(raw);

                trade.setPrice(trade.getPrice() + 1);
                bbo.setOfferPrice(bbo.getOfferPrice() + 1);
            }

            timestamp += TimeConstants.SECOND;
            trade.setSize(trade.getSize() + 10);
            bbo.setOfferSize(bbo.getOfferSize() + 10);
        }

        loader.close();
    }

    private static RecordClassSet requestSchema(String streamName, String user, String password) throws IOException, JAXBException {
        GetSchemaRequest r = new GetSchemaRequest();
        r.stream = streamName;
        Marshaller m = TBJAXBContext.createMarshaller();

        final URL url = getPath("tb/xml");
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        if (user != null) {
            String authStr = user + ":" + password;
            final String encodedAuth = Base64EncoderEx.encode(authStr.getBytes());
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        }
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();

        m.marshal(r, os);
        int rc = conn.getResponseCode();
        if (rc != 200) {
            throw new RuntimeException("HTTP rc=" + rc + " " + conn.getResponseMessage());
        }

        InputStream is = conn.getInputStream();
        Unmarshaller u = UHFJAXBContext.createUnmarshaller();
        return (RecordClassSet) u.unmarshal(is);
    }

    private static MessageChannel<RawMessage> getLoader(boolean useCompression, boolean isBigEndian, String streamName, RecordClassDescriptor[] rcds, short maxAllowedErrors, String user, String password) throws IOException {
        final URL url = getPath("tb/bin");
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        if (user != null) {
            String authStr = user + ":" + password;
            final String encodedAuth = Base64EncoderEx.encode(authStr.getBytes());
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        }
        conn.setDoOutput(true);
        // Send in chunks (to avoid out of memory error)
        conn.setChunkedStreamingMode(SIZE_1MB);
        final OutputStream os;
        GZIPOutputStream gzip_out;
        if (useCompression) {
            conn.setRequestProperty(HTTPProtocol.CONTENT_ENCODING, HTTPProtocol.GZIP);
            os = gzip_out = new GZIPOutputStream(conn.getOutputStream(), 0x1000, true);
        } else {
            os = conn.getOutputStream();
            gzip_out = null;
        }
        final DataOutput dout = isBigEndian ? new DataOutputStream(os) : new LittleEndianDataOutputStream(os);

        // endianness version stream write_mode allowed_errors
        dout.writeByte(isBigEndian ? 1 : 0);
        dout.writeShort(HTTPProtocol.VERSION);
        dout.writeUTF(streamName);
        dout.write(WriteMode.REWRITE.ordinal());
        dout.writeShort(maxAllowedErrors);

        return new HTTPLoader(conn, dout, gzip_out, rcds);
    }
}