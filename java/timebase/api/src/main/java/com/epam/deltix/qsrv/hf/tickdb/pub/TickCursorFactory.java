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
package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;

public class TickCursorFactory {
    
    public static TickCursor    create(DXTickStream stream, long time) {
        return stream.select(time, null);
    }

    public static TickCursor    create(DXTickStream stream, long time, IdentityKey ... entities) {
        return stream.select(time, null, null, entities);
    }

    public static TickCursor    create(DXTickStream stream, long time, String ... symbols) {
        return create(stream, time, null, symbols);
    }

    public static TickCursor    create(DXTickStream stream, long time, SelectionOptions options) {
        return stream.select(time, options);
    }

    public static TickCursor    create(
             DXTickStream           stream,
             long                   time,
             SelectionOptions       options,
             String ...             symbols)
    {
        IdentityKey[] ids = new IdentityKey[symbols.length];
        for (int i = 0; i < symbols.length; i++)
            ids[i] = new ConstantIdentityKey(symbols[i]);

        return stream.select(time, options, null, ids);
    }
}