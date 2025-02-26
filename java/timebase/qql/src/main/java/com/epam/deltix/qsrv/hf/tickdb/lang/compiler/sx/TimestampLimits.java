/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.qsrv.hf.tickdb.lang.pub.OrderRelation;
import com.epam.deltix.util.collections.generated.IntegerArrayList;

/**
 *
 */
public class TimestampLimits {
    public static final int    EXCLUSIVE_BIT = 0x80000000;
    public static final int    NANOS_BIT = 0x40000000;
    
    private long                inclusiveMinimum = Long.MIN_VALUE;
    private IntegerArrayList    minParameters = null;
    private long                inclusiveMaximum = Long.MAX_VALUE;
    private IntegerArrayList    maxParameters = null;

    private void                addMinParam (int idx, boolean exclusive, boolean nanos) {
        if (nanos) {
            idx |= NANOS_BIT;
        } else {
            if (exclusive) {
                idx |= EXCLUSIVE_BIT;
            }
        }
        
        if (minParameters == null)
            minParameters = new IntegerArrayList ();
        
        minParameters.add (idx);
    }
    
    private void                addMaxParam (int idx, boolean exclusive, boolean nanos) {
        if (nanos) {
            idx |= NANOS_BIT;
        } else {
            if (exclusive) {
                idx |= EXCLUSIVE_BIT;
            }
        }
        
        if (maxParameters == null)
            maxParameters = new IntegerArrayList ();
        
        maxParameters.add (idx);
    }

    public void update(CompiledExpression<?> e, OrderRelation code, boolean timestampOnRight) {
        code = convertCode(code, timestampOnRight);

        if (e instanceof CompiledConstant) {
            CompiledConstant cc = (CompiledConstant) e;
            long t = (Long) cc.value;
            updateInterval(t, code, true, false);
        } else if (e instanceof ParamAccess) {
            ParamAccess pa = (ParamAccess) e;
            updateInterval(pa, code, false);
        }
    }

    public void update(long t, OrderRelation code, boolean timestampOnRight, boolean matchExact, boolean isConstNs) {
        updateInterval(t, convertCode(code, timestampOnRight), matchExact, isConstNs);
    }

    public void update(ParamAccess pa, OrderRelation code, boolean timestampOnRight, boolean nanos) {
        updateInterval(pa, convertCode(code, timestampOnRight), nanos);
    }

    private OrderRelation convertCode(OrderRelation code, boolean timestampOnRight) {
        if (timestampOnRight) {
            switch (code) {
                case GT:
                    code = OrderRelation.LT;
                    break;
                case GE:
                    code = OrderRelation.LE;
                    break;
                case LT:
                    code = OrderRelation.GT;
                    break;
                case LE:
                    code = OrderRelation.GE;
                    break;
            }
        }
        return code;
    }

    private void updateInterval(long t, OrderRelation code, boolean matchExact, boolean isConstNs) {
        switch (code) {
            case GT:
                inclusiveMinimum = Math.max(inclusiveMinimum, matchExact ? t + 1 : t);
                break;
            case GE:
                inclusiveMinimum = Math.max(inclusiveMinimum, t);
                break;
            case LT:
                if (isConstNs) {
                    t += 1;
                }
                inclusiveMaximum = Math.min(inclusiveMaximum, matchExact ? t - 1 : t);
                break;
            case LE:
                if (isConstNs) {
                    t += 1;
                }
                inclusiveMaximum = Math.min(inclusiveMaximum, t);
                break;
            case EQ:
                if (isConstNs) {
                    t += 1;
                }
                inclusiveMinimum = Math.max(inclusiveMinimum, t);
                inclusiveMaximum = Math.min(inclusiveMaximum, t);
                break;
            default:
                throw new UnsupportedOperationException(code.name());
        }
    }

    private void updateInterval(ParamAccess pa, OrderRelation code, boolean nanos) {
        int idx = pa.ref.index;
        switch (code) {
            case GT:
                addMinParam(idx, true, nanos);
                break;
            case GE:
                addMinParam(idx, false, nanos);
                break;
            case LT:
                addMaxParam(idx, true, nanos);
                break;
            case LE:
                addMaxParam(idx, false, nanos);
                break;
            case EQ:
                addMaxParam(idx, false, nanos);
                addMinParam(idx, false, nanos);
                break;
            default:
                throw new UnsupportedOperationException(code.name());
        }
    }
    
    public long         getInclusiveMaximum () {
        return (inclusiveMaximum);
    }
    
    public long         getInclusiveMinimum () {
        return (inclusiveMinimum);
    }

    public IntegerArrayList minParameters() {
        return minParameters;
    }

    public IntegerArrayList maxParameters() {
        return maxParameters;
    }
}
