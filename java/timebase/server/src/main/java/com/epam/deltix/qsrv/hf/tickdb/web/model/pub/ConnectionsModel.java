package com.epam.deltix.qsrv.hf.tickdb.web.model.pub;

import com.epam.deltix.util.vsocket.VSServerFramework;

/**
 *
 */
public interface ConnectionsModel extends TimeBaseModel {

    VSServerFramework getServerFramework();

}
