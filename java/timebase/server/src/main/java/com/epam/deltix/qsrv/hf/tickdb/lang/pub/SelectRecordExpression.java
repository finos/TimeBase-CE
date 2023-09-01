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
public final class SelectRecordExpression extends ComplexExpression {
    private TypeIdentifier typeId;
    private Expression when;

    public SelectRecordExpression(long location, Expression[] selectors, TypeIdentifier typeId, Expression when) {
        super(location, cat(when, selectors));
        this.typeId = typeId;
        this.when = when;
    }

    private static Expression[] cat(Expression when, Expression... selectors) {
        int n = selectors == null ? 0 : selectors.length;
        Expression[] a = new Expression[n + 1];

        a[0] = when;
        if (n != 0) {
            System.arraycopy(selectors, 0, a, 1, n);
        }

        return (a);
    }

    public Expression[] getSelectors() {
        int n = args.length - 1;
        Expression[] a = new Expression[n];
        System.arraycopy(args, 1, a, 0, n);
        return (a);
    }

    public TypeIdentifier getTypeId() {
        return typeId;
    }

    public Expression getWhen() {
        return when;
    }

    @Override
    protected void print(int outerPriority, StringBuilder s) {
        s.append("RECORD ");
        printCommaSepArgs(1, args.length, s);
        typeId.print(s);
        s.append(" WHEN ");
        when.print(s);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SelectExpression))
            return false;
        SelectExpression se = (SelectExpression) obj;
        return super.equals(obj)
            && Objects.equals(typeId, se.typeId);
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 41 * 31  + Objects.hashCode(typeId);
    }
}
