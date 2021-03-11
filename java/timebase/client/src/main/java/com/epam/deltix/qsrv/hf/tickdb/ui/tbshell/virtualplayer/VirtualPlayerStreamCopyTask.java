package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.virtualplayer;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.tickdb.schema.SchemaConverter;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.virtualplayer.time.MessageTimeConverter;

/**
 * @author Alexei Osipov
 */
class VirtualPlayerStreamCopyTask {
    private final MessageSource<RawMessage> src;
    private final MessageChannel<RawMessage> dest;
    private final SchemaConverter schemaConverter;
    private final MessageTimeConverter timeConverter;
    private boolean hasNext = false;

    public VirtualPlayerStreamCopyTask(MessageSource<RawMessage> src, MessageChannel<RawMessage> dest, SchemaConverter schemaConverter, MessageTimeConverter timeConverter) {
        this.src = src;
        this.dest = dest;
        this.schemaConverter = schemaConverter;
        this.timeConverter = timeConverter;
    }

    /**
     * Copies all messages with timestamp < {@code time}.
     * @param time upper limit for message timestamp
     */
    public void copyMessagesUpToTime(long time) {
        //System.out.println("Got copyMessagesUpToTime with time=" + time);
        boolean hasNext = this.hasNext || src.next();
        RawMessage message;
        while (hasNext && (message = src.getMessage()).getTimeStampMs() < time) {
            RawMessage convertedMessage = schemaConverter.convert(message);
            if (convertedMessage != null) {
                timeConverter.convertTime(convertedMessage);
                dest.send(convertedMessage);
            } else {
                onMessageConversionError(message);
            }

            hasNext = src.next();
        }
        this.hasNext = hasNext;
    }

    protected void onMessageConversionError (RawMessage msg) {
        System.err.println("Cannot convert message:" + msg);
    }
}
