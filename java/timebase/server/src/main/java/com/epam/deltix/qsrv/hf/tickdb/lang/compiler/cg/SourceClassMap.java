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

import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.DataFieldRef;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.*;

import java.util.*;
import java.util.function.Consumer;

/**
 *
 */
class SourceClassMap {
    public final RecordClassDescriptor []           concreteTypes;

    private final Map <RecordClassDescriptor, ClassSelectorInfo>  map =
        new HashMap <RecordClassDescriptor, ClassSelectorInfo> ();

    private final Set <TypeCheckInfo>               typeChecks =
        new HashSet <TypeCheckInfo> ();

    private ClassSelectorInfo                        getOrCreate (
        RecordClassDescriptor                           type
    )
    {
        ClassSelectorInfo           csi = map.get (type);

        if (csi == null) {
            RecordClassDescriptor   parentRCD = type.getParent ();

            ClassSelectorInfo       parentCSI =
                parentRCD == null ?
                    null :
                    getOrCreate (parentRCD);

            csi = new ClassSelectorInfo (parentCSI, type);

            map.put (type, csi);
        }
        
        return (csi);
    }

    public SourceClassMap (RecordClassDescriptor [] concreteTypes) {
        this.concreteTypes = concreteTypes;

        int                         n = concreteTypes.length;

        for (int ii = 0; ii < n; ii++) {
            RecordClassDescriptor   rcd = concreteTypes [ii];
            ClassSelectorInfo       csi = getOrCreate (rcd);
            csi.ordinal = ii;
        }
    }

    public Collection <ClassSelectorInfo>       allClassInfo () {
        return (map.values ());        
    }

    public Collection <TypeCheckInfo>           allTypeChecks () {
        return (typeChecks);
    }

    public ClassSelectorInfo                    getSelectorInfo (
        RecordClassDescriptor                       rcd
    )
    {
        return (map.get (rcd));
    }

    public void                                 discoverFieldSelectors (
        CompiledExpression                          e
    )
    {
        if (e instanceof FieldSelector) {
            register((FieldSelector) e);
        } else if (e instanceof TypeCheck) {
            TypeCheck typeCheck = (TypeCheck) e;
            if (typeCheck.args[0] instanceof ThisSelector) {
                register((TypeCheck) e);
            }
        }

        if (e instanceof CompiledComplexExpression) {
            CompiledComplexExpression   ccx = (CompiledComplexExpression) e;

            for (CompiledExpression arg : ccx.args)
                discoverFieldSelectors (arg);
        }
    }

    public void discoverFieldAccessors(CompiledExpression e) {
        if (e instanceof FieldAccessor) {
            register((FieldAccessor) e);
        }

        if (e instanceof CompiledComplexExpression) {
            CompiledComplexExpression ccx = (CompiledComplexExpression) e;

            if (ccx instanceof Predicate) {
                // skip discover predicate expression
                discoverFieldAccessors(((Predicate) e).compiledSelector);
            } else {
                for (CompiledExpression arg : ccx.args) {
                    discoverFieldAccessors(arg);
                }
            }
        }
    }

    private void register(TypeCheck typeCheck) {
        typeChecks.add(new TypeCheckInfo(typeCheck));
    }

    private void                                register (
        FieldSelector                               fieldSelector
    )
    {
        DataFieldRef            fieldRef = fieldSelector.fieldRef;
        DataField               df = fieldRef.field;

        if (!(df instanceof NonStaticDataField))
            return;
        
        for (ClassSelectorInfo csi : map.values ()) {
            csi.nonStaticFieldUsedFrom(fieldSelector, fieldRef);
        }
    }

    private void register(FieldAccessor fieldAccessor) {
        for (ClassSelectorInfo csi : map.values()) {
            DataFieldRef[] fieldRef = fieldAccessor.fieldRefs;

            for (int i = 0; i < fieldRef.length; ++i) {
                DataField df = fieldRef[i].field;
                if (!(df instanceof NonStaticDataField))
                    return;

                csi.nonStaticFieldUsedFrom(fieldAccessor, fieldRef[i]);
            }
        }
    }

    public void forEachField(Consumer<FieldSelectorInfo> consumer) {
        for (int i = 0; i < concreteTypes.length; i++) {
            ClassSelectorInfo csi = getSelectorInfo(concreteTypes[i]);
            if (csi.hasUsedFields()) {
                for (int j = 0; j < csi.highestUsedIdx + 1; j++) {
                    consumer.accept(csi.fields[j]);
                }
            }
        }
    }

    protected void forEachValue(Consumer<QValue> consumer) {
        Set<QValue> values = new HashSet<>();
        for (int i = 0; i < concreteTypes.length; i++) {
            ClassSelectorInfo csi = getSelectorInfo(concreteTypes[i]);
            if (csi.hasUsedFields()) {
                for (int j = 0; j < csi.highestUsedIdx + 1; j++) {
                    QValue value = csi.fields[j].cache;
                    if (value != null && !values.contains(value)) {
                        values.add(value);
                        consumer.accept(value);
                    }
                }
            }
        }
    }

    protected void forEachFieldSelector(Consumer<FieldSelectorInfo> consumer) {
        Set<QValue> values = new HashSet<>();
        for (int i = 0; i < concreteTypes.length; i++) {
            ClassSelectorInfo csi = getSelectorInfo(concreteTypes[i]);
            if (csi.hasUsedFields()) {
                for (int j = 0; j < csi.highestUsedIdx + 1; j++) {
                    QValue value = csi.fields[j].cache;
                    if (value != null && !values.contains(value)) {
                        values.add(value);
                        consumer.accept(csi.fields[j]);
                    }
                }
            }
        }
    }
}