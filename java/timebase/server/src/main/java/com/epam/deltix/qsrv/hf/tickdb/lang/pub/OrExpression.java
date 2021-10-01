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
public final class OrExpression extends ComplexExpression {
    public OrExpression (long location, Expression left, Expression right) {
        super (location, left, right);
    }

    public OrExpression (Expression left, Expression right) {
        this (NO_LOCATION, left, right);
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        printBinary (outerPriority, " OR ", OpPriority.LOGICAL_OR, InfixAssociation.LEFT, s);
    }
}