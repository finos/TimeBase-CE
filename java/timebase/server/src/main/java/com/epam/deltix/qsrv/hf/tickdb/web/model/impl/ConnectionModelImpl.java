package com.epam.deltix.qsrv.hf.tickdb.web.model.impl;


import com.epam.deltix.qsrv.hf.tickdb.web.model.pub.ConnectionModel;
import com.epam.deltix.util.vsocket.VSDispatcher;

/**
 *
 */
public class ConnectionModelImpl extends ConnectionsModelImpl implements ConnectionModel {

    private final VSDispatcher dispatcher;

    public ConnectionModelImpl(String id) {
        dispatcher = getServerFramework().getDispatcher(id);
    }

    public String getTitle() {
        return "Connection";
    }

    @Override
    public VSDispatcher getDispatcher() {
        return dispatcher;
    }
}
