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
package com.epam.deltix.test.qsrv.hf.tickdb.testframework;

;
import com.epam.deltix.qsrv.hf.pub.ChannelQualityOfService;

import com.epam.deltix.qsrv.test.messages.TestEnum;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.qsrv.hf.pub.codec.AlphanumericCodec;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.util.collections.CharSubSequence;
import com.epam.deltix.util.collections.generated.ByteArrayList;
import com.epam.deltix.util.io.Home;
import java.io.File;
import junit.framework.Assert;

/**
 *
 */
public class TestAllTypesStreamCreator {
    public static final String              STREAM_KEY = "alltypes";
    public static final long                BASETIME = 1293840000000L;  // 2011
    public static final int                 LAST_SEQ_NO = 99;
    public static final int                 NUM_SYMBOLS = 2;
    public static final String []           TEXT =
        ("I want a hero: an uncommon want,\n" +
        "When every year and month sends forth a new one,\n" +
        "Till, after cloying the gazettes with cant,\n" +
        "The age discovers he is not the true one;\n" +
        "Of such as these I should not care to vaunt,\n" +
        "I 'll therefore take our ancient friend Don Juan—\n" +
        "We all have seen him, in the pantomime,\n" +
        "Sent to the devil somewhat ere his time.\n").split ("[\n ]+");
    
    private final DXTickDB                  db;
    private DXTickStream                    stream;
    private final AlphanumericCodec         ancodec = new AlphanumericCodec (10);
    private final CharSubSequence           sub = new CharSubSequence ("123456789AB");
    private final ByteArrayList             bal = new ByteArrayList ();
    
    public TestAllTypesStreamCreator (DXTickDB db) {
        this.db = db;
    }

    public void                             fillMessage (
            int                                     entNo,
            int                                     seqNo,
            TestAllTypesMessage                     msg
    ) {
        fillMessage(entNo, seqNo, msg, false);
    }
    
    public void                             fillMessage (
        int                                     entNo,
        int                                     seqNo, 
        TestAllTypesMessage                     msg,
        boolean                                 currentTime
    ) 
    {
        int             seesaw; // up to 25 down to -25 and back up  to 0
        boolean         skip = seqNo % 10 == 9;
        
        if (seqNo <= 25)
            seesaw = seqNo;
        else if (seqNo <= 75)
            seesaw = 50 - seqNo;
        else
            seesaw = seqNo - 100;


        //msg.setTimeStampMs(BASETIME + seqNo * 1000);
        //
        //  Odd events happen at the same time. 
        //  Even events happen at distinct times.
        //

//        if (seqNo % 2 == 0)
//            msg.setTimeStampMs(msg.getTimeStampMs() + entNo);
        if (currentTime)
            msg.setNanoTime(System.currentTimeMillis() * TimeStamp.NANOS_PER_MS);  //new time...
        else
            msg.setNanoTime((BASETIME + seqNo * 1000 + entNo) * TimeStamp.NANOS_PER_MS);  //new time...
        
        //
        //  Symbols are of the form Sn
        //
        msg.setSymbol("S" + entNo);
        
        msg.sequence = seqNo;        
        
        msg.bool_c = seqNo % 3 == 1;
        
        msg.float_c_32 = seesaw + 0.25F;   
        
        msg.float_n_32 = skip ? FloatDataType.IEEE32_NULL : msg.float_c_32;
        
        msg.float_c_64 = seesaw + 0.25;
        
        msg.float_n_64 = skip ? FloatDataType.IEEE64_NULL : msg.float_c_64;
        
        msg.float_c_dec = seesaw * 0.00001;
        
        msg.float_n_dec = skip ? FloatDataType.IEEE64_NULL : msg.float_c_dec;
        
        msg.float_c_dec2 = seesaw * 0.005;
        
        msg.float_n_dec2 = skip ? FloatDataType.IEEE64_NULL : msg.float_c_dec2;
        
        //
        //  Test minimums
        //
        switch (seqNo) {
            case 0:
                msg.int_c_8 = (byte) 0x81;
                msg.int_c_16 = (short) 0x8001;
                msg.int_c_32 = 0x80000001;
                msg.int_c_64 = 0x8000000000000001L;
                msg.puint_c_30 = 0;
                msg.puint_c_61 = 0;
                msg.char_c = '\t'; // test unprintable
                msg.varchar_c_utf8 = "";    
                break;
                
            case LAST_SEQ_NO:
                msg.int_c_8 = (byte) 0x7F;
                msg.int_c_16 = (short) 0x7FFF;
                msg.int_c_32 = 0x7FFFFFFF;
                msg.int_c_64 = 0x7FFFFFFFFFFFFFFFL;
                msg.puint_c_30 = 0x3FFFFFFE;
                msg.puint_c_61 = 0x1FFFFFFFFFFFFFFEL;  
                msg.char_c = '\u4e00'; // Chinese
                msg.varchar_c_utf8 = "\u27721\u35821\u28450\u35486";
                break;
                
            default:
                msg.int_c_8 = (byte) seesaw;
                msg.int_c_16 = (short) seesaw;
                msg.int_c_32 = seqNo % 10;          // for some queries
                msg.int_c_64 = seesaw;
                msg.puint_c_30 = seqNo * 10000000;  // tests all sizes
                msg.puint_c_61 = seqNo * 10000000000000000L; // tests all sizes
                msg.char_c = (char) ('A' + seqNo - 1);
                msg.varchar_c_utf8 = TEXT [seqNo % TEXT.length];
                break;
        }       
        
        msg.int_n_8 = skip ? IntegerDataType.INT8_NULL : msg.int_c_8;        
        msg.int_n_16 = skip ? IntegerDataType.INT16_NULL : msg.int_c_16;  
        msg.int_n_32 = skip ? IntegerDataType.INT32_NULL : msg.int_c_32; 
        msg.int_n_64 = skip ? IntegerDataType.INT64_NULL : msg.int_c_64;                 
        msg.puint_n_30 = skip ? IntegerDataType.PUINT30_NULL : msg.puint_c_30;        
        msg.puint_n_61 = skip ? IntegerDataType.PUINT61_NULL : msg.puint_c_61;        
        msg.char_n = skip ? CharDataType.NULL : msg.char_c;        
        msg.varchar_n_utf8 = skip ? null : msg.varchar_c_utf8;
        
        sub.end = seqNo % 11;
        msg.varchar_c_alpha10 = ancodec.encodeToLong (sub);                
        msg.varchar_n_alpha10 = skip ? VarcharDataType.ALPHANUMERIC_NULL : msg.varchar_c_alpha10;
        
        sub.end = seqNo % 6;
        msg.varchar_c_alpha5_s = sub;
        msg.varchar_n_alpha5_s = skip ? null : msg.varchar_c_alpha5_s;
        
        msg.tod_c = seqNo * 60000;  // minutes
        msg.tod_n = skip ? TimeOfDayDataType.NULL : msg.tod_c;
        
        msg.date_c = BASETIME + 24L * 3600000L * seqNo;   // Days of 2011
        msg.date_n = skip ? DateTimeDataType.NULL : msg.date_c;
        
        msg.enum_c = TestEnum.values () [seqNo % TestEnum.values ().length];
        msg.enum_n = skip ? null : msg.enum_c;
        
        msg.bool_n = 
            skip ? 
                BooleanDataType.NULL : 
            msg.bool_c ? 
                BooleanDataType.TRUE : 
                BooleanDataType.FALSE;
        
//        msg.bitmask_c = seqNo & ((1 << TestBitmask.values ().length) - 1);
        
        bal.clear ();
        
        for (int ii = 0; ii < seqNo; ii++)
            bal.add ((byte) seqNo);
        
        // BUG 10303
        //msg.binary_c = bal;
        
        msg.binary_n = skip ? null : bal;
    }
        
    public void                             createStream () {
        stream = createStream(STREAM_KEY, db);
    }

    public DXTickStream                     createStream (String key, DXTickDB db) {
        DXTickStream stream = db.getStream (key);

        if (stream != null)
            stream.delete ();

        StreamOptions       options =
                new StreamOptions (StreamScope.DURABLE, key, null, 1);

        options.setFixedType (TestAllTypesMessage.getClassDescriptor ());

        stream = db.createStream (key, options);

        return stream;
    }
    
    public void                             loadTestData (
        ChannelQualityOfService                 qos
    ) 
    {
        loadTestData(qos, stream, false);
    }

    public void                             loadTestData (
            ChannelQualityOfService                 qos,
            DXTickStream                            stream,
            boolean                                 currentTime
    )
    {
        TestAllTypesMessage     msg = new TestAllTypesMessage ();
        LoadingOptions          options = new LoadingOptions ();

        options.channelQOS = qos;

        try (TickLoader loader = stream.createLoader (options)) {
            for (int s = 0; s <= LAST_SEQ_NO; s++) {
                for (int e = 0; e < NUM_SYMBOLS; e++) {
                    fillMessage(e, s, msg, currentTime);

                    Assert.assertEquals("Message failed the self-equality test", msg, msg);

                    loader.send(msg);
                }
            }
        }

    }
    
    public void                             verifyTestData (
        ChannelQualityOfService                 qos
    ) 
    {
        TestAllTypesMessage     msg = new TestAllTypesMessage ();
        SelectionOptions        options = new SelectionOptions ();
        
        options.channelQOS = qos;
        
        InstrumentMessageSource cur = stream.createCursor (options);
        
        cur.subscribeToAllEntities ();
        cur.reset (BASETIME);
        
        for (int s = 0; s <= LAST_SEQ_NO; s++) {
            for (int e = 0; e < NUM_SYMBOLS; e++) {
                String  diag = "At s=" + s + "; e=" + e + ": ";
                
                Assert.assertTrue (diag + "No next message" + e, cur.next ());
                
                TestAllTypesMessage actual = (TestAllTypesMessage) cur.getMessage ();
                
                fillMessage (e, s, msg);
                
                msg.assertEquals (diag, actual);                
            }
        }  
        
        cur.close ();
    }
    
    public static void main (String [] args) throws Exception {
        File        f = Home.getFile ("temp/test/qdb");
        
        f.mkdirs ();
        
        DXTickDB    db = TickDBFactory.create (f);
        
        try {
            db.format ();

            TestAllTypesStreamCreator   c = new TestAllTypesStreamCreator (db);

            c.createStream ();        
            c.loadTestData (ChannelQualityOfService.MAX_THROUGHPUT);
            c.verifyTestData (ChannelQualityOfService.MAX_THROUGHPUT);
        } finally {
            db.close ();
        }
    }
}