//package com.epam.deltix.qsrv.hf.tickdb.impl.metrics;
//
///**
// * Generic Counter
// */
//public class GenericCounter extends AbstractCounter {
//    private String id;
//
//    public GenericCounter() {
//    }
//
//    void init(String id, CounterManagerImpl manager) {
//        super.init(manager);
//        assert id != null;
//        this.id = id;
//    }
//
//    @Override
//    public CharSequence getId() {
//        return id;
//    }
//
//    void clear() {
//        super.clear();
//        id = null;
//    }
//}
