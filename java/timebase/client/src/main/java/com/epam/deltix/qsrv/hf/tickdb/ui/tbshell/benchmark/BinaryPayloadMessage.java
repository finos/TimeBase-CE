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
package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.benchmark;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.BinaryDataType;
import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.collections.generated.ByteArrayList;

/**
 * @author Alexei Osipov
 */
public class BinaryPayloadMessage extends InstrumentMessage {

    public ByteArrayList payload = new ByteArrayList();

    public ByteArrayList getPayload() {
        return payload;
    }

    public void setPayload(ByteArrayList payload) {
        this.payload = payload;
    }

    public static RecordClassDescriptor getRecordClassDescriptor() {
        final String name = BinaryPayloadMessage.class.getName();
        final DataField[] fields = {
                new NonStaticDataField(
                        "payload", "Binary payload",
                        new BinaryDataType(false, BinaryDataType.MIN_COMPRESSION)),
        };

        return new RecordClassDescriptor(name, name, false, null, fields);
    }
}