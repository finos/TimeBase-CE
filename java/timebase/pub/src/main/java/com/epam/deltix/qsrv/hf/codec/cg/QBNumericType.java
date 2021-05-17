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

import com.epam.deltix.util.jcg.JExpr;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

/**
 *
 */
public class QBNumericType<T extends QNumericType> extends QBoundType<T> {

    public QBNumericType(T qType, Class<?> javaType, QAccessor accessor) {
        super(qType, javaType, accessor);
    }

    @Override
    public JExpr readIsConstraintViolated() {
        final Number min = qType.getMin();
        final Number max = qType.getMax();

        if (max != null || min != null) {
            return getConstraintExpression(this, min, max);
        } else
            throw new IllegalStateException("Range is not defined for this type " + qType.dt);
    }

    JExpr getConstraintExpression(QBoundType type, Number min, Number max) {
        JExpr valueExpr = type.accessor.read();
        // skip conditions with negative value for unsigned integer
        final JExpr minCondition = (min != null)  ?
            CTXT.binExpr(valueExpr, "<", QCGHelpers.objtoex(min)) :
            null;
        final JExpr maxCondition = (max != null) ?
            CTXT.binExpr(valueExpr, ">", QCGHelpers.objtoex(max)) :
            null;

        JExpr rangeCondition;
        if (minCondition != null) {
            rangeCondition = (maxCondition != null) ?
                CTXT.binExpr(minCondition, "||", maxCondition) :
                minCondition;
        } else
            rangeCondition = maxCondition;

        return type.qType.isNullable() ?
            CTXT.binExpr(type.readIsNull(false), "&&", rangeCondition) :
            rangeCondition;
    }
}
