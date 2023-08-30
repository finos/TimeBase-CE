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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.ramdisk.FD;

import java.io.IOException;

/*
    Totally depends on MessageSizeCodec code. Changes should be synchronized!
 */

class LocalMessageSizeCodec {

    private final MessageSizeCodec instance = new MessageSizeCodec();

    public static long          write (long destOffset, int t, FD out) throws IOException {
        if (t < 0)
            throw new IllegalArgumentException (t + " is negative");
        else if (t < 0x80)
            out.write (destOffset++, (byte) t);
        else if (t < 0x4000) {
            out.write (destOffset++, (byte) (0x80 | t & 0x3F));
            out.write (destOffset++, (byte) (t >> 6));
        }
        else if (t < 0x400000) {
            out.write (destOffset++, (byte) (0xC0 | t & 0x3F));
            out.write (destOffset++, (byte) (t >> 6));
            out.write (destOffset++, (byte) (t >> 14));
        }
        else
            throw new IllegalArgumentException (t + " is too large");

        return (destOffset);
    }
}