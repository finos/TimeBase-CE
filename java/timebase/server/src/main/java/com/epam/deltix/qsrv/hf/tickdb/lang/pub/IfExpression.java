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

package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import java.util.Objects;

/**
 *
 */
public final class IfExpression extends ComplexExpression {

    public Expression condition;
    public Expression thenExpression;
    public Expression elseExpression;

    public IfExpression(long location, Expression condition, Expression thenExpression, Expression elseExpression) {
        super(location, condition, thenExpression, elseExpression);

        this.condition = condition;
        this.thenExpression = thenExpression;
        this.elseExpression = elseExpression;
    }

    @Override
    protected void print(int outerPriority, StringBuilder s) {
        thenExpression.print(s);
        s.append(" IF ");
        condition.print(s);
        s.append(" ELSE ");
        if (elseExpression != null) {
            elseExpression.print(s);
        } else {
            s.append("null");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        IfExpression that = (IfExpression) o;
        return Objects.equals(condition, that.condition) &&
            Objects.equals(thenExpression, that.thenExpression) &&
            Objects.equals(elseExpression, that.elseExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), condition, thenExpression, elseExpression);
    }
}
