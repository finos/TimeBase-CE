package com.epam.deltix.data.stream;

import com.epam.deltix.util.lang.Filter;
import com.epam.deltix.util.memory.MemoryDataInput;

/**
 *
 */
public interface MessageDecoder <T> {
    public T                    decode (
        Filter <? super T>          filter,
        MemoryDataInput             in
    );
}
