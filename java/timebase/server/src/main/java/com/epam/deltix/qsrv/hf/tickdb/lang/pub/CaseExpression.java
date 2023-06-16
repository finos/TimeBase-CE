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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 */
public final class CaseExpression extends ComplexExpression {

    public Expression caseExpression;
    public List<WhenExpression> whenExpressions;
    public Expression elseExpression;

    public CaseExpression(long location, Expression caseExpression, List<WhenExpression> whenExpressions, Expression elseExpression) {
        super(location, makeList(caseExpression, whenExpressions, elseExpression));

        this.caseExpression = caseExpression;
        this.whenExpressions = whenExpressions;
        this.elseExpression = elseExpression;
    }

    private static Expression[] makeList(Expression caseExpression, List<WhenExpression> whenExpressions, Expression elseExpression) {
        List<Expression> expressions = new ArrayList<>();
        expressions.add(caseExpression);
        expressions.addAll(whenExpressions);
        expressions.add(elseExpression);
        return expressions.toArray(new Expression[0]);
    }

    @Override
    protected void print(int outerPriority, StringBuilder s) {
        s.append("CASE ");
        if (caseExpression != null) {
            caseExpression.print(s);
        }
        whenExpressions.forEach(w -> {
            s.append(" ");
            w.print(s);
            s.append(" ");
        });
        s.append("ELSE ");
        if (elseExpression != null) {
            elseExpression.print(s);
        } else {
            s.append("null");
        }

        s.append(" END");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CaseExpression that = (CaseExpression) o;
        return Objects.equals(caseExpression, that.caseExpression) &&
            Objects.equals(whenExpressions, that.whenExpressions) &&
            Objects.equals(elseExpression, that.elseExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), caseExpression, whenExpressions, elseExpression);
    }
}
