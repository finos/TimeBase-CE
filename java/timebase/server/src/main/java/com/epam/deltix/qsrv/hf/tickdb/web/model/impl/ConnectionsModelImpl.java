package com.epam.deltix.qsrv.hf.tickdb.web.model.impl;

import com.epam.deltix.qsrv.hf.tickdb.web.model.pub.ConnectionsModel;
import com.epam.deltix.util.vsocket.VSServerFramework;

/**
 *
 */
public class ConnectionsModelImpl extends TimeBaseModelImpl implements ConnectionsModel {

    public String getTitle() {
        return "Communication Framework";
    }

    @Override
    public VSServerFramework getServerFramework() {
        return VSServerFramework.INSTANCE;
    }
}
