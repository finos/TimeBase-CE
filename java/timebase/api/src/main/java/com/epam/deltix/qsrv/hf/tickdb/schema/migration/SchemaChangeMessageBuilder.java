package com.epam.deltix.qsrv.hf.tickdb.schema.migration;

import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.EnumClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.schema.*;
import com.epam.deltix.timebase.messages.schema.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.collections.generated.ObjectList;
import rtmath.containers.ObjObjPair;

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

        ObjectArrayList<ClassDescriptorInfo> newState = convert(targetDescriptors);
        ObjectArrayList<ClassDescriptorInfo> previousState = convert(sourceDescriptors);

        changeMessage.setNewState(newState);
        changeMessage.setPreviousState(previousState);
        changeMessage.setSymbol(symbol);
        changeMessage.setTimeStampMs(timestamp);

        ObjectArrayList<SchemaDescriptorChangeActionInfo> actions = new ObjectArrayList<>();

        Map<String, ? extends ClassDescriptorInfo> newStateMap = toMap(newState);
        Map<String, ? extends ClassDescriptorInfo> previousStateMap = toMap(previousState);

        List<? extends ClassDescriptorInfo> descriptorsToAdd = getDescriptorsToAdd(newStateMap, previousStateMap);
        List<? extends ClassDescriptorInfo> descriptorsToRemove = getDescriptorsToRemove(newStateMap, previousStateMap);
        List<ObjObjPair<? extends ClassDescriptorInfo, ? extends ClassDescriptorInfo>> descriptorsToAlter = getDescriptorsToAlter(newStateMap, previousStateMap);
        List<ObjObjPair<RecordClassDescriptorInfo, RecordClassDescriptorInfo>> descriptorsWithDisabledContentClass = getDescriptorsWithDisabledContentClass(descriptorsToAlter);
        //mutable
        List<ObjObjPair<? extends ClassDescriptorInfo, ? extends ClassDescriptorInfo>> descriptorsToRename = getDescriptorsToRename(descriptorsToAdd, descriptorsToRemove);

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
            ClassDescriptorInfo newDescriptor = descriptorPair.getFirst();
            ClassDescriptorInfo previousDescriptor = descriptorPair.getSecond();

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

    private ObjectArrayList<ClassDescriptorInfo> convert(RecordClassSet recordClassSet) {
        ObjectArrayList<ClassDescriptorInfo> descriptors = new ObjectArrayList<>();

        if (recordClassSet != null && recordClassSet.getClassDescriptors() != null) {
            deltix.qsrv.hf.pub.md.ClassDescriptor[] contentClasses = recordClassSet.getClassDescriptors();
            for (int i = 0; i < contentClasses.length; i++) {
                ClassDescriptor contentClass = contentClasses[i];
                if (contentClass instanceof RecordClassDescriptor) {
                    deltix.timebase.messages.schema.RecordClassDescriptor recordClassDescriptor = map((RecordClassDescriptor) contentClass);

                    if (recordClassSet.getContentClass(contentClass.getGuid()) != null) {
                        recordClassDescriptor.setIsContentClass(true);
                    }
                    descriptors.add(recordClassDescriptor);
                } else {
                    descriptors.add(DescriptorToMessageMapper.map((EnumClassDescriptor) contentClass));
                }
            }
        }

        return descriptors;
    }

    private Map<String, ? extends ClassDescriptorInfo> toMap(ObjectArrayList<? extends ClassDescriptorInfo> descriptors) {
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

    private List<? extends ClassDescriptorInfo> getDescriptorsToAdd(Map<String, ? extends ClassDescriptorInfo> newState,
                                                                Map<String, ? extends ClassDescriptorInfo> previousState) {
        return newState.entrySet()
                .stream()
                .filter(entry -> !previousState.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    private List<? extends ClassDescriptorInfo> getDescriptorsToRemove(Map<String, ? extends ClassDescriptorInfo> newState,
                                                                   Map<String, ? extends ClassDescriptorInfo> previousState) {
        return previousState.entrySet()
                .stream()
                .filter(entry -> !newState.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    private List<ObjObjPair<? extends ClassDescriptorInfo, ? extends ClassDescriptorInfo>> getDescriptorsToAlter(Map<String, ? extends ClassDescriptorInfo> newState,
                                                                  Map<String, ? extends ClassDescriptorInfo> previousState) {
        List<ObjObjPair<? extends ClassDescriptorInfo, ? extends ClassDescriptorInfo>> result = new ArrayList<>();
        newState.forEach((key, value) -> {
            if (previousState.containsKey(key)) {
                if (!previousState.get(key).equals(value)) {
                    result.add(new ObjObjPair<>(value, previousState.get(key)));
                }
            }
        });

        return result;
    }

    private List<ObjObjPair<RecordClassDescriptorInfo, RecordClassDescriptorInfo>> getDescriptorsWithDisabledContentClass(
            List<ObjObjPair<? extends ClassDescriptorInfo, ? extends ClassDescriptorInfo>> descriptorsToAlter) {
        List<ObjObjPair<RecordClassDescriptorInfo, RecordClassDescriptorInfo>> result = new ArrayList<>();
        descriptorsToAlter.forEach(pair -> {
            ClassDescriptorInfo newState = pair.getFirst();
            ClassDescriptorInfo previousState = pair.getSecond();

            if (newState instanceof deltix.timebase.messages.schema.RecordClassDescriptor
                    && previousState instanceof deltix.timebase.messages.schema.RecordClassDescriptor) {
                deltix.timebase.messages.schema.RecordClassDescriptor newRCD = (deltix.timebase.messages.schema.RecordClassDescriptor) newState;
                deltix.timebase.messages.schema.RecordClassDescriptor prevRCD = (deltix.timebase.messages.schema.RecordClassDescriptor) previousState;

                if (!newRCD.isContentClass() && prevRCD.isContentClass()) {
                    result.add(new ObjObjPair<>(newRCD, prevRCD));
                }
            }
        });

        return result;
    }

    // mutable
    private List<ObjObjPair<? extends ClassDescriptorInfo, ? extends ClassDescriptorInfo>> getDescriptorsToRename(List<? extends ClassDescriptorInfo> descriptorsToAdd,
                                                                              List<? extends ClassDescriptorInfo> descriptorsToRemove) {
        List<ObjObjPair<? extends ClassDescriptorInfo, ? extends ClassDescriptorInfo>> descriptorsToRename = new ArrayList<>();

        descriptorsToAdd.forEach(descriptorToAdd -> {
            if (descriptorToAdd instanceof RecordClassDescriptorInfo) {
                ObjectList<DataFieldInfo> dataFieldsToAdd = ((RecordClassDescriptorInfo) descriptorToAdd).getDataFields();
                descriptorsToRemove.forEach(descriptorToRemove -> {
                    ObjectList<DataFieldInfo> dataFieldsToRemove = ((RecordClassDescriptorInfo) descriptorToRemove).getDataFields();

                    if (dataFieldsToAdd.equals(dataFieldsToRemove)) {
                        descriptorsToRename.add(new ObjObjPair<>(descriptorToAdd, descriptorToRemove));
                    }
                });
            } else {
                ObjectList<EnumValueInfo> valuesToAdd = ((EnumClassDescriptorInfo) descriptorToAdd).getValues();
                descriptorsToRemove.forEach(descriptorToRemove -> {
                    ObjectList<EnumValueInfo> valuesToRemove = ((EnumClassDescriptorInfo) descriptorToRemove).getValues();

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

    private SchemaDescriptorChangeActionInfo buildAddDescriptorChangeAction(ClassDescriptorInfo descriptor) {
        SchemaDescriptorChangeAction action = new SchemaDescriptorChangeAction();
        action.setNewState(descriptor);
        action.setChangeTypes(SchemaDescriptorChangeType.ADD);

        return action;
    }

    private SchemaDescriptorChangeActionInfo buildRemoveDescriptorAction(ClassDescriptorInfo descriptor) {
        SchemaDescriptorChangeAction action = new SchemaDescriptorChangeAction();
        action.setPreviousState(descriptor);
        action.setChangeTypes(SchemaDescriptorChangeType.DELETE);

        return action;
    }

    private SchemaDescriptorChangeActionInfo buildRenameDescriptorAction(ObjObjPair<? extends ClassDescriptorInfo, ? extends ClassDescriptorInfo> descriptorPair) {
        SchemaDescriptorChangeAction action = new SchemaDescriptorChangeAction();
        action.setNewState(descriptorPair.getFirst());
        action.setPreviousState(descriptorPair.getSecond());
        action.setChangeTypes(SchemaDescriptorChangeType.RENAME);

        return action;
    }

    private SchemaDescriptorChangeActionInfo buildDropRecordsDescriptorAction(ObjObjPair<RecordClassDescriptorInfo, RecordClassDescriptorInfo> descriptorPair) {
        SchemaDescriptorChangeAction action = new SchemaDescriptorChangeAction();
        action.setNewState(descriptorPair.getFirst());
        action.setPreviousState(descriptorPair.getSecond());
        action.setChangeTypes(SchemaDescriptorChangeType.CONTENT_TYPE_CHANGE);
        SchemaDescriptorTransformation transformation = new SchemaDescriptorTransformation();
        transformation.setTransformationType(SchemaDescriptorTransformationType.DROP_RECORD);
        action.setDescriptorTransformation(transformation);

        return action;
    }

    private ObjectArrayList<SchemaFieldChangeActionInfo> buildFieldsChangeAction(ClassDescriptorChange descriptorChange) {
        ObjectArrayList<SchemaFieldChangeActionInfo> actions = new ObjectArrayList<>();

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

    private SchemaFieldChangeActionInfo build(CreateFieldChange change) {
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

    private SchemaFieldChangeActionInfo build(DeleteFieldChange change) {
        SchemaFieldChangeAction action = new SchemaFieldChangeAction();
        action.setChangeTypes(SchemaFieldChangeType.DELETE);
        action.setNewState(map(change.getTarget()));
        action.setPreviousState(map(change.getSource()));

        return action;
    }

    private SchemaFieldChangeActionInfo build(FieldChange change) {
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

    private SchemaFieldChangeActionInfo build(FieldModifierChange change) {
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

    private SchemaFieldChangeActionInfo build(FieldPositionChange change) {
        SchemaFieldChangeAction action = new SchemaFieldChangeAction();
        action.setChangeTypes(SchemaFieldChangeType.ORDINAL_CHANGE);
        action.setNewState(map(change.getTarget()));
        action.setPreviousState(map(change.getSource()));

        return action;
    }

    private SchemaFieldChangeActionInfo build(StaticFieldChange change) {
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

    private SchemaFieldChangeActionInfo build(FieldRelationChange change) {
        SchemaFieldChangeAction action = new SchemaFieldChangeAction();
        action.setChangeTypes(SchemaFieldChangeType.RELATION_CHANGE);
        action.setNewState(map(change.getTarget()));
        action.setPreviousState(map(change.getSource()));

        return action;
    }

    private SchemaFieldChangeActionInfo build(FieldTypeChange change) {
        SchemaFieldChangeAction action = new SchemaFieldChangeAction();
        action.setChangeTypes(SchemaFieldChangeType.DATA_TYPE_CHANGE);
        action.setNewState(map(change.getTarget()));
        action.setPreviousState(map(change.getSource()));

        if (change.getResolution() != null && ErrorResolution.Result.Ignored.equals(change.getResolution().result)) {
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

    private SchemaFieldChangeActionInfo build(FieldValueChange change) {
        SchemaFieldChangeAction action = new SchemaFieldChangeAction();
        action.setChangeTypes(SchemaFieldChangeType.STATIC_VALUE_CHANGE);
        action.setNewState(map(change.getTarget()));
        action.setPreviousState(map(change.getSource()));

        return action;
    }
}
