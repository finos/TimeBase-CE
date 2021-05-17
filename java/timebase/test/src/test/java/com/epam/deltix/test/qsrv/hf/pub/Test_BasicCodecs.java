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
package com.epam.deltix.test.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.qsrv.hf.pub.FwdStringCodec;
import com.epam.deltix.qsrv.hf.pub.codec.TimeCodec;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.io.ByteArrayInputStreamEx;
import com.epam.deltix.util.io.ByteArrayOutputStreamEx;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(JUnitCategories.TickDBCodecs.class)
public class Test_BasicCodecs {
    private MemoryDataOutput        out;
    private MemoryDataInput         in;
            
    @Before
    public void     setUp () {
        out = new MemoryDataOutput (1); 
        in = new MemoryDataInput ();
    }
    
    @Test
    public void     testTimeCodec () throws IOException {
        testTimeCodec (1, 6);
        testTimeCodec (1000, 5);
        testTimeCodec (10000, 4);
        testTimeCodec (60000, 4);
        testTimeCodec (3600000, 3);
    }
    
    private void    testTimeCodec (long scale, int maxSize) throws IOException {
        out.reset ();
        
        for (int n = -10; n < 10; n++)
            TimeCodec.writeTime (n * scale, out);
        
        assertTrue (21 * maxSize >= out.getSize ());
        
        in.setBytes (out);
        
        for (int n = -10; n < 10; n++) {
            long        actual = TimeCodec.readTime (in);
            assertEquals (n * scale, actual);
        }
        
        DataInputStream dis = 
            new DataInputStream (new ByteArrayInputStream (out.getBuffer (), 0, out.getSize ()));
        
//        for (int n = -10; n < 10; n++) {
//            long        actual = TimeCodec.readTime (dis);
//            assertEquals (n * scale, actual);
//        }
        
        dis.close ();
    }

    @Test
    public void     testBarSizeCodec () throws IOException {
        testBarSizeCodec (1000, 60);
        testBarSizeCodec (60000, 60);
        testBarSizeCodec (3600000, 48);
    }
    
    private void    testBarSizeCodec (int scale, int max) throws IOException {
        out.reset ();
        
        for (int n = 1; n <= max; n++)
            com.epam.deltix.qsrv.hf.pub.codec.TimeIntervalCodec.write(n * scale, out);
        
        assertEquals (max, out.getSize ());
        
        in.setBytes (out);
        
        for (int n = 1; n <= max; n++) {
            int        actual = com.epam.deltix.qsrv.hf.pub.codec.TimeIntervalCodec.read(in);
            assertEquals (n * scale, actual);
        }
        
        DataInputStream dis = 
            new DataInputStream (new ByteArrayInputStream (out.getBuffer (), 0, out.getSize ()));
        
        for (int n = 1; n <= max; n++) {
            int        actual = com.epam.deltix.qsrv.hf.pub.codec.TimeIntervalCodec.read(dis);
            assertEquals (n * scale, actual);
        }
        
        dis.close ();
    }

    @Test
    public void     testNulls () throws IOException {
        out.reset();
        com.epam.deltix.qsrv.hf.pub.codec.TimeIntervalCodec.write(IntegerDataType.PINTERVAL_NULL, out);
        out.writePackedUnsignedInt(0);
        out.writePackedUnsignedLong(0);

        in.setBytes(out);
        int actual = com.epam.deltix.qsrv.hf.pub.codec.TimeIntervalCodec.read(in);
        assertEquals(IntegerDataType.PINTERVAL_NULL, actual);

        actual = in.readPackedUnsignedInt();
        assertEquals(0, actual);

        long actual2 = in.readPackedUnsignedLong();
        assertEquals(0, actual2);
    }

    @Test
    public void     testMessageSizeCodec () throws IOException {
        testMessageSizeCodec (0, 1);
        testMessageSizeCodec (1, 1);
        testMessageSizeCodec (127, 1);
        testMessageSizeCodec (128, 2);
        testMessageSizeCodec (255, 2);
        testMessageSizeCodec (256, 2);
        testMessageSizeCodec (0x3FFF, 2);
        testMessageSizeCodec (0x4000, 3);
        testMessageSizeCodec (0x3FFFFF, 3);

        out.reset ();
        try {
            MessageSizeCodec.write (0x400000, out);
            assertTrue (false);
        } catch (IllegalArgumentException x) {
        }

        try {
            MessageSizeCodec.write (-1, out);
            assertTrue (false);
        } catch (IllegalArgumentException x) {
        }

        int     x = MessageSizeCodec.read (new ByteArrayInputStreamEx ());

        assertEquals (-1, x);
    }

    private void    testMessageSizeCodec (int v, int expSize) throws IOException {
        ByteArrayOutputStreamEx     bs = new ByteArrayOutputStreamEx ();
        byte []                     arr = new byte [4];

        out.reset ();

        MessageSizeCodec.write (v, out);
        MessageSizeCodec.write (v, bs);

        int                         arrlen = MessageSizeCodec.write (v, arr, 0);

        assertEquals (expSize, out.getSize ());
        assertEquals (expSize, arrlen);
        assertEquals (expSize, bs.size ());

        for (int ii = 0; ii < expSize; ii++) {
            assertEquals (out.getBuffer () [ii], arr [ii]);
            assertEquals (out.getBuffer () [ii], bs.getInternalBuffer () [ii]);
        }

        in.setBytes (out);

        int     check = MessageSizeCodec.read (in);

        assertEquals (v, check);
        
        InputStream dis = new ByteArrayInputStream (out.getBuffer (), 0, out.getSize ());

        check = MessageSizeCodec.read (dis);

        assertEquals (v, check);
        
        dis.close ();
    }

    @Test
    public void     testFwdString() throws IOException {
        out.reset(50);
        final byte vByte1 = (byte) 1;
        final String vString1 = "Hi Nikola!!!";
        out.writeByte(vByte1);
        out.writeString(vString1);
        final String vString2 = "Gene's favorite message";
        FwdStringCodec.write(vString2, out);

        in.setBytes(out);
        final byte vByte2 = in.readByte();
        assertEquals(vByte1, vByte2);
        String vString12 = in.readString();
        assertEquals(vString12, vString1);
        String vString22 = FwdStringCodec.read(in).toString();
        assertEquals(vString22, vString2);

        // demonstrate that the encoding is resistant to array's shift
        in.setBytes(out.getBuffer(), 1, out.getSize() - 1);
        vString12 = in.readString();
        assertEquals(vString12, vString1);
        vString22 = FwdStringCodec.read(in).toString();
        assertEquals(vString22, vString2);
    }

    private static final int STEP = 0x5000;
    private static final int MAX = 0xFFFF;

    // test correct UTF encoding/decoding in of 1,2 and 3 octet cases 
    @Test
    public void     testUTF() throws IOException {

        final StringBuilder sb = new StringBuilder(STEP);
        for (int start = 0; start < MAX; start += STEP) {
            int end = start + STEP;
            if (end > MAX)
                end = MAX + 1;

            sb.setLength(0);
            for (int i = start; i < end; i++)
                sb.append((char) i);

            final String s = sb.toString();
            final MemoryDataOutput out = new MemoryDataOutput();
            out.writeString(s);
            final MemoryDataInput in = new MemoryDataInput(out);
            final String s2 = in.readString();
            assertTrue("range " + Integer.toHexString(start) + '-' + Integer.toHexString(end), s.equals(s2));
        }

        // test mixed 1+2+3 octets chars
        sb.setLength(0);
        sb.append("Hi Kolia!"); // 1-octet
        sb.append("\u041e\u041f\u0420"); // 2-octets (???)
        sb.append("Hi Gene!"); // 1-octet
        sb.append("\u040e\u041a\u041b"); // 2-octets (???)
        sb.append('\u6771').append('\u6772').append('\u6773'); // 3-octets (Chinese)
        sb.append("Hi Alex!"); // 1-octet

        final String s = sb.toString();
        final MemoryDataOutput out = new MemoryDataOutput();
        out.writeString(s);
        final MemoryDataInput in = new MemoryDataInput(out);
        final String s2 = in.readString();
        assertTrue("mixed case " + s, s.equals(s2));
    }


}
