package com.epam.deltix.qsrv.hf.topic.consumer;

import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessageProcessor;
import io.aeron.logbuffer.ControlledFragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
class MessageFragmentHandler implements ControlledFragmentHandler {
    private final ExpandableArrayBuffer arrayBuffer = new ExpandableArrayBuffer(); // Contains only current message
    private final DirectMessageDecoder decoder;
    private MessageProcessor processor;
    private Throwable exception = null;

    MessageFragmentHandler(boolean raw, CodecFactory codecFactory, TypeLoader typeLoader, List<RecordClassDescriptor> types, ConstantIdentityKey[] mapping, MappingProvider mappingProvider) {
        this.decoder = new DirectMessageDecoder(arrayBuffer, raw, codecFactory, typeLoader, types, mapping, mappingProvider);
    }

    void setProcessor(MessageProcessor processor) {
        this.processor = processor;
    }

    void clearProcessor() {
        this.processor = null;
    }

    @Override
    public Action onFragment(DirectBuffer buffer, int offset, int length, Header header) {
        buffer.getBytes(offset, arrayBuffer, 0, length);
        try {
            InstrumentMessage message = decoder.processSingleMessageFromBuffer(length);
            if (message != null) {
                processor.process(message);
            }
            return Action.CONTINUE;
        } catch (Throwable e) {
            this.exception = e;
            // Mark current message as processed but stop further batch processing
            return Action.BREAK;
        }
    }

    /**
     * Checks if there is a pending unhandled exception.
     * This method should be executed after each poll by this fragment handler.
     */
    void checkException() {
        if (this.exception != null) {
            processException();
        }
    }

    private void processException() {
        Throwable exception = this.exception;
        this.exception = null; // Clear the exception
        if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        } else {
            // TODO: Decide on better handling
            throw new RuntimeException(exception);
        }
    }

    boolean checkIfSessionGracefullyClosed(int sessionId) {
        return decoder.checkIfSessionGracefullyClosed(sessionId);
    }
}
