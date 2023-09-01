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

package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.qsrv.hf.pub.md.DataType;

/**
 *
 */
public class CompiledIfExpression extends CompiledComplexExpression {
    public final CompiledExpression<?> condition;
    public final CompiledExpression<?> thenExpression;
    public final CompiledExpression<?> elseExpression;

    public CompiledIfExpression(DataType type,
                                CompiledExpression<?> condition,
                                CompiledExpression<?> thenExpression,
                                CompiledExpression<?> elseExpression)
    {
        super(type, condition, thenExpression, elseExpression);

        this.condition = condition;
        this.thenExpression = thenExpression;
        this.elseExpression = elseExpression;
    }

    @Override
    public void print(StringBuilder out) {
        thenExpression.print(out);
        out.append(" IF ");
        condition.print(out);
        out.append(" ELSE ");
        elseExpression.print(out);
    }

}
