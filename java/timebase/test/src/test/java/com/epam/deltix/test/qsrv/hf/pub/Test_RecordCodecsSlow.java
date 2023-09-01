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

import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * User: BazylevD
 * Date: Dec 10, 2009
 * Time: 9:23:48 PM
 */
@Category(JUnitCategories.TickDBCodecs.class)
public class Test_RecordCodecsSlow extends Test_RecordCodecsBase {

    private static final RecordClassDescriptor cdMsgClassBinary =
            new RecordClassDescriptor(
                    "My Binary Descriptor",
                    "My Binary class def",
                    false,
                    null,
                    new NonStaticDataField("int", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true)),
                    new NonStaticDataField("long", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true)),
                    new NonStaticDataField("string", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),

                    new NonStaticDataField("binary1", null, new BinaryDataType(true, 0)),
                    new NonStaticDataField("binary2", null, new BinaryDataType(true, 1024, 0) )
            );

    @Test
    public void testBinaryAPIComp() throws Exception {
        setUpComp();
        testBinaryAPI();
    }

    @Test
    public void testBinaryAPIIntp() throws Exception {
        setUpIntp();
        testBinaryAPI();
    }

    private void testBinaryAPI() throws Exception {
        // test allowed nulls
        final List<Object> values = new ArrayList<Object>(4);
        values.add(10000);
        values.add(9000000000L);
        values.add("Hi Nicolia!");
        final byte[] bin1 = new byte[0x10000000]; // 16MB
        for (int i = 0; i < bin1.length; i++) {
            bin1[i] = (byte)i;
        }
        values.add(bin1);
        values.add(null);

        MemoryDataOutput out = unboundEncode(values, cdMsgClassBinary);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());

        UnboundDecoder udec = factory.createFixedUnboundDecoder(cdMsgClassBinary);
        udec.beginRead(in);
        for (int i = 0; i < values.size() - 1; i++) {
            udec.nextField();
        }

        int len = udec.getBinaryLength();
        ByteArrayOutputStream os = new ByteArrayOutputStream(len);
        udec.getBinary(0, len, os);
        Assert.assertArrayEquals(bin1, os.toByteArray());

        len = udec.getBinaryLength();
        os.reset();
        udec.getBinary(0, len, os);
        Assert.assertArrayEquals(bin1, os.toByteArray());

        final byte[] binOut = new byte[len];
        udec.getBinary(0, len, binOut, 0);
        Assert.assertArrayEquals(bin1, binOut);

        udec.getBinary(0, len, binOut, 0);
        Assert.assertArrayEquals(bin1, binOut);
    }
}