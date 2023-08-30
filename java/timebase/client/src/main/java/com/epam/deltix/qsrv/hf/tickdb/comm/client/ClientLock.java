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
package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.qsrv.hf.tickdb.pub.lock.DBLockImpl;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockType;

/**
 * User: alex
 * Date: Nov 15, 2010
 */
class ClientLock extends DBLockImpl {
    private final TickStreamClient stream;
    private int usages = 1;

    public ClientLock(TickStreamClient stream, LockType type, String guid) {
        super(type, guid);
        this.stream = stream;
    }

    public void         reuse() {
        synchronized (stream) {
            usages++;
        }
    }

    @Override
    public boolean      isValid() {
        return stream.isValid(this);
    }

    @Override
    public void         release() {
        synchronized (stream) {
            usages--;
            if (usages == 0)
                stream.unlock();
        }
    }
}