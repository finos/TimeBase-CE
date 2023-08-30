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
package com.epam.deltix.qsrv.dtb.store.codecs;

/**
 *
 */
public class TSFFormat {
    public static final int     NUM_ENTS_MASK =       0xFFFFFF;
    public static final int     COMPRESSED_FLAG =   0x80000000;
    public static final int     ALGORITHM_FLAG =    0x70000000;

    public final static short   INDEX_FORMAT_VERSION = 2;
    public final static short   FILE_FORMAT_VERSION = 3;

    public static byte          getAlgorithmCode(int flags) {
        return (byte)((flags & ALGORITHM_FLAG) >> 28);
    }

    public static int          setAlgorithmCode(int flags, int code) {
        return flags | COMPRESSED_FLAG | (code << 28);
    }
}