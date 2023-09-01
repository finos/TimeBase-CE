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
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.DataTypeHelper;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.IllegalTypeCombinationException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.BinaryLogicalOperation;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Expression;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.QRT;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

public class LogicalOperation extends BinaryExpression {

    private final BinaryLogicalOperation operation;

    public LogicalOperation(BinaryLogicalOperation operation, CompiledExpression<?> leftArg, CompiledExpression<?> rightArg) {
        super(leftArg, rightArg, DataTypeHelper.logicalOperationResult(leftArg.type, rightArg.type));
        this.operation = operation;
    }

    @Override
    public void generateOperation(QValue left, QValue right, QValue out, JCompoundStatement addTo) {
        if (out instanceof QArrayValue) {
            generateOperation(left, right, (QArrayValue) out, addTo);
        } else {
            JExpr leftE = left.read();
            JExpr rightE = right.read();
            JExpr e = CTXT.staticCall(QRT.class, operation.getQrtMethod(), leftE, rightE);
            JStatement s = out.write(e);
            JExpr globalNullCheck = CTXT.binExpr(
                    left.readIsNull(true),
                    "||",
                    right.readIsNull(true)
            );
            addTo.add(CTXT.ifStmt(globalNullCheck, out.writeNull(), s));
        }
    }

    private void generateOperation(QValue left, QValue right, QArrayValue out, JCompoundStatement addTo) {
        JCompoundStatement statement = CTXT.compStmt();
        statement.add(out.setInstance());
        statement.add(CTXT.staticCall(BooleanFunctions.class, operation.getArrayMethod(), left.read(), right.read(), out.read()));
        statement.add(out.setChanged());
        JExpr globalNullCheck = CTXT.binExpr(
                left.readIsNull(true),
                "||",
                right.readIsNull(true)
        );
        addTo.add(CTXT.ifStmt(globalNullCheck, out.writeNull(), statement));
    }

    @Override
    public String getOperator() {
        return operation.getOperator();
    }

    public BinaryLogicalOperation getOperation() {
        return operation;
    }

    public static void validate(Expression e, CompiledExpression<?> left, CompiledExpression<?> right)
            throws IllegalTypeCombinationException {
        if (!StandardTypes.isBooleanOrBooleanArray(left.type) || !StandardTypes.isBooleanOrBooleanArray(right.type)) {
            throw new IllegalTypeCombinationException(e, left.type, right.type);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        LogicalOperation that = (LogicalOperation) o;

        return operation == that.operation;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + operation.hashCode();
        return result;
    }
}