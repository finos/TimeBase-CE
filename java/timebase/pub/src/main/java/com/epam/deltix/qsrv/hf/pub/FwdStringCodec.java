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
package com.epam.deltix.qsrv.hf.pub;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;

/**
 * Simple String codec
 */
public class FwdStringCodec {
    private static final Log LOG = LogFactory.getLog(FwdStringCodec.class);

    public static final int NULL = -1;

    public static CharSequence read(MemoryDataInput in) {
        final int               offset = in.readInt ();

        if (offset == NULL)
            return (null);

        final int               savedPosition = in.getPosition ();

        in.seek (offset + savedPosition - 4);

        final CharSequence      result;
        try {
            result = in.readCharSequence();
        } catch (RuntimeException e) {
            LOG.error("Error reading (offset=%s savedPosition=%s): %s").with(offset).with(savedPosition).with(e);
            throw e;
        }

        in.seek (savedPosition);

        return (result);
    }

    public static String readString(MemoryDataInput in) {
        final CharSequence value = read(in);
        return value != null ? value.toString() : null;
    }

    public static void write(CharSequence value, MemoryDataOutput out) {
        if (value == null)
            out.writeInt (NULL);
        else {
            final int               endPos = out.getSize ();

            assert endPos - out.getPosition() >= 4 : "endPos=" + endPos + " pos=" + out.getPosition();
            out.writeInt (endPos - out.getPosition ());

            final int               savedPosition = out.getPosition ();

            out.seek (endPos);
            out.writeString (value);
            out.seek (savedPosition);
        }
    }
}