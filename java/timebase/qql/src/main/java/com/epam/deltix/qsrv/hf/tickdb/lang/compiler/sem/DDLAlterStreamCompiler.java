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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.pub.codec.FieldLayout;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.codec.RecordLayout;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.UnknownIdentifierException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants.IntegerConstant;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.schema.SchemaMapping;
import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.util.parsers.Element;

import java.util.*;

public class DDLAlterStreamCompiler {

    private final DXTickStream stream;
    private final ClassDescriptor[] streamDescriptors;

    private final DDLCompiler ddlCompiler;
    private final EnvironmentFrame senv;

    private final ClassDef[] streamClassDefs;

    private final Map<String, ClassDef> droppedTypes = new HashMap<>();
    private final Map<String, ClassDef> addedTypes = new HashMap<>();
    private final Map<String, ClassDef> alteredTypes = new HashMap<>();
    private final Map<String, ClassDef> allTypes = new HashMap<>();
    private final Map<String, ClassDescriptor> compiledTypes = new HashMap<>();
    private final HashMap<String, String> newFieldNameToOriginal = new HashMap<>();
    private final HashMap<String, String> newEnumValueNameToOriginal = new HashMap<>();
    private final HashMap<String, String> newTypeNameToOriginal = new HashMap<>();

    private final RecordClassSet targetRcs = new RecordClassSet();
    private final Map<DataField, ModifyFieldData> defaults = new HashMap<>();
    private final SchemaMapping mapping = new SchemaMapping();

    public DDLAlterStreamCompiler(DXTickStream stream, DDLCompiler ddlCompiler, AlterStreamStatement alterStream) {
        this.stream = stream;
        this.streamDescriptors = stream.getStreamOptions().getMetaData().getClassDescriptors();
        this.ddlCompiler = ddlCompiler;
        this.senv = new EnvironmentFrame(ddlCompiler.env());
        this.streamClassDefs = getStreamClassDefs(stream);

        preserveMultilines();

        Set<String> modifiedTypes = new HashSet<>();
        for (ClassDef alteration : alterStream.alterations) {
            String typeName = identifier(alteration);
            if (modifiedTypes.contains(typeName)) {
                throw new CompilationException("Duplicate modification for the same identifier.", alteration);
            }

            if (alteration instanceof RecordClassDef || alteration instanceof EnumClassDef) {
                addedTypes.put(typeName, alteration);
                modifiedTypes.add(typeName);
            } else if (alteration instanceof DropClass || alteration instanceof DropEnum) {
                droppedTypes.put(typeName, alteration);
                modifiedTypes.add(typeName);
            } else if (alteration instanceof AlterClass || alteration instanceof AlterEnum || alteration instanceof UpdateDef) {
                alteredTypes.put(typeName, alteration);
                modifiedTypes.add(typeName);
            }
        }

        collectAllTypes();
    }

    RecordClassSet recordClassSet() {
        return targetRcs;
    }

    Map<DataField, ModifyFieldData> defaults() {
        return defaults;
    }

    SchemaMapping mapping() {
        return mapping;
    }

    void compile() {
        for (ClassDef classDef : streamClassDefs) {
            compileAlterClassDef(classDef);
        }

        for (ClassDef classDef : addedTypes.values()) {
            String typeName = identifier(classDef);
            if (compiledTypes.containsKey(typeName)) {
                continue;
            }
            compileClassDef(classDef, false);
        }

        if (!droppedTypes.isEmpty()) {
            throw new CompilationException("Identifier specified in DROP doesn't exist.", droppedTypes.entrySet().stream().findFirst().get().getValue().id);
        }

        if (!alteredTypes.isEmpty()) {
            throw new CompilationException("Identifier specified in ALTER doesn't exist.", alteredTypes.entrySet().stream().findFirst().get().getValue().id);
        }
    }

    private void compileAlterClassDef(ClassDef classDef) {
        if (classDef == null) {
            return;
        }

        String typeName = identifier(classDef);
        if (compiledTypes.containsKey(typeName)) {
            return;
        }

        if (droppedTypes.remove(typeName) != null) {
            return;
        }

        ClassDef targetClassDef = classDef;
        ClassDef alteration = alteredTypes.remove(typeName);
        if (alteration instanceof AlterClass && classDef instanceof RecordClassDef) {
            targetClassDef = alterRecordClassDef((AlterClass) alteration, (RecordClassDef) classDef);
        } else if (alteration instanceof AlterEnum && classDef instanceof EnumClassDef) {
            targetClassDef = alterEnumClassDef((AlterEnum) alteration, (EnumClassDef) classDef);
        } else if (alteration instanceof UpdateDef) {
            ClassDef cdef = ((UpdateDef) alteration).inner;
            if (classDef instanceof RecordClassDef) {
                targetClassDef = new RecordClassDef(
                        cdef.location,
                        classDef.id,
                        cdef.title,
                        cdef.guid,
                        cdef.comment,
                        ((RecordClassDef) cdef).auxiliary,
                        ((RecordClassDef) cdef).instantiable,
                        ((RecordClassDef) cdef).parent,
                        ((RecordClassDef) cdef).attributes
                );
            } else {
                targetClassDef = new EnumClassDef(
                        cdef.location,
                        classDef.id,
                        cdef.title,
                        cdef.comment,
                        false,
                        ((EnumClassDef) cdef).values
                );
            }
        }

        ClassDescriptor cd = compileClassDef(targetClassDef, true);
        if (alteration instanceof AlterClass && cd instanceof RecordClassDescriptor) {
            compileDefaultResolutions(((AlterClass) alteration).defaultResolutions, (RecordClassDescriptor) cd);
        }
    }

    private ClassDescriptor compileClassDef(ClassDef classDef, boolean isStreamClass) {
        String typeName = identifier(classDef);
        if (compiledTypes.containsKey(typeName)) {
            throw new IllegalStateException("Duplicate type: " + typeName);
        }

        compileDependencies(classDef);

        ClassDescriptor cd = compileClassDefErrorCheck(classDef, isStreamClass);
        targetRcs.addClasses(cd);
        compiledTypes.put(typeName, cd);

        String originalTypeName = newTypeNameToOriginal.getOrDefault(typeName, typeName);
        compiledTypes.put(originalTypeName, cd);

        ClassDescriptor originalCd = findOriginalCdAndFillTypeMapping(cd, originalTypeName);
        if (cd instanceof RecordClassDescriptor) {
            RecordClassDescriptor rcd = (RecordClassDescriptor) cd;
            validateDuplicateFields(rcd);

            boolean auxiliary = classDef instanceof RecordClassDef && ((RecordClassDef) classDef).auxiliary;
            if (!rcd.isAbstract() && !auxiliary) {
                targetRcs.addContentClasses(rcd);
            }

            senv.bindNoDup(new TypeIdentifier(originalTypeName), new ClassDataType(true, rcd));
            fillFieldsMapping(rcd, (RecordClassDescriptor) originalCd);
        } else if (cd instanceof EnumClassDescriptor) {
            EnumClassDescriptor ecd = (EnumClassDescriptor) cd;
            senv.bindNoDup(new TypeIdentifier(originalTypeName), new EnumDataType(true, ecd));
            ddlCompiler.registerEnum(ecd);
            fillEnumValuesMapping(ecd, (EnumClassDescriptor) originalCd);
        } else {
            throw new RuntimeException(cd.toString());
        }

        return cd;
    }

    private ClassDescriptor compileClassDefErrorCheck(ClassDef cdef, boolean isStreamClass) {
        try {
            return ddlCompiler.compileClassDef(senv, cdef, defaults);
        } catch (CompilationException t) {
            if (isStreamClass) {
                throw new CompilationException("Failed to compile class '" + cdef.id.typeName + "': " + t.diag, 0);
            } else {
                throw t;
            }
        } catch (Throwable t) {
            if (isStreamClass) {
                throw new CompilationException(t.getMessage(), 0);
            } else {
                throw t;
            }
        }

    }

    private static void validateDuplicateFields(RecordClassDescriptor rcd) {
        RecordLayout recordLayout = new RecordLayout(rcd);
        Map<String, NonStaticFieldLayout> nameToField = new HashMap<>();
        for (NonStaticFieldLayout field : recordLayout.getNonStaticFields()) {
            if (nameToField.containsKey(field.getName().toUpperCase())) {
                throw new CompilationException("Duplicate field: " + field.getName(), 0);
            }
            nameToField.put(field.getName(), field);
        }
    }

    private RecordClassDef alterRecordClassDef(AlterClass alterClass, RecordClassDef recordClassDef) {
        String classId = identifier(alterClass.id);
        if (alterClass.newName != null) {
            registerTypeRename(identifier(alterClass.newName), classId);
            classId = identifier(alterClass.newName);
        }

        ArrayList<AttributeDef> attributes = new ArrayList<>();
        Map<String, AlterFieldAlteration> alteredFields = new HashMap<>();
        Map<String, UpdateFieldAlteration> updatedFields = new HashMap<>();
        Map<String, DropFieldAlteration> droppedFields = new HashMap<>();
        Set<String> modifiedFields = new HashSet<>();
        if (alterClass.attributes != null) {
            for (AttributeDef attribute : alterClass.attributes) {
                String attributeName = identifier(attribute);
                if (modifiedFields.contains(attributeName)) {
                    throw new CompilationException("Duplicate modification for the same identifier.", attribute);
                }

                if (attribute instanceof UpdateFieldAlteration) {
                    updatedFields.put(attributeName, (UpdateFieldAlteration) attribute);
                    modifiedFields.add(attributeName);
                } else if (attribute instanceof AlterFieldAlteration) {
                    alteredFields.put(attributeName, (AlterFieldAlteration) attribute);
                    modifiedFields.add(attributeName);
                } else if (attribute instanceof DropFieldAlteration) {
                    droppedFields.put(attributeName, (DropFieldAlteration) attribute);
                    modifiedFields.add(attributeName);
                }
            }
        }
        for (AttributeDef attribute : recordClassDef.attributes) {
            String attributeName = identifier(attribute);

            if (droppedFields.containsKey(attributeName)) {
                droppedFields.remove(attributeName);
                continue;
            }

            if (updatedFields.containsKey(attributeName)) {
                AttributeDef adef = updatedFields.remove(attributeName).attributeDef;
                if (adef instanceof StaticAttributeDef) {
                    attribute = new StaticAttributeDef(
                            adef.location,
                            attribute.id,
                            adef.title,
                            adef.comment,
                            ((StaticAttributeDef) adef).type,
                            ((StaticAttributeDef) adef).value,
                            adef.tags
                    );
                } else {
                    attribute = new NonStaticAttributeDef(
                            adef.location,
                            attribute.id,
                            adef.title,
                            adef.comment,
                            ((NonStaticAttributeDef) adef).type,
                            ((NonStaticAttributeDef) adef).relativeId,
                            ((NonStaticAttributeDef) adef).defval,
                            adef.tags
                    );
                }
            }

            if (alteredFields.containsKey(attributeName)) {
                AlterFieldAlteration change = alteredFields.remove(attributeName);
                if (change.newName != null) {
                    registerFieldRename(classId, change.newName, attribute.id);
                }
                attribute = alterAttributeDef(attribute, change);
            }
            attributes.add(attribute);
        }

        if (alterClass.attributes != null) {
            for (AttributeDef ad : alterClass.attributes) {
                if (ad instanceof AddFieldAlteration) {
                    attributes.add(((AddFieldAlteration) ad).attributeDef);
                }
            }
        }

        if (!droppedFields.isEmpty()) {
            throw new CompilationException("Identifier specified in DROP FIELD doesn't exist.", droppedFields.entrySet().stream().findFirst().get().getValue());
        }

        if (!alteredFields.isEmpty()) {
            throw new CompilationException("Identifier specified in ALTER FIELD doesn't exist.", alteredFields.entrySet().stream().findFirst().get().getValue());
        }

        if (!updatedFields.isEmpty()) {
            throw new CompilationException("Identifier specified in UPDATE FIELD doesn't exist.", updatedFields.entrySet().stream().findFirst().get().getValue());
        }

        return alterRecordClassDef(recordClassDef, alterClass, attributes);
    }

    private EnumClassDef alterEnumClassDef(AlterEnum alterEnum, EnumClassDef enumClassDef) {
        String classId = identifier(alterEnum.id);
        if (alterEnum.newName != null) {
            registerTypeRename(identifier(alterEnum.newName), classId);
            classId = identifier(alterEnum.newName);
        }
        ArrayList<EnumValueDef> enumValueDefs = new ArrayList<>();
        Map<Identifier, Expression> changedEnumValues = new HashMap<>();
        Map<Identifier, Identifier> changedEnumNames = new HashMap<>();
        Set<Identifier> valueModified = new HashSet<>();
        Set<Identifier> nameModified = new HashSet<>();
        if (alterEnum.alterations != null)
            for (EnumValueAlteration alteration : alterEnum.alterations) {
                if (alteration.action.equals("ADD")) {
                    if (nameModified.contains(alteration.name)) {
                        throw new CompilationException("Duplicate name modification for the same identifier.", alteration.name);
                    }
                    if (valueModified.contains(alteration.name)) {
                        throw new CompilationException("Duplicate value modification for the same identifier.", alteration.name);
                    }
                    enumValueDefs.add(new EnumValueDef(alteration.name, alteration.newValue));
                    nameModified.add(alteration.name);
                    valueModified.add(alteration.name);
                }
                if (alteration.action.equals("VALUE")) {
                    if (valueModified.contains(alteration.name)) {
                        throw new CompilationException("Duplicate value modification for the same identifier.", alteration.name);
                    }
                    changedEnumValues.put(alteration.name, alteration.newValue);
                    valueModified.add(alteration.name);
                }
                if (alteration.action.equals("NAME")) {
                    if (nameModified.contains(alteration.name)) {
                        throw new CompilationException("Duplicate name modification for the same identifier.", alteration.name);
                    }
                    changedEnumNames.put(alteration.name, alteration.newName);
                    nameModified.add(alteration.name);
                }
                if (alteration.action.equals("DROP")) {
                    if (nameModified.contains(alteration.name)) {
                        throw new CompilationException("Duplicate name modification for the same identifier.", alteration.name);
                    }
                    if (valueModified.contains(alteration.name)) {
                        throw new CompilationException("Duplicate value modification for the same identifier.", alteration.name);
                    }
                    changedEnumNames.put(alteration.name, null);
                    nameModified.add(alteration.name);
                }
            }

        for (EnumValueDef value : enumClassDef.values) {
            if (changedEnumValues.containsKey(value.id) || changedEnumNames.containsKey(value.id)) {
                Identifier newName = value.id;
                Expression newValue = value.value;
                if (changedEnumValues.containsKey(value.id)) {
                    newValue = changedEnumValues.remove(value.id);
                }
                if (changedEnumNames.containsKey(value.id)) {
                    newName = changedEnumNames.remove(value.id);
                    if (newName != null) {
                        registerEnumValueRename(classId, newName.id, value.id.id);
                    }
                }
                if (newName != null) {
                    enumValueDefs.add(new EnumValueDef(newName, newValue));
                }
            } else {
                enumValueDefs.add(value);
            }
        }

        if (!changedEnumValues.isEmpty()) {
            throw new CompilationException("Identifier specified in SET VALUE doesn't exist.", changedEnumValues.entrySet().stream().findFirst().get().getKey());
        }

        if (!changedEnumNames.isEmpty()) {
            throw new CompilationException("Identifier specified in SET NAME doesn't exist.", changedEnumNames.entrySet().stream().findFirst().get().getKey());
        }

        enumValueDefs.sort(Comparator.comparingLong(o -> ((IntegerConstant) o.value).value));
        return new EnumClassDef(
            alterEnum.newName != null ? alterEnum.newName : enumClassDef.id,
            alterEnum.title != null ? alterEnum.title : enumClassDef.title,
            alterEnum.comment != null ? alterEnum.comment : enumClassDef.comment,
            false,
            enumValueDefs.toArray(new EnumValueDef[0])
        );
    }

    private RecordClassDef alterRecordClassDef(RecordClassDef rcdef, AlterClass alterClass, ArrayList<AttributeDef> attributes) {
        return new RecordClassDef(
            rcdef.location,
            alterClass.newName != null ? alterClass.newName : rcdef.id,
            alterClass.title != null ? alterClass.title : rcdef.title,
            alterClass.guid != null ? alterClass.guid : rcdef.guid,
            alterClass.comment != null ? alterClass.comment : rcdef.comment,
            alterClass.auxiliary != null ? alterClass.auxiliary : rcdef.auxiliary,
            alterClass.instantiable != null ? alterClass.instantiable : rcdef.instantiable,
            alterClass.parent != null ? alterClass.parent : rcdef.parent,
            attributes.toArray(new AttributeDef[0])
        );
    }

    private void compileDefaultResolutions(Map<String, Expression> resolutions, RecordClassDescriptor rcd) {
        RecordLayout sourceLayout = new RecordLayout(rcd);
        resolutions.forEach((name, value) -> {
            FieldLayout<?> fieldLayout = sourceLayout.getField(name);
            if (fieldLayout != null) {
                defaults.put(fieldLayout.getField(),
                    new ModifyFieldData(
                        ddlCompiler.compileConstant(value, fieldLayout.getField().getType()),
                        value.location
                    )
                );
            }
        });
    }

    private AttributeDef alterAttributeDef(AttributeDef ad, AlterFieldAlteration change) {
        if (change.isStatic != null) {
            if (change.isStatic) {
                return alterStaticAttributeDef(ad, change);
            } else {
                return alterNonStaticAttributeDef(ad, change);
            }
        } else {
            if (ad instanceof StaticAttributeDef) {
                return alterStaticAttributeDef(ad, change);
            } else {
                return alterNonStaticAttributeDef(ad, change);
            }
        }
    }

    private NonStaticAttributeDef alterNonStaticAttributeDef(AttributeDef ad, AlterFieldAlteration change) {
        return new NonStaticAttributeDef(
            change.location,
            change.newName != null ? change.newName : ad.id,
            change.title != null ? change.title : ad.title,
            change.comment != null ? change.comment : ad.comment,
            alterDataTypeSpec(AttributeDef.getTypeSpec(ad), change),
            change.relativeId != null ? change.relativeId : AttributeDef.getRelativeTo(ad),
            change.defval != null ? change.defval : AttributeDef.getDefaultValue(ad),
            change.tags != null ? change.tags : ad.tags
        );
    }

    private StaticAttributeDef alterStaticAttributeDef(AttributeDef ad, AlterFieldAlteration change) {
        return new StaticAttributeDef(
            change.location,
            change.newName != null ? change.newName : ad.id,
            change.title != null ? change.title : ad.title,
            change.comment != null ? change.comment : ad.comment,
            alterDataTypeSpec(AttributeDef.getTypeSpec(ad), change),
            change.value != null ? change.value : AttributeDef.getValue(ad),
            change.tags != null ? change.tags : ad.tags
        );
    }

    private DataTypeSpec alterDataTypeSpec(DataTypeSpec spec, AlterFieldAlteration change) {
        if (change.type != null) {
            if (change.type instanceof SimpleDataTypeSpec) {
                if (spec instanceof SimpleDataTypeSpec) {
                    // both actual and change types are simple, merge them
                    return new SimpleDataTypeSpec(
                        change.location,
                        ((SimpleDataTypeSpec) change.type).typeId,
                        change.isNull != null ? change.isNull : spec.nullable,
                        change.encoding,
                        (change.dimension == null ? 0 : change.dimension),
                        change.min != null ? change.min : ((SimpleDataTypeSpec) spec).min,
                        change.max != null ? change.max : ((SimpleDataTypeSpec) spec).max
                    );
                } else {
                    // actual is not simple, so use only change type
                    return new SimpleDataTypeSpec(
                        change.location,
                        ((SimpleDataTypeSpec) change.type).typeId,
                        change.isNull != null ? change.isNull : spec.nullable,
                        change.encoding,
                        (change.dimension == null ? 0 : change.dimension),
                        change.min,
                        change.max
                    );
                }
            } else if (change.type instanceof PolymorphicDataTypeSpec) {
                if (change.isNull != null && change.isNull != change.type.nullable) {
                    if (change.type instanceof ArrayDataTypeSpec) {
                        ArrayDataTypeSpec arrayType = (ArrayDataTypeSpec) change.type;
                        return new ArrayDataTypeSpec(arrayType.location, arrayType.elementsTypeSpec, change.isNull);
                    } else if (change.type instanceof ClassDataTypeSpec) {
                        ClassDataTypeSpec classType = (ClassDataTypeSpec) change.type;
                        return new ClassDataTypeSpec(classType.location, classType.elementsTypeSpec, change.isNull);
                    }
                }

                return change.type;
            } else {
                throw new CompilationException("Invalid type", change);
            }
        } else {
            if (spec instanceof SimpleDataTypeSpec) {
                // actual type is simple and change type is null, so type shouldn't be changed;
                // so change only non-type properties (encoding, dimension, etc.)
                return new SimpleDataTypeSpec(
                    change.location,
                    ((SimpleDataTypeSpec) spec).typeId,
                    change.isNull != null ? change.isNull : spec.nullable,
                    change.encoding != null ? change.encoding : ((SimpleDataTypeSpec) spec).encoding,
                    change.dimension != null ? change.dimension : ((SimpleDataTypeSpec) spec).dimension,
                    change.min != null ? change.min : ((SimpleDataTypeSpec) spec).min,
                    change.max != null ? change.max : ((SimpleDataTypeSpec) spec).max
                );
            } else {
                if (change.encoding != null) {
                    throw new CompilationException("Can't apply encoding for complex type", change);
                }
                if (change.dimension != null) {
                    throw new CompilationException("Can't apply dimension for complex type", change);
                }
                if (change.min != null) {
                    throw new CompilationException("Can't apply min for complex type", change);
                }
                if (change.max != null) {
                    throw new CompilationException("Can't apply max for complex type", change);
                }
                return spec;
            }
        }
    }

    private ClassDescriptor findOriginalCdAndFillTypeMapping(ClassDescriptor cd, String originalTypeName) {
        ClassDescriptor originalCd = null;
        for (ClassDescriptor streamCd : streamDescriptors) {
            if (Objects.equals(identifier(streamCd), originalTypeName)) {
                originalCd = streamCd;
                mapping.descriptors.put(originalCd.getGuid(), cd.getGuid());
            }
        }

        return originalCd != null ? originalCd : cd;
    }

    private void fillEnumValuesMapping(EnumClassDescriptor ecd, EnumClassDescriptor originalCd) {
        for (EnumValue ev : ecd.getValues()) {
            String originalEnumValueName = getOriginalEnumValueName(identifier(ecd), ev.symbol);
            if (originalEnumValueName != null) {
                EnumValue oldEnumValue = null;
                for (EnumValue evOriginal : originalCd.getValues()) {
                    if (Objects.equals(evOriginal.symbol, originalEnumValueName)) {
                        oldEnumValue = evOriginal;
                    }
                }
                if (oldEnumValue != null) {
                    mapping.enumValues.put(oldEnumValue, ev);
                } else {
                    throw new CompilationException("Enum symbol '" + originalEnumValueName + "' was not found in class " + originalCd.getName(), 0); // this should not happen
                }
            }
        }
    }

    private void fillFieldsMapping(RecordClassDescriptor rcd, RecordClassDescriptor originalCd) {
        for (DataField df : rcd.getFields()) {
            String originalFieldName = getOriginalFieldName(identifier(rcd), df.getName());
            if (originalFieldName != null) {
                DataField oldField = originalCd.getField(originalFieldName);
                if (oldField != null) {
                    mapping.fields.put(oldField, df);
                } else {
                    throw new CompilationException("Field '" + originalFieldName + "' was not found in class " + originalCd.getName(), 0); // this should not happen
                }
            }
        }
    }

    private void compileDependencies(ClassDef classDef) {
        if (classDef instanceof RecordClassDef) {
            RecordClassDef recordClassDef = (RecordClassDef) classDef;
            compileDependencies(recordClassDef.attributes, recordClassDef.parent);
        }
    }

    private void compileDependencies(AttributeDef[] attributes, TypeIdentifier parent) {
        for (AttributeDef attribute : attributes) {
            DataTypeSpec dataTypeSpec = AttributeDef.getTypeSpec(attribute);
            List<ClassDef> dependClasses = extractClassDefDependencies(dataTypeSpec);
            for (ClassDef cd : dependClasses) {
                compileAlterClassDef(cd);
            }
        }
        if (parent != null) {
            compileAlterClassDef(allTypes.get(identifier(parent)));
        }
    }

    private List<ClassDef> extractClassDefDependencies(DataTypeSpec dataTypeSpec) {
        if (dataTypeSpec instanceof SimpleDataTypeSpec) {
            return extractClassDefDependencies((SimpleDataTypeSpec) dataTypeSpec);
        } else if (dataTypeSpec instanceof PolymorphicDataTypeSpec) {
            PolymorphicDataTypeSpec classDataTypeSpec = (PolymorphicDataTypeSpec) dataTypeSpec;
            List<ClassDef> dependencies = new ArrayList<>();
            for (DataTypeSpec spec : classDataTypeSpec.elementsTypeSpec) {
                dependencies.addAll(extractClassDefDependencies(spec));
            }
            return dependencies;
        }

        return new ArrayList<>();
    }

    private List<ClassDef> extractClassDefDependencies(SimpleDataTypeSpec spec) {
        String typeName = identifier(spec);
        try {
            senv.lookUp(NamedObjectType.TYPE, typeName, spec.location);
        } catch (UnknownIdentifierException e) {
            ClassDef def = allTypes.get(typeName);
            return def != null ? List.of(def) : new ArrayList<>();
        }

        return new ArrayList<>();
    }

    private ClassDef[] getStreamClassDefs(DXTickStream stream) {
        String description = "CREATE " + stream.describe();
        Element el = CompilerUtil.parse(description);
        assert el instanceof CreateStreamStatement;
        CreateStreamStatement statement = (CreateStreamStatement) el;
        return statement.members;
    }

    private void collectAllTypes() {
        for (ClassDef type : streamClassDefs) {
            String typeName = identifier(type);
            if (!droppedTypes.containsKey(typeName)) {
                allTypes.put(typeName, type);
            }
        }
        for (ClassDef type : addedTypes.values()) {
            allTypes.put(identifier(type), type);
        }
    }

    private void preserveMultilines() {
        for (ClassDef cdef : streamClassDefs) {
            RecordClassDef rcdef;
            if (cdef instanceof RecordClassDef)
                rcdef = (RecordClassDef) cdef;
            else
                continue;

            RecordClassDescriptor cd = (RecordClassDescriptor) stream.getStreamOptions().getMetaData().getClassDescriptor(cdef.id.typeName);
            for (AttributeDef adef : rcdef.attributes) {
                if (adef instanceof StaticAttributeDef) {
                    StaticAttributeDef sadef = (StaticAttributeDef) adef;
                    if (sadef.type instanceof SimpleDataTypeSpec) {
                        DataField dfield = cd.getField(adef.id);
                        if (dfield == null) {
                            continue;
                        }
                        DataType dtype = dfield.getType();
                        if (dtype instanceof VarcharDataType) {
                            ((SimpleDataTypeSpec) sadef.type).multiline = ((VarcharDataType) dtype).isMultiLine();
                        }
                    }
                } else {
                    NonStaticAttributeDef nsadef = (NonStaticAttributeDef) adef;
                    if (nsadef.type instanceof SimpleDataTypeSpec) {
                        DataField dfield = cd.getField(adef.id);
                        if (dfield == null) {
                            continue;
                        }
                        DataType dtype = dfield.getType();
                        if (dtype instanceof VarcharDataType) {
                            ((SimpleDataTypeSpec) nsadef.type).multiline = ((VarcharDataType) dtype).isMultiLine();
                        }
                    }
                }
            }
        }
    }

    private void registerEnumValueRename(String enumName, String oldEnumValueName, String newEnumValueName) {
        newEnumValueNameToOriginal.put(getFullEnumValueKey(enumName, oldEnumValueName), newEnumValueName);
    }

    private String getOriginalEnumValueName(String enumName, String oldEnumValueName) {
        return newEnumValueNameToOriginal.get(getFullEnumValueKey(enumName, oldEnumValueName));
    }

    private String getFullEnumValueKey(String className, String fieldName) {
        return className.toUpperCase() + "." + fieldName.toUpperCase();
    }

    private void registerFieldRename(String className, String oldFieldName, String newFieldName) {
        newFieldNameToOriginal.put(getFullFieldKey(className, oldFieldName), newFieldName);
    }

    private String getOriginalFieldName(String className, String oldFieldName) {
        return newFieldNameToOriginal.get(getFullFieldKey(className, oldFieldName));
    }

    private String getFullFieldKey(String className, String fieldName) {
        return className.toUpperCase() + "." + fieldName.toUpperCase();
    }

    private void registerTypeRename(String newTypeName, String oldTypeName) {
        newTypeNameToOriginal.put(newTypeName, oldTypeName);
    }

    private String identifier(ClassDef classDef) {
        return classDef.id.typeName.toUpperCase();
    }

    private String identifier(SimpleDataTypeSpec spec) {
        return spec.typeId.typeName.toUpperCase();
    }

    private String identifier(TypeIdentifier identifier) {
        return identifier.typeName.toUpperCase();
    }

    private String identifier(AttributeDef attribute) {
        return attribute.id.toUpperCase();
    }

    private String identifier(ClassDescriptor cd) {
        return cd.getName().toUpperCase();
    }
}
