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
public class TupleExpression extends ComplexExpression {
    public final TypeIdentifier             typeId;

    public TupleExpression (long location, TypeIdentifier typeId, Expression ... args) {
        super (location, args);
        this.typeId = typeId;
    }

    public TupleExpression (TypeIdentifier typeId, Expression ... args) {
        this (NO_LOCATION, typeId, args);
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        s.append ("new ");
        if (typeId == null)
            s.append ("?");
        else
            typeId.print (s);

        s.append ("(");
        args [0].print (OpPriority.COMMA, s);

        for (int ii = 1; ii < args.length; ii++) {
            s.append (", ");
            args [ii].print (OpPriority.COMMA, s);
        }

        s.append (")");
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            typeId.equals (((TupleExpression) obj).typeId)
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * 41 + typeId.hashCode ());
    }
}