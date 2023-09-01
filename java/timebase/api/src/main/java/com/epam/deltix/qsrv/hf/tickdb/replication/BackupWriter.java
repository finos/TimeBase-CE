/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.replication;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.stream.DXDataWriter;
import com.epam.deltix.util.lang.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 */
public class BackupWriter {

    private static final String     FORMAT = "%s-%s" + StreamReplicator.EXTENSION;

    private ReplicationOptions      options;
    private File                    folder;
    final ArrayList<File>           files;
    private long                    version;
    private RecordClassDescriptor[] classes;

    private String                  key;

    private DXDataWriter            writer;
    private File                    tmp;
    private long                    time = Long.MIN_VALUE;
    private long                    count = 0;

    private final StreamReplicator  replicator = new StreamReplicator();

    public BackupWriter(File folder, ArrayList<File> files, long version, ReplicationOptions options) {
        this.folder = folder;
        this.files = files;
        this.options = options;
        this.version = version;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setClasses(RecordClassDescriptor[] classes) {
        this.classes = classes;
    }

    public void         commitFile() {
        File file = new File(folder, String.format(FORMAT, time, key));
        // we can skip last time data

        if (tmp.renameTo(file)) {
            files.add(file);
            replicator.LOGGER.info("File has been created: " + file.getName());
        } else {
            replicator.LOGGER.error("Temporary backup file name rename failed: " + tmp.getName() + " " + file.getName());
        }
    }

    public void         send(InstrumentMessage msg) {
        try {
            boolean startNewFile = false;

            if (writer == null) {
                tmp = File.createTempFile(key, ".tmp.gz", folder);
                writer = new DXDataWriter(tmp);
                writer.startBlock(String.valueOf(version), classes);
                time = Long.MIN_VALUE;

                count = 0;
            }

            //System.out.println(msg.timestamp);
            writer.send(msg);

            // start new file if timestamp changed only
            if (time == msg.getTimeStampMs())
                startNewFile = false;
            else if (count % 100 == 0)
                startNewFile = writer.count() > (options.rollSize << 20);

            // we can get messages with old time
            if (msg.getTimeStampMs() > time)
                time = msg.getTimeStampMs();

            if (++count % options.threshold == 0) {
                writer.endBlock();
                writer.startBlock(String.valueOf(version));
            } else if (startNewFile) {
                writer.endBlock();
                writer.close();
                writer = null;
                commitFile();
            }
        } catch (IOException e) {
            Util.close(writer);
            writer = null;
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    public long         onTruncate(long version, long timestamp) {
        this.version = version;
        Util.close(writer);
        writer = null;

        return time = replicator.truncateFiles(timestamp, files);
    }

    public void         onSchemaChange(long version) {
        this.version = version;
        Util.close(writer);
        writer = null;

        replicator.clearFiles(files);
        time = Long.MIN_VALUE;
    }

    public void         close() {
        try {
            if (writer != null) {
                writer.endBlock();
                Util.close(writer);
                writer = null;
                commitFile();
            }
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        } finally {
            Util.close(writer);
        }
    }
}