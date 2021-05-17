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
package com.epam.deltix.qsrv.hf.tickdb.tool;

import com.google.gson.JsonParseException;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.util.json.parser.JsonMessageSource;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.s3.S3DataStore;
import com.epam.deltix.util.s3.S3Reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

public class S3RawMessageReader extends S3Reader<RawMessage> {
    private final RecordClassDescriptor[] descriptors;
    private final S3SpaceMetadata metadata;
    private final boolean oldFormat;

    private JsonMessageSource messageSource;

    public S3RawMessageReader(S3DataStore dataStore, String dataKey, long startTime, long endTime, RecordClassDescriptor[] descriptors) throws IOException {
        super(dataStore, dataKey, startTime, endTime);
        this.descriptors = descriptors;
        boolean oldFormat = false;
        S3SpaceMetadata spaceMetadata = null;
        try {
            spaceMetadata = S3Utils.INSTANCE.parseMetadata(getUserMetadata());
        } catch (JsonParseException exc) {
            oldFormat = true;
        }
        this.metadata = spaceMetadata;
        this.oldFormat = oldFormat;
    }

    @Override
    protected void startBatch(InputStream batchData) throws IOException {
        GZIPInputStream zin = new GZIPInputStream(batchData);
        BufferedReader reader = new BufferedReader(new InputStreamReader(zin));
        messageSource = new JsonMessageSource(descriptors, reader);
    }

    @Override
    protected RawMessage readNextRecord() {
        return messageSource != null && messageSource.next() ? messageSource.getMessage(): null;
    }

    @Override
    protected String getDataFormat() {
        return "json.gz";
    }

    public S3SpaceMetadata getSpaceMetadata() {
        return metadata;
    }

    public boolean isOldFormat() {
        return oldFormat;
    }

    @Override
    public void close() throws IOException {
        super.close();
        Util.close(messageSource);
    }
}
