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

public class LimitExpression extends ComplexExpression {

    public final Expression offset;
    public final Expression limit;

    public LimitExpression(long location, Expression limit) {
        super(location, limit);
        this.offset = null;
        this.limit = limit;
    }

    public LimitExpression(long location, Expression offset, Expression limit) {
        super(location, offset, limit);
        this.offset = offset;
        this.limit = limit;
    }

    @Override
    protected void print(int outerPriority, StringBuilder s) {
        s.append("LIMIT ");
        limit.print(outerPriority, s);

        if (offset != null) {
            s.append(" OFFSET ");
            offset.print(outerPriority, s);
        }
    }
}