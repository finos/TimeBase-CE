package com.epam.deltix.test.qsrv.hf.tickdb.http;

import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.http.*;
import com.epam.deltix.qsrv.hf.tickdb.http.download.*;
import com.epam.deltix.qsrv.hf.tickdb.http.stream.*;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickDBJAXBContext;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.qsrv.test.messages.BestBidOfferMessage;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.qsrv.testsetup.TickDBCreator;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;

import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.lang.Util;
import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(JUnitCategories.TickDBFast.class)
public class Test_HttpServer extends BaseTest {

    private DXTickStream        getBars () {
        DXTickDB tickdb = runner.getTickDb();

        DXTickStream tickStream = tickdb.getStream(TickDBCreator.BARS_STREAM_KEY);
        if (tickStream == null)
            tickStream = TickDBCreator.createBarsStream (tickdb, TickDBCreator.BARS_STREAM_KEY);

        return tickStream;
    }

    @Test
    public void createStream() throws IOException, JAXBException {
        StreamOptions options = getBars().getStreamOptions();

        CreateStreamRequest request = new CreateStreamRequest();
        request.key = "newbars";
        request.options = new StreamDef(options);
        request.options.name = "newbars";

        StringWriter writer = new StringWriter();
        TickDBJAXBContext.createMarshaller().marshal(options.getMetaData(), writer);
        request.options.metadata = writer.getBuffer().toString();

        TestXmlQueries.query(URL, null, null, request);
    }

    @Test
    public void validateQQL() throws IOException, JAXBException {

        ValidateQQLRequest request = new ValidateQQLRequest();
        request.qql = "SELECT * from bars";

        ValidateQQLResponse result = (ValidateQQLResponse) TestXmlQueries.query(URL, null, null, request);

        Assert.assertEquals(-1, result.result.errorLocation);
        Assert.assertEquals(4, result.result.tokens.size());

        request = new ValidateQQLRequest();
        request.qql = "SELECT z, from bars";

        result = (ValidateQQLResponse) TestXmlQueries.query(URL, null, null, request);

        Assert.assertTrue(result.result.errorLocation != -1);
    }

    //@Test
    public void createFileStream() throws IOException, JAXBException {
        CreateFileStreamRequest request = new CreateFileStreamRequest();
        request.key = "newbars";
        request.dataFile = Home.getFile("testdata/qsrv/hf/tickdb/bars.gz").getAbsolutePath();

        TestXmlQueries.query(URL, null, null, request);
    }

    @Test
    public void createEmptyStream() throws IOException, JAXBException {
        StreamOptions options = getBars().getStreamOptions();

        CreateStreamRequest request = new CreateStreamRequest();
        request.key = "empty";
        request.options = new StreamDef(options);
        request.options.name = "empty";

        TestXmlQueries.query(URL, null, null, request);
    }

    @Test
    public void createEmptyUniqueStream() throws IOException, JAXBException {
        StreamOptions options = getBars().getStreamOptions();

        CreateStreamRequest request = new CreateStreamRequest();
        request.key = "empty.unique";
        request.options = new StreamDef(options);
        request.options.name = request.key;
        request.options.unique = true;

        TestXmlQueries.query(URL, null, null, request);
    }

    @Test
    public void createTransientStream() throws IOException, JAXBException {
        StreamOptions options = getBars().getStreamOptions();

        CreateStreamRequest request = new CreateStreamRequest();
        request.key = "transient";
        request.options = new StreamDef(options);
        request.options.scope = StreamScope.TRANSIENT;
        request.options.bufferOptions = new StreamDef.BufferOptions();
        request.options.bufferOptions.lossless = true;
        request.options.name = request.key;

        TestXmlQueries.query(URL, null, null, request);
    }

    @Test
    public void createUniqueStream() throws IOException, JAXBException {
        StreamOptions options = getBars().getStreamOptions();

        CreateStreamRequest request = new CreateStreamRequest();
        request.key = "unique";
        request.options = new StreamDef(options);
        request.options.name = request.key;
        request.options.unique = true;

        StringWriter writer = new StringWriter();
        TickDBJAXBContext.createMarshaller().marshal(options.getMetaData(), writer);
        request.options.metadata = writer.getBuffer().toString();

        TestXmlQueries.query(URL, null, null, request);

        assertTrue(runner.getTickDb().getStream("unique").getStreamOptions().unique);
    }

    @Test
    public void listStreams() throws IOException, JAXBException {
        ListStreamsResponse result = (ListStreamsResponse) TestXmlQueries.query(URL, null, null, new ListStreamsRequest());
        assertTrue(result.streams != null);
        assertTrue(Arrays.asList(result.streams).contains(TickDBCreator.BARS_STREAM_KEY));
    }

    public void         truncate() throws IOException, JAXBException {
        long[] range = getBars().getTimeRange();
        TruncateRequest req = new TruncateRequest();
        req.stream = getBars().getKey();
        req.time = (range[0] + range[1])/2;

        TestXmlQueries.query(URL, null, null, req);
    }

    @Test
    public void getSchema() throws IOException, JAXBException {
        GetSchemaRequest request = new GetSchemaRequest();
        request.stream = TickDBCreator.BARS_STREAM_KEY;

        TestXmlQueries.query(URL, null, null, request, TickDBJAXBContext.createUnmarshaller());
    }

    public Object getSchema(String name) throws IOException, JAXBException {
        GetSchemaRequest request = new GetSchemaRequest();
        request.stream = name;

        return TestXmlQueries.query(URL, null, null, request, TickDBJAXBContext.createUnmarshaller());
    }

    @Test
    public void TestSchema() throws IOException, JAXBException {
        ChangeSchemaRequest request = new ChangeSchemaRequest();
        request.stream = TickDBCreator.BARS_STREAM_KEY;

        RecordClassDescriptor d2 = StreamConfigurationHelper.mkBarMessageDescriptor(null,
                "", null, FloatDataType.ENCODING_FIXED_FLOAT, FloatDataType.ENCODING_FIXED_FLOAT);

        RecordClassSet set = new RecordClassSet();
        set.addContentClasses(d2);

        StringWriter writer = new StringWriter();
        TickDBJAXBContext.createMarshaller().marshal(set, writer);
        request.schema = writer.getBuffer().toString();
        request.background = false;
        request.mappings = new HashMap<>();
        request.mappings.put("name", "value");
        request.mappings.put("name1", "value1");

        TestXmlQueries.query(URL, null, null, request);

        RecordClassSet result = (RecordClassSet)getSchema(request.stream);
        assertEquals(d2.getGuid(), result.getTopType(0).getGuid());

        selectBars(true, false);


//        SetSchemaRequest request = new SetSchemaRequest();
//        request.stream = TickDBCreator.BARS_STREAM_KEY;
//
//        StringWriter writer = new StringWriter();
//        TickDBJAXBContext.createMarshaller().marshal(getBars().getStreamOptions().getMetaData(), writer);
//        request.schema = writer.getBuffer().toString();
//
//        TestXmlQueries.query(URL, null, null, request);
    }

    @Test
    public void loadStreams() throws IOException, JAXBException {
        LoadStreamsResponse result = (LoadStreamsResponse) TestXmlQueries.query(URL, null, null, new LoadStreamsRequest());
        assertTrue(result.streams != null);
        assertTrue(result.options != null);
        assertTrue(Arrays.asList(result.streams).contains(TickDBCreator.BARS_STREAM_KEY));
    }

    @Test
    public void listEntities() throws IOException, JAXBException {
        ListEntitiesRequest request = new ListEntitiesRequest();
        request.stream = TickDBCreator.BARS_STREAM_KEY;

        ListEntitiesResponse result = (ListEntitiesResponse) TestXmlQueries.query(URL, null, null, request);
        assertTrue(result.identities != null);
        System.out.println("stream: " + request.stream + " " + Util.printArray(result.identities));
    }

    @Test
    public void testLocking() throws IOException, JAXBException {
        LockStreamRequest lock = new LockStreamRequest();
        lock.stream = TickDBCreator.BARS_STREAM_KEY;
        lock.write = true;

        LockStreamResponse result = (LockStreamResponse)TestXmlQueries.query(URL, null, null, lock);
        assertTrue(result.id != null);

        UnlockStreamRequest unlock = new UnlockStreamRequest(result.id, true);
        unlock.stream = TickDBCreator.BARS_STREAM_KEY;
        TestXmlQueries.query(URL, null, null, unlock);

        result = (LockStreamResponse)TestXmlQueries.query(URL, null, null, lock);

        assertTrue(result.id != null);
    }

    @Test
    public void         testSelect() throws Throwable {
        selectBars(false, false);
        selectBars(true, false);
    }

    @Test
    public void         testEmptySelect() throws Throwable {

        SelectRequest sr = new SelectRequest();
        sr.streams = new String[] {TickDBCreator.BARS_STREAM_KEY};
        sr.symbols = new String[0];
        sr.isBigEndian = true;
        sr.useCompression = false;
        sr.typeTransmission = TypeTransmission.DEFINITION;

        int count = selectCount(sr, getBars().getStreamOptions().getMetaData());
        assertEquals(0, count);

        sr.symbols = null;
        sr.types = new String[0];
        count = selectCount(sr, getBars().getStreamOptions().getMetaData());
        assertEquals(0, count);
    }

    @Test
    public void         testReverse() throws Throwable {

        SelectRequest sr = new SelectRequest();
        sr.streams = new String[] {TickDBCreator.BARS_STREAM_KEY};
        sr.reverse = true;
        sr.symbols = null;
        sr.isBigEndian = false;
        sr.useCompression = false;
        sr.typeTransmission = TypeTransmission.DEFINITION;
        sr.from = Long.MIN_VALUE;
        sr.to = Long.MAX_VALUE;

        final long ts0 = System.currentTimeMillis();
        int count = selectCount(sr, getBars().getStreamOptions().getMetaData());

        assertEquals(94851, count);

        System.out.println("select time: " + (System.currentTimeMillis() - ts0) + "ms " + Thread.currentThread().getName() + ",  messages: " + count);
    }

    @Test
    public void         testQuery() throws Throwable {
        QQLRequest sr = new QQLRequest();
        sr.qql = "select * from bars";
        sr.isBigEndian = false;
        sr.typeTransmission = TypeTransmission.DEFINITION; //TypeTransmission.NAME;// TypeTransmission.GUID;

        int count = query(sr);
        assertEquals(94851, count);
    }

    @Test
    public void         testQuery1() throws Throwable {
        QQLRequest sr = new QQLRequest();
        sr.qql = "select open from bars";
        sr.isBigEndian = false;
        sr.typeTransmission = TypeTransmission.DEFINITION; //TypeTransmission.NAME;// TypeTransmission.GUID;

        int count = query(sr);
        assertEquals(94851, count);
    }

    @Test
    public void         testQueryWithParams() throws Throwable {
        QQLRequest sr = new QQLRequest();
        sr.qql = "select * from bars where open > n";
        sr.isBigEndian = false;
        sr.typeTransmission = TypeTransmission.DEFINITION;
        sr.parameters = new QQLParameter[] {new QQLParameter("n", "INTEGER", 60)};

        int count = query(sr);
        assertEquals(60139, count);
    }

    @Test
    public void         testGZIPSelect() throws Throwable {
        selectBars(false, true);
    }

    public int         selectCount(SelectRequest sr, RecordClassSet set) throws JAXBException, IOException {
        int count = 0;
        try (HTTPCursor cursor = select(sr)) {
            cursor.setRecordClassSet(set);

            while (cursor.next()) {
                count++;
            }
            close(cursor);
        }

        return count;
    }

    public void         selectBars(boolean isBigEndian, boolean compressed) throws JAXBException, IOException {
        SelectRequest sr = new SelectRequest();
        sr.streams = new String[] {TickDBCreator.BARS_STREAM_KEY};
        sr.symbols = new String[] { "AAPL" };
        sr.isBigEndian = isBigEndian;
        sr.useCompression = compressed;
        sr.typeTransmission = TypeTransmission.DEFINITION; //TypeTransmission.NAME;// TypeTransmission.GUID;

        //sr.typeTransmission = TypeTransmission.NAME;
        final RecordClassSet rcs = getBars().getStreamOptions().getMetaData();

        final long ts0 = System.currentTimeMillis();

        HTTPCursor cursor = select(sr);
        cursor.setRecordClassSet(rcs);

        int count = 0;
        while (cursor.next()) {
            count++;

            if (count == 500)
                addTypes(cursor, BarMessage.CLASS_NAME);
//
//            System.out.println(cursor.getMessage());
        }

        close(cursor);

        assertEquals(23789, count);

        final long ts1 = System.currentTimeMillis();
        System.out.println("select time: " + (ts1 - ts0) + "ms " + Thread.currentThread().getName() + ",  messages: " + count);
    }

    public int          query(QQLRequest request) throws JAXBException, IOException {
        //sr.typeTransmission = TypeTransmission.NAME;
        final RecordClassSet rcs = getBars().getStreamOptions().getMetaData();

        final long ts0 = System.currentTimeMillis();

        HTTPCursor cursor = select(request);
        cursor.setRecordClassSet(rcs);

        int count = 0;
        while (cursor.next()) {
            count++;

            if (count == 500)
                addTypes(cursor, BarMessage.CLASS_NAME);
//
//            System.out.println(cursor.getMessage());
        }

        close(cursor);

        final long ts1 = System.currentTimeMillis();
        System.out.println("select time: " + (ts1 - ts0) + "ms " + Thread.currentThread().getName() + ",  messages: " + count);

        return count;
    }
    @Ignore("Why hang?")
    @Test
    public void testLive() throws JAXBException, IOException {
        String name = "trades";

        RecordClassDescriptor base =
                StreamConfigurationHelper.mkMarketMessageDescriptor(840);

        RecordClassDescriptor rd1 = StreamConfigurationHelper.mkBBOMessageDescriptor(base,
                true, "", 840, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);
        RecordClassDescriptor rd2 = StreamConfigurationHelper.mkTradeMessageDescriptor(base,
                "", 840, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);

        DXTickStream stream1 = runner.getTickDb().createStream(name,
                StreamOptions.polymorphic(StreamScope.DURABLE, name, null, 1, rd1, rd2));

        assertTrue(stream1 != null);

        TDBRunner.BBOGenerator gn = new TDBRunner.BBOGenerator(new GregorianCalendar(2015, 0, 1), 1000, 10000, "MSFT", "ORCL");

        try (TickLoader loader = stream1.createLoader()) {

            while (gn.next())
                loader.send(gn.getMessage());
        }

        long endTime = gn.getMessage().getTimeStampMs();

        SelectRequest sr = new SelectRequest();
        sr.types = new String[] { BestBidOfferMessage.CLASS_NAME };
        sr.streams = new String[] { name };
        sr.live = true;
        sr.symbols = new String[] { "MSFT" };

        sr.isBigEndian = true;
        sr.useCompression = false;
        sr.typeTransmission = TypeTransmission.DEFINITION; //TypeTransmission.NAME;// TypeTransmission.GUID;

        final RecordClassSet rcs = getBars().getStreamOptions().getMetaData();
        final long ts0 = System.currentTimeMillis();

        HTTPCursor cursor = select(sr);
        cursor.setRecordClassSet(rcs);

        int count = 0;
        long lastTime = 0;

        while (lastTime < endTime && cursor.next()) {
            count++;

            if (count == 100) {
                addTypes(cursor, TradeMessage.CLASS_NAME);
            }

            if (count == 200) {
                addEntities(cursor, new ConstantIdentityKey("ORCL"));
            }

            lastTime = cursor.getMessage().getTimeStampMs();

            //System.out.println(cursor.getMessage());
        }

        addTypes(cursor, BarMessage.CLASS_NAME);

        assertTrue(cursor.next());

        reset(cursor, 0);

        assertTrue(cursor.next());

        close(cursor);

        final long ts1 = System.currentTimeMillis();
        System.out.println("select time: " + (ts1 - ts0) + "ms " + Thread.currentThread().getName() + ", messages: " + count);
    }

    private HTTPCursor select(String stream, long time, String[] types, String... ids) throws IOException, JAXBException {

        SelectRequest sr = new SelectRequest();
        sr.from = time;
        sr.types = new String[] { BestBidOfferMessage.CLASS_NAME };
        sr.streams = new String[] { stream };
        sr.symbols = ids;

        sr.isBigEndian = true;
        sr.useCompression = false;

        sr.typeTransmission = TypeTransmission.DEFINITION; //TypeTransmission.NAME;// TypeTransmission.GUID;

        return select(sr);
    }


    private HTTPCursor select(SelectRequest select) throws IOException, JAXBException {

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

    private void        reset(HTTPCursor cursor, long time) throws IOException, JAXBException {
        ResetRequest request = new ResetRequest(cursor.getId());
        request.time = time;
        CursorResponse r = (CursorResponse) TestXmlQueries.query(URL, null, null, request);
        cursor.waitCommand(r.serial);
    }

    private void    close(HTTPCursor cursor) throws IOException, JAXBException {
        TestXmlQueries.query(URL, null, null, new CloseRequest(cursor.getId()));
        cursor.close();
    }

    private void    addEntities(HTTPCursor cursor, IdentityKey... ids) throws IOException, JAXBException {
        EntitiesRequest change = new EntitiesRequest();
        change.id = cursor.getId();
        change.mode = ChangeAction.ADD;
        change.entities = ids;

        TestXmlQueries.query(URL, null, null, change);
    }

    private void    addTypes(HTTPCursor cursor, String ... types) throws IOException, JAXBException {
        TypesRequest change = new TypesRequest();
        change.id = cursor.getId();
        change.mode = ChangeAction.ADD;
        change.types = types;

        TestXmlQueries.query(URL, null, null, change);
    }
}
