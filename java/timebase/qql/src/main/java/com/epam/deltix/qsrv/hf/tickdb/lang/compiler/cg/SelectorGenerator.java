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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.jcg.*;

import java.util.*;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

/**
 *
 */
abstract class SelectorGenerator {

    private static final int FLAGS_TYPE_CHECK_OPTIMIZATION_THRESHOLD = 3;

    public final JClass globalClass;
    public final EvalGenerator evalGenerator;
    protected final JExpr inVar;
    protected final SourceClassMap sourceClassMap;

    private final SubtypeIdStorage subtypeIdStorage;
    private final TypeAdjustmentGenerator typeAdjustmentGenerator;

    private final List<QValue> decodedFields = new ArrayList<>();

    public SelectorGenerator(JClass globalClass, EvalGenerator evalGenerator, SourceClassMap sourceClassMap, JExpr inVar) {
        this.globalClass = globalClass;
        this.evalGenerator = evalGenerator;
        this.sourceClassMap = sourceClassMap;
        this.inVar = inVar;
        this.typeAdjustmentGenerator = new TypeAdjustmentGenerator(evalGenerator);
        this.subtypeIdStorage = new SubtypeIdStorage(sourceClassMap.concreteTypes);
    }

    protected abstract JExpr getTypeIdxExpr();

    protected JStatement writeStatement(QValue target) {
        return target.decode(inVar);
    }

    protected JStatement writeNullStatement(QValue target) {
        return target.writeNull();
    }

    protected JStatement wrap(JStatement selectors) {
        return selectors;
    }

    protected abstract void genInit(JCompoundStatement addTo);

    protected void genNullValues(List<QValue> decoded, JCompoundStatement addTo) {
    }

    protected boolean isSkipFieldDecoding(FieldSelectorInfo fsi) {
        return fsi.cache == null || (fsi.fieldAccessor == null && !fsi.usedAsBase);
    }

    protected void genTypeChecks(Collection<TypeCheckInfo> typeChecks, RecordClassDescriptor type, JCompoundStatement addTo) {
        for (TypeCheckInfo tci : typeChecks) {
            ClassDescriptor testClass = tci.typeCheck.targetType;
            boolean test = testClass instanceof RecordClassDescriptor &&
                ((RecordClassDescriptor) testClass).isAssignableFrom(type);
            addTo.add(tci.cache.write(QBooleanType.getLiteral(test)));
        }
    }

    void genSelectors() {
        declareAndBindCacheVariables();

        decodedFields.clear();
        JCompoundStatement addTo = CTXT.compStmt();
        RecordClassDescriptor[] concreteTypes = sourceClassMap.concreteTypes;
        genInit(addTo);
        if (concreteTypes.length > 1) {
            genConcreteDecoders(concreteTypes, addTo);
        } else {
            genDecoderForOneType(concreteTypes[0], addTo);
        }
        evalGenerator.addTo.add(wrap(addTo));
    }

    private void declareAndBindCacheVariables() {
        //
        //  Declare cache variables and bind to the evaluation environment
        //
        for (TypeCheckInfo tci : sourceClassMap.allTypeChecks()) {
            QBooleanType qtype = (QBooleanType) QType.forDataType(tci.typeCheck.type);
            tci.cache = qtype.declareValue(
                "Result of " + tci.typeCheck,
                evalGenerator.interimStateVarContainer,
                evalGenerator.classRegistry,
                false
            );

            evalGenerator.bind(tci.typeCheck, tci.cache);
        }

        for (ClassSelectorInfo csi : sourceClassMap.allClassInfo()) {
            int numFields = csi.highestUsedIdx + 1;

            for (int ii = 0; ii < numFields; ii++) {
                FieldSelectorInfo fsi = csi.fields[ii];

                if (fsi.cache != null) {
                    continue;
                }

                if (fsi.fieldSelector == null && !fsi.usedAsBase) {
                    continue;
                }

                QValue cache;
                QType fstype = fsi.qtype;

                String comment = "Decoded " + fsi.fieldSelector + " [" + csi.type.getName() + "]";

                if (fsi.fieldSelector == null)  // base field, but not used anywhere else
                    if (fstype.instanceAllocatesMemory()) {
                        cache = fstype.declareValue(comment, evalGenerator.interimStateVarContainer, evalGenerator.classRegistry, false);
                    } else {
                        cache = fstype.declareValue(comment, evalGenerator.localVarContainer, evalGenerator.classRegistry, false);
                    }
                else {
                    cache = fstype.declareValue(comment, evalGenerator.interimStateVarContainer, evalGenerator.classRegistry, true);
                    evalGenerator.bind(fsi.fieldSelector, cache);
                }

                fsi.cache = cache;
            }
        }
    }

    private void genConcreteDecoders(RecordClassDescriptor[] types, JCompoundStatement addTo) {
        Set<RecordClassDescriptor> processedTypes = new HashSet<>();
        for (RecordClassDescriptor type : types) {
            genDecoder(type, processedTypes, addTo);
        }
    }

    private void genDecoder(RecordClassDescriptor type,
                            Set<RecordClassDescriptor> processedTypes,
                            JCompoundStatement addTo) {

        if (type == null || processedTypes.contains(type)) {
            return;
        }

        genDecoder(type.getParent(), processedTypes, addTo);
        processedTypes.add(type);

        JCompoundStatement statement = CTXT.compStmt();
        genDecoderForConcreteType(type, statement);
        if (sourceClassMap.hasConcreteType(type)) {
            genNullValues(decodedFields, statement);
        }
        if (!statement.isEmpty()) {
            JExpr ifCondition = genTypeCheckCondition(type);

            addTo.addComment("Decode type " + type.getName());
            addTo.add(CTXT.ifStmt(ifCondition, statement));
            addTo.addComment("End Decode type " + type.getName());
        }
    }

    private void genDecoderForOneType(RecordClassDescriptor type, JCompoundStatement addTo) {
        genTypeChecks(sourceClassMap.allTypeChecks(), type, addTo);
        ClassSelectorInfo csi = sourceClassMap.getSelectorInfo(type);
        genDecoderForFields(csi.fields, 0, csi.highestUsedIdx + 1, addTo);
    }

    private void genDecoderForConcreteType(RecordClassDescriptor type, JCompoundStatement addTo) {
        genTypeChecks(sourceClassMap.allTypeChecks(), type, addTo);
        ClassSelectorInfo csi = sourceClassMap.getSelectorInfo(type);
        genDecoderForFields(csi.fields, csi.lowestOwnIdx, csi.highestOwnUsedIdx + 1, addTo);
    }

    private void genDecoderForFields(FieldSelectorInfo[] fields, int startIndex, int numFields, JCompoundStatement addTo) {
        QByteSkipContext skipper = new QByteSkipContext(inVar, addTo);
        for (int ii = startIndex; ii < numFields; ii++) {
            FieldSelectorInfo fsi = fields[ii];
            QType type = fsi.qtype;

            addTo.addComment("Decode field " + fsi.field.getName());
            if (isSkipFieldDecoding(fsi)) {
                int n = type.getEncodedFixedSize();
                if (n != QType.SIZE_VARIABLE)
                    skipper.skipBytes(n);
                else {
                    skipper.flush();

                    JStatement ifMore = CTXT.ifStmt(
                        inVar.call("hasAvail"), type.skip(inVar)
                    );

                    addTo.add(ifMore);
                }
            } else {
                QValue targetValue = fsi.cache;
                skipper.flush();
                JCompoundStatement decodeStatement = CTXT.compStmt();
                if (fsi.relativeTo != null) {
                    decodeStatement.add(fsi.cache.decodeRelative(inVar, fsi.relativeTo.cache));
                } else {
                    decodeStatement.add(writeStatement(targetValue));
                }
                typeAdjustmentGenerator.genTypeAdjustment(fsi, targetValue, decodeStatement);

                JStatement ifMore = CTXT.ifStmt(
                    inVar.call("hasAvail"), decodeStatement, writeNullStatement(targetValue)
                );

                addTo.add(ifMore);
                decodedFields.add(targetValue);
            }
        }

        skipper.flush();
    }

    private JExpr genTypeCheckCondition(RecordClassDescriptor type) {
        List<Integer> indices = subtypeIdStorage.subtypeIds(type);
        if (indices.size() < FLAGS_TYPE_CHECK_OPTIMIZATION_THRESHOLD) {
            return genDisjunctiveTypeCheckCondition(indices);
        } else {
            return genFlagsTypeCheckCondition(type, subtypeIdStorage.subtypeIdsFlags(type));
        }
    }

    private JExpr genDisjunctiveTypeCheckCondition(List<Integer> indices) {
        return CTXT.disjunction(
            indices.stream()
                .map(i -> CTXT.binExpr(getTypeIdxExpr(), "==", CTXT.intLiteral(i)))
                .toArray(JExpr[]::new)
        );
    }

    private JExpr genFlagsTypeCheckCondition(RecordClassDescriptor type, boolean[] indicesMap) {
        JArrayInitializer arrayInitializer = CTXT.arrayInitializer(boolean[].class);
        for (int i = 0; i < indicesMap.length; ++i) {
            arrayInitializer.add(CTXT.booleanLiteral(indicesMap[i]));
        }

        JVariable variable = evalGenerator.interimStateVarContainer.addVar(
            "Decode Type check index for " + type.getName(),
            true, boolean[].class, arrayInitializer
        );

        return evalGenerator.interimStateVarContainer.access(variable).index(getTypeIdxExpr());
    }
}
