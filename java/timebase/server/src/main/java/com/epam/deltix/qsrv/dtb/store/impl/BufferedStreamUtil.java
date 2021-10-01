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
package com.epam.deltix.qsrv.dtb.store.impl;

import com.epam.deltix.qsrv.dtb.fs.chunkcache.ChunkCacheInputStream;

import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * @author Alexei Osipov
 */
class BufferedStreamUtil {
    private BufferedStreamUtil() {
    }

    /**
     * Wraps stream into a {@link BufferedInputStream} if this stream is not already buffered.<p>
     *
     * This method helps to avoid double buffering of data.
     */
    static InputStream wrapWithBuffered(InputStream in) {
        if (in instanceof BufferedInputStream || in instanceof ChunkCacheInputStream) {
            // This is already a buffered stream. Don't wrap it.
            return in;
        } else {
            // Buffer data from this stream
            return new BufferedInputStream(in);
        }
    }
}