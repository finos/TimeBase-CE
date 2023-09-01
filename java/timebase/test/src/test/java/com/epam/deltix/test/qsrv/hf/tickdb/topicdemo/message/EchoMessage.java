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
package com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.message;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

/**
 * @author Alexei Osipov
 */
public class EchoMessage extends InstrumentMessage {
    public int experimentId;
    public long originalMessageId;
    public long originalNanoTime;
    public long originalTimeStamp;
    public byte consumerNumber;

    public int getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(int experimentId) {
        this.experimentId = experimentId;
    }

    public long getOriginalMessageId() {
        return originalMessageId;
    }

    public void setOriginalMessageId(long originalMessageId) {
        this.originalMessageId = originalMessageId;
    }

    public long getOriginalNanoTime() {
        return originalNanoTime;
    }

    public void setOriginalNanoTime(long originalNanoTime) {
        this.originalNanoTime = originalNanoTime;
    }

    public long getOriginalTimeStamp() {
        return originalTimeStamp;
    }

    public void setOriginalTimeStamp(long originalTimeStamp) {
        this.originalTimeStamp = originalTimeStamp;
    }

    public byte getConsumerNumber() {
        return consumerNumber;
    }

    public void setConsumerNumber(byte consumerNumber) {
        this.consumerNumber = consumerNumber;
    }

    public static RecordClassDescriptor getRecordClassDescriptor() {
        final String name = EchoMessage.class.getName();
        final DataField[] fields = {
                new NonStaticDataField(
                        "experimentId", "Experiment ID",
                        new IntegerDataType(IntegerDataType.ENCODING_INT32, false)),
                new NonStaticDataField(
                        "originalMessageId", "Original message ID",
                        new IntegerDataType(IntegerDataType.ENCODING_INT64, false)),
                new NonStaticDataField(
                        "originalNanoTime", "Original message nano time",
                        new IntegerDataType(IntegerDataType.ENCODING_INT64, false)),
                new NonStaticDataField(
                        "originalTimeStamp", "Original message timeStamp",
                        new IntegerDataType(IntegerDataType.ENCODING_INT64, false)),
                new NonStaticDataField(
                        "consumerNumber", "Consumer ID",
                        new IntegerDataType(IntegerDataType.ENCODING_INT8, false)),
        };

        return new RecordClassDescriptor(name, name, false, null, fields);
    }
}