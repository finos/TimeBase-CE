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

import com.epam.deltix.qsrv.hf.tickdb.pub.lock.DBLockImpl;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockType;
import com.epam.deltix.util.lang.Util;

/**
 * User: alex
 * Date: Nov 16, 2010
 */
public class ServerLock extends DBLockImpl {
    protected String            clientId;

    private static char         SEPARATOR = ':';
    private static String       SEPARATOR_VALUE = String.valueOf(SEPARATOR);

    public ServerLock(LockType type, String guid) {
        super(type, guid);
    }

    public ServerLock(LockType type, String guid, String clientId) {
        super(type, guid);
        this.clientId = clientId;
    }

    @Override
    public boolean          isValid() {
        throw new UnsupportedOperationException ();
    }

    @Override
    public void             release() {
        throw new UnsupportedOperationException();
    }

    public String           getClientId() {
        return clientId;
    }

    public void             setClientId(String clientId) {
        this.clientId = clientId;
    }

    public static String    getOwner(String clientId) {
        if (clientId != null) {
            String[] parts = clientId.split(SEPARATOR_VALUE);
            return parts.length > 1 ? parts[1] : null;
        }

        return null;
    }

    public static String    getHost(String clientId) {
        if (clientId != null) {
            String[] parts = clientId.split(SEPARATOR_VALUE);
            return parts[0];
        }

        return null;
    }

    public boolean          isAcceptable(String id) {
        if (clientId != null && id != null) {
            return isOwnerEquals(clientId, id);
            //return Util.equals(getOwner(this.clientId), getOwner(clientId));
        } else if (clientId == null && id == null) {
            return true;
        }

        return false;
    }

    private static boolean     isOwnerEquals(String id, String other) {
        int start1 = id.indexOf(SEPARATOR) + 1;
        int start2 = other.indexOf(SEPARATOR) + 1;

        int index = 0;

        char c;
        while ((c = id.charAt(start1 + index)) != SEPARATOR) {
            if (c != other.charAt(start2 + index))
                return false;
            index++;
        }

        return true;
    }
}
