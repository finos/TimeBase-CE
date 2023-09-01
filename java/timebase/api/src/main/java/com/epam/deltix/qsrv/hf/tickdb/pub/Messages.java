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
package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.timebase.messages.service.*;

/**
 * Created by Alex Karpovich on 29/07/2020.
 */
public abstract class Messages {

    public static final RecordClassDescriptor DATA_LOSS_MESSAGE_DESCRIPTOR = new RecordClassDescriptor(
            DataLossMessage.class,
            null,
            new DataField[] {
                    new NonStaticDataField("bytes", "Bytes Lost", new IntegerDataType(IntegerDataType.ENCODING_INT64, false)),
                    new NonStaticDataField("fromTime", "From Time", new DateTimeDataType(true))
            }
    );

    public static final RecordClassDescriptor STREAM_TRUNCATED_MESSAGE_DESCRIPTOR = new RecordClassDescriptor (
            StreamTruncatedMessage.class,
            null,
            new DataField[] {
                    new NonStaticDataField("version", "Version", new IntegerDataType(IntegerDataType.ENCODING_INT64, false)),
                    new NonStaticDataField("nanoTime", "Truncation Time", new IntegerDataType(IntegerDataType.ENCODING_INT64, true)),
                    new NonStaticDataField("instruments", "Instruments", new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, true))
            }
    );

    public static final RecordClassDescriptor META_DATA_CHANGE_MESSAGE_DESCRIPTOR = new RecordClassDescriptor (
            MetaDataChangeMessage.CLASS_NAME, MetaDataChangeMessage.CLASS_NAME, false, null,
            new NonStaticDataField ("version", "Version", new IntegerDataType(IntegerDataType.ENCODING_INT64, true)),
            new NonStaticDataField ("converted", "Converted", new BooleanDataType(false))
    );

    public static final RecordClassDescriptor REAL_TIME_START_MESSAGE_DESCRIPTOR = new RecordClassDescriptor (
            RealTimeStartMessage.DESCRIPTOR_GUID, RealTimeStartMessage.CLASS_NAME, RealTimeStartMessage.CLASS_NAME, false, null);


    public static final RecordClassDescriptor BINARY_MESSAGE_DESCRIPTOR = new RecordClassDescriptor(
            BinaryMessage.class,
            null,
            new DataField[] {
                    new NonStaticDataField("data", "Data buffer", BinaryDataType.getDefaultInstance())
            }
    );

    public static final RecordClassDescriptor ERROR_MESSAGE_DESCRIPTOR = new RecordClassDescriptor(
            ErrorMessage.DESCRIPTOR_GUID, ErrorMessage.CLASS_NAME, ErrorMessage.CLASS_NAME, false, null,
            new NonStaticDataField("errorType",     "Type",     VarcharDataType.getDefaultInstance()),
            new NonStaticDataField("seqNum",        "Sequence Number",    new IntegerDataType(IntegerDataType.ENCODING_INT64, false, null, null)),
            new NonStaticDataField("level",         "Level",    new EnumDataType(true, new EnumClassDescriptor(ErrorLevel.class))),
            new NonStaticDataField("messageText",   "Text",     VarcharDataType.getDefaultInstance()),
            new NonStaticDataField("details",       "Details",  VarcharDataType.getDefaultInstance())
    );

}