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
package com.epam.deltix.qsrv.hf.tickdb.pub.lock;

public abstract class DBLockImpl implements DBLock {
    private final String    guid; // client id
    private LockType        type;

    public DBLockImpl(LockType type, String guid) {
        this.type = type;
        this.guid = guid;
    }

    public String       getGuid() {
        return guid;
    }

    @Override
    public LockType     getType() {
        return type;
    }

    @Override
    public int          hashCode() {
        return guid != null ? guid.hashCode() : 0;
    }

    @Override
    public boolean      equals(Object o) {
        if (this == o)
            return true;

        if (o instanceof DBLockImpl)
            return guid.equals(((DBLockImpl) o).guid);

        return false;
    }
}