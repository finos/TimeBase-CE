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
public final class FieldAccessorExpression extends ComplexExpression {
    public final Expression parent;
    public final FieldIdentifier identifier;
    public final boolean fetchNulls;

    public FieldAccessorExpression(long location, Expression parent, FieldIdentifier identifier) {
        this(location, parent, identifier, false);
    }

    public FieldAccessorExpression(long location, Expression parent, FieldIdentifier identifier, boolean fetchNulls) {
        super(location, parent);
        this.parent = parent;
        this.identifier = identifier;
        this.fetchNulls = fetchNulls;
    }

    public FieldAccessorExpression(Expression parent, FieldIdentifier identifier) {
        this(NO_LOCATION, parent, identifier);
    }

    @Override
    protected void print(int outerPriority, StringBuilder s) {
        if (parent != null) {
            parent.print(s);
            s.append(".");
        }
        identifier.print(s);
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object obj) {
        return (
            super.equals(obj) &&
                identifier.equals(((FieldAccessorExpression) obj).identifier) &&
                fetchNulls == ((FieldAccessorExpression) obj).fetchNulls
        );
    }

    @Override
    public int hashCode() {
        return (super.hashCode() * 31 * 31 + identifier.hashCode() * 31 + Objects.hash(fetchNulls));
    }
}