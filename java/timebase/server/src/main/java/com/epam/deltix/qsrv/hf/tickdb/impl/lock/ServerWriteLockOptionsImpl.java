/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.impl.lock;

import com.epam.deltix.qsrv.hf.tickdb.pub.lock.WriteLockOptionsImpl;

public class ServerWriteLockOptionsImpl extends WriteLockOptionsImpl implements ServerLockOptions {
    private final String clientId;

    protected ServerWriteLockOptionsImpl(String clientId) {
        this(Long.MIN_VALUE, Long.MAX_VALUE, clientId);
    }

    protected ServerWriteLockOptionsImpl(long startTime, long endTime, String clientId) {
        super(startTime, endTime);
        this.clientId = clientId;
    }

    @Override
    public String getClientId() {
        return clientId;
    }
}