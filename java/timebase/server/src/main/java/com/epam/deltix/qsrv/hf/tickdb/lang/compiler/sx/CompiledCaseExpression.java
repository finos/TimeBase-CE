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

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class CompiledCaseExpression extends CompiledComplexExpression {
    public final CompiledExpression<?> caseExpression;
    public final List<CompiledWhenExpression> whenExpressions;
    public final CompiledExpression<?> elseExpression;

    public CompiledCaseExpression(DataType type,
                                  CompiledExpression<?> caseExpression,
                                  List<CompiledWhenExpression> whenExpressions,
                                  CompiledExpression<?> elseExpression)
    {
        super(type, makeList(caseExpression, whenExpressions, elseExpression));

        this.caseExpression = caseExpression;
        this.whenExpressions = whenExpressions;
        this.elseExpression = elseExpression;
    }

    private static CompiledExpression<?>[] makeList(CompiledExpression<?> caseExpression,
                                                    List<CompiledWhenExpression> whenExpressions,
                                                    CompiledExpression<?> elseExpression) {

        List<CompiledExpression<?>> expressions = new ArrayList<>();
        expressions.add(caseExpression);
        expressions.addAll(whenExpressions);
        expressions.add(elseExpression);
        return expressions.toArray(new CompiledExpression<?>[0]);
    }

    @Override
    public void print(StringBuilder out) {
        out.append("case ");
        if (caseExpression != null) {
            caseExpression.print(out);
        }
        whenExpressions.forEach(w -> {
            out.append(" ");
            w.print(out);
            out.append(" ");
        });
        out.append("else ");
        if (elseExpression != null) {
            elseExpression.print(out);
        } else {
            out.append("null");
        }

        out.append(" end");
    }

}
