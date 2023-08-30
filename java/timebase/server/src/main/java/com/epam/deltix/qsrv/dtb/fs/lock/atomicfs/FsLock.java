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
package com.epam.deltix.qsrv.dtb.fs.lock.atomicfs;

import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;

/**
 * @author Alexei Osipov
 */
public class FsLock {
    private final AbstractPath targetPath;
    private final String lockKey;
    private final long timestamp;

    public FsLock(AbstractPath targetPath, String lockKey, long timestamp) {

        this.targetPath = targetPath;
        this.lockKey = lockKey;
        this.timestamp = timestamp;
    }

    public AbstractPath getTargetPath() {
        return targetPath;
    }

    public String getLockKey() {
        return lockKey;
    }

    public long getTimestamp() {
        return timestamp;
    }
}