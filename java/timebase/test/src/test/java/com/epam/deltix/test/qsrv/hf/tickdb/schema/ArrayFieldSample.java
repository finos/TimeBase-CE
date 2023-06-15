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
package com.epam.deltix.test.qsrv.hf.tickdb.schema;

import com.epam.deltix.timebase.messages.IdentityKey;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.WritableValue;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.FixedUnboundEncoder;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldInfo;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;

/**
 * Demonstrates how to create, write and read a stream with a field of an array type (primitive base element)
 */
public class ArrayFieldSample {
    public static final String      STREAM_KEY = "array.stream";

    public static final RecordClassDescriptor CUSTOM_ARRAY_CLASS =
            new RecordClassDescriptor (
                    "MyArrayClass",
                    "Custom Type with Array fields",
                    false,
                    null,
                    new NonStaticDataField(
                            "prices",
                            "Prices (FLOAT)",
                            new ArrayDataType(true, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true))
                    ),
                    new NonStaticDataField(
                            "sizes",
                            "Sizes (INT)",
                            new ArrayDataType(true, new IntegerDataType(IntegerDataType.ENCODING_INT64, true))
                    )
            );

    public static DXTickStream      createSampleStream (DXTickDB db) {
        DXTickStream stream = db.getStream (STREAM_KEY);

        if (stream == null) {
            stream =
                    db.createStream (
                            STREAM_KEY,
                            STREAM_KEY,
                            "Description Line1\nLine 2\nLine 3",
                            0
                    );

            stream.setFixedType (CUSTOM_ARRAY_CLASS);
        }

        return stream;
    }

    public static void      readData (DXTickDB db) {
        DXTickStream            stream = db.getStream (STREAM_KEY);
        RecordClassDescriptor   classDescriptor = stream.getFixedType ();

        //
        //  Always use raw = true for custom messages.
        //
        SelectionOptions options = new SelectionOptions (true, false);

        //
        //  List of entities to subscribe (if null, all stream entities will be used)
        //
        IdentityKey[] entities = null;

        //
        //  List of types to subscribe - select only "MyClass" messages
        //
        String[] types = new String[] { "MyArrayClass" };

        //
        //  Cursor is equivalent to a JDBC ResultSet
        //
        TickCursor cursor = stream.select (Long.MIN_VALUE, options, types, entities);

        MemoryDataInput in = new MemoryDataInput ();
        UnboundDecoder decoder =
                CodecFactory.COMPILED.createFixedUnboundDecoder (classDescriptor);

        try {
            while (cursor.next ()) {
                //
                //  We can safely cast to RawMessage because we have requested
                //  a raw message cursor.
                //
                RawMessage msg = (RawMessage) cursor.getMessage ();
                //
                //  Print out standard fields
                //
                System.out.printf (
                        "%tT.%<tL %s",
                        msg.getTimeStampMs(),
                        msg.getSymbol()
                );
                //
                //  Iterate over custom fields.
                //
                in.setBytes (msg.data, msg.offset, msg.length);
                decoder.beginRead (in);

                while (decoder.nextField ()) {
                    NonStaticFieldInfo df = decoder.getField ();
                    //
                    //  In case of array type getString () method has a little usefulness.
                    //
                    DataType type = decoder.getField().getType();
                    System.out.printf(",%s: %s", df.getName(), UnboundUtils.readArray((ArrayDataType) type, decoder));
                }

                System.out.println ();
            }
        } finally {
            cursor.close ();
        }
    }

    public static void writeIntoStream(DXTickStream stream) {

        //DXTickStream            stream = db.getStream (STREAM_KEY);
        RecordClassDescriptor   classDescriptor = stream.getFixedType ();
        RawMessage              msg = new RawMessage (classDescriptor);

        //  Always use raw = true for custom messages.
        LoadingOptions options = new LoadingOptions (true);
        TickLoader loader = stream.createLoader (options);

        //  Re-usable buffer for collecting the encoded message
        MemoryDataOutput dataOutput = new MemoryDataOutput ();
        FixedUnboundEncoder encoder =
                CodecFactory.COMPILED.createFixedUnboundEncoder (classDescriptor);

        final double[] doubles = new double[5];
        final long[] longes = new long[5];

        try {
            //  Generate a few messages
            for (int ii = 1; ii < 100; ii++) {
                //
                //  Set up standard fields
                //
                msg.setTimeStampMs(System.currentTimeMillis ());
                msg.setSymbol("AAPL");

                //
                //  Set up custom fields
                //
                dataOutput.reset ();
                encoder.beginWrite (dataOutput);
                //
                //  Fields must be set in the order the encoder
                //  expects them, which in the case of a fixed-type stream
                //  with a non-inherited class descriptor is equivalent to the
                //  order of the class descriptor's fields.
                //
                encoder.nextField(); // prices
                DataType type = encoder.getField().getType();
                for (int i = 0; i < doubles.length; i++) {
                    doubles[i] = ii + (double) (i + 1) / 100;
                }
                writeArray(doubles, (ArrayDataType) type, encoder);

                encoder.nextField();   // sizes
                type = encoder.getField().getType();
                for (int i = 0; i < longes.length; i++) {
                    longes[i] = ii + i;
                }
                writeArray(longes, (ArrayDataType) type, encoder);

                if (encoder.nextField ())   // make sure we are at end
                    throw new RuntimeException ("unexpected field: " + encoder.getField ().toString ());

                encoder.endWrite();

                msg.setBytes (dataOutput, 0);

                loader.send (msg);
            }
        } finally {
            loader.close ();
        }
    }

    private static void writeArray(double[] values, ArrayDataType type, WritableValue uenc) {
        final int len = values.length;
        uenc.setArrayLength(len);

        for (int i = 0; i < len; i++) {
            final double v = values[i];
            if (Double.isNaN(v) && type.getElementDataType().isNullable())
                continue;

            final WritableValue wv = uenc.nextWritableElement();
            wv.writeDouble(v);
        }
    }

    private static void writeArray(long[] values, ArrayDataType type, WritableValue uenc) {
        final int len = values.length;
        uenc.setArrayLength(len);

        for (int i = 0; i < len; i++) {
            final long v = values[i];
            if (v == IntegerDataType.INT64_NULL && type.getElementDataType().isNullable())
                continue;

            final WritableValue wv = uenc.nextWritableElement();
            wv.writeLong(v);
        }
    }

    public static void      main (String [] args) {
        if (args.length == 0)
            args = new String [] { "dxtick://localhost:8011" };

        DXTickDB db = TickDBFactory.createFromUrl(args[0]);

        db.open (false);

        try {
            createSampleStream (db);
            writeIntoStream (db.getStream(STREAM_KEY));
            readData (db);
        } finally {
            db.close ();
        }
    }
}