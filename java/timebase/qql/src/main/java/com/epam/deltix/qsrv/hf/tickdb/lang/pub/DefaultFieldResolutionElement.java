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


public class DefaultFieldResolutionElement extends Expression {

    public final Identifier id;
    public final Expression e;

    public DefaultFieldResolutionElement(long location, Identifier id, Expression e) {
        super(location);
        this.id = id;
        this.e = e;
    }

    @Override
    protected void print(int outerPriority, StringBuilder s) {
        s.append("RESOLVE ")
            .append(id)
            .append(" SET DEFAULT ");
        e.print(s);
    }
}
