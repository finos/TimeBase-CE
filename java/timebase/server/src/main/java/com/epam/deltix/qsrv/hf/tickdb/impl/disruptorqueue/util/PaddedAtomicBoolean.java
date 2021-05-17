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
package com.epam.deltix.qsrv.hf.tickdb.impl.disruptorqueue.util;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link AtomicBoolean} with some padding to prevent "False Sharing".
 * <p>
 * // TODO: Investigate if the padding on the correct side.
 *
 * @author Alexei Osipov
 */
public class PaddedAtomicBoolean extends AtomicBoolean {
    protected volatile int i1 = 1, i2 = 2, i3 = 3, i4 = 4, i5 = 5, i6 = 6;
    protected volatile int i7 = 7, i8 = 8, i9 = 9, i10 = 10, i11 = 11, i12 = 12, i13 = 13, i14 = 14, i15 = 15, i16 = 16;

    public PaddedAtomicBoolean(boolean initialValue) {
        super(initialValue);
    }

    @SuppressWarnings("unused")
    public int sumPaddingToPreventOptimisation() {
        return i1 + i2 + i3 + i4 + i5 + i6 + i7 + i8 + i9 + i10 + i11 + i12 + i13 + i14 + i15 + i16;
    }
}
