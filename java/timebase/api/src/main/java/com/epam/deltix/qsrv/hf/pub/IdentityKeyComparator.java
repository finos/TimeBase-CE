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
package com.epam.deltix.qsrv.hf.pub;

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.util.lang.Util;
import java.util.Comparator;

/**
 *
 */
public class IdentityKeyComparator
    implements Comparator <IdentityKey>
{
    private final boolean       symbolAscending;

    public IdentityKeyComparator (boolean symbolAscending) {
        this.symbolAscending = symbolAscending;
    }

    public int      compare (IdentityKey a, IdentityKey b) {
        int     dif;

        if ((dif = Util.compare (a.getSymbol (), b.getSymbol (), false)) != 0)
            return (symbolAscending ? dif : -dif);

        return (0);
    }

//    private int     ctype (IdentityKey a, IdentityKey b) {
//        return (a.getInstrumentType().ordinal () - b.getInstrumentType().ordinal ());
//    }

    public static final IdentityKeyComparator   DEFAULT_INSTANCE =
        new IdentityKeyComparator (true);

}