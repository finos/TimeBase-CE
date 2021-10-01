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
package com.epam.deltix.util.jcg;

/**
 *
 */
public interface JExpr {
    public JExpr        cast (Class <?> toClass);

    public JExpr        cast (JType toType);

    public JExpr        index (JExpr index);

    public JExpr        index (int index);

    public JExpr        field (String fieldId);

    public JStatement   asStmt ();

    public JExpr        call (String method, JExpr ... args);

    public JExpr        not ();

    public JExpr        negate ();

    public JExpr        incAndGet ();

    public JExpr        decAndGet ();

    public JExpr        getAndInc ();

    public JExpr        getAndDec ();

    public JStatement   inc ();

    public JStatement   dec ();

    public JStatement   alter (String op, JExpr arg);

    public JStatement   throwStmt ();

    public JStatement   returnStmt ();

    public JSwitchStatement switchStmt ();

    public JSwitchStatement switchStmt (String label);

    public JStatement       assign (JExpr value);

    public JExpr            assignExpr (JExpr value);    
}