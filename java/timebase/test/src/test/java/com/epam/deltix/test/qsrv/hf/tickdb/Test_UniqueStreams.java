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

import com.epam.deltix.qsrv.hf.pub.md.VarcharDataType;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField;
import com.epam.deltix.util.lang.Util;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

/**
 * Date: Feb 2, 2010
 */
@Category(TickDBFast.class)
public class Test_UniqueStreams extends TDBTestBase {

    public Test_UniqueStreams() {
        super(true);
    }

    public static class MyMessage extends BarMessage {
        public MyMessage() {
        }

        public String key;

        public double M1;

        public double M2;

        public String test;

        @Override
        public String toString() {
            return "MyMessage [key:" + key + ":" + M1 + "," + M2 + ";" + super.toString() + "]";
        }
    }

    @Test
    public void test() {

        DXTickDB db = getTickDb();

        RecordClassDescriptor mk = StreamConfigurationHelper.mkMarketMessageDescriptor(null);
        RecordClassDescriptor descriptor = StreamConfigurationHelper.mkBarMessageDescriptor(
                mk, null, null, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);

        String name = MyMessage.class.getName();        
        RecordClassDescriptor rcd = new RecordClassDescriptor(
            name, name, false,
            descriptor,
            new NonStaticDataField("key", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false), true, null),
            new NonStaticDataField("M1", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false), false, null),
            new NonStaticDataField("M2", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false), false, null)
        );

        StreamOptions options = new StreamOptions (StreamScope.DURABLE, null, null, 1);
        options.setFixedType (rcd);
        options.unique = true;
        
        DXTickStream stream = db.createStream ("test", options);

        MyMessage msg = new MyMessage();
        TickLoader        loader = null;
        try {
            LoadingOptions lo = new LoadingOptions(false);
            lo.typeLoader = new TypeLoaderImpl(MyMessage.class.getClassLoader());
            loader = stream.createLoader (lo);

            long time = System.currentTimeMillis() - BarMessage.BAR_DAY;
            for (int i = 1; i < 10; i++) {
                msg.M1 = i;
                msg.M2 = i * 10;
                msg.key = String.valueOf(i % 3);
                msg.setTimeStampMs(time + i);
                loader.send(msg);
            }

            loader.close();
            loader = null;
        }
        finally {
            Util.close(loader);
        }

        SelectionOptions o = new SelectionOptions(false, false);
        o.allowLateOutOfOrder = true;
        o.rebroadcast = true;
        TickCursor cursor = null;


        int count = 0;
        stream = db.getStream ("test");
        try {
            cursor = stream.select(System.currentTimeMillis(), o);
            while (cursor.next()) {
                assertTrue(!cursor.getMessage().toString().contains("key:0:0.0"));
                count++;
            }
            cursor.close();
            cursor = null;
        } finally {
           Util.close(cursor);
        }

        assertEquals(3, count);
        getServerDb().close();

        getServerDb().open(false);
        stream = getServerDb().getStream("test");
        
        count = 0;
        try {
            cursor = stream.select(System.currentTimeMillis(), o);
            assertTrue(cursor.getMessage() == null);
            while (cursor.next()) {
                assertTrue(!cursor.getMessage().toString().contains("key:0:0.0"));
                count++;
            }
            cursor.close();
            cursor = null;
        } finally {
           Util.close(cursor);
        }
        assertEquals(3, count);
    }

    @Ignore
    public void testNull() throws InterruptedException {

        DXTickDB db = getTickDb();

        RecordClassDescriptor mk = StreamConfigurationHelper.mkMarketMessageDescriptor(null);
        RecordClassDescriptor descriptor = StreamConfigurationHelper.mkBarMessageDescriptor(
                mk, null, null, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);

        String name = MyMessage.class.getName();
        RecordClassDescriptor rcd = new RecordClassDescriptor(
            name, name, false,
            descriptor,
            new NonStaticDataField("key", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false), true, null),
            new NonStaticDataField("M1", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false), false, null),
            new NonStaticDataField("M2", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false), false, null),
            new NonStaticDataField("test", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false), true, null)
        );

        StreamOptions options = new StreamOptions (StreamScope.DURABLE, null, null, 1);
        options.setFixedType (rcd);
        options.unique = true;

        DXTickStream stream = db.createStream ("test", options);

        MyMessage msg = new MyMessage();
        TickLoader        loader = null;
        try {
            LoadingOptions lo = new LoadingOptions(false);
            lo.typeLoader = new TypeLoaderImpl(MyMessage.class.getClassLoader());
            loader = stream.createLoader (lo);

            long time = System.currentTimeMillis() - BarMessage.BAR_DAY;
            for (int i = 1; i < 10; i++) {
                msg.M1 = i;
                msg.M2 = i * 10;
                msg.key = String.valueOf(i % 3);
                msg.setTimeStampMs(time + i);
                loader.send(msg);
            }

            loader.close();
            loader = null;
        }
        finally {
            Util.close(loader);
        }
    }

    @Test
    public void test1() throws InterruptedException {

        DXTickDB db = getTickDb();

        RecordClassDescriptor mk = StreamConfigurationHelper.mkMarketMessageDescriptor(null);
        RecordClassDescriptor descriptor = StreamConfigurationHelper.mkBarMessageDescriptor(
                mk, null, null, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);

        String name = MyMessage.class.getName();
        RecordClassDescriptor rcd = new RecordClassDescriptor(
            name, name, false,
            descriptor,
            new NonStaticDataField("key", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false), true, null),
            new NonStaticDataField("M1", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false), false, null),
            new NonStaticDataField("M2", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false), false, null)
        );

        StreamOptions options = new StreamOptions (StreamScope.DURABLE, null, null, 1);
        options.setFixedType (rcd);
        options.unique = true;

        DXTickStream stream = db.createStream ("test", options);

        MyMessage msg = new MyMessage();
        TickLoader        loader = null;
        try {
            LoadingOptions lo = new LoadingOptions(false);
            lo.typeLoader = new TypeLoaderImpl(MyMessage.class.getClassLoader());
            loader = stream.createLoader (lo);

            long time = System.currentTimeMillis() - BarMessage.BAR_DAY;
            for (int i = 1; i < 10; i++) {
                msg.M1 = i;
                msg.M2 = i * 10;
                msg.key = String.valueOf(i % 3);
                msg.setTimeStampMs(time + i);
                loader.send(msg);
            }

            loader.removeUnique(msg);
            
            loader.close();
            loader = null;
        }
        finally {
            Util.close(loader);
        }

        SelectionOptions o = new SelectionOptions(false, false);
        o.allowLateOutOfOrder = true;
        o.rebroadcast = true;
        TickCursor cursor = null;

        int count = 0;
        stream = db.getStream ("test");
        try {
            cursor = stream.select(System.currentTimeMillis(), o);
            while (cursor.next()) {
                assertTrue(!cursor.getMessage().toString().contains("key:0:0.0"));
                //System.out.println(cursor.getMessage());
                count++;
            }
            cursor.close();
            cursor = null;
        } finally {
           Util.close(cursor);
        }

        assertEquals(2, count);
//        getServerDb().close();
//
//        getServerDb().open(false);
//        stream = getServerDb().getStream("test");
//
//        count = 0;
//        try {
//            cursor = stream.select(System.currentTimeMillis(), FeedFilter.createUnrestricted(), o);
//            assertTrue(cursor.getMessage() == null);
//            while (cursor.next()) {
//                assertTrue(!cursor.getMessage().toString().contains("key:0:0.0"));
//                count++;
//            }
//            cursor.close();
//            cursor = null;
//        } finally {
//           Util.close(cursor);
//        }
//        assertEquals(3, count);
    }

    @Test
    public void testDuplicates() throws InterruptedException {
        testDuplicates(getServerDb());
    }
    
    @Test
    public void testDuplicates_Remote() throws InterruptedException {
        testDuplicates(getTickDb());
    }

    public void testDuplicates(DXTickDB db) throws InterruptedException {

        RecordClassDescriptor mk = StreamConfigurationHelper.mkMarketMessageDescriptor(null);
        RecordClassDescriptor descriptor = StreamConfigurationHelper.mkBarMessageDescriptor(
                mk, null, null, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);

        String name = MyMessage.class.getName();
        RecordClassDescriptor rcd = new RecordClassDescriptor(
            name, name, false,
            descriptor,
            new NonStaticDataField("key", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false), true, null),
            new NonStaticDataField("M1", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false), false, null),
            new NonStaticDataField("M2", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false), false, null)
        );

        StreamOptions options = new StreamOptions (StreamScope.TRANSIENT, null, null, 1);
        options.setFixedType (rcd);
        options.unique = true;
        options.duplicatesAllowed = false;

        DXTickStream stream = db.getStream("test");
        if (stream != null)
            stream.delete();

        stream = db.createStream ("test", options);

        MyMessage msg = new MyMessage();
        msg.setTimeStampMs(System.currentTimeMillis());
        TickLoader        loader = null;
        try {
            LoadingOptions lo = new LoadingOptions(false);
            lo.typeLoader = new TypeLoaderImpl(MyMessage.class.getClassLoader());            
            loader = stream.createLoader (lo);

            for (int i = 0; i < 10000; i++) {
                msg.M1 = i % 3;
                msg.M2 = (i % 3) * 10;
                msg.key = "Garbage message_" + String.valueOf(i % 10);
                msg.setSymbol("X" + String.valueOf(i % 3));
                loader.send(msg);
            }

            loader.close();
            loader = null;
        }
        finally {
            Util.close(loader);
        }

        SelectionOptions o = new SelectionOptions(false, false);
        o.rebroadcast = true;
        TickCursor cursor = null;

        int count = 0;
        try {

            cursor = stream.select(0, o);
            while (cursor.next()) {
                count++;
            }
            cursor.close();
            cursor = null;
        } finally {
           Util.close(cursor);
        }

        assertEquals(10, count);
    }

    @Test
    public void testNoKeys() {

        DXTickDB db = getServerDb();

        RecordClassDescriptor rcd = StreamConfigurationHelper.mkUniversalBarMessageDescriptor();

        StreamOptions options = new StreamOptions (StreamScope.DURABLE, null, null, 1);
        options.setFixedType (rcd);
        options.unique = true;
        options.duplicatesAllowed = false;

        DXTickStream stream = db.getStream("test");
        if (stream != null)
            stream.delete();

        stream = db.createStream ("test", options);

        BarMessage msg = new BarMessage();
        msg.setTimeStampMs(System.currentTimeMillis());

        TickLoader        loader = null;
        try {
            LoadingOptions lo = new LoadingOptions(false);
            lo.typeLoader = new TypeLoaderImpl(MyMessage.class.getClassLoader());
            loader = stream.createLoader (lo);

            for (int i = 0; i < 10; i++) {
                msg.setOpen(i);
                msg.setClose(i % 3);
                msg.setSymbol("X" + (i % 3));
                msg.setTimeStampMs(msg.getTimeStampMs() + 1);

                loader.send(msg);
            }
        }
        finally {
            Util.close(loader);
        }

        SelectionOptions o = new SelectionOptions(false, false);
        o.rebroadcast = true;
        o.allowLateOutOfOrder = true;
        TickCursor cursor = null;

        int count = 0;
        try {
            cursor = stream.select(0, o);
            while (cursor.next()) {
                //System.out.println(cursor.getMessage());
                count++;
            }
        } finally {
            Util.close(cursor);
        }

        // 10 messages + 3 unique (compared by default primary key {symbol, instrumentType})
        assertEquals(13, count);
    }
}
