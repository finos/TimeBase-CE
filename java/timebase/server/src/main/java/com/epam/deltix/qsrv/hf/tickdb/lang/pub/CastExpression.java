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
public final class CastExpression extends ComplexExpression {
    public final String             typeId;

    public CastExpression (long location, Expression arg, String typeId) {
        super (location, arg);
        this.typeId = typeId;
    }

    public CastExpression (Expression arg, String typeId) {
        this (NO_LOCATION, arg, typeId);
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        s.append ("cast (");
        
        args [0].print (OpPriority.COMMA, s);
        
        s.append (", ");
        
        GrammarUtil.escapeIdentifier (NamedObjectType.TYPE, typeId, s);
        
        s.append (")");
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            typeId.equals (((CastExpression) obj).typeId)
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * 41 + typeId.hashCode ());
    }
}
