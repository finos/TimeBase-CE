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
package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;


import com.epam.deltix.util.memory.*;
import org.junit.*;

import java.util.Random;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;


import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

/**
 *  Store, then read a large message with a binary field.
 */
@Category(TickDBFast.class)
public class Test_LargeMessage {
    public static final String      STREAM_KEY = "test.stream";

    public static final RecordClassDescriptor   CUSTOM_CLASS =
        new RecordClassDescriptor (
            "MyClass",
            "My Custom Class Title",
            false,
            null,
            new NonStaticDataField (
                "longField",
                "Very Long Field (BINARY)",
                new BinaryDataType (false, 0),
                null
            )
        );

    private DXTickDB     db;

    @Before
    public final void           startup() throws Throwable {
        db = TickDBFactory.create (TDBRunner.getTemporaryLocation());
        db.format ();
    }

    @After
    public final void           teardown () {
        db.close ();
    }

    class LargeMessageStoreRetrieve extends TickDBTest {
        private final CodecFactory        codecFactory;

        public LargeMessageStoreRetrieve (CodecFactory codecFactory) {
            this.codecFactory = codecFactory;
        }

        @Override
        public void                 run (DXTickDB db) throws Exception {
            StreamOptions               options =
                new StreamOptions (
                    StreamScope.DURABLE,
                    "Stream Name",
                    "Description Line1\nLine 2\nLine 3",
                    StreamOptions.MAX_DISTRIBUTION
                );

            options.setFixedType (CUSTOM_CLASS);
            options.bufferOptions = new BufferOptions ();
            options.bufferOptions.maxBufferSize = 100000;

            final DXTickStream          stream = db.createStream (STREAM_KEY, options);

            RecordClassDescriptor   classDescriptor = stream.getFixedType ();
            RawMessage              msg = new RawMessage (classDescriptor);
            
            msg.setSymbol("DLTX");

            LoadingOptions          loptions = new LoadingOptions (true);
            TickLoader              loader = stream.createLoader (loptions);
            MemoryDataOutput        dataOutput = new MemoryDataOutput ();
            byte []                 binaryData = new byte [0x1000000];

            for (int ii = 0; ii < binaryData.length; ii++)
                binaryData [ii] = (byte) ii;

            FixedUnboundEncoder     encoder =
                codecFactory.createFixedUnboundEncoder (classDescriptor);

            for (int ii = 1; ii < 20; ii++) {
                int sizeToUse = getExpectedSize (ii);

                dataOutput.reset ();
                encoder.beginWrite (dataOutput);

                encoder.nextField ();
                encoder.writeBinary (binaryData, 0, sizeToUse);

                assertFalse (encoder.nextField ());

                msg.setBytes (dataOutput, 0);

                loader.send (msg);
            }

            loader.close ();            

            SelectionOptions        soptions = new SelectionOptions (true, false);
            TickCursor              cursor = stream.select (0, soptions);
            MemoryDataInput         in = new MemoryDataInput ();
            UnboundDecoder          decoder =
                codecFactory.createFixedUnboundDecoder (classDescriptor);

            try {
                for (int ii = 1; cursor.next (); ii++) {
                    msg = (RawMessage) cursor.getMessage ();

                    in.setBytes (msg.data, msg.offset, msg.length);
                    decoder.beginRead (in);

                    decoder.nextField ();
                    assertEquals ("At message #" + ii, getExpectedSize (ii), decoder.getBinaryLength ());

                    assertFalse (decoder.nextField ());
                }
            } finally {
                cursor.close ();
            }
        }
    }

    class TransientTestLargeMessage extends TickDBTest {
        private final CodecFactory        codecFactory;

        public TransientTestLargeMessage (CodecFactory codecFactory) {
            this.codecFactory = codecFactory;
        }

        @Override
        public void                 run (DXTickDB db) throws Exception {
            StreamOptions               options =
                new StreamOptions (
                    StreamScope.TRANSIENT,
                    "Stream Name",
                    "Description Line1\nLine 2\nLine 3",
                    StreamOptions.MAX_DISTRIBUTION
                );

            options.setFixedType (CUSTOM_CLASS);
            options.bufferOptions = new BufferOptions ();
            options.bufferOptions.lossless = true;
            //options.bufferOptions.maxBufferSize = 50000;

            final DXTickStream          stream = db.createStream (STREAM_KEY, options);

            final RecordClassDescriptor   classDescriptor = stream.getFixedType ();
            RawMessage              msg = new RawMessage (classDescriptor);

            msg.setSymbol("DLTX");

            LoadingOptions          loptions = new LoadingOptions (true);
            TickLoader              loader = stream.createLoader (loptions);
            MemoryDataOutput        dataOutput = new MemoryDataOutput ();
            byte []                 binaryData = new byte [0x1000000];

            for (int ii = 0; ii < binaryData.length; ii++)
                binaryData [ii] = (byte) ii;

            FixedUnboundEncoder     encoder =
                codecFactory.createFixedUnboundEncoder (classDescriptor);

            final TickCursor cursor = stream.select (0, new SelectionOptions(true, true));

            final int messageCount = 200;
            final int[] sizes = new int[messageCount];

            Thread consumer = new Thread("Consumer") {

                @Override
                public void run() {
                    int count = 0;
                    MemoryDataInput         in = new MemoryDataInput ();
                    UnboundDecoder          decoder =
                        codecFactory.createFixedUnboundDecoder (classDescriptor);

                    while (count < messageCount && cursor.next()) {

                        RawMessage msg = (RawMessage) cursor.getMessage ();

                        in.setBytes (msg.data, msg.offset, msg.length);
                        decoder.beginRead (in);

                        decoder.nextField ();
                        assertEquals ("At message #" + count, sizes[count], decoder.getBinaryLength ());

                        assertFalse (decoder.nextField ());

                        count++;
                    }
                }
            };

            consumer.start();

            Random rnd = new Random(2010);

            for (int ii = 0; ii < messageCount; ii++) {
                int sizeToUse = sizes[ii] = rnd.nextInt(options.bufferOptions.maxBufferSize - 10) + 10;

                dataOutput.reset ();
                encoder.beginWrite (dataOutput);

                encoder.nextField ();
                encoder.writeBinary (binaryData, 0, sizeToUse);

                assertFalse (encoder.nextField ());

                msg.setBytes (dataOutput, 0);

                loader.send (msg);
            }

            loader.close ();            

//            MemoryDataInput         in = new MemoryDataInput ();
//            UnboundDecoder          decoder =
//                codecFactory.createFixedUnboundDecoder (classDescriptor);
//
//            try {
//                for (int ii = 1; cursor.next (); ii++) {
//                    msg = (RawMessage) cursor.getMessage ();
//
//                    in.setBytes (msg.data, msg.offset, msg.length);
//                    decoder.beginRead (in);
//
//                    decoder.nextField ();
//                    assertEquals ("At message #" + ii, getExpectedSize (ii), decoder.getBinaryLength ());
//
//                    assertFalse (decoder.nextField ());
//                }
//            } finally {
//                cursor.close ();
//            }
        }
    }

    private static int getExpectedSize (int ii) throws RuntimeException {
        int sizeToUse;
        switch (ii % 5) {
            case 0:
                sizeToUse = 23;
                break; // 1B
            case 1:
                sizeToUse = 501;
                break; // 2B
            case 2:
                //sizeToUse = 100000;
                sizeToUse = 63345;
                break; // 3B
            case 3:
                //sizeToUse = 16000000;
                sizeToUse = 63653;
                break;

            case 4:
                //sizeToUse = 16000000;
                sizeToUse = 0x400000 - 100;
                break;

            default:
                throw new RuntimeException ();
        }
        return sizeToUse;
    }

    @Test
    public void             interpLocal () throws Exception {
        new LargeMessageStoreRetrieve (CodecFactory.INTERPRETED).run (db);
    }

    @Test
    public void             interpRemote () throws Exception {
        new LargeMessageStoreRetrieve (CodecFactory.INTERPRETED).runRemote (db);
    }

    @Test
    public void             testTransient() throws Exception {
        new TransientTestLargeMessage (CodecFactory.INTERPRETED).run (db);
    }

    @Test
    public void             compLocal () throws Exception {
        new LargeMessageStoreRetrieve (CodecFactory.COMPILED).run (db);
    }
}
