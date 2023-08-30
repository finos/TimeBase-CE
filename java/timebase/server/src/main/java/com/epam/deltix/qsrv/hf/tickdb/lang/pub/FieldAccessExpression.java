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
public final class FieldAccessExpression extends Expression {
    public final TypeIdentifier     typeId;
    public final FieldIdentifier    fieldId;

    public FieldAccessExpression (long location, TypeIdentifier typeId, FieldIdentifier fieldId) {
        super (location);
        this.typeId = typeId;
        this.fieldId = fieldId;
    }

    public FieldAccessExpression (TypeIdentifier typeId, FieldIdentifier fieldId) {
        this (NO_LOCATION, typeId, fieldId);
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        typeId.print (s);
        s.append (":");
        fieldId.print (s);
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            typeId.equals (((FieldAccessExpression) obj).typeId) &&
            fieldId.equals (((FieldAccessExpression) obj).fieldId)
        );
    }

    @Override
    public int                      hashCode () {
        return ((super.hashCode () * 41 + typeId.hashCode ()) * 31 + fieldId.hashCode ());
    }
}