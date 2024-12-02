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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class PredicateExpression extends ComplexExpression {
    public Expression selectorExpression;
    public Expression[] predicateExpressions;

    public PredicateExpression(long location, Expression selectorExpression, Expression... predicateExpression) {
        super(location, concat(selectorExpression, predicateExpression));
        this.selectorExpression = selectorExpression;
        this.predicateExpressions = predicateExpression;
    }

    private static Expression[] concat(Expression expression, Expression... expressions) {
        List<Expression> list = new ArrayList<>();
        list.add(expression);
        list.addAll(Arrays.asList(expressions));
        return list.toArray(new Expression[0]);
    }

    @Override
    protected void print(int outerPriority, StringBuilder s) {
        selectorExpression.print(outerPriority, s);
        s.append("[");
        for (int i = 0; i < predicateExpressions.length; ++i) {
            if (i > 0) {
                s.append(",");
            }
            predicateExpressions[i].print(outerPriority, s);
        }
        s.append("]");
    }

}
