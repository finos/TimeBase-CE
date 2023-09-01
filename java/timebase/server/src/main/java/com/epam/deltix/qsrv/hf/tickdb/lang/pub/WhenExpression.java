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
public final class WhenExpression extends ComplexExpression {

    public Expression whenExpression;
    public Expression thenExpression;
    public WhenExpression(long location, Expression whenExpression, Expression thenExpression) {
        super(location, whenExpression, thenExpression);

        this.whenExpression = whenExpression;
        this.thenExpression = thenExpression;
    }

    @Override
    protected void print(int outerPriority, StringBuilder s) {
        s.append("WHEN ");
        whenExpression.print(s);
        s.append(" THEN ");
        thenExpression.print(s);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        WhenExpression that = (WhenExpression) o;
        return Objects.equals(whenExpression, that.whenExpression) && Objects.equals(thenExpression, that.thenExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), whenExpression, thenExpression);
    }
}
