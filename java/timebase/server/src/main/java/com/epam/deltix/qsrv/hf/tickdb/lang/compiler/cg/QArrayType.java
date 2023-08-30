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

import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.qsrv.hf.pub.codec.NestedObjectCodec;
import com.epam.deltix.qsrv.hf.pub.md.ArrayDataType;
import com.epam.deltix.qsrv.hf.pub.md.BooleanDataType;
import com.epam.deltix.qsrv.hf.pub.md.CharDataType;
import com.epam.deltix.qsrv.hf.pub.md.ClassDataType;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.DateTimeDataType;
import com.epam.deltix.qsrv.hf.pub.md.EnumDataType;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.TimeOfDayDataType;
import com.epam.deltix.qsrv.hf.pub.md.VarcharDataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.containers.AlphanumericInstanceArray;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.containers.ByteInstanceArray;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.containers.CharSequenceInstanceArray;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.containers.CharacterInstanceArray;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.containers.DoubleInstanceArray;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.containers.FloatInstanceArray;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.containers.InstanceArray;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.containers.InstanceArrays;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.containers.IntegerInstanceArray;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.containers.LongInstanceArray;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.containers.ShortInstanceArray;
import com.epam.deltix.util.jcg.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;


public class QArrayType extends QType<ArrayDataType> {

    enum PoolType {
        VARCHAR,
        INSTANCE
    }

    private static class ClassInfo {
        private final Class<?> cls;
        private final RecordClassDescriptor descriptor;

        public ClassInfo(Class<?> cls, RecordClassDescriptor descriptor) {
            this.cls = cls;
            this.descriptor = descriptor;
        }
    }

    public final QType elementType;

    private final Class<?> type;
    private final String getMethod;
    private final boolean objectArray;
    private final PoolType poolType;
    private final boolean arrayOfPrimitives;
    private final ClassInfo[] classes;

    public QArrayType(ArrayDataType dt) {
        super(dt);

        this.type = getClassType(dt.getElementDataType());
        this.getMethod = getMethod(type);
        this.poolType = getPoolType(type);
        this.arrayOfPrimitives = isArrayOfPrimitives(dt.getElementDataType());
        this.elementType = QType.forDataType(dt.getElementDataType());

        this.objectArray = true;
        this.classes = extractClasses(dt);
    }

    public QArrayType(ArrayDataType dt, QType elementType) {
        super(dt);

        this.type = getClassType(elementType.dt);
        this.getMethod = getMethod(type);
        this.poolType = getPoolType(type);
        this.arrayOfPrimitives = isArrayOfPrimitives(elementType.dt);
        this.elementType = elementType instanceof QArrayType ? ((QArrayType) elementType).elementType : elementType;

        this.objectArray = false;
        this.classes = extractClasses(dt);
    }

    private static ClassInfo[] extractClasses(ArrayDataType arrayDataType) {
        return arrayDataType.getElementDataType() instanceof ClassDataType ?
                extractClasses(((ClassDataType) arrayDataType.getElementDataType()).getDescriptors()): null;
    }

    private static ClassInfo[] extractClasses(RecordClassDescriptor[] rcds) {
        List<ClassInfo> classes = new ArrayList<>();
        for (int i = 0; i < rcds.length; i++) {
            RecordClassDescriptor rcd = rcds[i];
            while (rcd != null) {
                try {
                    classes.add(new ClassInfo(
                            Class.forName(rcd.getName()),
                            rcds[i]
                    ));
                    break;
                } catch (ClassNotFoundException exc) {
                    rcd = rcd.getParent();
                }
            }
        }

        return classes.toArray(new ClassInfo[0]);
    }

    private static String getMethod(Class<?> clazz) {
        if (clazz == ByteInstanceArray.class) {
            return "getByte";
        } else if (clazz == ShortInstanceArray.class) {
            return "getShort";
        } else if (clazz == IntegerInstanceArray.class) {
            return "getInteger";
        } else if (clazz == LongInstanceArray.class) {
            return "getLong";
        } else if (clazz == FloatInstanceArray.class) {
            return "getFloat";
        } else if (clazz == DoubleInstanceArray.class) {
            return "getDouble";
        } else if (clazz == CharacterInstanceArray.class) {
            return "getCharacter";
        } else {
            return "get";
        }
    }

    private PoolType getPoolType(Class<?> clazz) {
        if (clazz == CharSequenceInstanceArray.class || clazz == AlphanumericInstanceArray.class) {
            return PoolType.VARCHAR;
        }

        if (clazz == InstanceArray.class || clazz == InstanceArrays.class) {
            return PoolType.INSTANCE;
        }

        return null;
    }

    private boolean isArrayOfPrimitives(DataType type) {
        if (type instanceof ArrayDataType) {
            ArrayDataType arrayDataType = (ArrayDataType) type;
            if (arrayDataType.getElementDataType().isPrimitive()) {
                return true;
            }
        }

        return false;
    }

    private Class<?> getClassType(DataType type) {
        if (type instanceof IntegerDataType) {
            switch (((IntegerDataType) type).getSize()) {
                case 1:
                    return ByteInstanceArray.class;
                case 2:
                    return ShortInstanceArray.class;
                case 4:
                    return IntegerInstanceArray.class;
                case 8:
                    return LongInstanceArray.class;
                default:
                    throw new RuntimeException("Unknown data type: " + type);
            }
        }

        if (type instanceof BooleanDataType) {
            return ByteInstanceArray.class;
        }

        if (type instanceof VarcharDataType) {
            VarcharDataType varcharDataType = (VarcharDataType) type;
            if (varcharDataType.getEncodingType() == VarcharDataType.ALPHANUMERIC) {
                return AlphanumericInstanceArray.class;
            } else {
                return CharSequenceInstanceArray.class;
            }
        }

        if (type instanceof FloatDataType) {
            switch (((FloatDataType) type).getScale()) {
                case FloatDataType.FIXED_FLOAT:
                    return FloatInstanceArray.class;
                case FloatDataType.FIXED_DOUBLE:
                    return DoubleInstanceArray.class;
                case FloatDataType.SCALE_DECIMAL64:
                    return LongInstanceArray.class;
                default:
                    throw new RuntimeException("Unknown data type: " + type);
            }
        }

        if (type instanceof DateTimeDataType) {
            return LongInstanceArray.class;
        }

        if (type instanceof EnumDataType) {
            switch (((EnumDataType) type).descriptor.computeStorageSize()) {
                case 1:
                    return ByteInstanceArray.class;
                case 2:
                    return ShortInstanceArray.class;
                case 4:
                    return IntegerInstanceArray.class;
                case 8:
                    return LongInstanceArray.class;
                default:
                    throw new RuntimeException("Unknown data type: " + type);
            }
        }

        if (type instanceof CharDataType) {
            return CharacterInstanceArray.class;
        }

        if (type instanceof TimeOfDayDataType) {
            return IntegerInstanceArray.class;
        }

        if (type instanceof ClassDataType) {
            return InstanceArray.class;
        }

        if (type instanceof ArrayDataType) {
            ArrayDataType arrayDataType = (ArrayDataType) type;
            if (arrayDataType.getElementDataType().isPrimitive()) {
                return getClassType(arrayDataType.getElementDataType());
            } else {
                return InstanceArrays.class;
            }
        }

        throw new RuntimeException("Unknown element type of array: " + type);
    }

    public QType getElementType() {
        return elementType;
    }

    public boolean isObjectArray() {
        return objectArray;
    }

    @Override
    public boolean instanceAllocatesMemory() {
        return (true);
    }

    @Override
    public int getEncodedFixedSize() {
        return SIZE_VARIABLE;
    }

    @Override
    public JStatement skip(JExpr input) {
        return CTXT.staticCall(NestedObjectCodec.class, "skip", input).asStmt();
    }

    @Override
    public Class<?> getJavaClass() {
        throw new UnsupportedOperationException(
                "Not implemented for " + getClass().getSimpleName()
        );
    }

    @Override
    public QValue declareValue(String comment, QVariableContainer container, QClassRegistry registry, boolean setNull) {
        JExpr init = newExpr(type);
        JVariable v = container.addVar(comment, true, type, init);

        return new QArrayValue(this, container.access(v));
    }

    private JExpr newExpr(Class<?> type) {
        if (poolType == null) {
            return CTXT.newExpr(type);
        }

        switch (poolType) {
            case VARCHAR:
                return CTXT.newExpr(type, CTXT.staticVarRef("this", "varcharPool"));
            case INSTANCE: {
                if (classes == null || classes.length == 0) {
                    return CTXT.newExpr(type, CTXT.staticVarRef("this", "instancePool"));
                } else {
                    List<JExpr> list = new ArrayList<>();
                    list.add(CTXT.staticVarRef("this", "instancePool"));

                    JArrayInitializer rcdArray = CTXT.arrayInitializer(RecordClassDescriptor.class);
                    for (ClassInfo classInfo : classes) {
                        rcdArray.add(CTXT.call("getDescriptor", CTXT.stringLiteral(classInfo.descriptor.getName())));
                    }
                    list.add(CTXT.newArrayExpr(RecordClassDescriptor.class, rcdArray));

                    JArrayInitializer classArray = CTXT.arrayInitializer(Class.class);
                    for (ClassInfo classInfo : classes) {
                        classArray.add(CTXT.classLiteral(classInfo.cls));
                    }
                    list.add(CTXT.newArrayExpr(Class.class, classArray));

                    return CTXT.newExpr(type, list.toArray(new JExpr[list.size()]));
                }
            }
        }

        return CTXT.newExpr(type);
    }

    @Override
    public void moveNoNullCheck(QValue from, QValue to, JCompoundStatement addTo) {
        if (to instanceof QPluginArgValue) {
            addTo.add(to.write(from.read()));
        } else {
            addTo.add(to.read().call("set", from.read()));
        }
    }

    @Override
    public JExpr getNullLiteral() {
        return CTXT.nullLiteral();
    }

    @Override
    protected void encodeNullImpl(JExpr output, JCompoundStatement addTo) {
        addTo.add(CTXT.staticCall(MessageSizeCodec.class,"write", CTXT.intLiteral(0), output));
    }

    public JExpr getElementNullLiteral() {
        return elementType.getNullLiteral();
    }

    @Override
    public JStatement decode(JExpr input, QValue value) {
        QArrayValue arrayValue = (QArrayValue) value;
        return arrayValue.variable().call("decode", input).asStmt();
    }

    public JStatement decodeElement(JExpr input, QValue value) {
        QArrayValue arrayValue = (QArrayValue) value;

        if (arrayOfPrimitives) {
            return arrayValue.variable().call("addAllFromInput", input).asStmt();
        }

        if (poolType != null) {
            return arrayValue.variable().call("addFromInput", input).asStmt();
        }

        return elementType.decode(input, arrayValue);
    }

    @Override
    public void encode(QValue value, JExpr output, JCompoundStatement addTo) {
        QArrayValue arrayValue = (QArrayValue) value;
        addTo.add(arrayValue.variable().call("encode", output));
    }

    public String listGetMethod() {
        return getMethod;
    }

    public Class<?> getType() {
        return type;
    }

    public boolean hasClasses() {
        return classes != null && classes.length > 0;
    }
}