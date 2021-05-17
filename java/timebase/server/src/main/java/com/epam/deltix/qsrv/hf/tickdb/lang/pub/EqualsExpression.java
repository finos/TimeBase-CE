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

/**
 *
 */
public final class EqualsExpression extends ComplexExpression {
    public final boolean        isEqual;

    public EqualsExpression (long location, Expression left, Expression right, boolean isEqual) {
        super (location, left, right);

        this.isEqual = isEqual;
    }

    public EqualsExpression (Expression left, Expression right, boolean isEqual) {
        this (NO_LOCATION, left, right, isEqual);
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        printBinary (
            outerPriority, isEqual ? " = " : " != ",
            OpPriority.RELATIONAL,
            InfixAssociation.LEFT,
            s
        );
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            isEqual == ((EqualsExpression) obj).isEqual
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * (isEqual ? 23 : 41));
    }
}
