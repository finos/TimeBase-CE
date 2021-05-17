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
package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.util.jcg.*;

/**
 *
 */
public abstract class QNumericType <T extends DataType> 
    extends QPrimitiveType <T>
{
    public final int            kind;
    public final Number         min;
    public final Number         max;

    protected QNumericType (T dt, int kind, Number min, Number max) {
        super (dt);
        this.kind = kind;
        this.min = min;
        this.max = max;
    }

    public abstract JExpr       getLiteral (Number value);

    @Override
    public JExpr makeConstantExpr(Object obj) {
        return obj == null ? getNullLiteral() : getLiteral((Number)obj);
    }

    protected boolean hasConstraint() {
        return getMin() != null || getMax() != null;
    }

    protected abstract Number getMin();

    protected abstract Number getMax();
}
