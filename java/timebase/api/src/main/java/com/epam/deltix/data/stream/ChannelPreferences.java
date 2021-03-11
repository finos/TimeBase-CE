package com.epam.deltix.data.stream;

import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;

public abstract class ChannelPreferences {

    /*
      Use raw messages (@see deltix.qsrv.hf.pub.RawMessage).
    */
    public boolean                  raw = false;

    /**
     * Loader for Timebase types.
     * <p>
     * Resolves a tickdb type to a class bound with it.
     * <code>null</code> value means the default loader.
     * Any code, which needs to get the field value, must use getter method to avoid <code>NullPointerException</code>.
     * </p>
     */
    public TypeLoader               typeLoader;

    public TypeLoader               getTypeLoader() {
        return typeLoader != null ? typeLoader : TypeLoaderImpl.DEFAULT_INSTANCE;
    }

    public ChannelPerformance       channelPerformance = ChannelPerformance.MIN_CPU_USAGE;

    public ChannelPerformance       getChannelPerformance() {
        return channelPerformance;
    }
}
