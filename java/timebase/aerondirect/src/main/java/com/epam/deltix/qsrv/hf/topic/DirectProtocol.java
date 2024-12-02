/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.topic;

import org.agrona.BitUtil;

/**
 * Communication types:
 * <ul>
 *     <li>A) From Loader to Consumer - Message Data - high traffic. Low latency required. Aeron.</li>
 *     <li>B) From Loader to Server and from Server to Loader - new loader registration process. VSChannel. Executed once per loader.</li>
 *     <li>C) From Consumer to Server and from Server to Consumer - new consumer registration process. VSChannel. Executed once per consumer.</li>
 * </ul>
 *
 * Conventions:
 * <ul>
 *     <li>Consumer reads all message data from A-type channel.</li>
 *     <li>Types are defined upon topic creation and can't be changed. We send only type index with each message.</li>
 *     <li>Symbol data is sent with each message. Reason: consumers can join "topics" at any moment,
 *     as result it very difficult to deliver symbol mapping data without potential latency penalties.</li>
 * </ul>
 *
 *
 * @author Alexei Osipov
 */
public class DirectProtocol {
    public static final int PROTOCOL_VERSION = 3;

    public static final int PROTOCOL_VERSION_ADDED_TARGET_STREAM = 3;


    public static final byte CODE_MSG = 1; // Data message
    public static final byte CODE_END_OF_STREAM = 4; // Indicated that specific loader is closed and will not publish more data

    // IMPORTANT! Code field is placed not at the beginning of message (offset 0) but at offset 8.
    // This is needed to have aligned access to the "TIME" field without wasting extra space for padding.


    // Data message structure
    private static final AlignedField TIME_FIELD = new AlignedField(Long.BYTES, 0);
    private static final AlignedField CODE_FIELD = AlignedField.create(Byte.BYTES, TIME_FIELD); // Special field - determines message type in topics protocol
    private static final AlignedField MSG_TYPE_FIELD = AlignedField.create(Byte.BYTES, CODE_FIELD);
    //private static final AlignedField INSTRUMENT_FIELD = AlignedField.create(Byte.BYTES, MSG_TYPE_FIELD);

    public static final int TIME_OFFSET = TIME_FIELD.getOffset(); // 0
    public static final int CODE_OFFSET = CODE_FIELD.getOffset(); // 8
    public static final int TYPE_OFFSET = MSG_TYPE_FIELD.getOffset(); // 9
    //public static final int INSTRUMENT_OFFSET = INSTRUMENT_FIELD.getOffset(); // 10
    public static final int SYMBOL_OFFSET = MSG_TYPE_FIELD.getEndOffset(); // 11
    public static final int DATA_MSG_MIN_HEAD_SIZE = SYMBOL_OFFSET;

    // "End of stream" message structure
    private static final AlignedField SESSION_ID_FIELD = AlignedField.create(Integer.BYTES, CODE_FIELD);
    public static final int SESSION_ID_OFFSET = SESSION_ID_FIELD.getOffset();
    public static final int END_OF_STREAM_MSG_SIZE = SESSION_ID_FIELD.getEndOffset();


    private static final class AlignedField {
        private final int byteSize; // Byte size of field
        private final int offset; // Offset of field in message (aligned to byteSize)

        AlignedField(int byteSize, int unalignedOffset) {
            this.byteSize = byteSize;
            this.offset = BitUtil.align(unalignedOffset, byteSize);
        }

        static AlignedField create(int byteSize, AlignedField previous) {
            return new AlignedField(byteSize, previous.getEndOffset());
        }

        int getOffset() {
            return offset;
        }

        int getEndOffset() {
            return offset + byteSize;
        }
    }
}