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
public final class BetweenExpression extends ComplexExpression {
    public BetweenExpression (long location, Expression arg, Expression min, Expression max) {
        super (location, arg, min, max);
    }

    public BetweenExpression (OrderRelation relation, Expression arg, Expression min, Expression max) {
        this (NO_LOCATION, arg, min, max);
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        boolean                     parenthesize = outerPriority > OpPriority.LOGICAL_AND;

        if (parenthesize)
            s.append ("(");

        int                         p = OpPriority.LOGICAL_AND + 1;
        
        args [0].print (p, s);
        s.append (" BETWEEN ");
        args [1].print (p, s);
        s.append (" AND ");
        args [2].print (p, s);

        if (parenthesize)
            s.append (")");
    }
}