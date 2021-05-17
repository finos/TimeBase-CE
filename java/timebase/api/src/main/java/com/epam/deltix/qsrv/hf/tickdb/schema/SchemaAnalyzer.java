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
package com.epam.deltix.qsrv.hf.tickdb.schema;

import com.epam.deltix.qsrv.hf.pub.codec.FieldLayout;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.codec.RecordLayout;
import com.epam.deltix.qsrv.hf.pub.codec.StaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.collections.generated.ObjectHashSet;
import com.epam.deltix.util.lang.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SchemaAnalyzer {

    public static final SchemaAnalyzer DEFAULT = new SchemaAnalyzer(new SchemaMapping());
    private SchemaMapping mapping;

    public SchemaAnalyzer(SchemaMapping mapping) {
        this.mapping = mapping;
    }

    public StreamMetaDataChange getChanges(RecordClassSet in,
                                           MetaDataChange.ContentType inType,
                                           RecordClassSet out,
                                           MetaDataChange.ContentType outType) {

        MetaData<RecordClassDescriptor> sorted = mapping.sort(in, out);
        StreamMetaDataChange dataChange = new StreamMetaDataChange(in, sorted, mapping);
        dataChange.sourceType = inType;
        dataChange.targetType = outType;
        buildChanges(dataChange);        

        return dataChange;
    }

    public static StreamMetaDataChange getChanges (final DXTickStream source, final DXTickStream target) {

        final RecordClassSet in = new RecordClassSet ();
        final RecordClassSet out = new RecordClassSet ();
        MetaDataChange.ContentType inType;
        MetaDataChange.ContentType outType;

        if (source.isFixedType ()) {
            inType = MetaDataChange.ContentType.Fixed;
            in.addContentClasses (source.getFixedType ());
        } else {
            inType = MetaDataChange.ContentType.Polymorphic;
            in.addContentClasses (source.getPolymorphicDescriptors ());
        }

        if (target.isFixedType ()) {
            outType = MetaDataChange.ContentType.Fixed;
            out.addContentClasses (target.getFixedType ());
        } else {
            outType = MetaDataChange.ContentType.Polymorphic;
            out.addContentClasses (target.getPolymorphicDescriptors ());
        }

        return DEFAULT.getChanges(in, inType, out, outType);
    }

    protected MetaDataChange buildChanges(MetaDataChange dataChange) {

        HashMap<RecordClassDescriptor, RecordClassDescriptor> processed = new HashMap<>();
         // check only top types
        RecordClassDescriptor[] input = dataChange.source.getContentClasses();
        RecordClassDescriptor[] output = dataChange.target.getContentClasses();

        for (RecordClassDescriptor source : input) {
            RecordClassDescriptor target = (RecordClassDescriptor)
                    mapping.findClassDescriptor(source, dataChange.target);

            if (target != null) {
                ClassDescriptorChange change = getChanges(dataChange, source, target);
                if (change != null)
                    dataChange.changes.add(change);

                SchemaChange.Impact impact = change != null ? change.getChangeImpact() : SchemaChange.Impact.None;

                if (impact == SchemaChange.Impact.None) {
                    if (Arrays.asList(input).indexOf(source) != Arrays.asList(output).indexOf(target)) {
                        if (change != null)
                            change.setImpact(SchemaChange.Impact.DataConvert);
                        else if (dataChange.targetType == dataChange.sourceType)
                            dataChange.changes.add(new ClassDescriptorChange(source, target, SchemaChange.Impact.DataConvert));
                    }
                }
            } else {
                dataChange.changes.add(new ClassDescriptorChange(source, null));
            }
            processed.put(source, target);
        }

        for (RecordClassDescriptor target : output) {
            if (!processed.containsValue(target)) {
                dataChange.changes.add(new ClassDescriptorChange(null, target));
            }
        }

        buildEnumChanges(dataChange);

        return dataChange;
    }

    private void buildEnumChanges(MetaDataChange dataChange) {
        EnumClassDescriptor[] sourceEcds = Arrays.stream(dataChange.source.getClasses())
                .filter(cd -> cd instanceof EnumClassDescriptor)
                .map(cd -> (EnumClassDescriptor) cd)
                .toArray(EnumClassDescriptor[]::new);

        EnumClassDescriptor[] targetEcds = Arrays.stream(dataChange.target.getClassDescriptors())
                .filter(cd -> cd instanceof EnumClassDescriptor)
                .map(cd -> (EnumClassDescriptor) cd)
                .toArray(EnumClassDescriptor[]::new);

        for (EnumClassDescriptor sourceEcd : sourceEcds) {
            String guid = dataChange.mapping.descriptors.get(sourceEcd.getGuid());
            if (guid != null) {
                EnumClassDescriptor found = null;
                for (EnumClassDescriptor targetEcd : targetEcds) {
                    if (guid.equals(targetEcd.getGuid())) {
                        found = targetEcd;
                        break;
                    }
                }
                if (found != null) {
                    dataChange.enumMapping.addMapping(sourceEcd, found, dataChange.mapping);
                    continue;
                }
            }
            EnumClassDescriptor found = null;

            for (EnumClassDescriptor targetEcd : targetEcds) {
                if (sourceEcd.getName().equalsIgnoreCase(targetEcd.getName())) {
                    found = targetEcd;
                    break;
                }
            }

            if (found != null) {
                dataChange.enumMapping.addMapping(sourceEcd, found, dataChange.mapping);
            }
        }
    }

    public MetaDataChange getChanges(ClassSet<RecordClassDescriptor> in,
                                     MetaDataChange.ContentType inType,
                                     MetaData<RecordClassDescriptor> out,
                                     MetaDataChange.ContentType outType)
    {
        MetaDataChange dataChange = new MetaDataChange(in, out, mapping);
        dataChange.sourceType = inType;
        dataChange.targetType = outType;

        buildChanges(dataChange);
        return dataChange;        
    }   

    public static boolean isEquals(DataField source, DataField target) {

        if (source != null && target != null) {            
            if (!Util.xequals(source.getName(), target.getName()))
                return false;

            if (source.getType().getClass() != target.getType().getClass()) {
                return false;
            } else if (!Util.xequals(source.getType().getEncoding(), target.getType().getEncoding())) {
                return false;
            }

            return true;
        }

        return Util.xequals(source, target);
    }    

    protected ClassDescriptorChange getChanges(MetaDataChange meta, RecordClassDescriptor in, RecordClassDescriptor out) {

        ArrayList<AbstractFieldChange> changes = new ArrayList<AbstractFieldChange>();
        ArrayList<AbstractFieldChange> positionChanges = new ArrayList<AbstractFieldChange>();

        RecordLayout sourceLayout = new RecordLayout(in);
        RecordLayout targetLayout = new RecordLayout(out);
        List<NonStaticFieldLayout> sourceFields = Arrays.asList(notNull(sourceLayout.getNonStaticFields()));
        List<NonStaticFieldLayout> targetFields = Arrays.asList(notNull(targetLayout.getNonStaticFields()));

        HashMap<FieldLayout, FieldLayout> processed = new HashMap<FieldLayout, FieldLayout>();

        for (NonStaticFieldLayout source : sourceFields) {
            FieldLayout match = mapping.findField(source, targetLayout);

            if (match instanceof NonStaticFieldLayout) {

                NonStaticFieldLayout target = (NonStaticFieldLayout) match;

                changes.addAll(getChanges(meta, source, target));

                // check if field position changed
                if (sourceFields.indexOf(source) != targetFields.indexOf(target))
                    positionChanges.add(new FieldPositionChange(source.getField(), target.getField()));

                processed.put(source, match);
            }
            else if (match instanceof StaticFieldLayout) {
                changes.add(new FieldModifierChange(source.getField(), match.getField(), false));
                processed.put(source, match);
            }
            else {
                changes.add(new DeleteFieldChange(source.getField()));
            }
        }

        // process static fields
        for (StaticFieldLayout source : notNull(sourceLayout.getStaticFields())) {
            FieldLayout target = mapping.findField(source, targetLayout);

            if (target instanceof NonStaticFieldLayout) {
                changes.add(new FieldModifierChange(source.getField(),
                        target.getField(), hasImpactOnCreate((NonStaticFieldLayout)target, targetFields, sourceLayout)));
            } else if (target instanceof StaticFieldLayout) {
                changes.addAll(getStaticChanges(meta, source, (StaticFieldLayout) target));
            } else {
                changes.add(new DeleteFieldChange(source.getField()));
            }

            if (target != null)
                processed.put(source, target);
        }

        // find new fields
        for (NonStaticFieldLayout target : targetFields) {
            if (!processed.containsValue(target)) {
                changes.add(new CreateFieldChange(target.getField(), hasImpactOnCreate(target, targetFields, sourceLayout)));
            }
        }

         // find new static fields
        for (StaticFieldLayout target : notNull(targetLayout.getStaticFields())) {
            if (!processed.containsValue(target)) {
                changes.add(new CreateFieldChange(target.getField()));
            }
        }

        ClassDescriptorChange change = null;
        if (changes.size() > 0) {
            SchemaChange.Impact impact = ClassDescriptorChange.getChangeImpact(changes);

            // append position changes if nothing changed in data
            if (impact == SchemaChange.Impact.None)
                changes.addAll(positionChanges);

            change = new ClassDescriptorChange(in, out, changes.toArray(new AbstractFieldChange[changes.size()]));

        } else if (positionChanges.size() > 0) {
            // if any field position change - we should convert data
            change = new ClassDescriptorChange(in, out,
                    positionChanges.toArray(new AbstractFieldChange[positionChanges.size()]));
        }

        return change;
    }

    private void processChanges(MetaDataChange meta, ClassDataType source, ClassDataType target) {

        List<RecordClassDescriptor> inList = source.getDescriptors() != null ?
                Arrays.asList(source.getDescriptors()) :
                new ArrayList<>();
        List<RecordClassDescriptor> outList = Arrays.asList(target.getDescriptors());

        for (int i = 0; i < inList.size(); i++) {
            RecordClassDescriptor rcd = inList.get(i);

            RecordClassDescriptor found = (RecordClassDescriptor) mapping.findClassDescriptor(rcd, meta.target, true);

            if (found != null) {
                ClassDescriptorChange change = getChanges(meta, rcd, found);
                if (change != null)
                    meta.changes.add(change);

                SchemaChange.Impact impact = change != null ? change.getChangeImpact() : SchemaChange.Impact.None;

                if (impact == SchemaChange.Impact.None) {

                    // index changed?
                    if (i != outList.indexOf(found)) {
                        if (change != null)
                            change.setImpact(SchemaChange.Impact.DataConvert);
                        else if (meta.targetType == meta.sourceType)
                            meta.changes.add(new ClassDescriptorChange(rcd, found, SchemaChange.Impact.DataConvert));
                    }
                }
            } else {
                meta.changes.add(new ClassDescriptorChange(rcd, null));
            }
        }
    }

    private List<AbstractFieldChange> getChanges(MetaDataChange meta,
                                                 NonStaticFieldLayout source,
                                                 NonStaticFieldLayout target) {
        ArrayList<AbstractFieldChange> changes = new ArrayList<AbstractFieldChange>();

        // is relation changed?
        if (!Util.xequals(source.getRelativeTo(), target.getRelativeTo())) {
            NonStaticFieldLayout srcRelation = source.getRelativeTo();
            NonStaticFieldLayout trgRelation = target.getRelativeTo();

            if (srcRelation != null && trgRelation != null) {
                if (!isEquals(srcRelation.getField(), trgRelation.getField()))
                    changes.add(new FieldRelationChange(source.getField(), target.getField()));
            }
            else {
                changes.add(new FieldRelationChange(source.getField(), target.getField()));
            }
        }

        DataType from = source.getType();
        DataType to = target.getType();

        // array types in compatible until element types in compatible
        if (from instanceof ArrayDataType && to instanceof ArrayDataType) {
            from = ((ArrayDataType)from).getElementDataType();
            to = ((ArrayDataType)to).getElementDataType();
        }

        if (from instanceof ClassDataType && to instanceof ClassDataType) {
            processChanges(meta, (ClassDataType) from, (ClassDataType) to);
        }

        // is type changed?
        if (from.getClass() != to.getClass()) {
            changes.add(new FieldTypeChange(source.getField(), target.getField()));
        }
        // is type encoding changed?
        else if (!Util.xequals(from.getEncoding(), to.getEncoding())) {
            changes.add(new FieldTypeChange(source.getField(), target.getField()));
        }
        else {
            // additional constraints check
            if (from instanceof FloatDataType) {
                FloatDataType sourceType = (FloatDataType) from;
                FloatDataType targetType = (FloatDataType) to;
                // is bounds changed?
                if (!Util.xequals(sourceType.getMin(), targetType.getMin()) ||
                        !Util.xequals(sourceType.getMax(), targetType.getMax())) {
                    changes.add(new FieldTypeChange(source.getField(), target.getField()));
                }
            }
            else if (from instanceof IntegerDataType) {
                IntegerDataType sourceType = (IntegerDataType) from;
                IntegerDataType targetType = (IntegerDataType) to;
                // is bounds changed?
                if (!Util.xequals(sourceType.getMin(), targetType.getMin()) ||
                        !Util.xequals(sourceType.getMax(), targetType.getMax())) {
                    changes.add(new FieldTypeChange(source.getField(), target.getField()));
                }
            }
            else if (from instanceof EnumDataType) {
                EnumDataType sourceType = (EnumDataType) from;
                EnumDataType targetType = (EnumDataType) to;

                // is enum values changed?
                if (!Util.xequals(sourceType.descriptor, targetType.descriptor)) {
                    if (!Arrays.equals(sourceType.descriptor.getValues(),
                            targetType.descriptor.getValues())) {
                        changes.add(new EnumFieldTypeChange(source.getField(), target.getField()));
                    }
                }
            } else if (from instanceof VarcharDataType) {
                VarcharDataType sourceType = (VarcharDataType) from;
                VarcharDataType targetType = (VarcharDataType) to;
                if (sourceType.isMultiLine() != targetType.isMultiLine())
                    changes.add(new FieldTypeChange(source.getField(), target.getField()));
            }

            // is nullable changed?
            if (from.isNullable() != to.isNullable()) {
                FieldTypeChange typeChange = new FieldTypeChange(source.getField(), target.getField());
                if (!changes.contains(typeChange))
                    changes.add(typeChange);
            }
        }

        // non-data related change
        if (!Util.xequals(source.getField().getTitle(), target.getField().getTitle()))
            changes.add(new FieldChange(source.getField(), target.getField(), FieldAttribute.Title));
        else if (!Util.xequals(source.getName(), target.getName()))
            changes.add(new FieldChange(source.getField(), target.getField(), FieldAttribute.Name));
        else if (!Util.xequals(source.getField().getDescription(), target.getField().getDescription()))
            changes.add(new FieldChange(source.getField(), target.getField(), FieldAttribute.Description));

        return changes;
    }


    private List<AbstractFieldChange> getStaticChanges(MetaDataChange meta, StaticFieldLayout source, StaticFieldLayout target) {
        ArrayList<AbstractFieldChange> changes = new ArrayList<AbstractFieldChange>();

        DataType from = source.getType();
        DataType to = target.getType();

        // array types in compatible until element types in compatible
        if (from instanceof ArrayDataType && to instanceof ArrayDataType) {
            from = ((ArrayDataType)from).getElementDataType();
            to = ((ArrayDataType)to).getElementDataType();
        }

        if (from instanceof ClassDataType && to instanceof ClassDataType) {
            processChanges(meta, (ClassDataType) from, (ClassDataType) to);
        }

        // is type changed?
        if (from.getClass() != to.getClass()) {
            changes.add(new StaticFieldChange(source.getField(), target.getField()));
        }
        // is type encoding changed?
        else if (!Util.xequals(from.getEncoding(), to.getEncoding())) {
            changes.add(new StaticFieldChange(source.getField(), target.getField()));
        }
        else {
            // additional constraints check
            if (from instanceof FloatDataType) {
                FloatDataType sourceType = (FloatDataType) from;
                FloatDataType targetType = (FloatDataType) to;
                // is bounds changed?
                if (!Util.xequals(sourceType.getMin(), targetType.getMin()) ||
                        !Util.xequals(sourceType.getMax(), targetType.getMax())) {
                    changes.add(new StaticFieldChange(source.getField(), target.getField()));
                }
            }
            else if (from instanceof IntegerDataType) {
                IntegerDataType sourceType = (IntegerDataType) from;
                IntegerDataType targetType = (IntegerDataType) to;
                // is bounds changed?
                if (!Util.xequals(sourceType.getMin(), targetType.getMin()) ||
                        !Util.xequals(sourceType.getMax(), targetType.getMax())) {
                    changes.add(new StaticFieldChange(source.getField(), target.getField()));
                }
            }
            else if (from instanceof EnumDataType) {
                EnumDataType sourceType = (EnumDataType) from;
                EnumDataType targetType = (EnumDataType) to;

                // is enum values changed?
                if (!Util.xequals(sourceType.descriptor, targetType.descriptor)) {
                    if (!Arrays.equals(sourceType.descriptor.getValues(),
                            targetType.descriptor.getValues())) {
                        changes.add(new StaticFieldChange(source.getField(), target.getField()));
                    }
                }
            } else if (from instanceof VarcharDataType) {
                VarcharDataType sourceType = (VarcharDataType) from;
                VarcharDataType targetType = (VarcharDataType) to;
                if (sourceType.isMultiLine() != targetType.isMultiLine())
                    changes.add(new StaticFieldChange(source.getField(), target.getField()));
            }

            // is nullable changed?
            if (from.isNullable() != to.isNullable()) {
                StaticFieldChange typeChange = new StaticFieldChange(source.getField(), target.getField());
                if (!changes.contains(typeChange))
                    changes.add(typeChange);
            }
        }

        if (!Util.xequals(source.getField().getStaticValue(), target.getField().getStaticValue())) // value changed?
            changes.add(new FieldValueChange(source.getField(), target.getField()));

        // non-data related change
        if (!Util.xequals(source.getField().getTitle(), target.getField().getTitle()))
            changes.add(new FieldChange(source.getField(), target.getField(), FieldAttribute.Title));
        if (!Util.xequals(source.getName(), target.getName()))
            changes.add(new FieldChange(source.getField(), target.getField(), FieldAttribute.Name));
        if (!Util.xequals(source.getField().getDescription(), target.getField().getDescription()))
            changes.add(new FieldChange(source.getField(), target.getField(), FieldAttribute.Description));

        return changes;
    }

    public boolean hasImpactOnDelete(NonStaticFieldLayout source, List<NonStaticFieldLayout> sourceFields, RecordLayout target) {
        int index = sourceFields.indexOf(source);

        if (index == sourceFields.size() - 1) {
            return false;
        } else {

            for (int i = sourceFields.size() - 2; i > index ; i--) {
                NonStaticFieldLayout next = sourceFields.get(i);
                FieldLayout field = mapping.findField(next, target);
                if (field != null)
                    return true;
            }
        }

        return false;
    }

    public boolean hasImpactOnCreate(NonStaticFieldLayout target, List<NonStaticFieldLayout> targetFields, RecordLayout source) {
        int index = targetFields.indexOf(target);

        if (index == targetFields.size() - 1) {
            return false;
        } else {
            for (int i = index + 1; i < targetFields.size(); i++) {
                NonStaticFieldLayout next = targetFields.get(i);
                FieldLayout field = mapping.findField(source, next);
                if (field != null)
                    return true;
            }
        }

        return false;
    }

    private static NonStaticFieldLayout[] notNull(NonStaticFieldLayout[] fields) {
        return fields != null ? fields : new NonStaticFieldLayout[0]; 
    }

    private StaticFieldLayout[] notNull(StaticFieldLayout[] fields) {
        return fields != null ? fields : new StaticFieldLayout[0];
    }
}
