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

import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

/**
 *
 */
public class QObjectValue extends QValue {
    protected final JExpr variable;

    public QObjectValue(QObjectType type, JExpr variable) {
        super(type);

        this.variable = variable;
    }

    public void skip(JExpr input, JCompoundStatement addTo) {

    }

    @Override
    public JExpr read() {
        return variable;
    }

    @Override
    public JExpr readIsNull(boolean eq) {
        JExpr e = variable.call("isNull");
        return eq ? e : e.not();
    }

    @Override
    public JStatement write(JExpr arg) {
        return variable.call("set", arg).asStmt();
    }

    public JExpr adjustTypes(JExpr value) {
        return variable.call("adjustTypeId", value);
    }

    public JExpr copy(JExpr another) {
        return variable.call("copyFrom", another);
    }

    public JExpr reset() {
        return variable.call("reset");
    }
}