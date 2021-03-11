package com.epam.deltix.qsrv.hf.tickdb.impl;

import java.util.*;

/**
 *  
 */
final class WaitingReaders {
    private final ArrayDeque<RawReaderBase <?>> readers =
        new ArrayDeque<RawReaderBase <?>> ();

    WaitingReaders () {
    }

    void                            launchNotifiers () {
        // readers synchronized by TickFile
        if (!readers.isEmpty()) {
            while (readers.size() > 0) {
                RawReaderBase<?> reader = readers.poll();
                reader.submitNotifier();
            }
        }
    }

    void                            add (RawReaderBase <?> reader) {
        if (!reader.waiting) {
            readers.offer(reader);
            reader.waiting = true;
        }
    }

    void                            remove (RawReaderBase <?> reader) {
        readers.remove(reader);
    }
}
