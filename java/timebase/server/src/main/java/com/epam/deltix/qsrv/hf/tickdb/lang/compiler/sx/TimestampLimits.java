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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.util.collections.generated.IntegerArrayList;

/**
 *
 */
public class TimestampLimits {
    private static final int    EXCLUSIVE_BIT = 0x80000000;
    
    private long                inclusiveMinimum = Long.MIN_VALUE;
    private IntegerArrayList    minParameters = null;
    private long                inclusiveMaximum = Long.MAX_VALUE;
    private IntegerArrayList    maxParameters = null;

    private void                addMinParam (int idx, boolean exclusive) {
        if (exclusive)
            idx |= EXCLUSIVE_BIT;
        
        if (minParameters == null)
            minParameters = new IntegerArrayList ();
        
        minParameters.add (idx);
    }
    
    private void                addMaxParam (int idx, boolean exclusive) {
        if (exclusive)
            idx |= EXCLUSIVE_BIT;
        
        if (maxParameters == null)
            maxParameters = new IntegerArrayList ();
        
        maxParameters.add (idx);
    }
        
    public void                 update (
        CompiledExpression          e,
        SimpleFunctionCode          code,
        boolean                     timestampOnRight
    )
    {
        if (timestampOnRight) {
            switch (code) {
                case TIMESTAMP_GT:
                    code = SimpleFunctionCode.TIMESTAMP_LT;
                    break;
                    
                case TIMESTAMP_GE:
                    code = SimpleFunctionCode.TIMESTAMP_LE;
                    break;
                
                case TIMESTAMP_LT:
                    code = SimpleFunctionCode.TIMESTAMP_GT;
                    break;
                    
                case TIMESTAMP_LE:
                    code = SimpleFunctionCode.TIMESTAMP_GE;
                    break;                    
            }
        }
        
        if (e instanceof CompiledConstant) {
            CompiledConstant        cc = (CompiledConstant) e;            
            long                    t = (Long) cc.value;
            
            switch (code) {
                case TIMESTAMP_GT:
                    inclusiveMinimum = Math.max (inclusiveMinimum, t + 1);
                    break;
                    
                case TIMESTAMP_GE:
                    inclusiveMinimum = Math.max (inclusiveMinimum, t);
                    break;
                    
                case TIMESTAMP_LT:
                    inclusiveMaximum = Math.min (inclusiveMaximum, t - 1);
                    break;
                    
                case TIMESTAMP_LE:
                    inclusiveMaximum = Math.min (inclusiveMaximum, t);
                    break;
                    
                case TIMESTAMP_EQ:
                    inclusiveMinimum = Math.max (inclusiveMinimum, t);
                    inclusiveMaximum = Math.min (inclusiveMaximum, t);
                    break;
                    
                default:
                    throw new UnsupportedOperationException (code.name ());
            }
        }
        else if (e instanceof ParamAccess) {
            ParamAccess             pa = (ParamAccess) e;
            int                     idx = pa.ref.index;
            
            switch (code) {
                case TIMESTAMP_GT:
                    addMinParam (idx, true);
                    break;
                    
                case TIMESTAMP_GE:
                    addMinParam (idx, false);
                    break;
                    
                case TIMESTAMP_LT:
                    addMaxParam (idx, true);
                    break;
                    
                case TIMESTAMP_LE:
                    addMaxParam (idx, false);
                    break;
                    
                case TIMESTAMP_EQ:
                    addMaxParam (idx, false);
                    addMinParam (idx, false);
                    break;
                    
                default:
                    throw new UnsupportedOperationException (code.name ());
            }
        }
    }
    
    public long         getInclusiveMaximum () {
        return (inclusiveMaximum);
    }
    
    public long         getInclusiveMinimum () {
        return (inclusiveMinimum);
    }
}
