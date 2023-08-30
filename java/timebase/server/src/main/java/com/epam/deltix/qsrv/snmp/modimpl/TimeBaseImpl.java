/*
 * Copyright 2023 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.qsrv.snmp.modimpl;

import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBLoader;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBObject;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBObjectMonitor;
import com.epam.deltix.qsrv.snmp.model.timebase.*;
import com.epam.deltix.snmp.pub.SNMP;
import com.epam.deltix.snmp.pub.Table;
import com.epam.deltix.util.collections.generated.IntegerToObjectHashMap;

/**
 *  PROTOTYPE IMPLEMENTATION
 */
public class TimeBaseImpl implements TimeBase, TBObjectMonitor {
    private final DataCacheImpl         cache = new DataCacheImpl ();

    private final Table<Cursor> cursors = SNMP.createTable(Cursor.class);
    private final Table<Loader> loaders = SNMP.createTable(Loader.class);

    private final IntegerToObjectHashMap<Cursor> idToCursor = new IntegerToObjectHashMap<>();
    private final IntegerToObjectHashMap<Loader> idToLoader = new IntegerToObjectHashMap<>();

    public TimeBaseImpl () {
    }

    @Override
    public TBDataCache getDataCache() {
        return cache;
    }

    @Override
    public Table<Cursor> getCursors() {
        return cursors;
    }

    @Override
    public Table<Loader> getLoaders() {
        return loaders;
    }

    @Override
    public LicenseInfo getLicense() {
        return null;
    }

    public void         addCursor(Cursor cursor, int id) {
        synchronized (idToCursor) {
            cursors.add(cursor);
            idToCursor.put(id, cursor);
        }
    }

    public void         removeCursor(int id) {
        synchronized (idToCursor) {
            Cursor cursor = idToCursor.remove(id, null);
            if (cursor != null)
                cursors.remove(cursor);
        }
    }

    public void         addLoader(Loader loader, int id) {
        synchronized (idToLoader) {
            loaders.add(loader);
            idToLoader.put(id, loader);
        }
    }

    public void         removeLoader(int id) {
        synchronized (idToLoader) {
            Loader loader = idToLoader.remove(id, null);
            if (loader != null)
                loaders.remove(loader);
        }
    }

    @Override
    public void objectCreated(TBObject obj, int id) {
        if (obj instanceof TBCursor)
            addCursor(new CursorImpl((TBCursor)obj), id);

        else if (obj instanceof TBLoader)
            addLoader(new LoaderImpl((TBLoader) obj), id);
    }

    @Override
    public void objectDeleted(TBObject obj, int id) {
        if (obj instanceof TBCursor)
            removeCursor(id);

        else if (obj instanceof TBLoader)
            removeLoader(id);
    }
}