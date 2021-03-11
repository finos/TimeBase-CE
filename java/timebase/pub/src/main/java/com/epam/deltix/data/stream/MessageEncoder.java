package com.epam.deltix.data.stream;

import com.epam.deltix.util.memory.MemoryDataOutput;

/**
 *
 */
public interface MessageEncoder <T> {
    public boolean                 encode (
        T                           message,
        MemoryDataOutput            out
    );

    public int                  getContentOffset();

    public int                  getTypeIndex();
}
