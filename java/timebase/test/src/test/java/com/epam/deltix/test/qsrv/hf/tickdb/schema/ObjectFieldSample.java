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
package com.epam.deltix.test.qsrv.hf.tickdb.schema;

import com.epam.deltix.timebase.messages.IdentityKey;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.WritableValue;
import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;
import com.epam.deltix.util.time.GMT;

import java.text.ParseException;

/**
 *
 */
public class ObjectFieldSample {
    public static final String STREAM_KEY = "object.stream";

    private static final RecordClassDescriptor INSTRUMENT_CLASS =
            new RecordClassDescriptor(
                    "InstrumentClass",
                    "Instrument",
                    false,
                    null,
                    new NonStaticDataField(
                            "symbol",
                            "Symbol",
                            new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, false)),
                    new NonStaticDataField(
                            "Product",
                            "product",
                            new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
                    new NonStaticDataField(
                            "CFICode",
                            "CFI Code",
                            new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
                    new NonStaticDataField(
                            "IssueDate",
                            "Issue Date",
                            new DateTimeDataType(true)),
                    new NonStaticDataField(
                            "SecurityDesc",
                            "Security Description",
                            new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false))
            );

    private static final RecordClassDescriptor MARKET_ORDER_TYPE_CLASS =
            new RecordClassDescriptor(
                    "MarketOrderType",
                    null,
                    false,
                    null
            );

    private static final RecordClassDescriptor LIMIT_ORDER_TYPE_CLASS =
            new RecordClassDescriptor(
                    "LimitOrderType",
                    null,
                    false,
                    null,
                    new NonStaticDataField(
                            "price",
                            "Limit price",
                            new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false)),
                    new NonStaticDataField(
                            "discretionAmount",
                            "Discretion offset",
                            new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true))
            );

    private static final RecordClassDescriptor STOP_ORDER_TYPE_CLASS =
            new RecordClassDescriptor(
                    "StopOrderType",
                    null,
                    false,
                    null,
                    new NonStaticDataField(
                            "stopPrice",
                            "Stop price",
                            new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false))
            );

    public static final RecordClassDescriptor CUSTOM_OBJECT_CLASS =
            new RecordClassDescriptor(
                    "MyObjectClass",
                    "Custom Type with Object fields",
                    false,
                    null,
                    new NonStaticDataField(
                            "fixed",
                            "Fixed Object",
                            new ClassDataType(true, INSTRUMENT_CLASS)),
                    new NonStaticDataField(
                            "poly",
                            "Polymorphic Object",
                            new ClassDataType(true, MARKET_ORDER_TYPE_CLASS, LIMIT_ORDER_TYPE_CLASS, STOP_ORDER_TYPE_CLASS))
            );


    public static DXTickStream createSampleStream(DXTickDB db) {
        DXTickStream stream = db.getStream(STREAM_KEY);

        if (stream == null) {
            stream =
                    db.createStream(
                            STREAM_KEY,
                            STREAM_KEY,
                            "Description Line1\nLine 2\nLine 3",
                            0
                    );

            stream.setFixedType(CUSTOM_OBJECT_CLASS);
        }

        return stream;
    }

    public static void readDataUnbound(DXTickDB db) {
        DXTickStream stream = db.getStream(STREAM_KEY);
        RecordClassDescriptor classDescriptor = stream.getFixedType();

        //
        //  Always use raw = true for custom messages.
        //
        SelectionOptions options = new SelectionOptions(true, false);

        //
        //  List of entities to subscribe (if null, all stream entities will be used)
        //
        IdentityKey[] entities = null;

        //
        //  List of types to subscribe - select only "MyClass" messages
        //
        String[] types = new String[]{"MyObjectClass"};

        //
        //  Cursor is equivalent to a JDBC ResultSet
        //
        TickCursor cursor = stream.select(Long.MIN_VALUE, options, types, entities);

        MemoryDataInput in = new MemoryDataInput();
        UnboundDecoder decoder =
                CodecFactory.COMPILED.createFixedUnboundDecoder(classDescriptor);

        try {
            while (cursor.next()) {
                //
                //  We can safely cast to RawMessage because we have requested
                //  a raw message cursor.
                //
                RawMessage msg = (RawMessage) cursor.getMessage();
                //
                //  Print out standard fields
                //
                System.out.printf(
                        "%tT.%<tL %s",
                        msg.getTimeStampMs(),
                        msg.getSymbol()
                );
                //
                //  Iterate over custom fields.
                //
                in.setBytes(msg.data, msg.offset, msg.length);
                decoder.beginRead(in);

                while (decoder.nextField()) {
                    NonStaticFieldInfo df = decoder.getField();
                    //
                    //  In case of object type getString () method has a little usefulness.
                    //
                    System.out.printf(",%s: %s", df.getName(), UnboundUtils.readObject(decoder));
                }

                System.out.println();
            }
        } finally {
            cursor.close();
        }
    }


    private static final int NUM_OF_RECORDS = 3;

    private static final String INSTRUMENT_DATA[][] = {
            {"AAPL", null, "ES", "1980-12-12", "Apple Inc."},
            {"T 2.75 11/15/23 Govt",null, "DB", "2013-11-15", "10-Year US TREASURY Note"},
            {"@NQZ13", null, "FF", null, "E-MINI NASDAQ 100 DECEMBER 2013"},
            {"IBM1419D100", null, "OC", null, "IBM APR 2014 C 100.000"}
    };

    private static final Object ORDER_DATA[][] = {
            {0},
            {1, 500.05, 0.05},
            {2, 510.05},
            {1, 503.05, 3.05}
    };

    public static void writeIntoStreamUnbound(DXTickStream stream) {
        //DXTickStream stream = db.getStream(STREAM_KEY);

        RecordClassDescriptor classDescriptor = stream.getFixedType();
        RawMessage msg = new RawMessage(classDescriptor);

        //  Always use raw = true for custom messages.
        LoadingOptions options = new LoadingOptions(true);
        TickLoader loader = stream.createLoader(options);

        //  Re-usable buffer for collecting the encoded message
        MemoryDataOutput dataOutput = new MemoryDataOutput();
        FixedUnboundEncoder encoder =
                CodecFactory.COMPILED.createFixedUnboundEncoder(classDescriptor);

        // substitute 1st element with RCD from Timebase
        final RecordClassDescriptor[] rcds = ((ClassDataType) classDescriptor.getField("poly").getType()).getDescriptors();
        for (int i = 0; i < NUM_OF_RECORDS; i++) {
            ORDER_DATA[i][0] = rcds[(Integer) ORDER_DATA[i][0]];
        }

        try {
            //  Generate a few messages
            for (int ii = 0; ii < NUM_OF_RECORDS; ii++) {
                //
                //  Set up standard fields
                //
                msg.setTimeStampMs(System.currentTimeMillis());
                msg.setSymbol(INSTRUMENT_DATA[ii][0]);
                //
                //  Set up custom fields
                //
                dataOutput.reset();
                encoder.beginWrite(dataOutput);
                //
                //  Fields must be set in the order the encoder
                //  expects them, which in the case of a fixed-type stream
                //  with a non-inherited class descriptor is equivalent to the
                //  order of the class descriptor's fields.
                //
                encoder.nextField(); // instrument
                writeObject(INSTRUMENT_DATA[ii], encoder, INSTRUMENT_CLASS);

                encoder.nextField();   // order type
                UnboundUtils.writeObjectPoly(ORDER_DATA[ii], encoder);

                if (encoder.nextField())   // make sure we are at end
                    throw new RuntimeException("unexpected field: " + encoder.getField().toString());

                encoder.endWrite();

                msg.setBytes(dataOutput, 0);

                loader.send(msg);
            }
        } finally {
            loader.close();
        }
    }

    private static void writeObject(Object[] values, WritableValue uenc, RecordClassDescriptor rcd) {
        final int len = values.length;
        final UnboundEncoder encoder = uenc.getFieldEncoder(rcd);

        for (int i = 0; i < len && encoder.nextField(); i++) {
            Object v = values[i];
            if (v == null)
                continue;

            if (v instanceof Double)
                encoder.writeDouble((Double) v);
            else if (v instanceof String) {
                if (encoder.getField().getType() instanceof DateTimeDataType)
                    try {
                        encoder.writeLong(GMT.parseDate((String) v).getTime());
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                else
                    encoder.writeString((CharSequence) v);
            }
            else
                throw new IllegalArgumentException("unsupported type " + v.getClass().getName());
        }
    }

    public static void main(String[] args) {
        if (args.length == 0)
            args = new String[]{"dxtick://localhost:8011"};

        DXTickDB db = TickDBFactory.createFromUrl(args[0]);

        db.open(false);

        try {
            createSampleStream(db);
            writeIntoStreamUnbound(db.getStream(STREAM_KEY));
            readDataUnbound(db);
        } finally {
            db.close();
        }
    }
}