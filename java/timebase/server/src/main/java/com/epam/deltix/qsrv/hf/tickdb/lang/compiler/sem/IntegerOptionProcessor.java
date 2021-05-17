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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.pub.md.StandardTypes;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.CompiledConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.IllegalOptionValueException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.OptionElement;

/**
 *
 */
public abstract class IntegerOptionProcessor <T> extends OptionProcessor <T> {
    public final Long           minInclusive;
    public final Long           maxInclusive;

    public IntegerOptionProcessor (
        String      key, 
        Long        minInclusive, 
        Long        maxInclusive
    )
    {
        super (key, StandardTypes.CLEAN_INTEGER);
        
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
    }
    
    protected abstract void     set (T target, long value);
    
    @Override
    public final void           process (OptionElement option, CompiledConstant value, T target) {
        if (value == null || value.isNull ())
            throw new IllegalOptionValueException (option, null);
            
        long            n = value.getLong ();

        if (minInclusive != null && n < minInclusive || 
            maxInclusive != null && n > maxInclusive)
            throw new IllegalOptionValueException (option, n, minInclusive, maxInclusive);

        set (target, n);
    }
}
