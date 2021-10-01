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

import java.util.Objects;

/**
 *
 */
public final class UnionExpression extends ComplexExpression {

    public Expression left;
    public Expression right;
    public LimitExpression limit;

    public UnionExpression(long location, Expression left, Expression right) {
        super(location, left, right);

        this.left = left;
        this.right = right;
    }

    public UnionExpression(long location, Expression left, Expression right, LimitExpression limit) {
        super(location, left, right);

        this.left = left;
        this.right = right;
        this.limit = limit;
    }

    @Override
    protected void print(int outerPriority, StringBuilder s) {
        s.append("(");
        printBinary(outerPriority, " UNION ", OpPriority.UNION, InfixAssociation.LEFT, s);
        s.append(") ");
        if (limit != null) {
            limit.print(outerPriority, s);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        UnionExpression that = (UnionExpression) o;
        return Objects.equals(limit, that.limit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), limit);
    }
}