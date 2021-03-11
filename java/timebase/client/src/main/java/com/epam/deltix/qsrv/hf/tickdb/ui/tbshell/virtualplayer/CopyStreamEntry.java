package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.virtualplayer;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.qsrv.hf.tickdb.schema.SchemaConverter;
import com.epam.deltix.timebase.messages.TimeStampedMessage;

/**
 * @author Alexei Osipov
 */
final class CopyStreamEntry<T extends TimeStampedMessage> {
    private final MessageSource<T> src;
    private final MessageChannel<T> dst;
    private final SchemaConverter schemaConverter;

    public CopyStreamEntry(MessageSource<T> src, MessageChannel<T> dst, SchemaConverter schemaConverter) {
        this.src = src;
        this.dst = dst;
        this.schemaConverter = schemaConverter;
    }

    public MessageSource<T> getSrc() {
        return src;
    }

    public MessageChannel<T> getDst() {
        return dst;
    }

    public SchemaConverter getConverter() {
        return schemaConverter;
    }
}
