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
public class ArrayJoin extends ComplexExpression {
    public final boolean left;

    public ArrayJoin(long location, Expression[] expressions, boolean left) {
        super(location, expressions);

        if (expressions.length == 0) {
            throw new RuntimeException("Array Join expression list is empty");
        }

        this.left = left;
    }

    @Override
    protected void print(int outerPriority, StringBuilder s) {
        if (left) {
            s.append("LEFT ");
        }

        s.append("ARRAY JOIN ");
        for (int i = 0; i < args.length; ++i) {
            if (i > 0) {
                s.append(", ");
            }

            args[i].print(outerPriority, s);
        }
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ArrayJoin that = (ArrayJoin) o;
        return left == that.left;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), left);
    }
}