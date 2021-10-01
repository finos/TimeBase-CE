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
import com.epam.deltix.qsrv.hf.codec.ClassCodecFactory.Type;
import com.epam.deltix.qsrv.hf.codec.CodecUtils;
import com.epam.deltix.qsrv.hf.codec.CompilationUnit;
import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.qsrv.hf.pub.codec.FixedExternalDecoder;
import com.epam.deltix.qsrv.hf.pub.codec.RecordLayout;
import com.epam.deltix.qsrv.hf.pub.codec.intp.CompoundDecoderImpl;
import com.epam.deltix.qsrv.hf.pub.codec.intp.PolyBoundEncoderImpl;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.jcg.*;

import java.lang.reflect.Modifier;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;
import static java.lang.reflect.Modifier.FINAL;
import static java.lang.reflect.Modifier.PRIVATE;

/**
 *
 */
public class QBClassType extends QBoundType<QClassType> {
    private final boolean isArray;
    private final boolean isFixedType;

    private JExpr encoder = null;
    private JExpr decoder = null;

    private Class<?> elementType;

    public QBClassType(QClassType qType, Class<?> javaType, Class<?> elementType, QAccessor accessor, CGContext context) {
        super(qType, javaType, accessor);

        isArray = accessor instanceof QAArrayList;
        isFixedType = qType.dt.isFixed();

        initHelperMembers(context);

        this.elementType = elementType;
    }

    private void initHelperMembers(CGContext context) {
        //context.clearDependencies();

        QVariableContainerLookup container = context.lookupContainer;

        if (container.isEncoding) {
            final String fieldName = "encoder_" + accessor.getFieldName();
            final String accessorName = QVariableContainerLookup.getAccessorName(fieldName);

            if (qType.dt.isFixed()) {
                // no need for a separate encoder instance per each field

                JVariable var = container.lookupVar(fieldName);
                if (var == null) {
                    final RecordLayout layout = new RecordLayout(context.typeLoader, qType.dt.getFixedDescriptor());
                    CompilationUnit encoderClass = createFixedBoundEncoder(layout, context);
                    var = container.addVar(PRIVATE, encoderClass.getJClass(), fieldName);
                    encoder = getEncoder(accessorName, var, encoderClass.getJClass(), container);
                } else {
                    encoder = container.lookupAccessor(accessorName);
                }
            }
            else {

                JVariable var = container.lookupVar(fieldName);
                if (var == null) {
                    var = container.addVar(PRIVATE, PolyBoundEncoderImpl.class, fieldName);

                    final RecordClassDescriptor[] rcds = qType.dt.getDescriptors();
                    final CompilationUnit[] encoders = new CompilationUnit[rcds.length];
                    for (int i = 0; i < rcds.length; i++) {
                        final RecordClassDescriptor rcd = rcds[i];
                        final RecordLayout layout = new RecordLayout(context.typeLoader, rcd);
                        encoders[i] = createFixedBoundEncoder(layout, context);
                    }

                    encoder = getPolyEncoder(accessorName, var, encoders, container);
                } else {
                    encoder = container.lookupAccessor(accessorName);
                }
            }
        } else {
            final JExpr layout = container.access(container.lookupVar("layout"));

            final String fieldName = "decoder_" + accessor.getFieldName();
            final String accessorName = QVariableContainerLookup.getAccessorName(fieldName);

            JVariable var = container.lookupVar(fieldName);

            if (var == null) {
                var = container.addVar(PRIVATE, CompoundDecoderImpl.class, fieldName);

                final RecordClassDescriptor[] rcds = qType.dt.getDescriptors();
                JArrayInitializer decodersArray = CTXT.arrayInitializer(FixedExternalDecoder.class);

                final CompilationUnit[] decoders = new CompilationUnit[rcds.length];
                final RecordLayout[] layouts = new RecordLayout[rcds.length];

                for (int i = 0; i < rcds.length; i++) {
                    layouts[i] = new RecordLayout(context.typeLoader, rcds[i]);
                    decoders[i] = createFixedBoundDecoder(layouts[i], context, true);

                    decodersArray.add(
                            decoders[i].getJClass().newExpr(
                                CTXT.staticCall(CodecUtils.class, "createFieldLayout",
                                        CTXT.stringLiteral(accessor.getSchemaFieldName()), layout, CTXT.intLiteral(i)),
                                container.access(container.lookupVar(CodecGenerator.MANAGER_NAME))
                            )
                    );
                }

                JStatement initializer = container.access(var).assign(
                        CTXT.newExpr(CompoundDecoderImpl.class,
                                CTXT.newArrayExpr(FixedExternalDecoder.class, decodersArray),
                                CTXT.booleanLiteral(!qType.dt.isFixed()),
                                container.access(container.lookupVar(CodecGenerator.MANAGER_NAME))
                        )
                );

                decoder = container.addAccessor(CompoundDecoderImpl.class, accessorName, (JMemberVariable) var, initializer);
            } else {
                decoder = container.lookupAccessor(accessorName);
            }
        }
    }

    @Override
    public void decode(JExpr input, JCompoundStatement addTo) {
        final JCompoundStatement stmt = CTXT.compStmt();
        addTo.add(stmt);

        // final int size = MessageSizeCodec.read(ctxt.in);
        final JLocalVariable size = stmt.addVar(0, int.class, "size0", CTXT.staticCall(MessageSizeCodec.class, "read", input));

        final JStatement ifBody;
        // assert NOT NULL in Java case
        if (!qType.isNullable()) {
            final JCompoundStatement compStmt = CTXT.compStmt();
            ifBody = compStmt;
            compStmt.add(writeNullNoCheck());
            compStmt.add(CTXT.assertStmt(
                    readIsNull(false),
                    CTXT.stringLiteral(String.format(isArray ? "'%s[]' field array element is not nullable" : "cannot read null. field '%s' is not nullable", accessor.getFieldName()))
            ));
        } else
            ifBody = writeNullNoCheck();

        final JCompoundStatement elseStmt = CTXT.compStmt();
        stmt.add(
                CTXT.ifStmt(CTXT.binExpr(size, " == ", CTXT.intLiteral(0)),
                        ifBody,
                        elseStmt)
        );

        if (isFixedType) {
            // int code = ctxt.in.readUnsignedByte();
            final JLocalVariable code = elseStmt.addVar(Modifier.FINAL, int.class, "code", input.call("readUnsignedByte"));
            elseStmt.add(size.dec());
        }

        final JCompoundStatement trickStmt = CTXT.compStmt();
        elseStmt.add(
                CTXT.ifStmt(CTXT.binExpr(size, " < ", input.call("getAvail")),
                        trickStmt,
                        assignValue(input, null))
        );

        // trick MemoryDataInput to limit available bytes
        final JLocalVariable limit = trickStmt.addVar(FINAL, int.class, "limit", CTXT.staticCall(CodecUtils.class, "limitMDI", size, input));
        assignValue(input, trickStmt);
        // recover backed up length
        // in.setLimit(limit)
        trickStmt.add(input.call("setLimit", limit));
    }

    @Override
    public void encode(JExpr output, JCompoundStatement addTo) {
        final JCompoundStatement elseStmt = CTXT.compStmt();

        if (isArray || qType.isNullable())
            addTo.add(
                    CTXT.ifStmt(readIsNull(true),
                            ((qType.isNullable()) ?
                                    // MessageSizeCodec.write(0, ctxt.out);
                                    CTXT.staticCall(MessageSizeCodec.class, "write", CTXT.intLiteral(0), output).asStmt() :
                                    QCGHelpers.throwIAX(String.format("'%s' field array element is not nullable", accessor.getFieldName()))),
                            elseStmt)
            );
        else
            addTo.add(elseStmt); // when OBJECT is a direct field, the NOT NULLABLE check is inserted by CodecGenerator


        // backup position and skip one byte to later save the field size there
        //final int pos = ctxt.out.getPosition();
        final JLocalVariable pos = elseStmt.addVar(FINAL, int.class, "pos", output.call("getPosition"));
        // ctxt.out.skip(1);
        elseStmt.add(output.call("skip", CTXT.intLiteral(1)));
        if (isFixedType)
            elseStmt.add(output.call("writeUnsignedByte", CTXT.intLiteral(0)));

        // else f_5.encode (msg.oPub1, out0);
        elseStmt.add(encoder.call("encode", accessor.read(), output));

        // CodecUtils.storeFieldSize(pos, out0);
        elseStmt.add(CTXT.staticCall(CodecUtils.class, "storeFieldSize", pos, output));
    }

    private JExpr getEncoder(String accessorName, JVariable var, JClass jClass, QVariableContainerLookup lookupContainer) {
        JExpr layout = lookupContainer.access(lookupContainer.lookupVar("layout"));

        JStatement init = lookupContainer.access(var).assign(jClass.newExpr(
                CTXT.staticCall(CodecUtils.class, "createFieldLayout", CTXT.stringLiteral(accessor.getSchemaFieldName()), layout)
        ));

        return lookupContainer.addAccessor(jClass, accessorName, (JMemberVariable) var, init);
    }

    private JExpr getPolyEncoder(String accessorName, JVariable var, CompilationUnit[] encoders, QVariableContainerLookup lookupContainer) {
        JArrayInitializer init = CTXT.arrayInitializer(Class.class);
        for (CompilationUnit cl : encoders)
            init.add(CTXT.classLiteral(cl.getJClass()));

        final JVariable varClasses = lookupContainer.addVar(Class[].class, "classes_" + accessor.getFieldName(), init);

        final JExpr layout = lookupContainer.access(lookupContainer.lookupVar("layout"));
        JStatement createEncoder = lookupContainer.access(var).assign(
                CTXT.staticCall(CodecUtils.class, "createPolyEncoder", lookupContainer.access(varClasses),
                        CTXT.stringLiteral(accessor.getSchemaFieldName()), layout));

        return lookupContainer.addAccessor(PolyBoundEncoderImpl.class, accessorName, (JMemberVariable) var, createEncoder);
    }

    private JStatement assignValue(JExpr input, JCompoundStatement stmt) {
        if (stmt == null)
            stmt = CTXT.compStmt();

        if (elementType != null ) {
            stmt.add(accessor.write(decoder.call("decode", input).cast(elementType)));
        } else
            stmt.add(accessor.write(decoder.call("decode", input).cast(getJavaClass())));

        return stmt;
    }

    private CompilationUnit createFixedBoundEncoder(RecordLayout layout, CGContext context) {
        CompilationUnit cu = context.lookup(layout.getDescriptor(), Type.BOUND_ENCODER);
        if (cu != null)
            return cu;

        final String className = ClassCodecFactory.getCodecName(layout.getDescriptor(), Type.BOUND_ENCODER);
        final CodecGenerator gen = new CodecGenerator(context);
        cu = new CompilationUnit(gen.generateEncoder(className, layout), new Class<?>[]{layout.getTargetClass()}, null);
        context.addCodec(cu, layout.getDescriptor(), Type.BOUND_ENCODER);
        return cu;
    }

    private CompilationUnit createFixedBoundDecoder(RecordLayout layout, CGContext context, boolean external) {
        CompilationUnit cu = context.lookup(layout.getDescriptor(), external ? Type.BOUND_EXTERNAL_DECODER : Type.BOUND_DECODER);
        if (cu != null)
            return cu;

        final CodecGenerator gen = new CodecGenerator(context);
        final String className = ClassCodecFactory.getCodecName(layout.getDescriptor(), external ? Type.BOUND_EXTERNAL_DECODER : Type.BOUND_DECODER);
        cu = new CompilationUnit(gen.generateDecoder(className, external, layout), new Class<?>[]{layout.getTargetClass()}, null);
        context.addCodec(cu, layout.getDescriptor(), external ? Type.BOUND_EXTERNAL_DECODER : Type.BOUND_DECODER);
        return cu;
    }

//    private CompilationUnit createFixedBoundEncoder(RecordLayout layout, CGContext context) {
//        CompilationUnit cu = context.lookup(layout.getDescriptor(), Type.BOUND_ENCODER);
//        if (cu != null)
//            return cu;
//
//        CodecCacheKey cacheKey = new CodecCacheKey(layout.getTargetClass().getName(), layout.getDescriptor().getGuid(), Type.BOUND_ENCODER.toString());
//
//        CodecCacheValue codec = context.codecCache.getCodec(cacheKey);
//        final CodecGenerator gen = new CodecGenerator(layout.getLoader(), null, context.codecCache);
//        if (codec == null) {
//            final String className = ClassCodecFactory.getCodecName(layout.getDescriptor(), Type.BOUND_ENCODER);
//            codec = new CodecCacheValue(gen.generateEncoder(className, layout), gen.getDependencies());
//            context.codecCache.addCodec(cacheKey, codec);
//        }
//
//        cu = new CompilationUnit(codec.getjClass(), new Class<?>[]{layout.getTargetClass()}, codec.getDependencies());
//        context.addCodec(cu, layout.getDescriptor(), Type.BOUND_ENCODER);
//        return cu;
//    }
//
//    private CompilationUnit createFixedBoundDecoder(RecordLayout layout, CGContext context, boolean external) {
//        CompilationUnit cu = context.lookup(layout.getDescriptor(), external ? Type.BOUND_EXTERNAL_DECODER : Type.BOUND_DECODER);
//        if (cu != null)
//            return cu;
//
//
//        CodecCacheKey cacheKey = new CodecCacheKey(layout.getTargetClass().getName(), layout.getDescriptor().getGuid(), (external ? Type.BOUND_EXTERNAL_DECODER : Type.BOUND_DECODER).toString());
//        CodecCacheValue codec = context.codecCache.getCodec(cacheKey);
//        final CodecGenerator gen = new CodecGenerator(layout.getLoader(), null, context.codecCache);
//        if (codec == null) {
//            final String className = ClassCodecFactory.getCodecName(layout.getDescriptor(), external ? Type.BOUND_EXTERNAL_DECODER : Type.BOUND_DECODER);
//            codec = new CodecCacheValue( gen.generateDecoder(className, external, layout), gen.getDependencies());
//            context.codecCache.addCodec(cacheKey, codec);
//        }
//
//        cu = new CompilationUnit(codec.getjClass(), new Class<?>[]{layout.getTargetClass()}, codec.getDependencies());
//        context.addCodec(cu, layout.getDescriptor(), external ? Type.BOUND_EXTERNAL_DECODER : Type.BOUND_DECODER);
//        return cu;
//    }


}