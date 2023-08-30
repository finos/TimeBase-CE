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
 *  The "*" in "select object.* ..."
 */
public final class ThisObject extends Expression {
    public final Expression parent;

    public ThisObject(long location, Expression parent) {
        super(location);

        this.parent = parent;
    }

    protected void print(int outerPriority, StringBuilder s) {
        parent.print(outerPriority, s);
        s.append('.');
        s.append('*');
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object obj) {
        return super.equals(obj) && Objects.equals(parent, ((ThisObject) obj).parent);
    }

    @Override
    public int hashCode() {
        return (super.hashCode() * 41 + (parent != null ? parent.hashCode() : 0)) * 31;
    }
}