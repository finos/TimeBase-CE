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
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;

import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;
import com.epam.deltix.util.time.Interval;
import org.junit.*;

import static org.junit.Assert.*;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

/**
 *  Test user-defined records.
 */
@Category(TickDBFast.class)
public class Test_DynamicAPI {
    private static final double     V_PRICE = 38.46;
    private static final int        V_SIZE = 132687;
    private static final String     V_KIND_1 = "Actual";
    private static final String     V_KIND_2 = "Estimated";
    private static final double     EPSILON = 0.00001;

    private final String            LOCATION      = TDBRunner.getTemporaryLocation();

    @Test
    public void         testFixedUnboundIntp () {
        testFixedUnbound (CodecFactory.INTERPRETED);
    }

    @Test
    public void         testFixedUnboundComp () {
        testFixedUnbound (CodecFactory.COMPILED);
    }

    private void        testFixedUnbound (CodecFactory factory) {
        EnumClassDescriptor     kindDescriptor =
            new EnumClassDescriptor ("KindEnum", "Kind Enum type", V_KIND_1, V_KIND_2);

        RecordClassDescriptor   myMsgClassDescr = 
            new RecordClassDescriptor (
                "Test", 
                "My message class def", 
                false, 
                null, // in this case, means InstrumentMessage is the parent
                new NonStaticDataField("price", "Price Field", new FloatDataType(FloatDataType.getEncodingScaled(4), false)),
                new NonStaticDataField ("size", "Size Field", new IntegerDataType (IntegerDataType.ENCODING_INT32, false)),
                new NonStaticDataField (
                    "kind", 
                    "Kind field",
                    new EnumDataType (false, kindDescriptor)
                )
            );
            
        RawMessage              msg = new RawMessage ();

        msg.setSymbol("DLTX");
        msg.setTimeStampMs(System.currentTimeMillis ());
        msg.type = myMsgClassDescr;

        MemoryDataOutput        out = new MemoryDataOutput ();
        FixedUnboundEncoder     encoder =
            factory.createFixedUnboundEncoder (myMsgClassDescr);

        encoder.beginWrite (out);

        while (encoder.nextField ()) {
            String              name = encoder.getField ().getName ();

            if (name.equals ("price"))
                encoder.writeDouble (V_PRICE);
            else if (name.equals ("size"))
                encoder.writeInt (V_SIZE);
            else if (name.equals ("kind"))
                encoder.writeString (V_KIND_2);
            else
                throw new RuntimeException ("Unrecognized field: " + name);
        }

        msg.setBytes (out, 0);
            
        {
            DXTickDB                db = 
                TickDBFactory.create (LOCATION);

            db.format ();

            DXTickStream            s1 = db.createStream ("S1", null, null, 0);
           
            s1.setFixedType (myMsgClassDescr);

            TickLoader              loader = s1.createLoader (new LoadingOptions (true));
           
            loader.send (msg);
            loader.close ();
            db.close ();
        }
    
        {
            DXTickDB                db = TickDBFactory.create (LOCATION);
            db.open (true);

            DXTickStream            s1 = db.getStream ("S1");

            TickCursor              cursor = 
                s1.select (0, new SelectionOptions (true, false));

            assertTrue (cursor.next ());

            RawMessage              msg2 = (RawMessage) cursor.getMessage ();

            UnboundDecoder    decoder =
                factory.createFixedUnboundDecoder (myMsgClassDescr);

            MemoryDataInput         in1 = new MemoryDataInput (msg.data, msg.offset, msg.length);
            MemoryDataInput         in2 = new MemoryDataInput (msg2.data, msg2.offset, msg2.length);

            //  Check compareAll:
            assertEquals (0, decoder.compareAll (in1, in2));

            //  Check toString, especially to make sure Instrumentmessage fields
            //  are carried over:        
            assertEquals (msg.toString (), msg2.toString ());

            //  Now check fields one-by-one:
            assertEquals (msg.getTimeStampMs(), msg2.getTimeStampMs());
            assertTrue (Util.equals (msg.getSymbol(), msg2.getSymbol()));

            in2.seek (0);
            decoder.beginRead (in2);

            while (decoder.nextField ()) {
                String              name = decoder.getField ().getName ();

                if (name.equals ("price"))
                    assertEquals (V_PRICE, decoder.getDouble (), EPSILON);
                else if (name.equals ("size"))
                    assertEquals (V_SIZE, decoder.getInt ());
                else if (name.equals ("kind"))
                    assertEquals (V_KIND_2, decoder.getString ());
                else
                    throw new RuntimeException ("Unrecognized field: " + name);
            }

            assertFalse (cursor.next ());

            cursor.close ();

            db.close ();
        }
    }
    
    @Test
    public void         testFixedBarIntp () {
        testFixedBar (CodecFactory.INTERPRETED);
    }

    @Test
    public void         testFixedBarComp () {
        testFixedBar (CodecFactory.COMPILED);
    }

    private void         testFixedBar (CodecFactory factory) {
        long                        timestamp = System.currentTimeMillis ();
        BarMessage bar = mkBar (timestamp);
        
        {
            DXTickDB                db = TickDBFactory.create (LOCATION);

            db.format ();

            DXTickStream            s1 = db.createStream ("S1", null, null, 0);

            StreamConfigurationHelper.setBarNoExchNoCur (s1, Interval.MINUTE);
            
            TickLoader              loader = s1.createLoader ();       

            loader.send (bar);

            loader.close ();
            db.close ();
        }
        
        {
            DXTickDB                db = TickDBFactory.create (LOCATION);
            db.open (true);

            DXTickStream            s1 = db.getStream ("S1");
            
            //  Read direct
            TickCursor              cursor = s1.select (0, null);

            assertTrue (cursor.next ());

            {
                BarMessage msg2 = (BarMessage) cursor.getMessage ();

                assertEquals (bar.getTimeStampMs(), msg2.getTimeStampMs());
                assertTrue (Util.equals (bar.getSymbol(), msg2.getSymbol()));
                //assertEquals (bar.barSize, msg2.barSize);
                assertEquals (bar.getOpen(), msg2.getOpen(), EPSILON);
                assertEquals (bar.getHigh(), msg2.getHigh(), EPSILON);
                assertEquals (bar.getLow(), msg2.getLow(), EPSILON);
                assertEquals (bar.getClose(), msg2.getClose(), EPSILON);
                assertEquals (bar.getVolume(), msg2.getVolume(), EPSILON);
                assertEquals (ExchangeCodec.NULL, msg2.getExchangeId());
                assertEquals (999, msg2.getCurrencyCode());
            }

            assertFalse (cursor.next ());

            cursor.close ();

            //  Read in raw form
            cursor = s1.select (0, new SelectionOptions (true, false));

            assertTrue (cursor.next ());

            {
                RawMessage              msg2 = (RawMessage) cursor.getMessage ();
                UnboundDecoder          decoder = factory.createFixedUnboundDecoder (msg2.type);
                MemoryDataInput         in2 = new MemoryDataInput (msg2.data, msg2.offset, msg2.length);

                decoder.beginRead (in2);

                while (decoder.nextField ()) {
                    String              name = decoder.getField ().getName ();

//                    if (name.equals ("barSize"))
//                        assertEquals (bar.barSize, decoder.getInt ());
//                    else 
                    if (name.equals ("open"))
                        assertEquals (bar.getOpen(), decoder.getDouble (), EPSILON);
                    else if (name.equals ("high"))
                        assertEquals (bar.getHigh(), decoder.getDouble (), EPSILON);
                    else if (name.equals ("low"))
                        assertEquals (bar.getLow(), decoder.getDouble (), EPSILON);
                    else if (name.equals ("close"))
                        assertEquals (bar.getClose(), decoder.getDouble (), EPSILON);
                    else if (name.equals ("volume"))
                        assertEquals (bar.getVolume(), decoder.getDouble (), EPSILON);
                    else if (name.equals ("sequenceNumber"))
                        assertEquals (bar.getSequenceNumber(), decoder.getLong (), EPSILON);
                    else
                        throw new RuntimeException ("Unrecognized field: " + name);
                }
            }

            assertFalse (cursor.next ());

            cursor.close ();

            db.close ();
        }
    }
    
    @Test
    public void         testBoundPoly () {
        long                        timestamp = System.currentTimeMillis ();
        BarMessage bar = mkBar (timestamp);
        TradeMessage trade = mkTrade (timestamp + 1);
        
        {
            DXTickDB                db = TickDBFactory.create (LOCATION);

            db.format ();

            DXTickStream            s1 = db.createStream ("S1", null, null, 0);

            StreamConfigurationHelper.setUniversalMarket (s1);
            
            TickLoader              loader = s1.createLoader ();       

            loader.send (bar);
            loader.send (trade);
            
            loader.close ();
            db.close ();
        }
        
        {
            DXTickDB                db = TickDBFactory.create (LOCATION);
            db.open (true);

            DXTickStream            s1 = db.getStream ("S1");
            
            //  Read direct
            TickCursor              cursor = s1.select (0, null);

            assertTrue (cursor.next ());

            {
                BarMessage msg2 = (BarMessage) cursor.getMessage ();

                assertEquals (bar.getTimeStampMs(), msg2.getTimeStampMs());
                assertTrue (Util.equals (bar.getSymbol(), msg2.getSymbol()));
                //assertEquals (bar.barSize, msg2.barSize);
                assertEquals (bar.getOpen(), msg2.getOpen(), EPSILON);
                assertEquals (bar.getHigh(), msg2.getHigh(), EPSILON);
                assertEquals (bar.getLow(), msg2.getLow(), EPSILON);
                assertEquals (bar.getClose(), msg2.getClose(), EPSILON);
                assertEquals (bar.getVolume(), msg2.getVolume(), EPSILON);
                assertEquals (bar.getExchangeId(), msg2.getExchangeId());
                assertEquals (bar.getCurrencyCode(), msg2.getCurrencyCode());
            }

            assertTrue (cursor.next ());

            {
                TradeMessage msg2 = (TradeMessage) cursor.getMessage ();

                assertEquals (trade.getTimeStampMs(), msg2.getTimeStampMs());
                assertTrue (Util.equals (trade.getSymbol(), msg2.getSymbol()));
                assertEquals (trade.getPrice(), msg2.getPrice(), EPSILON);
                assertEquals (trade.getSize(), msg2.getSize(), EPSILON);
                assertEquals (trade.getExchangeId(), msg2.getExchangeId());
                assertEquals (trade.getCurrencyCode(), msg2.getCurrencyCode());
            }
            
            assertFalse (cursor.next ());

            cursor.close ();

            /*  Read in raw form
            cursor = s1.select (0, ufilter, new SelectionOptions (true, false));

            assertTrue (cursor.next ());

            {
                RawMessage              msg2 = (RawMessage) cursor.getMessage ();

                UnboundDecoder    decoder = msg2.newDecoder ();

                decoder.beginRead (msg2.newDataInput ());

                while (decoder.nextField ()) {
                    String              name = decoder.getField ().name;

                    if (name.equals ("barSize"))
                        assertEquals (bar.barSize, decoder.getInt ());
                    else if (name.equals ("open"))
                        assertEquals (bar.open, decoder.getDouble ());
                    else if (name.equals ("high"))
                        assertEquals (bar.high, decoder.getDouble ());
                    else if (name.equals ("low"))
                        assertEquals (bar.low, decoder.getDouble ());
                    else if (name.equals ("close"))
                        assertEquals (bar.close, decoder.getDouble ());
                    else if (name.equals ("volume"))
                        assertEquals (bar.volume, decoder.getDouble ());
                    else if (name.equals ("exchangeCode"))
                        assertEquals (bar.exchangeCode, decoder.getInt ());
                    else if (name.equals ("currencyCode"))
                        assertEquals (bar.currencyCode, decoder.getInt ());
                    else
                        throw new RuntimeException ("Unrecognized field: " + name);
                }
            }

            assertFalse (cursor.next ());

            cursor.close ();
            */
            
            db.close ();
        }
    }

    private BarMessage mkBar (long timestamp) {
        BarMessage msg = new BarMessage();

        msg.setTimeStampMs(timestamp);
        msg.setSymbol("DLTX");

        //msg.barSize = BarMessage.BAR_MINUTE;
        msg.setSequenceNumber(0);
        msg.setOpen(1.78);
        msg.setHigh(1.79);
        msg.setLow(1.74);
        msg.setClose(1.75);
        msg.setVolume(182100.0);
        msg.setExchangeId(ExchangeCodec.codeToLong("EX"));
        msg.setCurrencyCode((short)113);

        return msg;
    }
    
    private TradeMessage mkTrade (long timestamp) {
        TradeMessage msg = new TradeMessage();

        msg.setTimeStampMs(timestamp);
        msg.setSymbol("DLTX");

        msg.setPrice(1.72);
        msg.setSize(23100.0);
        msg.setExchangeId(ExchangeCodec.codeToLong("EX"));
        msg.setCurrencyCode((short)113);

        return msg;
    }
}
