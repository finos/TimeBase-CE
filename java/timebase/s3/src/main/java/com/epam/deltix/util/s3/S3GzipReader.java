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
