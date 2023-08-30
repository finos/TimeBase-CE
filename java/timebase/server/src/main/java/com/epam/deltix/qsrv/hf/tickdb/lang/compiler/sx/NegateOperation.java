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

import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.md.ArrayDataType;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.StandardTypes;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QArrayValue;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QType;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QValue;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.DataTypeHelper;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.NumericType;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.UnexpectedTypeException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.UnaryMinusExpression;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JLocalVariable;
import com.epam.deltix.util.jcg.JStatement;

import java.lang.reflect.Modifier;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QType.wrapWithNullCheck;

public class NegateOperation extends UnaryExpression {

    public NegateOperation(CompiledExpression<?> value, DataType result) {
        super(value, result);
    }

    @Override
    public void print(StringBuilder out) {
        out.append("-");
        args[0].print(out);
    }

    @Override
    public void generateOperation(QValue value, QValue out, JCompoundStatement addTo) {
        if (args[0].type instanceof ArrayDataType) {
            generateForArray((QArrayValue) value, (QArrayValue) out, addTo);
        } else {
            JExpr argExpr = value.read();
            JExpr e = operation(argExpr, BinaryExpression.isDecimal64(args[0])).cast(value.type.getJavaClass());
            JStatement s = out.write(e);
            s = wrapWithNullCheck(s, value, out);
            addTo.add(s);
        }
    }

    private static JExpr operation(JExpr argExpr, boolean isDecimal) {
        if (isDecimal) {
            return CTXT.staticCall(Decimal64Utils.class, "negate", argExpr);
        } else {
            return argExpr.negate();
        }
    }

    private void generateForArray(QArrayValue value, QArrayValue out, JCompoundStatement addTo) {
        DataType elementType = ((ArrayDataType) args[0].type).getElementDataType();
        QType<?> elementQType = value.getElementType();
        boolean isDecimal = DataTypeHelper.isDecimal64(elementType);
        JExpr resultList = out.read();
        JExpr size = value.size();
        JCompoundStatement statement = CTXT.compStmt();
        JLocalVariable i = statement.addVar(0, int.class, "i");
        JLocalVariable n = statement.addVar(Modifier.FINAL, int.class, "n", size);
        statement.add(out.setInstance());
        statement.add(resultList.call("setSize", n));
        JCompoundStatement forBody = CTXT.compStmt();
        JExpr nullCheck = elementQType.checkNull(value.getElement(i), true);
        JExpr setValue = resultList.call("set", i, operation(value.getElement(i), isDecimal).cast(elementQType.getJavaClass()));
        JExpr setNull = resultList.call("set", i, elementQType.getNullLiteral());
        forBody.add(CTXT.ifStmt(nullCheck, setNull.asStmt(), setValue.asStmt()));
        forBody.add(setValue);
        statement.add(CTXT.forStmt(i.assignExpr(CTXT.intLiteral(0)), CTXT.binExpr(i, "<", n), i.getAndInc(), forBody));
        statement.add(out.setChanged());
        JExpr globalNullCheck = value.readIsNull(true);
        addTo.add(CTXT.ifStmt(globalNullCheck, out.writeNull(), statement));
    }

    public static void validate(UnaryMinusExpression e, CompiledExpression<?> arg) throws UnexpectedTypeException {
        if (!NumericType.isNumericOrNumericArray(arg.type)) {
            throw new UnexpectedTypeException(e, arg.type, StandardTypes.CLEAN_INTEGER, StandardTypes.CLEAN_FLOAT);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof NegateOperation)) return false;
        return super.equals(obj);
    }
}