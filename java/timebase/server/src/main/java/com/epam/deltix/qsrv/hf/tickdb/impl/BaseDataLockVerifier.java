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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingErrorListener;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.*;

import java.util.ArrayList;
import java.util.List;

/*
 * Not optimized implementation of data lock verification.
 */
public class BaseDataLockVerifier {

    private final List<DBLock> writeLocks = new ArrayList<>();

    public synchronized void addWriteLock(DBLock lock) {
        if (lock.getOptions() instanceof WriteLockOptions)
            writeLocks.add(lock);
        else
            throw new IllegalArgumentException("Expecting WriteLockOptions for the " + lock);
    }

    public synchronized void removeLock(DBLock lock) {
        writeLocks.remove(lock);
    }

    public synchronized boolean isExclusive() {
        return writeLocks.size() == 1 && !((WriteLockOptions) writeLocks.get(0).getOptions()).isRanged();
    }

    public synchronized boolean verifyLock(String stream, DBLock lock, long time) throws StreamLockedException {
        return verify(null, stream, lock, time, time);
    }

    public synchronized boolean verifyLock(LoadingErrorListener listener, String stream, DBLock lock, long time) throws StreamLockedException {
        return verify(listener, stream, lock, time, time);
    }

    public synchronized void verifyStreamLock(String stream, DBLock lock, long startTime, long endTime) throws StreamLockedException {
        verify(e -> {
            throw new StreamLockedException(e.getMessage());
        }, stream, lock, startTime, endTime);
    }

    private boolean verify(LoadingErrorListener listener, String stream, DBLock lock, long startTime, long endTime) {

        if (writeLocks.isEmpty())
            return true;

        // if we are holding a lock, we can easily check time interval belongs to our lock interval
        if (includes(lock, startTime, endTime))
            return true;

        for (int i = 0; i < writeLocks.size(); ++i) {
            DBLock writeLock = writeLocks.get(i);

            if (!writeLock.equals(lock)) {
                if (intersects((WriteLockOptions) writeLock.getOptions(), startTime, endTime)) {
                    if (listener != null) {
                        String message = (endTime == startTime) ? StreamDataLockedException.buildMessage(stream, writeLock, startTime) :
                                StreamDataLockedException.buildMessage(stream, writeLock, startTime, endTime);

                        listener.onError(new StreamDataLockedException(message));
                    }
                    return false;
                }
            }
        }

        return true;
    }

    static boolean includes(DBLock lock, long startTime, long endTime) {
        if (lock != null && lock.getType() == LockType.WRITE)
            return includes((WriteLockOptions) lock.getOptions(), startTime, endTime);

        return false;
    }

    static boolean  includes(DBLock lock, long time) {
        if (lock != null && lock.getType() == LockType.WRITE)
            return includes((WriteLockOptions) lock.getOptions(), time);

        return false;
    }

    static boolean intersects(WriteLockOptions options1, WriteLockOptions options2) {
        return intersects(
            options1.getStartTime(), options1.getEndTime(),
            options2.getStartTime(), options2.getEndTime()
        );
    }

    static boolean intersects(WriteLockOptions options1, long b1, long b2) {
        return intersects(
            options1.getStartTime(), options1.getEndTime(), b1, b2
        );
    }

    static boolean includes(WriteLockOptions options, long value) {
        return value >= options.getStartTime() && value <= options.getEndTime();
    }

    static boolean intersects(long a1, long a2, long b1, long b2) {
        return a2 >= b1 && b2 >= a1;
    }

    static boolean includes(WriteLockOptions options1, long b1, long b2) {
        return includes(
            options1.getStartTime(), options1.getEndTime(), b1, b2
        );
    }

    static boolean includes(long a1, long a2, long b1, long b2) {
        return b1 >= a1 && b2 <= a2;
    }

}