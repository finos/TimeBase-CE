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

import java.util.Arrays;
import java.util.Objects;

/**
 *
 */
public final class SelectExpression extends ComplexExpression {
    public static final int MODE_DISTINCT = 1;
    public static final int MODE_RUNNING = 1 << 1;
    public static final int MODE_TOTAL = 1 << 2;

    private final int mode;
    public final TypeIdentifier typeId;
    public final Expression[] groupBy;
    private final OverExpression overExpression;
    private Expression[] with;
    private LimitExpression limit;
    private long endTime = Long.MIN_VALUE;

    private static Expression[] cat(Expression source, Expression filter, ArrayJoin arrayJoin, Expression... selectors) {
        int n = selectors == null ? 0 : selectors.length;
        Expression[] a = new Expression[n + 3];

        a[0] = source;
        a[1] = filter;
        a[2] = arrayJoin;

        if (n != 0)
            System.arraycopy(selectors, 0, a, 3, n);

        return (a);
    }

    public SelectExpression(long location, Expression source, ArrayJoin arrayJoin, Expression filter, TypeIdentifier typeId, int mode,
                            Expression[] groupBy, OverExpression overExpression, LimitExpression limit, Expression... selectors) {
        super(location, cat(source, filter, arrayJoin, selectors));
        this.mode = mode;
        this.typeId = typeId;
        this.groupBy = groupBy;
        this.overExpression = overExpression;
        this.limit = limit;
    }

    public SelectExpression(Expression source, ArrayJoin arrayJoin, Expression filter, TypeIdentifier typeId, int mode, Expression[] groupBy,
                            OverExpression overExpression, LimitExpression limit, Expression... selectors
    ) {
        this(NO_LOCATION, source, arrayJoin, filter, typeId, mode, groupBy, overExpression, limit, selectors);
    }

    public boolean isDistinct() {
        return ((mode & MODE_DISTINCT) != 0);
    }

    public boolean isRunning() {
        return ((mode & MODE_RUNNING) != 0);
    }

    public boolean isTotal() {
        return ((mode & MODE_TOTAL) != 0);
    }

    public boolean isSelectAll() {
        return (args.length == 3);
    }

    public Expression[] getSelectors() {
        int n = args.length - 3;

        Expression[] a = new Expression[n];

        System.arraycopy(args, 3, a, 0, n);

        return (a);
    }

    public Expression getSource() {
        return (args[0]);
    }

    public Expression getFilter() {
        return (args[1]);
    }

    public ArrayJoin getArrayJoin() {
        return (ArrayJoin) (args[2]);
    }

    @Override
    protected void print(int outerPriority, StringBuilder s) {
        if (with != null) {
            s.append("WITH ");
            for (int i = 0; i < with.length; ++i) {
                if (i > 0) {
                    s.append(", ");
                }
                with[i].print(s);
            }
            s.append(" ");
        }

        s.append("SELECT ");

        if (isDistinct())
            s.append("DISTINCT ");

        if (isRunning())
            s.append("RUNNING ");

        printCommaSepArgs(3, args.length, s);

        if (typeId != null) {
            s.append(" TYPE ");
            typeId.print(s);
        }

        s.append(" FROM ");
        getSource().print(OpPriority.QUERY, s);

        if (getArrayJoin() != null) {
            getArrayJoin().print(OpPriority.QUERY, s);
        }

        if (getFilter() != null) {
            s.append(" WHERE ");
            getFilter().print(OpPriority.QUERY, s);
        }

        if (groupBy != null) {
            s.append(" GROUP BY ");
            groupBy[0].print(s);

            for (int ii = 1; ii < groupBy.length; ii++) {
                s.append(", ");
                groupBy[ii].print(s);
            }
        }

        if (limit != null) {
            limit.print(outerPriority, s);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SelectExpression))
            return false;
        SelectExpression se = (SelectExpression) obj;
        return super.equals(obj) && mode == se.mode
                && Arrays.equals(groupBy, se.groupBy)
                && Arrays.equals(with, se.with)
                && Objects.equals(overExpression, se.overExpression)
                && Objects.equals(typeId, se.typeId)
                && Objects.equals(limit, se.limit);
    }

    @Override
    public int hashCode() {
        return (super.hashCode() * 41 + mode) * 31 * 31 * 31 * 31 * 31 + Arrays.hashCode(groupBy) * 31 * 31 * 31 * 31 +
            Arrays.hashCode(with) * 31 * 31 * 31 + Objects.hashCode(overExpression) * 31 * 31 +
            Objects.hashCode(typeId) * 31 + Objects.hashCode(limit);
    }

    public void setWithExpressions(Expression[] with) {
        this.with = with;
    }

    public Expression[] getWithExpressions() {
        return with;
    }

    public OverExpression getOverExpression() {
        return overExpression;
    }

    public LimitExpression getLimit() {
        return limit;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
}