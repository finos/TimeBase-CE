package com.epam.deltix.test.qsrv.hf.tickdb.http;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.concurrent.CountDownLatch;
import java.util.zip.GZIPInputStream;

import com.epam.deltix.qsrv.hf.tickdb.http.*;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.http.download.ChangeAction;
import com.epam.deltix.qsrv.hf.tickdb.http.download.EntitiesRequest;
import com.epam.deltix.qsrv.hf.tickdb.http.download.ResetRequest;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.util.codec.Base64EncoderEx;
import com.epam.deltix.util.io.StreamPump;
import com.epam.deltix.util.time.GMT;

/**
 *
 */
public class TestHttpSelect {

    final static String URL = "http://localhost:8011/tb/xml";

    public static void main(String[] args) throws Exception {
        final long ts0 = System.currentTimeMillis();

        try {
            final boolean isBigEndian = args.length > 0 && "big".equals(args[0]);
            final boolean useCompression = args.length > 1 && HTTPProtocol.GZIP.equals(args[1]);
            final String user = args.length > 2 ? args[2] : null;
            final String password = args.length > 3 ? args[3] : "";
            test(useCompression, isBigEndian, user, password);
            //stressTest(useCompression, isBigEndian, user, password, 1000);
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
        }
        finally {
            final long ts1 = System.currentTimeMillis();
            System.out.println("Total time: " + (ts1 - ts0) + "ms");
        }
    }

    private static void test(boolean useCompression, boolean isBigEndian, String user, String password) throws Exception {
        Marshaller m = TBJAXBContext.createMarshaller();

        //SelectRequest sr = request2Multiple();
        //SelectRequest sr = request2L1Live();
        SelectRequest sr = request2Bars();
        //SelectRequest sr = request2L1();
        //SelectRequest sr = request2TheWholeStream("L1-Security");
        //SelectRequest sr = request2L2();
        sr.isBigEndian = isBigEndian;
        sr.useCompression = useCompression;

        sr.typeTransmission = TypeTransmission.DEFINITION; //TypeTransmission.NAME;// TypeTransmission.GUID;
        //sr.typeTransmission = TypeTransmission.NAME;
        final RecordClassSet rcs;
        if (sr.typeTransmission != TypeTransmission.DEFINITION) {
            // imitate precompiled case
            DXTickDB db = TickDBFactory.connect("localhost", 8011);
            db.open(true);
            try {
                rcs = db.getStream(sr.streams[0]).getStreamOptions().getMetaData();
            } finally {
                db.close();
            }
        }
        else
            rcs = null;

        //sr.streams[0] = "bad stream";
        //m.marshal(sr, new File("c:\\temp\\select_request_v3.xml"));
        final long ts0 = System.currentTimeMillis();

        final HttpURLConnection conn = (HttpURLConnection) new URL(URL).openConnection();

        if (user != null) {
            String authStr = user + ":" + password;
            final String encodedAuth = Base64EncoderEx.encode(authStr.getBytes());
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        }
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();

        m.marshal(sr, os);
        int rc = conn.getResponseCode();
        if (rc != 200) {
            System.out.println("HTTP rc=" + rc + " " + conn.getResponseMessage());
            return;
        }

        // Content-Encoding [gzip]
        final InputStream is = HTTPProtocol.GZIP.equals(conn.getHeaderField(HTTPProtocol.CONTENT_ENCODING)) ?
                new GZIPInputStream(conn.getInputStream()) : conn.getInputStream();
        readSelectResponse(is, sr.isBigEndian, sr.typeTransmission, rcs);
        //readSelectResponse(is, "c:\\temp\\x.bin");
        final long ts1 = System.currentTimeMillis();
        System.out.println("select time: " + (ts1 - ts0) + "ms " + Thread.currentThread().getName());
    }

    private static void stressTest(final boolean useCompression, final boolean isBigEndian, final String user, final String password, int num_of_threads) throws Exception {
        final CountDownLatch latch = new CountDownLatch(num_of_threads);
        for (int i = 0; i < num_of_threads; i++) {
             Thread th = new Thread(new Runnable() {
                 @Override
                 public void run() {
                     try {
                         test(useCompression, isBigEndian, user, password);
                     } catch (Exception e) {
                         if (e instanceof java.net.ConnectException)
                             System.out.println(e.getMessage() + " " + Thread.currentThread().getName());
                         else
                             e.printStackTrace();
                     }
                     finally {
                         latch.countDown();
                     }
                     //System.out.println(Thread.currentThread().getName() + " completed");
                 }
             }, "WorkingThread" + i);
            th.setDaemon(true);
            th.start();
        }

        System.out.println("Threads started");
        latch.await();
        System.out.println("Threads completed");
    }

    private static SelectRequest request2Bars() throws ParseException {
        SelectRequest sr = new SelectRequest();
        sr.streams = new String[] {"agg"};
        sr.symbols = new String[] {
                 "AAPL"
//                new ConstantIdentityKey(InstrumentType.EQUITY, "MSFT"),
//                new ConstantIdentityKey(InstrumentType.EQUITY, "ORCL"),
//                new ConstantIdentityKey(InstrumentType.FUTURE, "CL1")
        };
        //sr.types = new String[]{"deltix.qsrv.hf.pub.BarMessage"};
        //sr.from = GMT.parseDateTime("2010-03-29 14:31:00").getTime();
        //sr.to = GMT.parseDateTime("2010-03-29 14:35:00").getTime();
        return sr;
    }

    private static SelectRequest request2L1() throws ParseException {
        SelectRequest sr = new SelectRequest();
        sr.streams = new String[]{"iq_fp"};
        sr.from = GMT.parseDateTime("2013-10-07 15:40:00").getTime();
        sr.to = GMT.parseDateTime("2013-10-07 17:50:00.0").getTime();
        return sr;
    }

    private static SelectRequest request2L1Live() throws ParseException {
        SelectRequest sr = new SelectRequest();
        sr.streams = new String[]{"iq_fp"};
        sr.from = GMT.parseDateTime("2013-10-07 17:49:35.0").getTime();
        sr.live = true;
        return sr;
    }

    private static SelectRequest request2TheWholeStream(String streamName) {
        SelectRequest sr = new SelectRequest();
        sr.streams = new String[]{streamName};
        return sr;
    }

    private static SelectRequest request2L2() throws ParseException {
        SelectRequest sr = new SelectRequest();
        sr.streams = new String[] { "qh_l2_p_temp" };
        sr.symbols = new String[] { "BRN_FMU0013", "CLU3"};
        sr.types = new String[]{
                "deltix.qsrv.hf.pub.Level2Message",
                "deltix.qsrv.hf.pub.L2SnapshotMessage",
                "deltix.qsrv.hf.pub.TradeMessage",
                "deltix.qsrv.hf.spi.data.ConnectionStatusChangeMessage"};

        return sr;
    }

    private static DownloadRequest request2Multiple() throws ParseException {
        SelectRequest sr = new SelectRequest();
        sr.streams = new String[]{"1min", "iq_fp", "L1-Security"};
        //sr.from = GMT.parseDateTime("2013-10-07 17:49:35.0").getTime();
        //sr.live = true;
        return sr;
    }

    private static void readSelectResponse(InputStream is, boolean isBigEndian, TypeTransmission tt, RecordClassSet rcs) throws IOException, JAXBException {

        long cnt = 100;
        HTTPCursor cur = new HTTPCursor(is, isBigEndian, tt, rcs);

        for (int i = 0; i < 100; i++) {
            cur.next();
        }

        ResetRequest reset = new ResetRequest(cur.getId());
        reset.time = 0;

        EntitiesRequest change = new EntitiesRequest();
        change.id = cur.getId();
        change.mode = ChangeAction.ADD;
        change.entities = new IdentityKey[] {
                new ConstantIdentityKey("A")
        };

        TestXmlQueries.query(null, null, change);

        while (cur.next()) {
            System.out.println(cur.getMessage());
            cnt++;
        }

        System.out.println("Number of messages: " + cnt);
    }

    private static void readSelectResponse(InputStream is, String fileName) throws IOException, InterruptedException {
        FileOutputStream fos = new FileOutputStream(fileName);
        StreamPump.pump(is, fos);
    }
}
