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
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.BinaryLogicalOperation;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.OrderRelation;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
abstract class QQLPostProcessingPatterns {
    public static boolean       isConjunction (CompiledExpression e) {
        return e instanceof LogicalOperation && ((LogicalOperation) e).getOperation() == BinaryLogicalOperation.AND;
    }
    
    public static boolean       isNull (CompiledExpression e) {
        return (e instanceof CompiledConstant && ((CompiledConstant) e).isNull ());
    }
    
    public static boolean       isSimpleFunction (
        CompiledExpression          e,
        SimpleFunctionCode          code
    )
    {
        return (
            e instanceof SimpleFunction &&
            ((SimpleFunction) e).code == code
        );
    }
    
    private static void                     flattenConjunction (
        CompiledExpression                      e,
        List <CompiledExpression>               out
    )
    {
        if (isConjunction(e)) {
            LogicalOperation ae = (LogicalOperation) e;

            flattenConjunction(ae.args[0], out);
            flattenConjunction(ae.args[1], out);
        }
        else
            out.add (e);
    }
    
    public static List <CompiledExpression> flattenConjunction (
        CompiledExpression                      e
    )
    {
        List <CompiledExpression>   ret = new ArrayList <CompiledExpression> ();
        
        flattenConjunction (e, ret);
        
        return (ret);
    }
    
    public static CompiledExpression    reconstructConjunction (
        List <CompiledExpression>           list
    )
    {
        int                     n = list.size ();
        
        if (n == 0)
            return (null);
        
        CompiledExpression      e = list.get (0);
        
        for (int ii = 1; ii < n; ii++)
            e = new LogicalOperation(BinaryLogicalOperation.AND, e, list.get (ii));
        
        return (e);
    }
    
    public static boolean       isStatic (CompiledExpression e) {
        return (e instanceof CompiledConstant || e instanceof ParamAccess);
    }

    public static TimestampLimits adjustTimestampLimits(List<CompiledExpression> condition, long endTimestamp) {
        if (endTimestamp == Long.MIN_VALUE) {
            return extractTimestampLimits(condition);
        }
        TimestampLimits limits = extractTimestampLimits(condition);
        if (limits == null) {
            limits = new TimestampLimits();
        }
        limits.update(new CompiledConstant(StandardTypes.CLEAN_TIMESTAMP, endTimestamp), OrderRelation.LE, false);
        return limits;
    }

    public static TimestampLimits extractTimestampLimits(List<CompiledExpression> condition) {
        TimestampLimits range = null;

        for (int ii = 0; ii < condition.size(); ) {
            CompiledExpression<?> e = condition.get(ii);
            boolean matched = false;
            if (e instanceof ComparisonOperation) {
                ComparisonOperation operation = (ComparisonOperation) e;
                CompiledExpression<?> left = operation.args[0];
                CompiledExpression<?> right = operation.args[1];
                if (left instanceof TimestampSelector && isStatic(right)) {
                    if (range == null)
                        range = new TimestampLimits();

                    range.update(right, operation.getRelation(), false);
                    matched = true;
                } else if (right instanceof TimestampSelector && isStatic(left)) {
                    if (range == null)
                        range = new TimestampLimits();

                    range.update(left, operation.getRelation(), true);
                    matched = true;
                }
            }

            if (matched)
                condition.remove(ii);
            else
                ii++;
        }

        return (range);
    }
}