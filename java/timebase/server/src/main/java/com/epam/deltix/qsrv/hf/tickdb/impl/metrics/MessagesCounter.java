//package com.epam.deltix.qsrv.hf.tickdb.impl.metrics;
//
//import deltix.util.time.TimeKeeper;
//
///**
// * Stream Messages Counter used for counting messages successfully processed by stream cursor or loader.
// */
//public class MessagesCounter extends AbstractCounter {
//    public static final char FLD_SEPARATOR = '_';
//
//    private final StringBuilder id = new StringBuilder();
//    private long tbObjectId;
//    private String streamKey;
//    private boolean sourceStream;
//    private String application;
//
//    private volatile long lastIncrementTimestamp;
//
//    MessagesCounter() {
//    }
//
//    void init(long tbObjectId, String streamKey, boolean sourceStream, String application, CounterManagerImpl manager) {
//        super.init(manager);
//        this.tbObjectId = tbObjectId;
//        this.streamKey = streamKey;
//        this.sourceStream = sourceStream;
//        this.application = application;
//        this.lastIncrementTimestamp = 0;
//        updateId();
//    }
//
//    private void updateId() {
//        String objType = sourceStream ? "cursor" : "loader";
//        this.id.setLength(0);
//        this.id.append(objType).append(FLD_SEPARATOR).append(tbObjectId).append(FLD_SEPARATOR).append(streamKey).append(FLD_SEPARATOR).append(application);
//    }
//
//    @Override
//    public long increment() {
//        lastIncrementTimestamp = TimeKeeper.currentTime;
//        return super.increment();
//    }
//
//    @Override
//    public CharSequence getId() {
//        return id;
//    }
//
//    public long getTbObjectId() {
//        return tbObjectId;
//    }
//
//    public String getStreamKey() {
//        return streamKey;
//    }
//
//    public boolean isSourceStream() {
//        return sourceStream;
//    }
//
//    public String getApplication() {
//        return application;
//    }
//
//    public long getLastIncrementTimestamp() {
//        return lastIncrementTimestamp;
//    }
//
//    @Override
//    void clear() {
//        super.clear();
//        id.setLength(0);
//        tbObjectId = 0;
//        streamKey = null;
//        sourceStream = false;
//        application = null;
//    }
//}
