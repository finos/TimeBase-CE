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
package com.epam.deltix.qsrv.hf.tickdb.repair.test;

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickDBImpl;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.io.*;
import com.epam.deltix.util.memory.*;

import java.io.File;

/**
 *
 */
public class InvalidMessageGenerator {
    public static final String      STREAM_KEY = "badmsg";

    public static final RecordClassDescriptor   Jekyll =
        new RecordClassDescriptor (
            "MyClass",
            "My Custom Class Title",
            false,
            null,
            new NonStaticDataField (
                "price",
                "Price (DOUBLE)",
                new FloatDataType (FloatDataType.ENCODING_FIXED_DOUBLE, false, 0, 1000),
                null
            ),
            new NonStaticDataField (
                "size",
                "Size (FLOAT)",
                new FloatDataType (FloatDataType.ENCODING_FIXED_FLOAT, false, 0, 1000),
                null
            ),
            new NonStaticDataField (
                "count",
                "Count (INTEGER)",
                new IntegerDataType (IntegerDataType.ENCODING_INT32, false, 0, 1000),
                null
            ),
            new NonStaticDataField (
                "description",
                "Description (VARCHAR)",
                new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, true),
                null
            )            
        );
    
    public static final RecordClassDescriptor   Hyde =
        new RecordClassDescriptor (
            "MyClass",
            "My Custom Class Title",
            false,
            null,
            new NonStaticDataField (
                "price",
                "Price (DOUBLE)",
                new FloatDataType (FloatDataType.ENCODING_FIXED_DOUBLE, true),
                null
            ),
            new NonStaticDataField (
                "size",
                "Size (FLOAT)",
                new FloatDataType (FloatDataType.ENCODING_FIXED_FLOAT, true),
                null
            ),
            new NonStaticDataField (
                "count",
                "Count (INTEGER)",
                new IntegerDataType (IntegerDataType.ENCODING_INT32, true),
                null
            ),
            new NonStaticDataField (
                "description",
                "Description (VARCHAR)",
                new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, true),
                null
            )            
        );
    
    public static void      createSampleStream (DXTickDB db) {
        DXTickStream            stream = db.getStream (STREAM_KEY);

        if (stream == null) {
            stream =
                db.createStream (
                    STREAM_KEY,
                    STREAM_KEY,
                    "Description Line1\nLine 2\nLine 3",
                    0
                );

            stream.setFixedType (Jekyll);
        }
    }
    
    public static void writeIntoStream(DXTickDB db) {
        DXTickStream            stream = db.getStream (STREAM_KEY);
        RecordClassDescriptor   classDescriptor = stream.getFixedType ();
        RawMessage              msg = new RawMessage (classDescriptor);

        
        //  Re-usable buffer for collecting the encoded message
        MemoryDataOutput        dataOutput = new MemoryDataOutput ();
        FixedUnboundEncoder     encoder =
            CodecFactory.COMPILED.createFixedUnboundEncoder (Hyde);

        try (TickLoader loader = stream.createLoader (new LoadingOptions (true))) {            
            //
            //  Set up standard fields
            //
            msg.setSymbol("AAPL");
            //
            //  Message 1: out of range values and illegal null
            //
            dataOutput.reset ();
            encoder.beginWrite (dataOutput);

            encoder.nextField ();
            encoder.writeDouble (1000.5);

            encoder.nextField ();
            encoder.writeFloat (1000.5F);

            encoder.nextField ();   // count
            encoder.writeInt (-19);

            encoder.nextField ();   // description
            encoder.writeNull ();

            if (encoder.nextField ())   // make sure we are at end
                throw new RuntimeException ("unexpected field: " + encoder.getField ().toString ());

            msg.setBytes (dataOutput, 0);
            loader.send (msg);                               
            //
            //  message 2: legal
            //
            dataOutput.reset ();
            encoder.beginWrite (dataOutput);

            encoder.nextField ();
            encoder.writeDouble (1000);

            encoder.nextField ();
            encoder.writeFloat (0);

            encoder.nextField ();   // count
            encoder.writeInt (1000);

            encoder.nextField ();   // description
            encoder.writeString ("Cheers");

            if (encoder.nextField ())   // make sure we are at end
                throw new RuntimeException ("unexpected field: " + encoder.getField ().toString ());
            
            msg.setBytes (dataOutput, 0);
            loader.send (msg);
            //
            //  Save the legal packet
            //
            int saveLength = msg.length;
            byte [] saveData = msg.data;            
            //
            //  Message 3: Too short
            //                        
            msg.length = 1;
            loader.send (msg);
            //
            //  Message 4: Too long
            //            
            msg.data = new byte [saveLength + 1];
            msg.length = saveLength + 1;
            System.arraycopy (saveData, 0, msg.data, 0, saveLength);
            loader.send (msg);
        }
    }

    public static void main (String [] args) throws Exception { 
        File url = Home.getFile ("temp/testhome/tickdb");
        
        try (DXTickDB db = new TickDBImpl(url)) {
            db.format ();

            createSampleStream (db);

            writeIntoStream (db);
        }                
    }
}
