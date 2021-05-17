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

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickLoader;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import java.io.IOException;

/**
 *
 */
public class StreamWriter extends RecordWriter<LongWritable, InstrumentMessage> {
    protected TickLoader loader;

    public StreamWriter(TickLoader loader) {
        this.loader = loader;
    }

    @Override
    public void write(LongWritable longWritable, InstrumentMessage msg) throws IOException, InterruptedException {
        if (loader == null)
            throw new NullPointerException("Loader is closed."); // TODO: Consider using another type of exception. Like WriterClosedException or IllegalStateException.

        loader.send(msg);
    }

    @Override
    public void close(TaskAttemptContext taskAttemptContext) throws IOException, InterruptedException {
        loader.close();
        loader = null;
    }
}
