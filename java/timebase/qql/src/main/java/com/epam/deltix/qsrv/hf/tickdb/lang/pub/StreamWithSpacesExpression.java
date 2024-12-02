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
public class StreamWithSpacesExpression extends ComplexExpression {

    public Expression stream;
    public Expression[] spaces;

    public StreamWithSpacesExpression(long location, Expression stream, Expression... spaces) {
        super(location, concat(stream, spaces));
        this.stream = stream;
        this.spaces = spaces;
    }

    private static Expression[] concat(Expression expression, Expression... expressions) {
        List<Expression> list = new ArrayList<>();
        list.add(expression);
        list.addAll(Arrays.asList(expressions));
        return list.toArray(new Expression[0]);
    }

    @Override
    protected void print(int outerPriority, StringBuilder s) {
        stream.print(outerPriority, s);
        s.append("[");
        for (int i = 0; i < spaces.length; ++i) {
            if (i > 0) {
                s.append(",");
            }
            spaces[i].print(outerPriority, s);
        }
        s.append("]");
    }
}
