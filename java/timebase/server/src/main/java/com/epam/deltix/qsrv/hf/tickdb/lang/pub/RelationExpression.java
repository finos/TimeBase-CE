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
public final class RelationExpression extends ComplexExpression {
    public final OrderRelation             relation;

    public RelationExpression (long location, OrderRelation relation, Expression left, Expression right) {
        super (location, left, right);
        this.relation = relation;
    }

    public RelationExpression (OrderRelation relation, Expression left, Expression right) {
        this (NO_LOCATION, relation, left, right);
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        String  op;

        switch (relation) {
            case GE:    op = " >= ";    break;
            case GT:    op = " > ";     break;
            case LE:    op = " <= ";    break;
            case LT:    op = " < ";     break;
            default:    throw new RuntimeException ();
        }

        printBinary (outerPriority, op, OpPriority.RELATIONAL, InfixAssociation.LEFT, s);
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            relation == ((RelationExpression) obj).relation
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * 41 + relation.hashCode ());
    }
}