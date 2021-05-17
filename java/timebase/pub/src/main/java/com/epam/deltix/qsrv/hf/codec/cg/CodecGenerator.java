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
package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.qsrv.hf.codec.ClassCodecFactory;
import com.epam.deltix.qsrv.hf.codec.CompilationUnit;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.qsrv.hf.pub.codec.intp.CompoundDecoderImpl;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.timebase.messages.SchemaElement;
import com.epam.deltix.util.jcg.*;
import com.epam.deltix.util.jcg.scg.JavaSrcGenContext;
import com.epam.deltix.util.jcg.scg.SourceCodePrinter;
import com.epam.deltix.util.lang.JavaCompilerHelper.SpecialClassLoader;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import static java.lang.reflect.Modifier.*;

/**
 * Generates a semantic tree based on RecordLayout
 */
public class CodecGenerator {

    public static final JContext CTXT = new JavaSrcGenContext();

    public static final String MANAGER_NAME = "manager";

    private final CGContext context;

//    public CGContext getContext () {
//        return context;
//    }

    // index of the last encoded/decoded field
    private int maxFieldIdx;
    // index of the last encoded/decoded base field
    private int maxBaseFieldIdx;

    private JClass jclass;
    private JConstructor constructor;
    private JInitMemberVariable layoutMember;
    private JInitMemberVariable manager;
    private JInitMemberVariable isExternal;
    private JMethod resetMethod;
    private QVariableContainerLookup lookupContainer;

    public CodecGenerator (TypeLoader typeLoader, SpecialClassLoader classLoader) {
        context = new CGContext(typeLoader, classLoader);
    }

    public CodecGenerator (CGContext ctx) {
        context = new CGContext(ctx.typeLoader, ctx.classLoader);

        // share dependencies
        context.cache = ctx.cache;
        context.dependencies = ctx.dependencies;
    }

    public JClass generateDecoder (String className, boolean external, RecordLayout layout) {
        generateClass(className, null, external ? FixedExternalDecoder.class : BoundDecoder.class, CharSequenceDecoder.class);

        manager = (JInitMemberVariable) lookupContainer.addVar(ObjectManager.class, MANAGER_NAME);

        lookupContainer.isEncoding = false;
        init(layout);
        context.addCodec(new CompilationUnit(jclass, null), layout.getDescriptor(),
                external ? ClassCodecFactory.Type.BOUND_EXTERNAL_DECODER : ClassCodecFactory.Type.BOUND_DECODER);

        if (external) {
            JConstructor c = jclass.addConstructor(PUBLIC);
            JMethodArgument layoutArg = c.addArg(FINAL, RecordLayout.class, "layout");
            c.call(layoutArg, CTXT.newExpr(ObjectManager.class));

            isExternal = (JInitMemberVariable) lookupContainer.container.addVar(PUBLIC, boolean.class, "isExternal", CTXT.falseLiteral());
            c.body().add(isExternal.access().assign(CTXT.trueLiteral()));
        }
        if (! external) {
            constructor.body().add(manager.access().assignExpr(CTXT.newExpr(ObjectManager.class)));
        } else {
            final JMethodArgument arg = constructor.addArg(FINAL, ObjectManager.class, MANAGER_NAME);
            constructor.body().add(manager.access().assign(arg));
        }

        implementCharSequenceDecoder(jclass  /*, decoder  */);
        this.resetMethod = addResetMethod(jclass, external);

        final JMethod setStaticFields = generateSetStaticFieldsMethod(jclass, layout);
        final JMethod jmethod = generateExternalDecodeMethod(jclass, layout);
        if (! external) {
            JInitMemberVariable message = generateDecodeMethod(jclass, jmethod, layout);
            constructor.body().add(setStaticFields.callThis(message.access()));
        }

        return jclass;
    }

    private JMethod implementCharSequenceDecoder (JClass jclass) {
        final JMethod method = jclass.addMethod(PUBLIC, CharSequence.class, "readCharSequence");
        JMethodArgument in = method.addArg(FINAL, MemoryDataInput.class, "input");

        method.body().add(manager.access().call("readCharSequence", in).returnStmt());

        return method;
    }

    private JMethod addResetMethod (JClass jclass, boolean external) {
        JMethod method = jclass.addMethod(PUBLIC, void.class, "reset");
        if (! external)
            method.body().add(manager.access().call("clean"));
        else {
            method.body().add(CTXT.ifStmt(
                    isExternal.access(),
                    manager.access().call("clean").asStmt()
            ));
        }
        return method;
    }

    private JInitMemberVariable generateDecodeMethod (JClass jclass, JMethod externalDecodeMethod, RecordLayout layout) {
        final Class<?> targetClass = layout.getTargetClass();
        final JInitMemberVariable message = jclass.addVar(PRIVATE | FINAL, layout.getTargetClass(), "message", CTXT.newExpr(targetClass));

        final JMethod decodeMethod = jclass.addMethod(PUBLIC, Object.class, "decode");
        final JMethodArgument in0 = decodeMethod.addArg(FINAL, MemoryDataInput.class, "in0");

        decodeMethod.body().add(externalDecodeMethod.callThis(in0, message.access()));
        decodeMethod.body().add(message.access().returnStmt());

        return message;
    }

    private JMethod generateExternalDecodeMethod (JClass jclass, RecordLayout layout) {
        final JMethod decodeMethod = jclass.addMethod(PUBLIC, void.class, "decode");
        final JMethodArgument in0 = decodeMethod.addArg(FINAL, MemoryDataInput.class, "in0");
        final JMethodArgument message = decodeMethod.addArg(FINAL, Object.class, "message");
        // @SuppressWarnings("unchecked")
        if (hasArrayOfObjects(layout.getDescriptor()))
            decodeMethod.addAnnotation(CTXT.annotation(SuppressWarnings.class, CTXT.stringLiteral("unchecked")));

        final JCompoundStatement stmt = decodeMethod.body();

        final JExpr msg = addCastedMessageVariable(stmt, layout.getTargetClass(), message);
        final JLocalVariable isTruncated = stmt.addVar(0, boolean.class, "isTruncated", CTXT.booleanLiteral(false));

        final QByteSkipContext skipper = new QByteSkipContext(in0, stmt);
        final QBFloatType[] baseVars = (maxBaseFieldIdx != - 1) ? new QBFloatType[maxBaseFieldIdx + 1] : null;
        final JLocalVariable[] baseNanVars = (maxBaseFieldIdx != - 1) ? new JLocalVariable[maxBaseFieldIdx + 1] : null;

        final NonStaticFieldLayout[] fields = layout.getNonStaticFields();
        for (int i = 0; i <= maxFieldIdx; i++) {
            final NonStaticFieldLayout f = fields[i];
            final QType type = getType(f.getType());

            if (! f.isBound()) {
                int n = type.getEncodedFixedSize();

                if (n != QType.SIZE_VARIABLE)
                    skipper.skipBytes(n);
                else {
                    final JExpr notTruncatedExpr = getNotTruncatedExpr(isTruncated, in0);
                    final JStatement skipStmt = skipper.flush2();
                    if (skipStmt != null)
                        stmt.add(CTXT.ifStmt(notTruncatedExpr, skipStmt));

                    JCompoundStatement compStmt = CTXT.compStmt();
                    type.skip(in0, compStmt);
                    stmt.add(CTXT.ifStmt(notTruncatedExpr, compStmt));
                }
            } else {
                final JStatement skipStmt = skipper.flush2();
                if (skipStmt != null) {
                    final JExpr notTruncatedExpr = getNotTruncatedExpr(isTruncated, in0);
                    stmt.add(CTXT.ifStmt(notTruncatedExpr, skipStmt));
                }

                JExpr truncatedExpr =
                        CTXT.binExpr(isTruncated, "||",
                                isTruncated.assignExpr(
                                        CTXT.binExpr(in0.call("getAvail"), "<=", CTXT.intLiteral(0))));

                final QBoundType cache = getFieldValue(msg, f, type);

                if (f.getOwnBaseIndex() != - 1)
                    baseVars[f.getOrdinal()] = (QBFloatType) cache;

                final JCompoundStatement elseStmt = CTXT.compStmt();
                if (f.relativeTo != null && cache instanceof QBFloatType) {
                    final int baseIdx = f.relativeTo.getOrdinal();

                    ((QBFloatType) cache).decodeRelative(in0, baseVars[baseIdx], baseNanVars[baseIdx], elseStmt);
                } else
                    cache.decode(in0, elseStmt);

                // #12059: make exception for boolean
                final boolean makeException = (type instanceof QBooleanType) && ! (type.isNullable()) && f.hasAccessMethods() && f.getSetterType() == boolean.class && ! f.hasSmartProperties();
                // java boolean and .NET enum have no NULL constant
                final boolean hasNullLiteral = cache.hasNullLiteral();


                JStatement nullStmt;
                if (cache.accessor instanceof QAccessMethod)
                    nullStmt = ! makeException ?
                            cache.writeNullify() :     //support only in QAccessMethod
                            QCGHelpers.throwISX("cannot write null to " + cache.getJavaBaseType() + " field");
                else
                    nullStmt = ! ((type instanceof QBooleanType) && f.getFieldType() == boolean.class) ?
                            cache.writeNullNoCheck() :
                            QCGHelpers.throwISX("cannot write null to " + cache.getJavaBaseType() + " field");

                stmt.add(
                        CTXT.ifStmt(truncatedExpr,
                                nullStmt,
                                elseStmt)
                );


                // java boolean and .NET enum have no NULL constant
                if (! type.isNullable() && hasNullLiteral &&
                        // #12059: make exception for boolean
                        ! makeException)
                    stmt.add(
                            CTXT.assertStmt(cache.readIsNull(false), CTXT.stringLiteral(String.format("'%s' field is not nullable", f.getFieldName()))
                            ));

                if (f.getOwnBaseIndex() != - 1)
                    baseNanVars[f.getOrdinal()] = stmt.addVar(FINAL, boolean.class, "nan_" + f.getFieldName(), cache.readIsNull(true));
            }
        }

        if (resetMethod != null) {
            List<JMemberVariable> vars = jclass.getVars();

            for (JMemberVariable var : vars) {
                if (CompoundDecoderImpl.class.getName().equals(var.type())) {
                    String name = QVariableContainerLookup.getAccessorName(var.name());
                    resetMethod.body().add(lookupContainer.lookupAccessor(name).call("reset").asStmt());
                    //resetMethod.body().add(jclass.thisVar().access().field(var.name()).call("reset").asStmt());
                }
            }
            stmt.addFront(resetMethod.callThis().asStmt());
        }

        return decodeMethod;
    }

    private static JExpr getNotTruncatedExpr (JExpr isTruncated, JExpr in0) {
        return
                CTXT.binExpr(isTruncated.not(), "&&",
                        isTruncated.assignExpr(
                                CTXT.binExpr(in0.call("getAvail"), "<=", CTXT.intLiteral(0))).not());

    }

    private JMethod generateSetStaticFieldsMethod (JClass jclass, RecordLayout layout) {
        final JMethod method = jclass.addMethod(PUBLIC, void.class, "setStaticFields");
        final JMethodArgument message = method.addArg(FINAL, Object.class, "message");

        final StaticFieldLayout[] staticFields = layout.getStaticFields();
        if (staticFields == null)
            return method;

        final JCompoundStatement stmt = method.body();

        final JExpr msg = addCastedMessageVariable(stmt, layout.getTargetClass(), message);
        for (StaticFieldLayout f : staticFields) {
            if (f.isBound()) {
                final QType type = getType(f);
                final QBoundType fieldVar = getFieldValue(msg, f, type);

                //final JExpr staticValue;
                final boolean hasStaticValue = f.getField().getStaticValue() != null;
                if (hasStaticValue) {
                    stmt.add(fieldVar.writeObject(f.getValue()));

                    // TODO: any optimization here?
                    if (fieldVar.qType.hasConstraint())
                        stmt.add(
                                CTXT.ifStmt(
                                        fieldVar.readIsConstraintViolated(),
                                        QCGHelpers.throwIAX(String.format("Static value is out of range. Field %s value %s", f.getFieldName(), f.getValue()))
                                ));
                } else {
                    if (! f.getField().getType().isNullable())
                        throw new IllegalStateException("field is not nullable " + f.getName());

                    if (! fieldVar.hasNullLiteral())
                        throw new IllegalStateException("cannot assign null to " + f.getFieldType().getName() + " " + f.getFieldName());

                    stmt.add(fieldVar.writeNull());
                }
            }
        }

        return method;
    }

    public JClass generateEncoder (String className, RecordLayout layout) {
        generateClass(className, null, FixedBoundEncoder.class);
        lookupContainer.isEncoding = true;
        init(layout);

        final JMethod method = jclass.addMethod(PUBLIC, Class.class, "getTargetClass");
        method.body().add(CTXT.classLiteral(layout.getTargetClass()).returnStmt());

        context.addCodec(new CompilationUnit(jclass, null), layout.getDescriptor(), ClassCodecFactory.Type.BOUND_ENCODER);

        generateEncodeMethod(jclass, layout);

        return jclass;
    }

    // getLastNonNull - supports tail truncation before null-values fields
    private JMethod generateLastNonNullMethod (RecordLayout layout) {
        final JMethod method = jclass.addMethod(PRIVATE | FINAL, short.class, "getLastNonNull");
        final Class<?> targetClass = layout.getTargetClass();
        final JMethodArgument message = method.addArg(FINAL, targetClass, "msg");

        final JCompoundStatement body = method.body();
        final NonStaticFieldLayout[] fields = layout.getNonStaticFields();
        for (int i = maxFieldIdx; i >= 0; i--) {
            final NonStaticFieldLayout f = fields[i];
            final QType type = getType(f.getType());

            if (f.isBound()) {
                final QBoundType field = getFieldValue(message, f, type);
                if (type.isNullable() && field.hasNullLiteral()) {
                    body.add(
                            CTXT.ifStmt(
                                    field.readIsNull(false),
                                    CTXT.intLiteral(i).returnStmt()
                            ));
                } else {
                    body.add(
                            CTXT.intLiteral(i).returnStmt()
                    );
                    return method;
                }
            }
        }

        body.add(
                CTXT.intLiteral(- 1).returnStmt()
        );

        return method;
    }

    private void generateEncodeMethod (JClass jclass, RecordLayout layout) {
        final JMethod method = jclass.addMethod(PUBLIC, void.class, "encode");
        final JMethodArgument message = method.addArg(FINAL, Object.class, "message");
        final JMethodArgument out0 = method.addArg(FINAL, MemoryDataOutput.class, "out0");
        if (maxFieldIdx == - 1)
            return;

        final JCompoundStatement stmt = method.body();

        final JExpr msg = addCastedMessageVariable(stmt, layout.getTargetClass(), message);

        final QBFloatType[] baseVars = (maxBaseFieldIdx != - 1) ? new QBFloatType[maxBaseFieldIdx + 1] : null;
        final JLocalVariable[] baseNanVars = (maxBaseFieldIdx != - 1) ? new JLocalVariable[maxBaseFieldIdx + 1] : null;
        final JLocalVariable tailIndexVar = stmt.addVar(FINAL, short.class, "tailIndex",
                generateLastNonNullMethod(layout).callThis(msg));

        final NonStaticFieldLayout[] fields = layout.getNonStaticFields();
        for (int i = 0; i <= maxFieldIdx; i++) {
            final NonStaticFieldLayout f = fields[i];
            final QType type = getType(f.getType());

            if (! f.isBound()) {
                type.encodeNull(out0, stmt);
            } else {

                final QBoundType cache = getFieldValue(msg, f, type);

                if (f.getOwnBaseIndex() != - 1)
                    baseVars[f.getOrdinal()] = (QBFloatType) cache;


                // java boolean and .NET enum have no NULL constant
                if (! type.isNullable() && cache.hasNullLiteral())
                    stmt.add(
                            CTXT.ifStmt(
                                    cache.readIsNull(true),
                                    QCGHelpers.throwIAX(String.format("'%s' field is not nullable", f.getFieldName()))
                            ));


                // truncate a tail with all null-values
                stmt.add(
                        CTXT.ifStmt(
                                CTXT.binExpr(tailIndexVar, "<", CTXT.intLiteral(i)),
                                CTXT.returnStmt()
                        )
                );

                if (cache.hasConstraint()) {
                    JExpr value = cache.accessor.read();

                    stmt.add(
                            CTXT.ifStmt(
                                    cache.readIsConstraintViolated(),
                                    QCGHelpers.throwIAX(CTXT.sum(CTXT.stringLiteral(f.getDescription() + " == "),
                                            CTXT.staticCall(String.class, "valueOf", value)))
                            ));
                }

                if (f.relativeTo != null && cache instanceof QBFloatType) {
                    int baseIdx = f.relativeTo.getOrdinal();

                    ((QBFloatType) cache).encodeRelative(baseVars[baseIdx], baseNanVars[baseIdx], out0, stmt);
                } else
                    cache.encode(out0, stmt);

                if (f.getOwnBaseIndex() != - 1)
                    baseNanVars[f.getOrdinal()] = stmt.addVar(FINAL, boolean.class, "nan_" + f.getFieldName(), cache.readIsNull(true));
            }
        }
    }

    private void generateClass (String className, Class<?> parent, Class<?>... interfaces) {
        jclass = CTXT.newClass(PUBLIC | FINAL, null, className, parent);

        for (int i = 0; interfaces != null && i < interfaces.length; i++)
            jclass.addImplementedInterface(interfaces[i]);

        lookupContainer = new QVariableContainerLookup(PRIVATE | FINAL, jclass);
        context.lookupContainer = lookupContainer;
        layoutMember = (JInitMemberVariable) lookupContainer.addVar(RecordLayout.class, "layout");

        constructor = jclass.addConstructor(PUBLIC);
        final JMethodArgument arg = constructor.addArg(FINAL, RecordLayout.class, "layout");

        // layout staff
        final JMethod method = jclass.addMethod(PUBLIC, RecordClassInfo.class, "getClassInfo");
        method.body().add(layoutMember.access().returnStmt());
        constructor.body().add(layoutMember.access().assign(arg));
    }

    private void init (RecordLayout layout) {
        // init indexes for non-static fields
        maxFieldIdx = maxBaseFieldIdx = - 1;

        final NonStaticFieldLayout[] fields = layout.getNonStaticFields();
        if (fields != null) {
            for (int i = 0; i < fields.length; i++) {
                final NonStaticFieldLayout f = fields[i];
                if (f.isBound()) {
                    maxFieldIdx = i;
                    if (f.getOwnBaseIndex() != - 1)
                        maxBaseFieldIdx = i;
                }
            }
        }
    }


    private JExpr addCastedMessageVariable (JCompoundStatement body, Class<?> clazz, JMethodArgument uncastedMessage) {
        return body.addVar(FINAL, clazz, "msg", uncastedMessage.cast(clazz));
    }

    private QType getType (DataType dt) {
        return QType.forDataType(dt);
    }

    private QType getType (StaticFieldLayout f) {
        final DataType dt = f.getType();

        // #11837: for a static field doesn't take into account encoding from DataType, but a type of the bound field
        if (dt instanceof IntegerDataType) {
            final Class<?> type = f.getFieldType();
            if (f.getField().getStaticValue() == null) {
                // if the bound field cannot store null for the specified encoding, choose encoding appropriate for the bound type
                final IntegerDataType idt = (IntegerDataType) dt;
                final int sizeEncoding = idt.getNativeTypeSize();
                final int sizeField = MdUtil.getSize(type);
                if (sizeEncoding > sizeField)
                    return getType(new IntegerDataType(IntegerDataType.getEncoding(sizeField), idt.isNullable(), idt.getMin(), idt.getMax()));
            }
        } else if (dt instanceof FloatDataType) {
            if (f.getField().getStaticValue() == null) {
                final Class<?> type = f.getFieldType();
                final FloatDataType fdt = (FloatDataType) dt;
                return getType(new FloatDataType(
                        (type == float.class) ?
                                FloatDataType.ENCODING_FIXED_FLOAT : FloatDataType.ENCODING_FIXED_DOUBLE,
                        fdt.isNullable(), fdt.getMin(), fdt.getMax()));
            }
        } else if (dt instanceof VarcharDataType) {
            final Class<?> type = f.getFieldType();
            if ((type == long.class) && ((VarcharDataType) dt).getEncodingType() != VarcharDataType.ALPHANUMERIC)
                return new QAlphanumericType(new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(10), dt.isNullable(), ((VarcharDataType) dt).isMultiLine()));
        }

        return getType(dt);
    }

    private QBoundType getFieldValue (JExpr object, FieldLayout f, QType type) {
        return getPrimitiveValue(object, (QPrimitiveType) type, f);
    }


    private QBoundType getPrimitiveValue (JExpr object, QPrimitiveType type, FieldLayout f) {
        final QAccessor accessor;

        if (f.hasAccessMethods()) {
            accessor = new QAccessMethod(object, f, type);
        } else {
            final Field javaField = f.getJavaField();
            SchemaElement schemaElement = javaField.getAnnotation(SchemaElement.class);
            String schemaFieldName = (schemaElement != null && !Util.xequals(schemaElement.name(), "")) ? schemaElement.name() : javaField.getName();
            accessor = new QAVariable(object.field(javaField.getName()), javaField, schemaFieldName);
        }
        return getPrimitiveValue(type, f.getFieldType(), f.getGenericClass(), accessor, context);
    }


    static QBoundType<?> getPrimitiveValue (QPrimitiveType type, Class<?> javaType, Class<?> elementType, QAccessor accessor, CGContext context) {
        if (type instanceof QFloatType)
            return new QBFloatType((QFloatType) type, javaType, accessor);
        else if (type instanceof QIntegerType)
            return new QBIntegerType((QIntegerType) type, javaType, accessor);
        else if (type instanceof QBooleanType)
            return new QBBooleanType((QBooleanType) type, javaType, accessor, context.lookupContainer);
        else if (type instanceof QCharType)
            return new QBoundType<>(type, javaType, accessor);
        else if (type instanceof QDateTimeType)
            return new QBDateTimeType((QDateTimeType) type, javaType, accessor);
        else if (type instanceof QTimeOfDayType)
            return new QBoundType<>(type, javaType, accessor);

        if (type instanceof QStringType)
            return new QBStringType((QStringType) type, javaType, accessor, context.lookupContainer);
        else if (type instanceof QAlphanumericType)
            return new QBAlphanumericType((QAlphanumericType) type, javaType, accessor, context.lookupContainer);
        else if (type instanceof QEnumType)
            return new QBEnumType((QEnumType) type, elementType != null ? elementType : javaType, accessor, context.lookupContainer);
        else if (type instanceof QBinaryType)
            return new QBBinaryType((QBinaryType) type, javaType, accessor, context.lookupContainer);
        else if (type instanceof QArrayType)
            return new QBArrayType((QArrayType) type, javaType, elementType, accessor, context);
        else if (type instanceof QClassType)
            return new QBClassType((QClassType) type, javaType, elementType, accessor, context);
        else
            throw new UnsupportedOperationException("unsupported type " + type.getClass().getSimpleName());
    }


    public static String toString (JClass jclass) {
        final StringBuilder buf = new StringBuilder();
        final SourceCodePrinter p = new SourceCodePrinter(buf);

        try {
            p.print(jclass);
            return buf.toString();
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        }
    }

    public void setDependencies (CompilationUnit[] dependencies) {
        context.addDependencies(dependencies);
    }

    public CompilationUnit[] getDependencies () {
        return context.dependencies.isEmpty() ? null : context.dependencies.toArray(new CompilationUnit[context.dependencies.size()]);
    }

    public JClass getJClass () {
        return jclass;
    }

    public void setJClass (JClass jClass) {
        this.jclass = jClass;
    }

    private static boolean hasArrayOfObjects (RecordClassDescriptor rcd) {
        DataField[] fields = rcd.getFields();
        if (fields != null)
            for (DataField field : fields) {
                if (field.getType() instanceof ArrayDataType && ((ArrayDataType) field.getType()).getElementDataType() instanceof ClassDataType)
                    return true;
            }

        return rcd.getParent() != null && hasArrayOfObjects(rcd.getParent());
    }
}

// TODO:
// 1. Byte with boundaries (non-static)
// 1.2 Non-native encoding boundaries (PINTERVAL, etc.)
// 2. FACTORY ?
// 3. QAlphanumericType.skip !!!


// Wiki: relative encoding where mixed nullable and not nullable bound types is not supported. For example: