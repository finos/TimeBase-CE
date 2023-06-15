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
public final class ArithmeticExpression extends ComplexExpression {
    public final ArithmeticFunction             function;

    public ArithmeticExpression (long location, ArithmeticFunction function, Expression left, Expression right) {
        super (location, left, right);
        this.function = function;
    }

    public ArithmeticExpression (ArithmeticFunction function, Expression left, Expression right) {
        this (NO_LOCATION, function, left, right);
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        String  op;
        int     p;

        switch (function) {
            case ADD:    op = " + ";  p = OpPriority.ADDITION;       break;
            case SUB:    op = " - ";  p = OpPriority.ADDITION;       break;
            case MUL:    op = " * ";  p = OpPriority.MULTIPLICATION; break;
            case DIV:    op = " / ";  p = OpPriority.MULTIPLICATION; break;
            case MOD:    op = " % ";  p = OpPriority.MULTIPLICATION; break;
            default:    throw new RuntimeException ();
        }

        printBinary (outerPriority, op, p, InfixAssociation.LEFT, s);
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            function == ((ArithmeticExpression) obj).function
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * 31 + function.hashCode ());
    }
}