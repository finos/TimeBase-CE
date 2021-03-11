package com.epam.deltix.util.s3;

import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 * S3GzipWriter - writes data records to S3 in compressed format
 */
public class S3GzipWriter extends S3Writer<String> {
    private DataOutputStream out = new DataOutputStream();
    private GZIPOutputStream zout = null;
    private OutputStreamWriter outWriter = null;

    public S3GzipWriter(S3DataStore dataStore, String dataKey, int maxBatchSize, long maxBatchTime) {
        super(dataStore, dataKey, maxBatchSize, maxBatchTime);
    }

    @Override
    protected void startBatch() throws IOException {
        out.reset();
        zout = new GZIPOutputStream(out);
        outWriter = new OutputStreamWriter(zout);
    }

    @Override
    protected void writeNextRecord(String record) throws IOException {
        outWriter.write(record);
        if (!record.endsWith("\n"))
            outWriter.write('\n');
    }

    @Override
    protected void finishBatch() throws IOException {
        outWriter.flush();
        zout.finish();
        zout.close();
    }

    // Warning: not thread safe. Returns internal writer buffer to skip an extra array copy
    @Override
    protected InputStream getBatchData() {
        return out.getData();
    }

    @Override
    protected String getDataFormat() {
        return "json.gz";
    }

    private static class DataOutputStream extends ByteArrayOutputStream {
        private InputStream getData() {
            return new ByteArrayInputStream(super.buf, 0, size());
        }
    }
}
