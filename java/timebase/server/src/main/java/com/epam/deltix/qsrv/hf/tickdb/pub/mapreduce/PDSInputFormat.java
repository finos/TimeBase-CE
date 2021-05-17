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
package com.epam.deltix.qsrv.hf.tickdb.pub.mapreduce;

import com.epam.deltix.qsrv.dtb.fs.pub.FSFactory;
import com.epam.deltix.qsrv.dtb.store.impl.PDSFactory;
import com.epam.deltix.qsrv.dtb.store.pub.*;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.util.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class PDSInputFormat extends InputFormat<LongWritable, InstrumentMessage> {

    static final Logger     LOGGER = Logger.getLogger("deltix.mapreduce");

    public static String    STREAM_ROOT = "deltix.mapreduce.stream.root";
    public static String    STREAM_SCHEMA = "deltix.mapreduce.stream.schema";
    public static String    RAW_READER = "deltix.mapreduce.reader.raw";
    public static String    SPLIT_SIZE = "deltix.mapreduce.splitsize";

    public static long      SPLIT_SIZE_DEF = 1 << 29;
    public static boolean   RAW_READER_DEF = false;

    public static PersistentDataStore           store;
    private TSRoot                              root;

    static {
        //PDSFactory.allocate((int) Math.min(PDSFactory.MAX_HEAP_SIZE, Util.getAvailableMemory() / 2));
        store = PDSFactory.create();
        store.start();
    }

    /**
     * Generate the list of files and make them into FileSplits.
     */
    @Override
    public synchronized List<InputSplit> getSplits(JobContext context) throws IOException, InterruptedException {
        Configuration configuration = context.getConfiguration();

        long splitSize = configuration.getLong(SPLIT_SIZE, SPLIT_SIZE_DEF);

        List<InputSplit> splits = new ArrayList<>();
        Path[] files = getInputPaths(context);
        for (Path file : files) {
            FileSystem fs = file.getFileSystem(configuration);
            FileStatus[] status = fs.listStatus(file);
            assert status.length > 0;

            long fileLength = status[0].getLen();
            BlockLocation[] blkLocations = fs.getFileBlockLocations(status[0], 0, fileLength);
            assert blkLocations.length > 0;
            String[] fileHosts = blkLocations[0].getHosts();

            boolean splitFound = false;
            for (InputSplit inputSplit : splits) {
                TSFileInputSplit split = (TSFileInputSplit) inputSplit;
                String[] splitHosts = split.getLocations();
                String[] hosts = getHostsIntersection(fileHosts, splitHosts);
                if (split.getLength() < splitSize && hosts.length > 0) {
                    split.addFile(file, fileLength);
                    split.setHosts(hosts);
                    splitFound = true;
                    break;
                }
            }

            if (!splitFound) {
                TSFileInputSplit split = new TSFileInputSplit();
                split.addFile(file, fileLength);
                split.setHosts(fileHosts);
                splits.add(split);
            }
        }

        logSplits(splits);

        return splits;
    }


    @Override
    public synchronized RecordReader<LongWritable, InstrumentMessage> createRecordReader(InputSplit inputSplit, TaskAttemptContext taskAttemptContext) throws IOException, InterruptedException {
        if (!(inputSplit instanceof TSFileInputSplit))
            throw new IllegalStateException("InputSplit " + inputSplit + " is not expected.");

        TSFileInputSplit split = (TSFileInputSplit) inputSplit;
        Configuration configuration = taskAttemptContext.getConfiguration();
        RecordClassSet recordClassSet = TDBProtocol.readClassSet(configuration.get(STREAM_SCHEMA));
        boolean isRaw = configuration.getBoolean(RAW_READER, RAW_READER_DEF);

        return new TSFileReader(getRoot(configuration), split.getFiles(), recordClassSet, isRaw);
    }

    private Path[] getInputPaths(JobContext context) throws IOException {
        TSRoot tsRoot = getRoot(context.getConfiguration());

        ArrayList<TSRef> inputFiles = new ArrayList<>();
        tsRoot.selectTimeSlices(null, null, inputFiles);
        Path[] result = new Path[inputFiles.size()];
        for (int i = 0; i < inputFiles.size(); i++) {
            String path = inputFiles.get(i).getPath();
            if (path != null)
                result[i] = new Path(path);
        }
        return result;
    }

    private synchronized TSRoot getRoot(Configuration config) throws IOException {
        if (root == null) {
            String path = config.get(STREAM_ROOT);
            root = store.createRoot(null, FSFactory.createPath(path));
            root.open(true);
        }

        return root;
    }

    private String[] getHostsIntersection(String[] hosts1, String[] hosts2) {
        if (hosts1 == null || hosts2 == null)
            return new String[0];

        List<String> intersected = new ArrayList<>();
        for (String host1 : hosts1) {
            for (String host2 : hosts2) {
                if (host1.equalsIgnoreCase(host2)) {
                    intersected.add(host1);
                    break;
                }
            }
        }

        return intersected.toArray(new String[intersected.size()]);
    }

    private void logSplits(List<InputSplit> splits) throws IOException, InterruptedException {
        for (InputSplit inputSplit : splits) {
            TSFileInputSplit split = (TSFileInputSplit) inputSplit;
            Path[] paths = split.getFiles();
            LOGGER.info("Split: " + paths.length + " files, " + split.getLength() + " bytes " +
                ", hosts: [" + StringUtils.createCommaSepList(split.getLocations()) + "]");
        }
    }

}
