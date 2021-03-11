package com.epam.deltix.qsrv.hf.tickdb.web.model.pub;

/**
 *
 */
public interface ModelFactory {

    LoadersModel        getLoadersModel();

    LoaderModel         getLoaderModel(long loaderId);

    CursorsModel        getCursorsModel();

    CursorModel         getCursorModel(long cursorId);

    ConnectionsModel    getConnectionsModel();

    ConnectionModel     getConnectionModel(String dispatcherId);

    LocksModel          getLocksModel();

}
