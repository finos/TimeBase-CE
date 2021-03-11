package com.epam.deltix.qsrv.hf.tickdb.web.model.impl;

import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBLoader;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBObject;
import com.epam.deltix.qsrv.hf.tickdb.web.model.pub.LoaderModel;

/**
 *
 */
public class LoaderModelImpl extends TimeBaseModelImpl implements LoaderModel {

    private final TBLoader loader;

    public LoaderModelImpl(long id) {
        TBObject tbObject = getMonitor().getObjectById(id);
        if (tbObject instanceof TBLoader)
            loader = (TBLoader) tbObject;
        else
            loader = null;
    }

    public String getTitle() {
        return "Cursor " + (loader != null ? loader.getId() : "");
    }

    @Override
    public TBLoader getLoader() {
        return loader;
    }
}
