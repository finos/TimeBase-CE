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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.computations.BooleanFunctions;
import com.epam.deltix.qsrv.hf.pub.md.StandardTypes;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QArrayValue;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QValue;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.UnexpectedTypeException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Expression;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.QRT;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

public class NotOperation extends UnaryExpression {

    public NotOperation(CompiledExpression<?> value) {
        super(value, value.type);
    }

    @Override
    public void print(StringBuilder out) {
        out.append("NOT ");
        args[0].print(out);
    }

    @Override
    public void generateOperation(QValue value, QValue out, JCompoundStatement addTo) {
        if (out instanceof QArrayValue) {
            generateOperation(value, (QArrayValue) out, addTo);
        } else {
            JExpr argExpr = value.read();
            addTo.add(out.write(CTXT.staticCall(QRT.class, "bnot", argExpr)));
        }
    }

    public void generateOperation(QValue value, QArrayValue out, JCompoundStatement addTo) {
        JCompoundStatement statement = CTXT.compStmt();
        statement.add(out.setInstance());
        statement.add(CTXT.staticCall(BooleanFunctions.class, "not", value.read(), out.read()));
        statement.add(out.setChanged());
        addTo.add(statement);
    }

    public static void validate(Expression expression, CompiledExpression<?> arg) throws UnexpectedTypeException {
        if (!StandardTypes.isBooleanOrBooleanArray(arg.type))
            throw new UnexpectedTypeException(expression, arg.type, StandardTypes.CLEAN_BOOLEAN);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof NotOperation)) return false;
        return super.equals(obj);
    }
}