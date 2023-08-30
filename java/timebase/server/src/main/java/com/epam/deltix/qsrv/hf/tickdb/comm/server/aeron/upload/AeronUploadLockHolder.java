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
package com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.upload;

import com.epam.deltix.qsrv.hf.tickdb.impl.ServerLock;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.DBLock;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockEventListener;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockHandler;
import com.epam.deltix.util.lang.Disposable;

/**
 * @author Alexei Osipov
 */
public class AeronUploadLockHolder implements LockEventListener, Disposable {

    private volatile ServerLock lock = null;
    private final LockHandler lockHandler;

    public AeronUploadLockHolder(LockHandler lockHandler) {
        this.lockHandler = lockHandler;
    }

    @Override
    public void         lockAdded(DBLock lock) {
        if (this.lock == null && lock instanceof ServerLock) {
            this.lock = (ServerLock) lock;
        }
    }

    @Override
    public void         lockRemoved(DBLock lock) {
    }

    public ServerLock getLock() {
        return lock;
    }

    @Override
    public void close() {
        if (lockHandler != null) {
            lockHandler.removeEventListener(this);
        }
    }
}