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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.jcg.*;

import java.util.*;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

/**
 *
 */
abstract class SelectorGenerator {
    public final JClass globalClass;
    public final EvalGenerator evalGenerator;
    protected final JExpr inVar;
    protected final SourceClassMap sourceClassMap;

    public SelectorGenerator(JClass globalClass, EvalGenerator evalGenerator, SourceClassMap sourceClassMap, JExpr inVar) {
        this.globalClass = globalClass;
        this.evalGenerator = evalGenerator;
        this.sourceClassMap = sourceClassMap;
        this.inVar = inVar;
    }

    protected abstract JExpr getTypeIdxExpr();

    protected void generateInit(JCompoundStatement addTo) {
        addTo.add(
            inVar.call(
                "setBytes",
                evalGenerator.inMsg.field("data"),
                evalGenerator.inMsg.field("offset"),
                evalGenerator.inMsg.field("length")
            )
        );
    }

    protected void addDefaultSwitch(JSwitchStatement switchStatement) {
    }

    void genSelectors() {
        //
        //  Declare cache variables and bind to the evaluation environment
        //
        for (TypeCheckInfo tci : sourceClassMap.allTypeChecks()) {
            QBooleanType qtype = (QBooleanType) QType.forDataType(tci.typeCheck.type);

            tci.cache =
                qtype.declareValue(
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

                if (fsi.cache != null)
                    continue;

                if (fsi.fieldSelector == null && !fsi.usedAsBase)
                    continue;

                QValue cache;
                QType fstype = fsi.qtype;

                String comment = "Decoded " + fsi.fieldSelector;

                if (fsi.fieldSelector == null)  // base field, but not used anywhere else
                    if (fstype.instanceAllocatesMemory())
                        cache = fstype.declareValue(comment, evalGenerator.interimStateVarContainer, evalGenerator.classRegistry, false);
                    else
                        cache = fstype.declareValue(comment, evalGenerator.localVarContainer, evalGenerator.classRegistry, false);
                else {
                    cache = fstype.declareValue(comment, evalGenerator.interimStateVarContainer, evalGenerator.classRegistry, true);
                    evalGenerator.bind(fsi.fieldSelector, cache);
                }

                fsi.cache = cache;
            }
        }
        //
        //  Generate decoders
        //
        RecordClassDescriptor[] concreteTypes = sourceClassMap.concreteTypes;
        int numInputTypes = concreteTypes.length;
        Collection<TypeCheckInfo> typeChecks = sourceClassMap.allTypeChecks();

        JCompoundStatement addTo = CTXT.compStmt();

        generateInit(addTo);

        if (numInputTypes > 1) {
            JExpr typeIdx = getTypeIdxExpr();
            JSwitchStatement sw = typeIdx.switchStmt("typeSwitch");
            boolean typeDependentCodeFound = false;

            for (int ii = 0; ii < numInputTypes; ii++) {
                RecordClassDescriptor rcd = concreteTypes[ii];
                ClassSelectorInfo csi = sourceClassMap.getSelectorInfo(rcd);

                if (csi.hasUsedFields() || !typeChecks.isEmpty()) {
                    sw.addCaseLabel(CTXT.intLiteral(ii), csi.type.getName());
                    genDecoderForOneType(typeChecks, csi, sw, inVar);
                    sw.addBreak();
                    typeDependentCodeFound = true;
                }
            }

            addDefaultSwitch(sw);

            if (typeDependentCodeFound)
                addTo.add(sw);
        } else
            genDecoderForOneType(
                typeChecks,
                sourceClassMap.getSelectorInfo(concreteTypes[0]),
                addTo,
                inVar
            );

        evalGenerator.addTo.add(addTo);
    }

    private void genDecoderForOneType(
        Collection<TypeCheckInfo> typeChecks,
        ClassSelectorInfo csi,
        JCompoundStatement addTo,
        JExpr in
    ) {
        for (TypeCheckInfo tci : typeChecks) {
            ClassDescriptor testClass = tci.typeCheck.checkType;

            boolean test =
                testClass instanceof RecordClassDescriptor &&
                    ((RecordClassDescriptor) testClass).isAssignableFrom(csi.type);

            addTo.add(tci.cache.write(QBooleanType.getLiteral(test)));
        }

        QByteSkipContext skipper = new QByteSkipContext(in, addTo);

        int numFields = csi.highestUsedIdx + 1;

        for (int ii = 0; ii < numFields; ii++) {
            FieldSelectorInfo fsi = csi.fields[ii];
            QType type = fsi.qtype;

            addTo.addComment("Decode field " + fsi.field.getName());

            if (fsi.fieldSelector == null && !fsi.usedAsBase) {
                int n = type.getEncodedFixedSize();

                if (n != QType.SIZE_VARIABLE)
                    skipper.skipBytes(n);
                else {
                    skipper.flush();

                    JStatement ifMore =
                        CTXT.ifStmt(
                            in.call("hasAvail"),
                            type.skip(in)
                        );

                    addTo.add(ifMore);
                }
            } else {
                QValue target = fsi.cache;

                skipper.flush();

                JCompoundStatement action = CTXT.compStmt();

                if (fsi.relativeTo != null) {
                    action.add(fsi.cache.decodeRelative(in, fsi.relativeTo.cache));
                } else {
                    action.add(target.decode(in));
                }

                JStatement ifMore =
                    CTXT.ifStmt(
                        in.call("hasAvail"),
                        action,
                        target.writeNull()
                    );

                addTo.add(ifMore);
            }
        }
    }
}