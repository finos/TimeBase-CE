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

import com.epam.deltix.qsrv.hf.pub.md.ArrayDataType;
import com.epam.deltix.qsrv.hf.pub.md.ClassDataType;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.FieldAccessor;
import com.epam.deltix.util.jcg.JArrayInitializer;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JVariable;

import java.util.ArrayList;
import java.util.List;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

class TypeAdjustmentGenerator {

    private final EvalGenerator evalGenerator;

    public TypeAdjustmentGenerator(EvalGenerator evalGenerator) {
        this.evalGenerator = evalGenerator;
    }

    void genTypeAdjustment(FieldSelectorInfo fieldSelector, QValue targetValue, JCompoundStatement addTo) {
        int[] adjustTypeIndices = adjustTypeIndices(fieldSelector);
        if (adjustTypeIndices.length > 0) {
            if (targetValue instanceof QObjectValue) {
                JVariable variable = makeTypeMapVariable(fieldSelector.field.getName(), adjustTypeIndices);
                addTo.add(((QObjectValue) targetValue).adjustTypes(evalGenerator.interimStateVarContainer.access(variable)));
            } else if (targetValue instanceof QArrayValue) {
                QArrayValue arrayValue = (QArrayValue) targetValue;
                if (arrayValue.isObjectArray()) {
                    JVariable variable = makeTypeMapVariable(fieldSelector.field.getName(), adjustTypeIndices);
                    addTo.add(arrayValue.adjustTypes(evalGenerator.interimStateVarContainer.access(variable)));
                }
            }
        }
    }

    private int[] adjustTypeIndices(FieldSelectorInfo fieldSelector) {
        DataType type = fieldSelector.qtype.dt;
        if (type instanceof ArrayDataType) {
            type = ((ArrayDataType) type).getElementDataType();
        }

        if (type instanceof ClassDataType) {
            ClassDataType classDataType = (ClassDataType) type;
            if (fieldSelector.fieldAccessor != null) {
                FieldAccessor fa = fieldSelector.fieldAccessor;
                DataType outputType = fa.type;
                if (outputType instanceof ArrayDataType) {
                    outputType = ((ArrayDataType) outputType).getElementDataType();
                }

                if (outputType instanceof ClassDataType) {
                    ClassDataType outputClassDataType = (ClassDataType) outputType;

                    RecordClassDescriptor[] sourceDescriptors = classDataType.getDescriptors();
                    RecordClassDescriptor[] targetDescriptors = outputClassDataType.getDescriptors();
                    List<Integer> mapping = new ArrayList<>();
                    for (int iSource = 0; iSource < sourceDescriptors.length; ++iSource) {
                        for (int iTarget = 0; iTarget < targetDescriptors.length; ++iTarget) {
                            if (sourceDescriptors[iSource].getName().equals(targetDescriptors[iTarget].getName())) {
                                mapping.add(iSource);
                                mapping.add(iTarget);
                                break;
                            }
                        }
                    }

                    return mapping.stream().mapToInt(m -> m).toArray();
                }
            }
        }

        return new int[0];
    }

    private JVariable makeTypeMapVariable(String fieldName, int[] adjustTypeIndices) {
        JArrayInitializer arrayInitializer = CTXT.arrayInitializer(int[].class);
        for (int i = 0; i < adjustTypeIndices.length; ++i) {
            arrayInitializer.add(CTXT.intLiteral(adjustTypeIndices[i]));
        }

        return evalGenerator.interimStateVarContainer.addVar(
            "Adjust types map for " + fieldName,
            true, int[].class,
            arrayInitializer
        );
    }
}
