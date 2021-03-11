package com.epam.deltix.qsrv.hf.topic.consumer;

import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessagePoller;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessageProcessor;
import com.epam.deltix.qsrv.hf.topic.consumer.annotation.AeronClientThread;
import com.epam.deltix.qsrv.hf.topic.consumer.annotation.AnyThread;
import com.epam.deltix.qsrv.hf.topic.consumer.annotation.ReaderThreadOnly;
import com.epam.deltix.util.concurrent.CursorIsClosedException;
import io.aeron.Aeron;
import io.aeron.ControlledFragmentAssembler;
import io.aeron.Image;
import io.aeron.Subscription;

import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.ByteOrder;
import java.util.List;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
class DirectMessageNonblockingPoller implements MessagePoller {
    private static final Log LOG = LogFactory.getLog(DirectMessageNonblockingPoller.class.getName());

    private final Subscription subscription;
    private final ControlledFragmentAssembler fragmentAssembler;
    //private final SubscriptionPublicationLimitCounterCache counterCache;
    //private final CountersReader countersReader;
    private final MessageFragmentHandler decodingFragmentHandler;
    private final IpcFilPercentageChecker fillChecker;


    // Indicates that poller should be stopped OR already stopped
    private volatile boolean stopFlag = false;
    // Indicates that poller is stopped
    private boolean stopped = false;
    // Indicates that poller detected a data loss before graceful stop
    private volatile boolean dataLoss = false;

    DirectMessageNonblockingPoller(Aeron aeron, boolean raw, String channel, int dataStreamId,
                                   List<RecordClassDescriptor> types, CodecFactory codecFactory, TypeLoader typeLoader,
                                   MappingProvider mappingProvider) {
        // TODO: Implement loading of temp indexes from server
        if (!ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            throw new IllegalArgumentException("Only LITTLE_ENDIAN byte order supported");
        }

        this.fillChecker = new IpcFilPercentageChecker();

        DoubleUnavailableImageHandler unavailableImageHandler = new DoubleUnavailableImageHandler(fillChecker, DirectMessageNonblockingPoller.this::onUnavailableImage);

        // The main caveat here that we first create a subscription (so we start to collect metadata)
        // and THEN get the mapping from the server using mappingProvider.
        // Otherwise it would be possible to get situation when we get stale data in the server response
        this.subscription = aeron.addSubscription(channel, dataStreamId, fillChecker, unavailableImageHandler);
        LOG.debug().append("Subscribed to dataStreamId=").appendLast(dataStreamId);
        // Load mapping: this can be slow and may involve network interaction
        ConstantIdentityKey[] mappingSnapshot = mappingProvider.getMappingSnapshot();
        LOG.debug("Got mapping snapshot for dataStreamId=%s of size=%s").with(dataStreamId).with(mappingSnapshot.length);

        this.decodingFragmentHandler = new MessageFragmentHandler(raw, codecFactory, typeLoader, types, mappingSnapshot, mappingProvider);
        this.fragmentAssembler = new ControlledFragmentAssembler(decodingFragmentHandler);
    }

    @ReaderThreadOnly
    @Override
    public int processMessages(int messageCountLimit, MessageProcessor messageProcessor) throws CursorIsClosedException {
        if (stopFlag) {
            handleStop();
        }
        if (messageProcessor == null) {
            throw new IllegalArgumentException("messageProcessor can't be null");
        }

        MessageFragmentHandler handler = this.decodingFragmentHandler;
        handler.setProcessor(messageProcessor);
        int result = subscription.controlledPoll(fragmentAssembler, messageCountLimit);
        handler.clearProcessor();
        decodingFragmentHandler.checkException();
        return result;
    }

    @ReaderThreadOnly
    private void handleStop() {
        if (!stopped) {
            close();
        }
        if (dataLoss) {
            throw new ClosedDueToDataLossException();
        } else {
            throw new CursorIsClosedException();
        }
    }

    /**
     * Closes allocated resources.
     * Please not that this method must be called from the same thread that polls messages.
     */
    @ReaderThreadOnly
    @Override
    public void close() {
        stopped = true;
        subscription.close();
        fillChecker.releaseResources();
        stopFlag = true;
    }

    @AnyThread
    @Override
    public byte getBufferFillPercentage() {
        return fillChecker.getBufferFillPercentage();
    }

    @AeronClientThread
    // That will be executed from an Aeron's thread
    private void onUnavailableImage(Image image) {
        int sessionId = image.sessionId();
        // Note: decodingFragmentHandler can be null during the initialization process
        if (!stopped && !stopFlag && decodingFragmentHandler != null && !decodingFragmentHandler.checkIfSessionGracefullyClosed(sessionId)) {
            // Not a graceful close
            onDataLossDetected();
        }
    }

    @AeronClientThread
    // That will be executed from an Aeron's thread
    private void onDataLossDetected() {
        dataLoss = true;
        stopFlag = true;
        LOG.debug("Data loss detected for subscriber with dataStreamId=%s").with(subscription.streamId());
    }
}
