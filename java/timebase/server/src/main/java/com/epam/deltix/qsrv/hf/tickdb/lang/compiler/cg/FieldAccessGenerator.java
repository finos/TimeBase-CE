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

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.FieldAccessor;
import com.epam.deltix.util.jcg.*;

import java.util.ArrayList;
import java.util.List;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

/**
 *
 */
abstract class FieldAccessGenerator {
    public final JClass globalClass;
    public final EvalGenerator evalGenerator;
    protected final JExpr inVar;
    protected final SourceClassMap sourceClassMap;

    public FieldAccessGenerator(JClass globalClass, EvalGenerator evalGenerator, SourceClassMap sourceClassMap) {
        this.globalClass = globalClass;
        this.evalGenerator = evalGenerator;
        this.sourceClassMap = sourceClassMap;

        this.inVar = globalClass.getVar("in").access();
    }

    protected abstract JExpr getTypeIdxExpr();

    protected abstract void generateInit(JCompoundStatement addTo);

    protected void addDefaultSwitch(JSwitchStatement switchStatement) {
    }

    protected JStatement wrapSelectors(JStatement selectors) {
        return selectors;
    }

    protected JStatement writeStatement(QValue target) {
        return target.decode(inVar);
    }

    protected JStatement writeNullStatement(QValue target) {
        return target.writeNull();
    }

    protected void addNullValues(JCompoundStatement addTo, List<QValue> written) {
    }

    void genSelectors() {
        //
        //  Generate decoders
        //
        RecordClassDescriptor[] concreteTypes = sourceClassMap.concreteTypes;
        int numInputTypes = concreteTypes.length;

        JCompoundStatement addTo = CTXT.compStmt();

        generateInit(addTo);

        if (numInputTypes > 1) {
            JExpr typeIdx = getTypeIdxExpr();
            JSwitchStatement sw = typeIdx.switchStmt("typeSwitch");
            boolean typeDependentCodeFound = false;

            for (int ii = 0; ii < numInputTypes; ii++) {
                RecordClassDescriptor rcd = concreteTypes[ii];
                ClassSelectorInfo csi = sourceClassMap.getSelectorInfo(rcd);

                if (csi.hasUsedFields()) {
                    sw.addCaseLabel(CTXT.intLiteral(ii), csi.type.getName());
                    List<QValue> written = genDecoderForOneType(csi, sw);
                    addNullValues(sw, written);
                    sw.addBreak();
                    typeDependentCodeFound = true;
                }
            }

            addDefaultSwitch(sw);

            if (typeDependentCodeFound) {
                addTo.add(sw);
            }
        } else {
            genDecoderForOneType(
                sourceClassMap.getSelectorInfo(concreteTypes[0]), addTo
            );
        }

        evalGenerator.addTo.add(wrapSelectors(addTo));
    }

    private List<QValue> genDecoderForOneType(ClassSelectorInfo csi, JCompoundStatement addTo) {
        List<QValue> written = new ArrayList<>();
        QByteSkipContext skipper = new QByteSkipContext(inVar, addTo);
        int numFields = csi.highestUsedIdx + 1;
        for (int ii = 0; ii < numFields; ii++) {
            FieldSelectorInfo fsi = csi.fields[ii];
            QType type = fsi.qtype;

            addTo.addComment("Decode field " + fsi.field.getName());

            if (fsi.fieldAccessor == null && !fsi.usedAsBase) {
                int n = type.getEncodedFixedSize();
                if (n != QType.SIZE_VARIABLE) {
                    skipper.skipBytes(n);
                } else {
                    skipper.flush();

                    JStatement ifMore = CTXT.ifStmt(
                        inVar.call("hasAvail"), type.skip(inVar)
                    );

                    addTo.add(ifMore);
                }
            } else {
                QValue target = fsi.cache;
                written.add(target);
                skipper.flush();
                JCompoundStatement action = CTXT.compStmt();
                if (fsi.relativeTo != null) {
                    action.add(fsi.cache.decodeRelative(inVar, fsi.relativeTo.cache));
                } else {
                    action.add(writeStatement(target));
                    int adjustTypeIndex = adjustTypeIndex(fsi);
                    if (adjustTypeIndex >= 0) {
                        if (target instanceof QObjectValue) {
                            action.add(((QObjectValue) target).adjustType(adjustTypeIndex));
                        } else if (target instanceof QArrayValue) {
                            QArrayValue arrayValue = (QArrayValue) target;
                            if (arrayValue.isObjectArray()) {
                                action.add(arrayValue.adjustType(adjustTypeIndex));
                            }
                        }
                    }
                }

                JStatement ifMore = CTXT.ifStmt(
                    inVar.call("hasAvail"), action, writeNullStatement(target)
                );

                addTo.add(ifMore);
            }
        }

        return written;
    }

    private int adjustTypeIndex(FieldSelectorInfo fsi) {
        DataType type = fsi.qtype.dt;
        if (type instanceof ArrayDataType) {
            type = ((ArrayDataType) type).getElementDataType();
        }

        if (type instanceof ClassDataType) {
            ClassDataType classDataType = (ClassDataType) type;

            int adjustIndex = 0;
            if (fsi.fieldAccessor != null) {
                FieldAccessor fa = fsi.fieldAccessor;
                for (int i = 0; i < fa.fieldRefs.length; ++i) {
                    DataType faType = fa.fieldRefs[i].field.getType();
                    if (faType instanceof ArrayDataType) {
                        faType = ((ArrayDataType) faType).getElementDataType();
                    }

                    if (faType instanceof ClassDataType) {
                        ClassDataType faClassDataType = (ClassDataType) faType;
                        if (faClassDataType.equals(classDataType)) {
                            return adjustIndex;
                        } else {
                            adjustIndex += faClassDataType.getDescriptors().length;
                        }
                    } else {
                        return -1;
                    }
                }
            }
        }

        return -1;
    }
}