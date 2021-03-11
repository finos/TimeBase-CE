package com.epam.deltix.util.s3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

public class S3GzipReader extends S3Reader<String> {
    private BufferedReader dataReader;

    public S3GzipReader(S3DataStore dataStore, String dataKey) throws IOException {
        super(dataStore, dataKey);
    }

    @Override
    protected void startBatch(InputStream batchData) throws IOException {
        GZIPInputStream zin = new GZIPInputStream(batchData);
        dataReader = new BufferedReader(new InputStreamReader(zin));
    }

    // returns next json record or null
    @Override
    protected String readNextRecord() throws IOException {
        return (dataReader != null ? dataReader.readLine() : null);
    }

    @Override
    protected String getDataFormat() {
        return "json.gz";
    }
}
