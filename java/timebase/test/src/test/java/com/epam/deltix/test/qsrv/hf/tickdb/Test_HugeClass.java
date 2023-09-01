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
package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;


import com.epam.deltix.util.memory.*;
import org.junit.*;
import static org.junit.Assert.*;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

/**
 *  Store, then read messages with a large number of fields. This also tests
 *  the transmission of large unicode symbols as part of the metadata.
 *  Finally, we test that the compiled codec factory falls back to
 *  interpreting logic when the class is too big.
 */
@Category(TickDBFast.class)
public class Test_HugeClass {
    public static final String      STREAM_KEY = "test.stream";
    public static final int         NUM_MESSAGES = 20;

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

    class HugeClassTester extends TickDBTest {
        private final int                   numFields;
        private final RecordClassDescriptor cd;
        private final CodecFactory          codecFactory;

        public HugeClassTester (int numFields, CodecFactory codecFactory) {
            this.codecFactory = codecFactory;
            this.numFields = numFields;

            DataField []    fields = new DataField [numFields];

            for (int ii = 0; ii < numFields; ii++) {
                fields [ii] =
                    new NonStaticDataField (
                        "\u041F\u043E\u043B\u0435" + ii,
                        "\u0412\u0441\u0435\u043C \u0431\u043E\u043B\u044C\u0448\u043E\u0439 \u043F\u0440\u0438\u0432\u0435\u0442" + ii,
                        new IntegerDataType ("INT8", true),
                        null
                    );
            }

            cd = new RecordClassDescriptor ("MyClass", "Title", false, null, fields);
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

            options.setFixedType (cd);

            final DXTickStream      stream = db.createStream (STREAM_KEY, options);

            RecordClassDescriptor   classDescriptor = stream.getFixedType ();
            RawMessage              msg = new RawMessage (classDescriptor);

            msg.setSymbol("DLTX");

            LoadingOptions          loptions = new LoadingOptions (true);
            TickLoader              loader = stream.createLoader (loptions);
            MemoryDataOutput        dataOutput = new MemoryDataOutput ();            

            FixedUnboundEncoder     encoder =
                codecFactory.createFixedUnboundEncoder (classDescriptor);

            for (int ii = 0; ii < NUM_MESSAGES; ii++) {
                dataOutput.reset ();
                encoder.beginWrite (dataOutput);

                int fidx;

                for (fidx = 0; encoder.nextField (); fidx++) {
                    int         x = (ii + fidx) & 0xFF;

                    if (x == 128)
                        encoder.writeNull ();
                    else {
                        if (x > 128)
                            x -= 256;
                        
                        encoder.writeInt (x);
                    }
                }
                
                assertEquals (numFields, fidx);

                msg.setBytes (dataOutput, 0);

                loader.send (msg);
            }

            loader.close ();
            
            SelectionOptions        soptions = new SelectionOptions (true, false);
            TickCursor              cursor = stream.select (0, soptions);
            MemoryDataInput         in = new MemoryDataInput ();
            UnboundDecoder          decoder =
                codecFactory.createFixedUnboundDecoder (classDescriptor);


            int                     midx;
            
            for (midx = 0; cursor.next (); midx++) {
                msg = (RawMessage) cursor.getMessage ();

                in.setBytes (msg.data, msg.offset, msg.length);
                decoder.beginRead (in);

                int fidx;

                for (fidx = 0; decoder.nextField (); fidx++) {
                    int         x = (midx + fidx) & 0xFF;

                    if (x == 128)
                        assertTrue (decoder.isNull ());
                    else {
                        assertFalse (decoder.isNull ());
                        
                        if (x > 128)
                            x -= 256;

                        assertEquals (x, decoder.getInt ());
                    }
                }

                assertEquals (numFields, fidx);
            }

            cursor.close ();            
        }
    }

    @Test
    public void             interpLocal () throws Exception {
        //  Smoke test
        new HugeClassTester (1000, CodecFactory.INTERPRETED).run (db);
    }

    @Test
    public void             interpRemote () throws Exception {
        // Test that remote layer can pass a huge metadata descriptor
        new HugeClassTester (1000, CodecFactory.INTERPRETED).runRemote (db);
    }

    @Test
    public void             compFallbackLocal () throws Exception {
        //  Test fallback that turns off compilation of a huge class
        new HugeClassTester (1000, CodecFactory.COMPILED).run (db);
    }

    @Test
    public void             compLocal () throws Exception {
        //  Test the biggest class that will be really compiled
        new HugeClassTester (
            CompiledCodecMetaFactory.MAX_COMPILED_FIELDS,
            CodecFactory.COMPILED
        ).run (db);
    }
}