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
package com.epam.deltix.qsrv.hf.tickdb.util;

import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;

import java.io.IOException;

/**
 *
 */
public class ReaderImpl implements Reader {

    private TickCursor cursor;
    private long                endTime;

    public ReaderImpl(final TickCursor cursor, final long endTime) {
        if (cursor == null)
            throw new IllegalArgumentException("TickCursor is null");

        this.cursor = cursor;
        this.endTime = endTime;
    }

    @Override
    public boolean next() {
        boolean has_next = cursor.next();
        if (has_next && cursor.getMessage().getTimeStampMs() > endTime)
            return false;

        return has_next;
    }

    @Override
    public Object getMessage() {
        return cursor.getMessage();
    }

    @Override
    public void close() throws IOException {
        cursor.close();
    }
}