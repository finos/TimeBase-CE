//package com.epam.deltix.qsrv.hf.tickdb.impl.metrics;
//
//import com.epam.deltix.gflog.api.Log;
//import com.epam.deltix.gflog.api.LogFactory;
//import com.epam.deltix.qsrv.hf.blocks.ObjectPool;
//import com.epam.deltix.qsrv.hf.pub.md.Introspector;
//import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
//import com.epam.deltix.qsrv.hf.tickdb.pub.*;
//import com.epam.deltix.qsrv.util.metrics.MetricsService;
//import com.epam.deltix.timebase.messages.service.CounterMessage;
//import com.epam.deltix.timebase.messages.service.StreamCounterMessage;
//import com.epam.deltix.util.collections.CharSequenceToObjectMap;
//import com.epam.deltix.util.time.TimeKeeper;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.concurrent.locks.LockSupport;
//
//import static com.epam.deltix.qsrv.hf.tickdb.impl.TickDBImpl.METRICS_STREAM_NAME;
//
//public class CounterManagerImpl implements CounterManager {
//    private static final Log LOG = LogFactory.getLog("deltix.tickdb.metrics");
//
//    private static final String DEFAULT_APPLICATION = "TimeBase";
//    private static final Class<?>[] METRICS_MSG_TYPES = { CounterMessage.class, StreamCounterMessage.class };
//
//    private static final long ACTIVE_LOADERS_GAUGE_PERIOD = Long.getLong("TimeBase.metrics.activeLoaders.periodMs", 30_000);
//
////    private final ObjectPool<GenericCounter> genCounterPool = new ObjectPool<>(20, GenericCounter::new);
////    private final ObjectPool<MessagesCounter> msgsCounterPool = new ObjectPool<>(20, MessagesCounter::new);
////    private final ObjectPool<CounterMessage> counterMsgsPool = new ObjectPool<>(20, CounterMessage::new);
////    private final ObjectPool<StreamCounterMessage> streamMsgsPool = new ObjectPool<>(20, StreamCounterMessage::new);
//
//    private final ObjectPool<GenericCounter> genCounterPool = new ObjectPool<>(20) {
//        @Override
//        protected GenericCounter newItem() {
//            return new GenericCounter();
//        }
//    };
//
//    private final ObjectPool<MessagesCounter> msgsCounterPool = new ObjectPool<>(20) {
//        @Override
//        protected MessagesCounter newItem() {
//            return new MessagesCounter();
//        }
//    };
//
//    private final ObjectPool<CounterMessage> counterMsgsPool = new ObjectPool<>(20) {
//        @Override
//        protected CounterMessage newItem() {
//            return new CounterMessage();
//        }
//    };
//
//    private final ObjectPool<StreamCounterMessage> streamMsgsPool = new ObjectPool<>(20) {
//        @Override
//        protected StreamCounterMessage newItem() {
//            return new StreamCounterMessage();
//        }
//    };
//
//    private final CharSequenceToObjectMap<GenericCounter> genericCounters = new CharSequenceToObjectMap<>();
//    private final CharSequenceToObjectMap<MessagesCounter> messagesCounters = new CharSequenceToObjectMap<>();
//
//    private final long snapshotIntervalNs;
//    private Thread snapshotThread = null;
//    private SnapshotWriter snapshotWriter = null;
//
//    private final AtomicInteger activeLoadersGauge = MetricsService.getInstance().registerGauge(
//        "timebase.active_loaders", new AtomicInteger()
//    );
//
//    public CounterManagerImpl(long snapshotIntervalMs) {
//        assert snapshotIntervalMs > 0;
//        this.snapshotIntervalNs = snapshotIntervalMs * 1000000;
//    }
//
//    @Override
//    public synchronized void open(DXTickDB timebase) {
//        assert timebase != null;
//
//        if (snapshotThread != null)
//            throw new IllegalStateException("Already open");
//
//        snapshotWriter = new SnapshotWriter(timebase);
//        snapshotThread = new Thread(snapshotWriter);
//        snapshotThread.setName("Snapshot Saving Thread for " + timebase);
//
//        snapshotThread.start();
//    }
//
//    @Override
//    public synchronized void close() {
//        if (snapshotWriter != null) {
//            snapshotWriter.stop();
//            try {
//                snapshotThread.join(5000);
//
//                // interrupt should be send to exit from any IO/waiting calls
//                snapshotThread.interrupt();
//            } catch (InterruptedException ex) {
//                // ignore
//            }
//            snapshotWriter = null;
//            snapshotThread = null;
//        }
//    }
//
//    @Override
//    public Counter registerCursorCounter(long cursorId, String streamKey, String application) {
//        // stream --> application
//        return registerMessagesCounter(cursorId, streamKey, true, application);
//    }
//
//    @Override
//    public MessagesCounter registerLoaderCounter(long loaderId, String streamKey, String application) {
//        // application --> stream
//        return registerMessagesCounter(loaderId, streamKey, false, application);
//    }
//
//    private MessagesCounter registerMessagesCounter(long tbObjectId, String streamKey, boolean sourceStream, String application) {
//
//        synchronized (messagesCounters) {
//            MessagesCounter counter = msgsCounterPool.borrow();
//            if (application == null)
//                application = DEFAULT_APPLICATION;
//            counter.init(tbObjectId, streamKey, sourceStream, application, this);
//            if (messagesCounters.containsKey(counter.getId()))
//                throw new IllegalArgumentException(counter.getId() + (sourceStream ? " cursor" : " loader") + " counter is already registered");
//            messagesCounters.put(counter.getId(), counter);
//
//            if (LOG.isDebugEnabled())
//                LOG.debug("Register message counter[" + counter.getId() + "] for [" + streamKey + "] under application: " + application);
//            return counter;
//        }
//    }
//
//    @Override
//    public Counter registerGenericCounter(String counterId) {
//        synchronized (genericCounters) {
//            if (SNAPSHOT_COUNTER.equals(counterId) || genericCounters.containsKey(counterId))
//                throw new IllegalArgumentException(counterId + " generic counter is already registered");
//            GenericCounter counter = genCounterPool.borrow();
//            counter.init(counterId, this);
//            genericCounters.put(counterId, counter);
//            return counter;
//        }
//    }
//
//    public void unregisterCounter(Counter counter) {
//        if (counter instanceof GenericCounter)
//            unregisterCounter((GenericCounter) counter);
//        else
//            unregisterCounter((MessagesCounter) counter);
//    }
//
//    private void unregisterCounter(GenericCounter counter) {
//        if (LOG.isDebugEnabled())
//            LOG.debug("Unregister counter[" + counter.getId() + "]");
//
//        synchronized (genericCounters) {
//            genericCounters.remove(counter.getId());
//            counter.clear();
//            genCounterPool.release(counter);
//        }
//    }
//
//    private void unregisterCounter(MessagesCounter counter) {
//        if (LOG.isDebugEnabled())
//            LOG.debug("Unregister counter[" + counter.getId() + "]");
//
//        synchronized (messagesCounters) {
//            messagesCounters.remove(counter.getId());
//            counter.clear();
//            msgsCounterPool.release(counter);
//        }
//    }
//
//    private class SnapshotWriter implements Runnable {
//        private DXTickDB timebase;
//        private TickLoader metricsLoader;
//
//        private volatile boolean running = false;
//        private List<StreamCounterMessage> streamMessages = new ArrayList<>();
//        private List<CounterMessage> counterMessages = new ArrayList<>();
//        private CounterMessage lastMessage = new CounterMessage();
//
//        public SnapshotWriter(DXTickDB timebase) {
//            this.timebase = timebase;
//            this.metricsLoader = openMetricsStream();
//        }
//
//        private TickLoader openMetricsStream() {
//            DXTickStream metricsStream = timebase.getStream(METRICS_STREAM_NAME);
//            if (metricsStream != null)
//                metricsStream.delete();
//
//            int distributionFactor = 0;
//            StreamOptions so = new StreamOptions(StreamScope.TRANSIENT, METRICS_STREAM_NAME, "Auto-created by CounterManager", distributionFactor);
//            so.setPolymorphic(introspect(METRICS_MSG_TYPES));
//            metricsStream = timebase.createStream(METRICS_STREAM_NAME, so);
//
//            LoadingOptions options = new LoadingOptions(false);
//            options.writeMode = LoadingOptions.WriteMode.APPEND;
//            return metricsStream.createLoader(options);
//        }
//
//        private void validateSchema(RecordClassDescriptor[] schema, Class<?>[] messageTypes) {
//            if (schema == null || schema.length != messageTypes.length)
//                throw new IllegalArgumentException("Unexpected schema of the stream " + METRICS_STREAM_NAME);
//
//            for (Class<?> messageType : messageTypes) {
//                if (!contain(schema, messageType.getName())) {
//                    throw new IllegalArgumentException(METRICS_STREAM_NAME + " stream schema is missing " + messageType.getName() + " descriptor");
//                }
//            }
//        }
//
//        private boolean contain(RecordClassDescriptor[] schema, String messageTypeName) {
//            for (RecordClassDescriptor descriptor : schema) {
//                if (descriptor.getName().equals(messageTypeName))
//                    return true;
//            }
//            return false;
//        }
//
//        public RecordClassDescriptor[] introspect(Class<?> ... messageClass) {
//            try {
//                Introspector introspector = Introspector.createEmptyMessageIntrospector();
//                RecordClassDescriptor [] result = new RecordClassDescriptor[messageClass.length];
//                for (int i=0; i < result.length; i++)
//                    result[i] = introspector.introspectRecordClass("CounterManager introspector", messageClass[i]);
//                return result;
//            } catch (Introspector.IntrospectionException e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//        private StreamCounterMessage getMessage(MessagesCounter counter, long timestamp) {
//            StreamCounterMessage message = streamMsgsPool.borrow();
//            message.reset();
//
////            message.setInstrumentType(InstrumentType.CUSTOM);
//
//            message.setSymbol(counter.getApplication() != null ? counter.getApplication() : "none");
//            message.setTimeStampMs(timestamp);
//            message.setValue(counter.getValue());
//            message.setId(counter.getTbObjectId());
//            message.setStreamKey(counter.getStreamKey());
//            message.setSourceStream(counter.isSourceStream());
//
//            return message;
//        }
//
//        private CounterMessage getMessage(GenericCounter counter, long timestamp) {
//            CounterMessage message = counterMsgsPool.borrow();
//            message.reset();
//
////            message.setInstrumentType(InstrumentType.CUSTOM);
//            message.setSymbol(counter.getId());
//            message.setTimeStampMs(timestamp);
//            message.setValue(counter.getValue());
//
//            return message;
//        }
//
//        @Override
//        public void run() {
//            running = true;
//            while (running) {
//                try {
//                    long snapshotTime = timebase.getServerTime();
//
//                    // why not store message inside each MessagesCounter? pools will be redundant then
//                    // - this was done to avoid synchronizing on counters collections while messages are written to the stream
//                    int activeLoaders = 0;
//                    long currentTime = TimeKeeper.currentTime;
//
//                    synchronized (messagesCounters) {
//                        streamMessages.clear();
//                        for (MessagesCounter counter : messagesCounters.values()) {
//                            streamMessages.add(getMessage(counter, snapshotTime));
//                            if (!counter.isSourceStream()) {
//                                if (currentTime - counter.getLastIncrementTimestamp() < ACTIVE_LOADERS_GAUGE_PERIOD) {
//                                    activeLoaders++;
//                                }
//                            }
//                        }
//                    }
//
//                    activeLoadersGauge.set(activeLoaders);
//
//                    synchronized (genericCounters) {
//                        counterMessages.clear();
//                        for (GenericCounter counter : genericCounters.values()) {
//                            counterMessages.add(getMessage(counter, snapshotTime));
//                        }
//                    }
//
//                    sendSnapshotMessages(snapshotTime);
//
//                    for (StreamCounterMessage msg : streamMessages)
//                        streamMsgsPool.release(msg);
//                    streamMessages.clear();
//
//                    for (CounterMessage msg : counterMessages)
//                        counterMsgsPool.release(msg);
//                    counterMessages.clear();
//
//                    LockSupport.parkNanos(snapshotIntervalNs);
//
//                    if (Thread.currentThread().isInterrupted())
//                        running = false;
//                }
//                catch (Exception ex) {
//                    LOG.error().append("Metrics Snapshot Writer received an exception and is exiting: ").append(ex).commit();
//                    running = false;
//
//                    // TODO any recovery? otherwise we need to restart timebase to start metrics working again
//                }
//            }
//            metricsLoader.close();
//            LOG.debug().append("Metrics Snapshot Writer closed").commit();
//        }
//
//        private void sendSnapshotMessages(long snapshotTime) throws IOException {
//            for (StreamCounterMessage message : streamMessages) {
//                metricsLoader.send(message);
//            }
//            for (CounterMessage message : counterMessages) {
//                metricsLoader.send(message);
//            }
//
//            lastMessage.setSymbol(SNAPSHOT_COUNTER);
//            lastMessage.setTimeStampMs(snapshotTime+1);
//            lastMessage.setValue(streamMessages.size() + counterMessages.size());
//
//            metricsLoader.send(lastMessage);
//        }
//
//        public void stop() {
//            running = false;
//        }
//    }
//}
