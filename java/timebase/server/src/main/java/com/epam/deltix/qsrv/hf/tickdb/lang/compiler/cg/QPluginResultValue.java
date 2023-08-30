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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;

/**
 *
 */
public class QPluginResultValue extends QValue {
    private final JExpr         instance;
    
    public QPluginResultValue (QType type, JExpr instance) {
        super (type);
        this.instance = instance;
    }

    @Override
    public JExpr                read () {
        return (instance.call ("get"));        
    }

    @Override
    public JStatement           write (JExpr arg) {
        throw new UnsupportedOperationException ("Can't write plugin result");
    }         
}