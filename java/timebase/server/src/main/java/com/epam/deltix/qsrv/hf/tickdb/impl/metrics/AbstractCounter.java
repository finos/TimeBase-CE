//package com.epam.deltix.qsrv.hf.tickdb.impl.metrics;
//
//import java.util.concurrent.atomic.AtomicLong;
//
///**
// * Atomic Counter
// */
//public abstract class AbstractCounter implements Counter {
//    private final AtomicLong value = new AtomicLong();
//    private CounterManagerImpl manager;
//
//    public void init(CounterManagerImpl manager) {
//        assert manager != null;
//        this.manager = manager;
//        this.value.set(0);
//    }
//
//    @Override
//    public long increment() {
//        return value.incrementAndGet();
//    }
//
//    @Override
//    public long getValue() {
//        return value.get();
//    }
//
//    void clear()  {
//        value.set(0);
//        manager = null;
//    }
//
//    @Override
//    public void close() {
//        if (manager != null)
//            manager.unregisterCounter(this);
//    }
//}
