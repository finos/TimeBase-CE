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
import org.apache.hadoop.mapreduce.JobContext;

import java.io.IOException;

/**
 *
 */
public interface WritableMessage<T> {
    public static String MAPPER_OUTPUT_TYPE = "deltix.mapreduce.mapper.output.type";

    public void     set(T message, JobContext context) throws IOException;
    public T        get(JobContext context) throws IOException;
}
