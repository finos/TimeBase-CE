package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.tickdb.pub.query.TypedMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.stream.Protocol;
import com.epam.deltix.streaming.MessageSource;

import java.io.File;
import java.io.IOException;

public class MessageReaderSource 
    implements MessageSource <InstrumentMessage>, TickStreamRelated, TypedMessageSource
{
    private long time;    
    private MessageSource<InstrumentMessage> source;
    private long firstTime = Long.MIN_VALUE;
    private long lastTime = Long.MIN_VALUE;

    private final FileStreamImpl stream;

    public MessageReaderSource(FileStreamImpl stream,
                               long time,
                               File dataFile,                             
                               SelectionOptions options) 
    {
        this.stream = stream;
        this.time = time;
        
        this.source = createReader(dataFile, options);
    }

    public final TickStream getStream () {
        return (stream);
    }

    private MessageSource<InstrumentMessage> createReader(File f, SelectionOptions options) {
        try {
            if (options.isRaw())
                return Protocol.openRawReader(f);
            
            return Protocol.openReader(f, options.typeLoader != null ? options.typeLoader : Protocol.getDefaultTypeLoader());
            
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    public InstrumentMessage getMessage() {
        return source.getMessage();
    }

    public boolean next() {
        if (!source.isAtEnd()) {
            while (source.next()) {
                InstrumentMessage message = source.getMessage();
                
                lastTime = message.getTimeStampMs();
                if (firstTime == Long.MIN_VALUE)
                    firstTime = lastTime;

                if (message.getTimeStampMs() >= time)
                    break;
            }

            if (source.isAtEnd())
                stream.setRange(new long[] {firstTime, lastTime});

            return !source.isAtEnd();
        } else {
            stream.setRange(new long[] {firstTime, lastTime});
        }
        return false;
    }

    public synchronized boolean isAtEnd() {
        return source.isAtEnd();
    }

    public synchronized void close() {
        source.close();        
    }

    @Override
    public int getCurrentTypeIndex() {
        return ((TypedMessageSource)source).getCurrentTypeIndex();
    }

    @Override
    public RecordClassDescriptor getCurrentType() {
        return ((TypedMessageSource)source).getCurrentType();
    }
}
