package com.epam.deltix.qsrv.hf.tickdb.web.model.impl;

import com.epam.deltix.qsrv.hf.tickdb.web.model.pub.*;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public class ModelFactoryImpl implements ModelFactory {

    private final MenuModel[] menuModels = new MenuModel[MenuModelImpl.MENU_SECTIONS.length];

    public ModelFactoryImpl() {
        for (MenuSection menuSection : MenuModelImpl.MENU_SECTIONS)
            menuModels[menuSection.ordinal()] = new MenuModelImpl(menuSection);
    }

//    @Override
//    public LicenseModel getLicenseModel(XLicense license) {
//        LicenseModel licenseModel = new LicenseModelImpl(license);
//        licenseModel.setMenuModel(getMenuModel(MenuSection.License));
//        return licenseModel;
//    }

    @Override
    public LoadersModel getLoadersModel() {
        LoadersModel loadersModel = new LoadersModelImpl();
        loadersModel.setMenuModel(getMenuModel(MenuSection.Loaders));
        return loadersModel;
    }

    @Override
    public LoaderModel getLoaderModel(long loaderId) {
        LoaderModel loaderModel = new LoaderModelImpl(loaderId);
        loaderModel.setMenuModel(getMenuModel(MenuSection.Loaders));
        return loaderModel;
    }

    @Override
    public CursorsModel getCursorsModel() {
        CursorsModel cursorsModel = new CursorsModelImpl();
        cursorsModel.setMenuModel(getMenuModel(MenuSection.Cursors));
        return cursorsModel;
    }

    @Override
    public CursorModel getCursorModel(long cursorId) {
        CursorModel cursorModel = new CursorModelImpl(cursorId);
        cursorModel.setMenuModel(getMenuModel(MenuSection.Cursors));
        return cursorModel;
    }

    @Override
    public ConnectionsModel getConnectionsModel() {
        ConnectionsModel connectionsModel = new ConnectionsModelImpl();
        connectionsModel.setMenuModel(getMenuModel(MenuSection.Connections));
        return connectionsModel;
    }

    @Override
    public ConnectionModel getConnectionModel(String dispatcherId) {
        ConnectionModel connectionModel = new ConnectionModelImpl(dispatcherId);
        connectionModel.setMenuModel(getMenuModel(MenuSection.Connections));
        return connectionModel;
    }

    @Override
    public LocksModel getLocksModel() {
        LocksModel locksModel = new LocksModelImpl();
        locksModel.setMenuModel(getMenuModel(MenuSection.Locks));
        return locksModel;
    }

    private MenuModel getMenuModel(MenuSection menuSection) {
        return menuModels[menuSection.ordinal()];
    }

}
