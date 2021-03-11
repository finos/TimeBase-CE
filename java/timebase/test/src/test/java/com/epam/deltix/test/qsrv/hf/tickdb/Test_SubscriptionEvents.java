package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TickDBUtil;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.SubscriptionChangeListener;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.SubscriptionManager;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.qsrv.test.messages.BestBidOfferMessage;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;

import com.epam.deltix.util.lang.Util;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

@Category(TickDBFast.class)
public class Test_SubscriptionEvents extends TDBRunnerBase {

    @Before
    public void             setup () {
        DXTickStream stream = getServerDb().getStream(STREAM_KEY);
        if (stream != null)
            stream.delete();

        DXTickStream s = getServerDb().createStream(STREAM_KEY, TickDBUtil.transientTradeStreamOptions());
        StreamConfigurationHelper.setTradeNoExchNoCur (s);
    }

    private static final String     STREAM_KEY = "stream";

    @Test
    public void         test11() throws Exception {
        run(getServerDb());
    }

    @Test
    public void         test12() throws Exception {
        run(getTickDb());
    }

    @Test
    public void         test21() throws Exception {
        run2(getServerDb());
    }

    @Test
    public void         test22() throws Exception {
        run2(getTickDb());
    }

    @Test
    public void         test31() throws Exception {
        run3(getServerDb());
    }

    @Test
    public void         test32() throws Exception {
        run3(getTickDb());
    }

    @Test
    public void         test41() throws Exception {
        run4(getServerDb());
    }

    @Test
    public void         test42() throws Exception {
        run4(getTickDb());
    }

    @Test
    public void         test51() throws Exception {
        testTypes(getServerDb());
    }

    @Test
    public void         test52() throws Exception {
        testTypes(getTickDb());
    }

    @Test
    public void         test61() throws Exception {
        testTypes1(getServerDb());
    }

    @Test
    public void         test62() throws Exception {
        testTypes1(getTickDb());
    }

    public void             testTypes (DXTickDB db) throws Exception {
        DXTickStream stream = db.getStream (STREAM_KEY);

        TickLoader loader = stream.createLoader ();

        SCListener listener = new SCListener ();
        SelectionOptions so = new SelectionOptions (true, true);

        loader.addSubscriptionListener(listener);
        int counter = 1;

        try (TickCursor c = stream.select(0, so, new String[]{}, new IdentityKey[]{})) {

            c.addEntity(new ConstantIdentityKey("AAPL"));
            c.addTypes(TradeMessage.class.getName());

            listener.testAdded(counter++, new ConstantIdentityKey("AAPL"));
            listener.testAdded(counter++, TradeMessage.class.getName());
        }

        listener.testRemoved(counter++, (IdentityKey[]) null);
        listener.testRemoved(counter++, (String[]) null);

        Util.close(loader);
    }

    public void             testTypes1 (DXTickDB db) throws Exception {
        DXTickStream stream = db.getStream (STREAM_KEY);

        try (TickLoader loader = stream.createLoader ()) {

            SCListener listener = new SCListener ();
            SelectionOptions so = new SelectionOptions (true, true);

            loader.addSubscriptionListener(listener);
            int counter = 1;

            try (TickCursor c = stream.select(0, so,
                    new String[] { TradeMessage.class.getName()},
                    new IdentityKey[] { new ConstantIdentityKey("AAPL")})) {

                listener.testAdded(counter++, TradeMessage.class.getName());
                listener.testAdded(counter++, new ConstantIdentityKey("AAPL"));

                TickCursor c1 = stream.select(0, so);

                listener.testAdded(counter++, (String[]) null);
                listener.testAdded(counter++, (IdentityKey[]) null);

                c1.close();
            }

            listener.testRemoved(counter++, (IdentityKey[]) null);
            listener.testRemoved(counter++, (String[]) null);
        }

    }

    public void             run (DXTickDB db) throws Exception {
        DXTickStream stream = db.getStream (STREAM_KEY);

        TickLoader loader = stream.createLoader ();
        TickCursor c1 = null;

        SCListener listener = new SCListener ();
        SelectionOptions so = new SelectionOptions (true, true);

        loader.addSubscriptionListener(listener);
        int counter = 1;

        try {
            c1 = stream.createCursor(so);
            ConstantIdentityKey[] keys = new ConstantIdentityKey[] {
                    new ConstantIdentityKey("AAPL"),
                    new ConstantIdentityKey("MSFT")
            };

            listener.testAdded(counter++, (String[]) null);

            c1.addEntities(keys, 0, keys.length);
            listener.testAdded(counter++, keys);

            test((SubscriptionManager) c1, keys);

            c1.clearAllEntities();
            listener.testRemoved(counter++, (IdentityKey[]) null);

//            ConstantIdentityKey[] keys1 = new ConstantIdentityKey[] {
//                    new ConstantIdentityKey("ORCL"),
//                    new ConstantIdentityKey("IBM")
//            };

//            c1.setFilter(FeedFilter.createEquitiesFilter("ORCL", "IBM"));
//            listener.testAdded(counter, keys1);
//            listener.testRemoved(counter, new ConstantIdentityKey[0]);
//            counter++;

            c1.subscribeToAllEntities();
            assertTrue(((SubscriptionManager) c1).isAllEntitiesSubscribed());
            listener.testAdded(counter++, (IdentityKey[]) null);

            c1.addTypes(TradeMessage.class.getName());
            listener.testRemoved(counter++, (String[]) null);
            listener.testAdded(counter++, TradeMessage.class.getName());
            test((SubscriptionManager)c1, TradeMessage.class.getName());

            c1.setTypes(BestBidOfferMessage.class.getName());
            listener.testRemoved(counter++, TradeMessage.class.getName());
            listener.testAdded(counter++, BestBidOfferMessage.class.getName());

            c1.removeTypes(BestBidOfferMessage.class.getName());
            listener.testRemoved(counter++, BestBidOfferMessage.class.getName());

            c1.subscribeToAllTypes();
            assertTrue(((SubscriptionManager) c1).isAllTypesSubscribed());
            listener.testAdded(counter++, (String[]) null);

            ConstantIdentityKey aapl = new ConstantIdentityKey("AAPL");
            c1.addEntity(aapl);
            listener.testRemoved(counter++, (IdentityKey[]) null);
            listener.testAdded(counter++, aapl);

            ConstantIdentityKey key = new ConstantIdentityKey("AAPL");

            c1.add(new IdentityKey[] {key}, new String[] {BarMessage.class.getName()} );
            listener.testAdded(counter++, key);
            listener.testAdded(counter++, BarMessage.class.getName());

        } finally {
            Util.close(c1);
        }

        listener.testRemoved(counter++, (IdentityKey[]) null);
        listener.testRemoved(counter, (String[]) null);

        loader.removeSubscriptionListener(listener);
        loader.close();

        assertTrue("Recieved " + listener.eventCounter + "; expected " + counter, listener.eventCounter == counter);
    }

    public void             run2 (DXTickDB db) throws Exception {
        DXTickStream stream = db.getStream (STREAM_KEY);

        TickLoader loader = stream.createLoader ();
        TickCursor c1 = null;

        SelectionOptions so = new SelectionOptions (true, true);
        SCListener listener = new SCListener ();

        try {
            c1 = stream.createCursor(so);
            ConstantIdentityKey[] keys = new ConstantIdentityKey[] {
                    new ConstantIdentityKey("AAPL1"),
                    new ConstantIdentityKey("MSFT1")
            };

            c1.addEntities(keys, 0, keys.length);
            c1.clearAllEntities();

            //c1.setFilter(FeedFilter.createEquitiesFilter("ORCL1", "IBM1"));
            c1.subscribeToAllEntities();

            c1.setTypes(TradeMessage.CLASS_NAME);
            c1.removeTypes(TradeMessage.CLASS_NAME);
            c1.subscribeToAllTypes();

            Thread.sleep(1000); // we should wait here until subscribeToAllTypes() will be processed on server
            loader.addSubscriptionListener(listener);

            listener.testAdded(1, (IdentityKey[])null);
            listener.testAdded(2, (String[])null);

        } finally {
            loader.close();
            loader.removeSubscriptionListener(listener);
            Util.close(c1);
        }
    }

    public void             run3 (DXTickDB db) throws Exception {
        DXTickStream stream = db.getStream(STREAM_KEY);

        TickCursor c1 = null;
        TickLoader loader = null;

        SelectionOptions so = new SelectionOptions (true, true);
        SCListener listener = new SCListener ();

        try {
            c1 = stream.createCursor(so);
            ConstantIdentityKey[] keys = new ConstantIdentityKey[] {
                    new ConstantIdentityKey("AAPL1"),
                    new ConstantIdentityKey("MSFT1")
            };

            c1.addEntities(keys, 0, keys.length);
            c1.clearAllEntities();

            //c1.setFilter(FeedFilter.createEquitiesFilter("ORCL1", "IBM1"));
            c1.subscribeToAllEntities();

            c1.setTypes(TradeMessage.CLASS_NAME);
            c1.removeTypes(TradeMessage.CLASS_NAME);
            c1.subscribeToAllTypes();

            Thread.sleep(500);
            loader = stream.createLoader ();
            loader.addSubscriptionListener(listener);

            listener.testAdded(1, (IdentityKey[]) null);
            listener.testAdded(2, (String[]) null);

        } finally {
            if (loader != null)
                loader.removeSubscriptionListener(listener);
            Util.close(loader);
            Util.close(c1);
        }
    }

    public void             run4(DXTickDB db) throws Exception {
        DXTickStream stream = db.getStream (STREAM_KEY);

        TickLoader loader = stream.createLoader ();
        TickCursor c1 = null;
        TickCursor c2 = null;

        SCListener listener = new SCListener ();
        SelectionOptions so = new SelectionOptions (true, true);

        loader.addSubscriptionListener(listener);

        int counter = 2;

        c1 = stream.createCursor(so);
        c2 = stream.createCursor(so);

        ConstantIdentityKey[] keys = new ConstantIdentityKey[] {
                new ConstantIdentityKey("AAPL"),
                new ConstantIdentityKey("MSFT")
        };

        c1.addEntities(keys, 0, keys.length);
        listener.testAdded(counter++, keys);

        c2.addEntity(new ConstantIdentityKey("MSFT"));

        Thread.sleep(300); // events may mess up, so we will get another picture

        c1.clearAllEntities();
        listener.testRemoved(counter++, new ConstantIdentityKey[] {
                new ConstantIdentityKey("AAPL")});

        ConstantIdentityKey[] keys1 = new ConstantIdentityKey[] {
                new ConstantIdentityKey("ORCL"),
                new ConstantIdentityKey("IBM")
        };

        c2.addEntities(keys1, 0, keys1.length);
        listener.testAdded(counter++, keys1);

        c2.subscribeToAllEntities();
        listener.testAdded(counter++, (IdentityKey[]) null);

        Thread.sleep(300); // events may mess up, so we will get another picture

        c1.subscribeToAllEntities();
        //listener.testAdded(counter++, new ConstantIdentityKey[0]);

        ConstantIdentityKey aapl = new ConstantIdentityKey("AAPL");
        c1.addEntity(aapl);

        Util.close(c1);
        Thread.sleep(300); // events may mess up, so we will get another picture
        Util.close(c2);

        listener.testRemoved(counter++, (ConstantIdentityKey[]) null);
        listener.testRemoved(counter, (String[]) null);

        loader.removeSubscriptionListener(listener);
        loader.close();

        assertTrue("Recieved " + listener.eventCounter + "; expected " + counter, listener.eventCounter == counter);
    }

    public void test(SubscriptionManager m, IdentityKey... identities)
            throws InterruptedException
    {
        List<IdentityKey> list = Arrays.asList(m.getSubscribedEntities());
        assertEquals(list.size(), identities.length);

        for (IdentityKey id : identities)
            assertTrue(list.contains(id));
    }

    public void test(SubscriptionManager m, String ... types)
            throws InterruptedException
    {
        List<String> list = Arrays.asList(m.getSubscribedTypes());
        assertEquals(list.size(), types.length);

        for (String type : types)
            assertTrue(list.contains(type));
    }

    final class SCListener implements SubscriptionChangeListener {
        int eventCounter = 0;

        ArrayList<IdentityKey> added = new ArrayList<IdentityKey>();
        ArrayList<IdentityKey> removed = new ArrayList<IdentityKey>();

        ArrayList<String> addedTypes = new ArrayList<String>();
        ArrayList<String> removedTypes = new ArrayList<String>();

        synchronized void   wait4Events(int count) throws InterruptedException {
            while (eventCounter < count)
                wait();
            //System.out.println("eventCounter = " + eventCounter + ", expected = " + count);
        }

        public synchronized void testAdded(int events, IdentityKey id) throws InterruptedException {
            testAdded(events, new IdentityKey[] {id});
        }            

        public synchronized void testAdded(int events, IdentityKey[] identities)
            throws InterruptedException
        {
            wait4Events(events);

            assertTrue(eventCounter >= events);
            if (identities == null) {
                assertTrue(added == null);
            } else {
                assertEquals(identities.length, added.size());

                for (IdentityKey id : identities)
                    assertTrue(added.contains(id));
            }
        }

        public synchronized void testRemoved(int count, IdentityKey[] identities)
                throws InterruptedException
        {
            wait4Events(count);

            assertTrue(eventCounter >= count);
            if (identities == null) {
                assertTrue(removed == null);
            } else {
                assertEquals(identities.length, removed.size());

                for (IdentityKey id : identities)
                    assertTrue(removed.contains(id));
            }
        }

        public synchronized void testAdded(int count, String type) throws InterruptedException {
            testAdded(count, new String[] {type});
        }

        public synchronized void testAdded(int count, String[] types)
                throws InterruptedException
        {
            wait4Events(count);

            assertTrue(eventCounter >= count);
            
            if (types == null) {
                assertTrue(addedTypes == null);
            } else {
                assertEquals(types.length, addedTypes.size());

                for (String id : types)
                    assertTrue(addedTypes.contains(id));
            }
        }

        public synchronized void testRemoved(int count, String type) throws InterruptedException {
            testRemoved(count, new String[] {type});
        }

        public synchronized void testRemoved(int count, String[] types)
                throws InterruptedException
        {
            wait4Events(count);

            assertTrue(eventCounter >= count);
            
            if (types == null) {
                assertTrue(removedTypes == null);
            } else {
                assertEquals(types.length, removedTypes.size());

                for (String id : types)
                    assertTrue(removedTypes.contains(id));
            }
        }

        private void            onEvent() {
            eventCounter++;
//            StackTraceElement[] trace = new Exception().getStackTrace(); // slow down execution a bit
//            System.out.println(eventCounter + ": " + trace[1]);
        }

        @Override
        public synchronized void entitiesAdded(Collection<IdentityKey> entities) {
            onEvent();

            if (added == null)
                added = new ArrayList<IdentityKey>();
            else
                added.clear();

            added.addAll(entities);
            notifyAll();
        }

        @Override
        public synchronized void entitiesRemoved(Collection<IdentityKey> entities) {
            onEvent();
            if (added == null)
                removed = new ArrayList<IdentityKey>();
            else
                removed.clear();

            removed.addAll(entities);
            notifyAll();
        }

        @Override
        public synchronized void allEntitiesAdded() {
            onEvent();
            added = null;
            notifyAll();
        }

        @Override
        public synchronized void allEntitiesRemoved() {
            onEvent();
            removed = null;
            notifyAll();
        }

        @Override
        public synchronized void typesAdded(Collection<String> types) {
            onEvent();
            if (addedTypes == null)
                addedTypes = new ArrayList<String>();
            else
                addedTypes.clear();

            addedTypes.addAll(types);
            notifyAll();
        }

        @Override
        public synchronized void typesRemoved(Collection<String> types) {
            onEvent();
            if (removedTypes == null)
                removedTypes = new ArrayList<String>();
            else
                removedTypes.clear();

            removedTypes.addAll(types);
            notifyAll();
        }

        @Override
        public synchronized void allTypesAdded() {
            onEvent();
            addedTypes = null;
            notifyAll();
        }

        @Override
        public synchronized void allTypesRemoved() {
            onEvent();
            removedTypes = null;
            notifyAll();
        }
    }
}