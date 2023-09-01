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
package com.epam.deltix.test.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.qsrv.hf.pub.codec.CompiledCodecMetaFactory;
import com.epam.deltix.qsrv.hf.pub.codec.FixedBoundEncoder;
import com.epam.deltix.qsrv.hf.pub.messages.BinaryMessage;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.collections.generated.ByteArrayList;
import com.epam.deltix.util.memory.MemoryDataOutput;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;

/**
 * @author Daniil Yarmalkevich
 * Date: 11/12/2019
 */
@Category(JUnitCategories.TickDBFast.class)
public class Test_DecoderToString {

    private static final byte[] BINARY = new byte[]{1, 2, 3, 4, 5};

    @Test
    public void testBinaryToString() {
        MemoryDataOutput out = new MemoryDataOutput();
        FixedBoundEncoder encoder = CompiledCodecMetaFactory.INSTANCE
                .createFixedBoundEncoderFactory(TypeLoaderImpl.DEFAULT_INSTANCE, BinaryMessage.getClassDescriptor())
                .create();
        long timestamp = System.currentTimeMillis();
        BinaryMessage msg = createNative(timestamp);
        encoder.encode(msg, out);
        RawMessage raw = new RawMessage(BinaryMessage.getClassDescriptor());
        raw.setBytes(out);
        assertEquals("com.epam.deltix.qsrv.hf.pub.messages.BinaryMessage,,<null>,binary_n:1, 2, 3, 4, 5,char_c:C,char_n:N",
                raw.toString());
    }

    public BinaryMessage createNative(long timestamp) {
        BinaryMessage binaryMessage = new BinaryMessage();
        binaryMessage.setSymbol("TEST");
        binaryMessage.setTimeStampMs(timestamp);
        binaryMessage.binary_n = new ByteArrayList(BINARY);
        binaryMessage.char_c = 'C';
        binaryMessage.char_n = 'N';
        return binaryMessage;
    }

}