package com.epam.deltix.data.stream;

import com.epam.deltix.streaming.MessageSource;

public interface LiveMessageSource<T> extends MessageSource<T> {

    /*
     *  If true, then {@link deltix.util.concurrent.AbstractCursor#next()} will never return false.
     *  Says only that next() never return false.
      **/

    public boolean isLive();
}
