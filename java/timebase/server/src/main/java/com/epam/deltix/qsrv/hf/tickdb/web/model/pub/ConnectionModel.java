package com.epam.deltix.qsrv.hf.tickdb.web.model.pub;

import com.epam.deltix.util.vsocket.VSDispatcher;

/**
 *
 */
public interface ConnectionModel extends TimeBaseModel {

    VSDispatcher        getDispatcher();

}
