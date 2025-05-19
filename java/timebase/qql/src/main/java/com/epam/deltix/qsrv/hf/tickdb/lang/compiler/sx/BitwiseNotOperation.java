/*
 * Copyright 2024 EPAM Systems, Inc
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

import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.StandardTypes;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QValue;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.NumericType;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.UnexpectedTypeException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.BitwiseNotExpression;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QType.wrapWithNullCheck;

public class BitwiseNotOperation extends UnaryExpression {

    public BitwiseNotOperation(CompiledExpression<?> value, DataType result) {
        super(value, result);
    }

    @Override
    public void print(StringBuilder out) {
        out.append("~");
        args[0].print(out);
    }

    @Override
    public void generateOperation(QValue value, QValue out, JCompoundStatement addTo) {
        JExpr argExpr = value.read();
        JExpr e = operation(argExpr).cast(value.type.getJavaClass());
        JStatement s = out.write(e);
        s = wrapWithNullCheck(s, value, out);
        addTo.add(s);
    }

    private static JExpr operation(JExpr argExpr) {
        return argExpr.bitwiseNot();
    }

    public static void validate(BitwiseNotExpression e, CompiledExpression<?> arg) throws UnexpectedTypeException {
        if (!NumericType.isIntegerType(arg.type)) {
            throw new UnexpectedTypeException(e, arg.type, StandardTypes.CLEAN_INTEGER);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BitwiseNotOperation)) return false;
        return super.equals(obj);
    }
}
