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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;

/**
 *
 */
public class QPluginArgValue extends QValue {
    private final JExpr         instance;
    private final int           argIdx;

    public QPluginArgValue (QType type, JExpr instance, int argIdx) {
        super (type);
        this.instance = instance;
        this.argIdx = argIdx;
    }

    @Override
    public JExpr                read () {
        if (type instanceof QArrayType) {
            return instance.call("get");
        }
        throw new UnsupportedOperationException ("Can't read plugin args");
    }

    @Override
    public JStatement           write (JExpr arg) {
        return (instance.call ("set" + (argIdx + 1), arg).asStmt ());
    }         
}