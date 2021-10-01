/*
 * Copyright 2021 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.tickdb.impl.mon.TBMonitorImpl;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockType;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBLock;
import com.epam.deltix.util.lang.DisposableListener;
import com.epam.deltix.util.vsocket.VSDispatcher;
import com.epam.deltix.util.vsocket.VSServerFramework;

class StreamLockImpl extends ServerLock implements TBLock {
    private final ServerStreamImpl  stream;

    private final long monId;
    private String user;
    private String application;

    StreamLockImpl(ServerStreamImpl stream, ServerLock lock) {
        super(lock.getType(), lock.getGuid(), lock.getClientId());
        this.stream = stream;
        this.monId = getMonId();
    }

    StreamLockImpl(ServerStreamImpl stream, LockType type, String guid) {
        super(type, guid);
        this.stream = stream;
        this.monId = getMonId();
    }

    private long getMonId() {
        return (stream.getDB() instanceof TBMonitorImpl) ?
                ((TBMonitorImpl) stream.getDB()).getLockId(this) : TBMonitorImpl.UNKNOWN_ID;
    }

    @Override
    public boolean      isValid() {
        return stream.hasLock(this);
    }

    @Override
    public void         release() {
        stream.removeLock(this);
    }

    @Override
    public void         setClientId(String clientId) {
        VSDispatcher dispatcher = VSServerFramework.INSTANCE.getDispatcher(clientId);

        // in case of HTTP requests dispatcher == null
        if (dispatcher != null && stream instanceof DisposableListener)
            dispatcher.addDisposableListener((DisposableListener)stream);

        super.setClientId(clientId);
    }

    @Override
    public long getId() {
        return monId;
    }

    @Override
    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public void setApplication(String application) {
        this.application = application;
    }

    @Override
    public String getApplication() {
        return application;
    }

    @Override
    public String getStreamKey() {
        return stream.getKey();
    }

    @Override
    public String getHost() {
        return getHost(clientId);
    }

    @Override
    public String toString() {
        return "Lock [" + stream.getKey() + ", " + getType() + ", " + getGuid() + "] from " + clientId;
    }
}