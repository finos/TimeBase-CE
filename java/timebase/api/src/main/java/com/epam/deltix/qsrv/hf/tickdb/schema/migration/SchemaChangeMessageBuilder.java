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
package com.epam.deltix.qsrv.hf.tickdb.schema.migration;

import com.epam.deltix.containers.ObjObjPair;
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.schema.*;
import com.epam.deltix.timebase.messages.schema.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.collections.generated.ObjectList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.epam.deltix.qsrv.hf.tickdb.schema.migration.DescriptorToMessageMapper.map;

public class SchemaChangeMessageBuilder {

    public SchemaChangeMessage build(StreamMetaDataChange metaDataChange) {
        return build(metaDataChange, "@SYSTEM", System.currentTimeMillis());
    }

    public SchemaChangeMessage build(StreamMetaDataChange metaDataChange, String symbol, long timestamp) {
        if (metaDataChange == null) {
            throw new IllegalArgumentException("Impossible to build SchemaChangeMessage without StreamMetaDataChange");
        }

        SchemaChangeMessage changeMessage = new SchemaChangeMessage();

        RecordClassSet targetDescriptors = metaDataChange.getMetaData();
        Map<String, ? extends ClassDescriptor> targetDescriptorsMap = toMap(targetDescriptors);
        RecordClassSet sourceDescriptors = metaDataChange.getSource();
        Map<String, ? extends ClassDescriptor> sourceDescriptorsMap = toMap(sourceDescriptors);

        ObjectArrayList<UniqueDescriptor> newState = DescriptorToMessageMapper.convert(targetDescriptors);
        ObjectArrayList<UniqueDescriptor> previousState = DescriptorToMessageMapper.convert(sourceDescriptors);

        changeMessage.setNewState(newState);
        changeMessage.setPreviousState(previousState);
        changeMessage.setSymbol(symbol);
        changeMessage.setTimeStampMs(timestamp);

        ObjectArrayList<SchemaDescriptorChangeAction> actions = new ObjectArrayList<>();

        Map<String, ? extends UniqueDescriptor> newStateMap = toMap(newState);
        Map<String, ? extends UniqueDescriptor> previousStateMap = toMap(previousState);

        List<? extends UniqueDescriptor> descriptorsToAdd = getDescriptorsToAdd(newStateMap, previousStateMap);
        List<? extends UniqueDescriptor> descriptorsToRemove = getDescriptorsToRemove(newStateMap, previousStateMap);
        List<ObjObjPair<? extends UniqueDescriptor, ? extends UniqueDescriptor>> descriptorsToAlter = getDescriptorsToAlter(newStateMap, previousStateMap);
        List<ObjObjPair<TypeDescriptor, TypeDescriptor>> descriptorsWithDisabledContentClass = getDescriptorsWithDisabledContentClass(descriptorsToAlter);
        //mutable
        List<ObjObjPair<? extends UniqueDescriptor, ? extends UniqueDescriptor>> descriptorsToRename = getDescriptorsToRename(descriptorsToAdd, descriptorsToRemove);

        // add new descriptors
        actions.addAll(
                descriptorsToAdd.stream()
                        .map(this::buildAddDescriptorChangeAction)
                        .collect(Collectors.toList())
        );
        // remove descriptors
        actions.addAll(
                descriptorsToRemove.stream()
                        .map(this::buildRemoveDescriptorAction)
                        .collect(Collectors.toList())
        );
        // rename descriptors
        actions.addAll(
                descriptorsToRename.stream()
                        .map(this::buildRenameDescriptorAction)
                        .collect(Collectors.toList())
        );
        // descriptors with disabled content class
        actions.addAll(
                descriptorsWithDisabledContentClass.stream()
                .map(this::buildDropRecordsDescriptorAction)
                .collect(Collectors.toList())
        );
        // alter descriptors
        descriptorsToAlter.forEach(descriptorPair -> {
            UniqueDescriptor newDescriptor = descriptorPair.getFirst();
            UniqueDescriptor previousDescriptor = descriptorPair.getSecond();

            ClassDescriptorChange descriptorChange = metaDataChange.getChange(
                    sourceDescriptorsMap.get(previousDescriptor.getName()),
                    targetDescriptorsMap.get(newDescriptor.getName())
            );

            if (descriptorChange != null) {
                SchemaDescriptorChangeAction action = new SchemaDescriptorChangeAction();
                action.setNewState(newDescriptor);
                action.setPreviousState(previousDescriptor);
                action.setChangeTypes(SchemaDescriptorChangeType.FIELDS_CHANGE);
                action.setFieldChangeActions(buildFieldsChangeAction(descriptorChange));
                actions.add(action);
            }
        });

        changeMessage.setDescriptorChangeActions(actions);

        return changeMessage;
    }

    @Deprecated
    public static ObjectArrayList<UniqueDescriptor> convert(RecordClassSet recordClassSet) {
        return DescriptorToMessageMapper.convert(recordClassSet);
    }

    private Map<String, ? extends UniqueDescriptor> toMap(ObjectArrayList<? extends UniqueDescriptor> descriptors) {
        return descriptors.stream()
                .collect(Collectors.toMap(entity -> entity.getName().toString(), Function.identity()));
    }

    private Map<String, ? extends ClassDescriptor> toMap(RecordClassSet recordClassSet) {
        Map<String, ClassDescriptor> map = new HashMap<>();

        if (recordClassSet != null && recordClassSet.getClassDescriptors() != null) {
            ClassDescriptor[] descriptors = recordClassSet.getClassDescriptors();
            for (int i = 0; i < descriptors.length; i++) {
                ClassDescriptor contentClass = descriptors[i];
                map.put(contentClass.getName(), contentClass);
            }
        }

        return map;
    }

    private List<? extends UniqueDescriptor> getDescriptorsToAdd(Map<String, ? extends UniqueDescriptor> newState,
                                                                Map<String, ? extends UniqueDescriptor> previousState) {
        return newState.entrySet()
                .stream()
                .filter(entry -> !previousState.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    private List<? extends UniqueDescriptor> getDescriptorsToRemove(Map<String, ? extends UniqueDescriptor> newState,
                                                                   Map<String, ? extends UniqueDescriptor> previousState) {
        return previousState.entrySet()
                .stream()
                .filter(entry -> !newState.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    private List<ObjObjPair<? extends UniqueDescriptor, ? extends UniqueDescriptor>> getDescriptorsToAlter(Map<String, ? extends UniqueDescriptor> newState,
                                                                  Map<String, ? extends UniqueDescriptor> previousState) {
        List<ObjObjPair<? extends UniqueDescriptor, ? extends UniqueDescriptor>> result = new ArrayList<>();
        newState.forEach((key, value) -> {
            if (previousState.containsKey(key)) {
                if (!previousState.get(key).equals(value)) {
                    result.add(new ObjObjPair<>(value, previousState.get(key)));
                }
            }
        });

        return result;
    }

    private List<ObjObjPair<TypeDescriptor, TypeDescriptor>> getDescriptorsWithDisabledContentClass(
            List<ObjObjPair<? extends UniqueDescriptor, ? extends UniqueDescriptor>> descriptorsToAlter) {
        List<ObjObjPair<TypeDescriptor, TypeDescriptor>> result = new ArrayList<>();
        descriptorsToAlter.forEach(pair -> {
            UniqueDescriptor newState = pair.getFirst();
            UniqueDescriptor previousState = pair.getSecond();

            if (newState instanceof TypeDescriptor
                    && previousState instanceof TypeDescriptor) {
                TypeDescriptor newRCD = (TypeDescriptor) newState;
                TypeDescriptor prevRCD = (TypeDescriptor) previousState;

                if (!newRCD.isContentClass() && prevRCD.isContentClass()) {
                    result.add(new ObjObjPair<>(newRCD, prevRCD));
                }
            }
        });

        return result;
    }

    // mutable
    private List<ObjObjPair<? extends UniqueDescriptor, ? extends UniqueDescriptor>> getDescriptorsToRename(List<? extends UniqueDescriptor> descriptorsToAdd,
                                                                              List<? extends UniqueDescriptor> descriptorsToRemove) {
        List<ObjObjPair<? extends UniqueDescriptor, ? extends UniqueDescriptor>> descriptorsToRename = new ArrayList<>();

        descriptorsToAdd.forEach(descriptorToAdd -> {
            if (descriptorToAdd instanceof TypeDescriptor) {
                ObjectList<Field> dataFieldsToAdd = ((TypeDescriptor) descriptorToAdd).getFields();
                descriptorsToRemove.forEach(descriptorToRemove -> {
                    ObjectList<Field> dataFieldsToRemove = ((TypeDescriptor) descriptorToRemove).getFields();

                    if (dataFieldsToAdd.equals(dataFieldsToRemove)) {
                        descriptorsToRename.add(new ObjObjPair<>(descriptorToAdd, descriptorToRemove));
                    }
                });
            } else if (descriptorToAdd instanceof EnumDescriptor) {
                ObjectList<EnumConstant> valuesToAdd = ((EnumDescriptor) descriptorToAdd).getValues();
                descriptorsToRemove.forEach(descriptorToRemove -> {
                    ObjectList<EnumConstant> valuesToRemove = ((EnumDescriptor) descriptorToRemove).getValues();

                    if (valuesToAdd.equals(valuesToRemove)) {
                        descriptorsToRename.add(new ObjObjPair<>(descriptorToAdd, descriptorToRemove));
                    }
                });
            }
        });

        if (!descriptorsToRename.isEmpty()) {
            descriptorsToRename.forEach(pair -> {
                descriptorsToAdd.remove(pair.getFirst());
                descriptorsToRemove.remove(pair.getSecond());
            });
        }
        return descriptorsToRename;
    }

    private SchemaDescriptorChangeAction buildAddDescriptorChangeAction(UniqueDescriptor descriptor) {
        SchemaDescriptorChangeAction action = new SchemaDescriptorChangeAction();
        action.setNewState(descriptor);
        action.setChangeTypes(SchemaDescriptorChangeType.ADD);

        return action;
    }

    private SchemaDescriptorChangeAction buildRemoveDescriptorAction(UniqueDescriptor descriptor) {
        SchemaDescriptorChangeAction action = new SchemaDescriptorChangeAction();
        action.setPreviousState(descriptor);
        action.setChangeTypes(SchemaDescriptorChangeType.DELETE);

        return action;
    }

    private SchemaDescriptorChangeAction buildRenameDescriptorAction(ObjObjPair<? extends UniqueDescriptor, ? extends UniqueDescriptor> descriptorPair) {
        SchemaDescriptorChangeAction action = new SchemaDescriptorChangeAction();
        action.setNewState(descriptorPair.getFirst());
        action.setPreviousState(descriptorPair.getSecond());
        action.setChangeTypes(SchemaDescriptorChangeType.RENAME);

        return action;
    }

    private SchemaDescriptorChangeAction buildDropRecordsDescriptorAction(ObjObjPair<TypeDescriptor, TypeDescriptor> descriptorPair) {
        SchemaDescriptorChangeAction action = new SchemaDescriptorChangeAction();
        action.setNewState(descriptorPair.getFirst());
        action.setPreviousState(descriptorPair.getSecond());
        action.setChangeTypes(SchemaDescriptorChangeType.CONTENT_TYPE_CHANGE);
        SchemaDescriptorTransformation transformation = new SchemaDescriptorTransformation();
        transformation.setTransformationType(SchemaDescriptorTransformationType.DROP_RECORD);
        action.setDescriptorTransformation(transformation);

        return action;
    }

    private ObjectArrayList<SchemaFieldChangeAction> buildFieldsChangeAction(ClassDescriptorChange descriptorChange) {
        ObjectArrayList<SchemaFieldChangeAction> actions = new ObjectArrayList<>();

        AbstractFieldChange[] fieldsChanges = descriptorChange.getChanges();
        for (AbstractFieldChange fieldChange : fieldsChanges) {

            if (fieldChange instanceof CreateFieldChange) {
                actions.add(build((CreateFieldChange) fieldChange));
            } else if (fieldChange instanceof DeleteFieldChange) {
                actions.add(build((DeleteFieldChange) fieldChange));
            } else if (fieldChange instanceof FieldModifierChange) {
                actions.add(build((FieldModifierChange) fieldChange));
            } else if (fieldChange instanceof FieldPositionChange) {
                actions.add(build((FieldPositionChange) fieldChange));
            } else if (fieldChange instanceof StaticFieldChange) {
                actions.add(build((StaticFieldChange) fieldChange));
            } else if (fieldChange instanceof FieldRelationChange) {
                actions.add(build((FieldRelationChange) fieldChange));
            } else if (fieldChange instanceof FieldTypeChange) {
                actions.add(build((FieldTypeChange) fieldChange));
            } else if (fieldChange instanceof FieldValueChange) {
                actions.add(build((FieldValueChange) fieldChange));
            } else if (fieldChange instanceof FieldChange) {
                actions.add(build((FieldChange) fieldChange));
            } else {
                throw new IllegalArgumentException("Unsupported field change " + fieldChange.getClass().getName());
            }
        }

        return actions;
    }

    private SchemaFieldChangeAction build(CreateFieldChange change) {
        SchemaFieldChangeAction action = new SchemaFieldChangeAction();
        action.setChangeTypes(SchemaFieldChangeType.ADD);
        action.setNewState(map(change.getTarget()));
        action.setPreviousState(map(change.getSource()));
        if (change.getInitialValue() != null) {
            SchemaFieldDataTransformation transformation = new SchemaFieldDataTransformation();
            transformation.setDefaultValue(change.getInitialValue());
            transformation.setTransformationType(SchemaFieldDataTransformationType.SET_DEFAULT);

            action.setDataTransformation(transformation);
        }

        return action;
    }

    private SchemaFieldChangeAction build(DeleteFieldChange change) {
        SchemaFieldChangeAction action = new SchemaFieldChangeAction();
        action.setChangeTypes(SchemaFieldChangeType.DELETE);
        action.setNewState(map(change.getTarget()));
        action.setPreviousState(map(change.getSource()));

        return action;
    }

    private SchemaFieldChangeAction build(FieldChange change) {
        SchemaFieldChangeAction action = new SchemaFieldChangeAction();
        action.setNewState(map(change.getTarget()));
        action.setPreviousState(map(change.getSource()));

        FieldAttribute changeAttribute = change.getAttribute();
        switch (changeAttribute) {
            case Name:
                action.setChangeTypes(SchemaFieldChangeType.RENAME);
                break;
            case Title:
                action.setChangeTypes(SchemaFieldChangeType.TITLE_CHANGE);
                break;
            case Description:
                action.setChangeTypes(SchemaFieldChangeType.DESCRIPTION_CHANGE);
                break;
            default:
                throw new IllegalArgumentException("Could not build FieldChange action with FieldAttribute:" + changeAttribute);
        }

        return action;
    }

    private SchemaFieldChangeAction build(FieldModifierChange change) {
        SchemaFieldChangeAction action = new SchemaFieldChangeAction();
        action.setChangeTypes(SchemaFieldChangeType.MODIFIER_CHANGE);
        action.setNewState(map(change.getTarget()));
        action.setPreviousState(map(change.getSource()));
        if (change.getInitialValue() != null) {
            SchemaFieldDataTransformation transformation = new SchemaFieldDataTransformation();
            transformation.setDefaultValue(change.getInitialValue());
            transformation.setTransformationType(SchemaFieldDataTransformationType.SET_DEFAULT);

            action.setDataTransformation(transformation);
        }

        return action;
    }

    private SchemaFieldChangeAction build(FieldPositionChange change) {
        SchemaFieldChangeAction action = new SchemaFieldChangeAction();
        action.setChangeTypes(SchemaFieldChangeType.ORDINAL_CHANGE);
        action.setNewState(map(change.getTarget()));
        action.setPreviousState(map(change.getSource()));

        return action;
    }

    private SchemaFieldChangeAction build(StaticFieldChange change) {
        SchemaFieldChangeAction action = new SchemaFieldChangeAction();
        action.setChangeTypes(SchemaFieldChangeType.STATIC_VALUE_CHANGE);
        action.setNewState(map(change.getTarget()));
        action.setPreviousState(map(change.getSource()));
        if (change.getDefaultValue() != null) {
            SchemaFieldDataTransformation transformation = new SchemaFieldDataTransformation();
            transformation.setDefaultValue(change.getDefaultValue());
            transformation.setTransformationType(SchemaFieldDataTransformationType.SET_DEFAULT);

            action.setDataTransformation(transformation);
        }

        return action;
    }

    private SchemaFieldChangeAction build(FieldRelationChange change) {
        SchemaFieldChangeAction action = new SchemaFieldChangeAction();
        action.setChangeTypes(SchemaFieldChangeType.RELATION_CHANGE);
        action.setNewState(map(change.getTarget()));
        action.setPreviousState(map(change.getSource()));

        return action;
    }

    private SchemaFieldChangeAction build(FieldTypeChange change) {
        SchemaFieldChangeAction action = new SchemaFieldChangeAction();
        action.setChangeTypes(SchemaFieldChangeType.DATA_TYPE_CHANGE);
        action.setNewState(map(change.getTarget()));
        action.setPreviousState(map(change.getSource()));

        ErrorResolution result = null; //change.getResolution();

        if (result != null && result.result == ErrorResolution.Result.Ignored) {
            SchemaFieldDataTransformation transformation = new SchemaFieldDataTransformation();
            transformation.setTransformationType(SchemaFieldDataTransformationType.DROP_RECORD);

            action.setDataTransformation(transformation);
        } else if (change.isDefaultValueRequired()) {
            SchemaFieldDataTransformation transformation = new SchemaFieldDataTransformation();
            transformation.setDefaultValue(change.getDefaultValue());
            transformation.setTransformationType(SchemaFieldDataTransformationType.SET_DEFAULT);

            action.setDataTransformation(transformation);
        } else {
            SchemaFieldDataTransformation transformation = new SchemaFieldDataTransformation();
            transformation.setDefaultValue(change.getDefaultValue());
            transformation.setTransformationType(SchemaFieldDataTransformationType.CONVERT_DATA);

            action.setDataTransformation(transformation);
        }

        return action;
    }

    private SchemaFieldChangeAction build(FieldValueChange change) {
        SchemaFieldChangeAction action = new SchemaFieldChangeAction();
        action.setChangeTypes(SchemaFieldChangeType.STATIC_VALUE_CHANGE);
        action.setNewState(map(change.getTarget()));
        action.setPreviousState(map(change.getSource()));

        return action;
    }
}
