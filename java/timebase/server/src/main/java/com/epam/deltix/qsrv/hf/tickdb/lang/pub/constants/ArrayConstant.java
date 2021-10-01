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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants;

import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Expression;
import com.epam.deltix.util.lang.Util;

import java.util.List;

public abstract class ArrayConstant extends Expression {

    private final List<Expression> expressions;

    public ArrayConstant(long location, List<Expression> expressions) {
        super(location);
        this.expressions = expressions;
    }

    @Override
    protected void print(int outerPriority, StringBuilder s) {
        if (expressions.isEmpty()) {
            s.append("[]");
        } else {
            s.append("[");
            expressions.get(0).print(s);
            for (int i = 1; i < expressions.size(); i++) {
                s.append(',');
                expressions.get(i).print(s);
            }
            s.append(']');
        }
    }

    public List<Expression> getExpressions() {
        return expressions;
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 31 + Util.arrayHashCode(expressions.toArray());
    }
}