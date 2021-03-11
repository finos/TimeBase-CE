package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;

import java.util.ArrayList;

public class StreamSelector<T extends DXTickStream> {
    private TickStream[] streams;

    public StreamSelector(TickStream[] streams) {
        this.streams = streams;
    }

    @SuppressWarnings ("unchecked")
    public java.util.List<T> getStreams(Class c) {
        ArrayList<T> result = new ArrayList<T>();
        for (TickStream stream : streams) {
            if (c.isInstance(stream))
                result.add((T) stream);
        }

        return result;
    }
 }
