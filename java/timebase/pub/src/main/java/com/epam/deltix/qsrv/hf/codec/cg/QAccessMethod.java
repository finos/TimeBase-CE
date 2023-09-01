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
package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.qsrv.hf.pub.codec.FieldLayout;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.codec.StaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.md.BooleanDataType;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JLocalVariable;
import com.epam.deltix.util.jcg.JStatement;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

/**
 * Accessor for new message format with get/set/nullify/has methods
 */
public class QAccessMethod  implements QAccessor{
    // reference to an object instance
    private final JExpr reference;

    private final String fieldName;
    private final String fieldSchemaName;
    private final String fieldDescription;
    private final QPrimitiveType type;

    private final String getterName;
    private final String setterName;
    private final String nullifierName;
    private final String haserName;

    public final boolean hasSmartProperties;

    private final FieldLayout layout;

    public QAccessMethod(JExpr reference, FieldLayout layout, QPrimitiveType type ) {
        this.reference = reference;
        this.type      = type;
        this.layout    = layout;

        this.fieldDescription = layout.getDescription();

        this.getterName =      layout.hasAccessMethods() ? layout.getGetter().getName() : null;
        this.setterName =      layout.hasAccessMethods() ? layout.getSetter().getName() : null;

        // name in rcd can be incorrect for compiled codecs. If getter exists - use it name
        this.fieldName = layout.hasAccessMethods() ? extractName(layout.getGetter().getName()) : layout.getName();
        this.fieldSchemaName = layout.getName();

        this.nullifierName =  layout.hasSmartProperties() ? layout.getNullifier().getName() : null;
        this.haserName =      layout.hasSmartProperties() ? layout.getHaser().getName() : null;

        this.hasSmartProperties = layout.hasSmartProperties();
    }

    private String extractName(String getterName) {
        String getterNameLowerCase = getterName.toLowerCase();
        if (getterNameLowerCase.startsWith("get"))
            return toJavaNotation(getterName.substring(3));
        else if (getterNameLowerCase.startsWith("is"))
            return toJavaNotation(getterName.substring(2));
        else return getterName;
    }

    private String toJavaNotation(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    JExpr readAtomicNoCast() {
        return reference.call(getterName);
    }

    @Override
    public JExpr read() {
        if (type.dt instanceof BooleanDataType) {
            if (type.dt.isNullable()) { // return param for out.writeByte();
                if (layout.getGetterReturnType() ==  boolean.class && layout.hasSmartProperties())
                    return CTXT.condExpr(haser(), castBooleanToByte(readAtomicNoCast()), CTXT.intLiteral(-1).cast(byte.class));
                else if (layout.getGetterReturnType() ==  byte.class)
                    return readAtomicNoCast();
                else throw new UnsupportedOperationException("Can`t read NULLABLE BOOLEAN using getter: \"" + getterName + "()\" that return boolean.class without HASER method.");
            } else { //not nullable case. return param for out.writeBoolean();
                return (layout.getGetterReturnType() ==  boolean.class) ?
                        readAtomicNoCast() : castByteToBoolean(readAtomicNoCast());
            }
        }

        return  readAtomicNoCast();
    }

    private JExpr castByteToBoolean(JExpr arg) {
        return CTXT.binExpr(arg,
                "==",
                CTXT.intLiteral(1).cast(byte.class)
        );
    }

    private JExpr castBooleanToByte(JExpr arg) {
        return CTXT.condExpr(arg,
                        CTXT.intLiteral(1).cast(byte.class),
                        CTXT.intLiteral(0).cast(byte.class)
                );
    }

    private JExpr haser() {
        return reference.call(haserName);
    }

    public JExpr haser(boolean eq) {
        return haserName != null ?
                (eq ? reference.call(haserName).not() : reference.call(haserName)) :
                CTXT.binExpr(reference.call(getterName), eq ? "==" : "!=", type.getNullLiteral());
    }

    @Override
    public JStatement writeNullify(JExpr nullExpr) {
        return nullifierName != null ?
                reference.call(nullifierName).asStmt() :
                reference.call(setterName, nullExpr).asStmt();
    }

    @Override
    public String getSchemaFieldName () {
        return fieldSchemaName;
    }

    @Override
    public Class getFieldType () {
        // using for cast in set static value
        return layout.getSetterType();
    }

    @Override
    public JStatement write(JExpr arg) {
        if (type.dt instanceof BooleanDataType && layout instanceof NonStaticFieldLayout) {
            if (!type.dt.isNullable()) {  // arg = in.readBoolean ()
                return layout.getSetterType() == boolean.class ?
                        reference.call(setterName, arg).asStmt() :
                        reference.call(setterName, castBooleanToByte(arg)).asStmt();

            } else { // arg = in.readByte ()
                if (layout.getSetterType() == byte.class) {
                    return reference.call(setterName, arg).asStmt();
                } else if (layout.getSetterType() == boolean.class && layout.hasSmartProperties()) {
                    JCompoundStatement statement = CTXT.compStmt();
                    JLocalVariable variable = statement.addVar(0, byte.class, "value", arg);
                    statement.add(CTXT.ifStmt(
                            CTXT.binExpr(variable, "==", CTXT.intLiteral(BooleanDataType.NULL)),
                            writeNullify(null),
                            reference.call(setterName, castByteToBoolean(variable)).asStmt()
                    ));
                    return statement;
                } else
                    throw new UnsupportedOperationException("Can`t write NULLABLE BOOLEAN using setter: \"" + setterName + "()\" that parameter type = boolean.class without NULLIFIER method.");
            }
        } else if (type.dt instanceof BooleanDataType &&
                layout instanceof StaticFieldLayout &&
                layout.hasSmartProperties() &&
                layout.getSetterType() == boolean.class &&
                (((StaticFieldLayout) layout).getField()).getStaticValue() == null) {
            return writeNullify(null);
        }

        return reference.call(setterName, arg).asStmt();
    }


    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String getFieldDescription() {
        return fieldDescription;
    }
}