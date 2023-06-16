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

import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.util.cmdline.DefaultApplication;
import com.epam.deltix.util.s3.S3DataStore;
import com.epam.deltix.util.s3.S3StorageOptions;

public class S3Importer extends DefaultApplication {

    public S3Importer(String[] args) {
        super(args);
    }

    @Override
    protected void run() throws Throwable {
        String timebaseUrl = getArgValue("-timebase", "dxtick://localhost:8011");
        String streamKey = getMandatoryArgValue("-stream");
        String targetStream = getArgValue("-targetStream", streamKey);
        String bucket = getMandatoryArgValue("-bucket");
        String region = getMandatoryArgValue("-region");
        String accessKeyId = getArgValue("-accessKeyId", System.getenv().get("AWS_ACCESS_KEY_ID"));
        if (accessKeyId == null) {
            throw new IllegalArgumentException("Access key ID must be specified in environment variable AWS_ACCESS_KEY_ID or with '-accessKeyId' argument.");
        }
        String accessKey = getArgValue("-accessKey", System.getenv().get("AWS_SECRET_ACCESS_KEY"));
        if (accessKey == null) {
            throw new IllegalArgumentException("Access key must be specified in environment variable AWS_SECRET_ACCESS_KEY or with '-accessKey' argument.");
        }
        S3StorageOptions options = new S3StorageOptions(bucket, region, accessKeyId, accessKey);
        S3DataStore dataStore = new S3DataStore(options);
        String startTimeArg = getArgValue("-startTime");
        final long startTime = startTimeArg == null ? Long.MIN_VALUE: parseDateTime(startTimeArg);
        String endTimeArg = getArgValue("-endTime");
        final long endTime = endTimeArg == null ? Long.MAX_VALUE: parseDateTime(endTimeArg);
        S3StreamImporter.ImportMode importMode = S3StreamImporter.ImportMode.valueOf(getArgValue("-importMode", "REPLACE"));
        String[] spaces = S3Utils.getSpaces(dataStore, streamKey);
        if (spaces.length == 0) {
            throw new IllegalArgumentException("There are no replicated spaces for stream " + streamKey + ".");
        }
        try (DXTickDB db = TickDBFactory.openFromUrl(timebaseUrl, false)) {
            final DXTickStream stream = db.getStream(targetStream);
            if (stream == null) {
                throw new IllegalArgumentException("Stream " + streamKey + " does not exist");
            }
            new S3StreamImporter(streamKey, stream, spaces, dataStore, startTime, endTime, importMode).run();
        }
    }

    public static void main(String[] args) throws Throwable {
        new S3Importer(args).run();
    }
}