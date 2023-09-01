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

/**
 *
 */
public class ArraySlicingExpression extends ComplexExpression {

    public ArraySlicingExpression(long location, Expression e1, Expression e2) {
        super(location, e1, e2, null);
    }

    public ArraySlicingExpression(long location, Expression e1, Expression e2, Expression e3) {
        super(location, e1, e2, e3);
    }

    @Override
    protected void print(int outerPriority, StringBuilder s) {
        if (args[0] != null) {
            args[0].print(s);
        }
        s.append(":");
        if (args[1] != null) {
            args[1].print(s);
        }
        s.append(":");
        if (args[2] != null) {
            args[2].print(s);
        }
    }

}