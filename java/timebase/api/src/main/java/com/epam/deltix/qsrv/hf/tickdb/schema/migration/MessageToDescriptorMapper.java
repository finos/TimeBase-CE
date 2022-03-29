package com.epam.deltix.qsrv.hf.tickdb.schema.migration;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.timebase.messages.schema.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.collections.generated.ObjectList;

import java.util.*;

public class MessageToDescriptorMapper {

    public static RecordClassSet convert(ObjectArrayList<UniqueDescriptor> descriptorsInfo) {
        List<RecordClassDescriptor> contentClasses = new ArrayList<>();
        Map<String, ClassDescriptor> descriptorsMap = new HashMap<>();
        ClassDescriptor[] descriptors = new ClassDescriptor[descriptorsInfo.size()];
        for (int i = 0; i < descriptorsInfo.size(); i++) {
            UniqueDescriptor info = descriptorsInfo.get(i);
            if (info instanceof TypeDescriptor) {
                RecordClassDescriptor rcd = map(descriptorsMap, (TypeDescriptor) info, descriptorsInfo);
                if (((TypeDescriptor) info).isContentClass()) {
                    contentClasses.add(rcd);
                }
                descriptors[i] = rcd;
            } else if (info instanceof EnumDescriptor) {
                EnumClassDescriptor ecd = map(descriptorsMap, (EnumDescriptor) info);
                descriptors[i] = ecd;
            }
        }

        RecordClassSet classSet = new RecordClassSet();
        classSet.setClassDescriptors(descriptors);
        classSet.addContentClasses(contentClasses.toArray(new RecordClassDescriptor[contentClasses.size()]));

        return classSet;
    }

    public static RecordClassDescriptor map(Map<String, ClassDescriptor> descriptorsMap,
                                            TypeDescriptor descriptorInfo,
                                            ObjectArrayList<UniqueDescriptor> allDescriptors) {
        String name = descriptorInfo.getName().toString();
        if (descriptorsMap.containsKey(name)) {
            return (RecordClassDescriptor) descriptorsMap.get(name);
        }

        ObjectList<Field> dataFields = descriptorInfo.getFields();
        DataField[] fields = new DataField[dataFields.size()];

        String title = descriptorInfo.getTitle() == null ? null : descriptorInfo.getTitle().toString();
        boolean isAbstract = descriptorInfo.isAbstract();
        RecordClassDescriptor parent = descriptorInfo.getParent() == null ? null : map(descriptorsMap, descriptorInfo.getParent(), allDescriptors);

        RecordClassDescriptor descriptor;
        if (descriptorInfo.hasGuid()) {
            descriptor = new RecordClassDescriptor(
                    descriptorInfo.getGuid().toString(),
                    name,
                    title,
                    isAbstract,
                    parent
            );
        } else {
            descriptor = new RecordClassDescriptor(
                    name,
                    title,
                    isAbstract,
                    parent
            );
        }

        descriptorsMap.put(descriptor.getName(), descriptor);

        for (int i = 0; i < dataFields.size(); i++) {
            fields[i] = map(descriptorsMap, dataFields.get(i), allDescriptors);
        }

        try {
            java.lang.reflect.Field f = descriptor.getClass().getDeclaredField("fields");
            f.setAccessible(true);
            f.set(descriptor, fields);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }

        return descriptor;
    }

    public static EnumClassDescriptor map(Map<String, ClassDescriptor> descriptorsMap,
                                          EnumDescriptor enumDescriptorInfo) {
        String name = enumDescriptorInfo.getName().toString();
        if (descriptorsMap.containsKey(name)) {
            return (EnumClassDescriptor) descriptorsMap.get(name);
        }

        ObjectList<EnumConstant> enumValues = enumDescriptorInfo.getValues();
        EnumValue[] values = new EnumValue[enumValues.size()];
        for (int i = 0; i < enumValues.size(); i++) {
            values[i] = map(enumValues.get(i));
        }
        EnumClassDescriptor enumClassDescriptor = new EnumClassDescriptor(
                enumDescriptorInfo.getName() == null ? null : enumDescriptorInfo.getName().toString(),
                enumDescriptorInfo.getTitle() == null ? null : enumDescriptorInfo.getName().toString(),
                enumDescriptorInfo.isBitmask(),
                values);

        descriptorsMap.put(enumClassDescriptor.getName(), enumClassDescriptor);

        return enumClassDescriptor;
    }

    public static DataField map(Field dataFieldInfo, ObjectArrayList<UniqueDescriptor> allDescriptors) {
        return map(Collections.emptyMap(), dataFieldInfo, allDescriptors);
    }

    private static DataField map(Map<String, ClassDescriptor> descriptorsMap, Field dataFieldInfo, ObjectArrayList<UniqueDescriptor> allDescriptors) {
        if (dataFieldInfo instanceof StaticField) {
            return map(descriptorsMap, (StaticField) dataFieldInfo, allDescriptors);
        } else if (dataFieldInfo instanceof NonStaticField) {
            return map(descriptorsMap, (NonStaticField) dataFieldInfo, allDescriptors);
        } else {
            throw new UnsupportedOperationException(dataFieldInfo.getClass() + " is not supported");
        }
    }

    public static EnumValue map(EnumConstant enumValueInfo) {
        return new EnumValue(
                enumValueInfo.getSymbol().toString(),
                enumValueInfo.getValue()
        );
    }

    private static StaticDataField map(Map<String, ClassDescriptor> descriptorsMap, StaticField field, ObjectArrayList<UniqueDescriptor> allDescriptors) {
        return new StaticDataField(
                field.getName() == null ? null : field.getName().toString(),
                field.getTitle() == null ? null : field.getTitle().toString(),
                map(descriptorsMap, field.getType(), allDescriptors),
                field.getStaticValue() == null ? null : field.getStaticValue().toString()
        );
    }

    private static NonStaticDataField map(Map<String, ClassDescriptor> descriptorsMap, NonStaticField dataFieldInfo, ObjectArrayList<UniqueDescriptor> allDescriptors) {
        return new NonStaticDataField(
                dataFieldInfo.getName() == null ? null : dataFieldInfo.getName().toString(),
                dataFieldInfo.getTitle() == null ? null : dataFieldInfo.getTitle().toString(),
                map(descriptorsMap, dataFieldInfo.getType(), allDescriptors)
        );
    }

    private static DataType map(Map<String, ClassDescriptor> descriptorsMap, FieldType dataTypeInfo, ObjectArrayList<UniqueDescriptor> allDescriptors) {
        DataType result;

        if (dataTypeInfo instanceof VarcharFieldType) {
            result = map((VarcharFieldType) dataTypeInfo);
        } else if (dataTypeInfo instanceof IntegerFieldType) {
            result = map((IntegerFieldType) dataTypeInfo);
        } else if (dataTypeInfo instanceof FloatFieldType) {
            result = map((FloatFieldType) dataTypeInfo);
        } else if (dataTypeInfo instanceof BooleanFieldType) {
            result = map((BooleanFieldType) dataTypeInfo);
        } else if (dataTypeInfo instanceof CharFieldType) {
            result = map((CharFieldType) dataTypeInfo);
        } else if (dataTypeInfo instanceof ClassFieldType) {
            result = map(descriptorsMap, (ClassFieldType) dataTypeInfo, allDescriptors);
        } else if (dataTypeInfo instanceof EnumFieldType) {
            result = map(descriptorsMap, (EnumFieldType) dataTypeInfo, allDescriptors);
        } else if (dataTypeInfo instanceof BinaryFieldType) {
            result = map((BinaryFieldType) dataTypeInfo);
        } else if (dataTypeInfo instanceof ArrayFieldType) {
            result = map(descriptorsMap, (ArrayFieldType) dataTypeInfo, allDescriptors);
        } else if (dataTypeInfo instanceof TimeOfDayFieldType) {
            result = map((TimeOfDayFieldType) dataTypeInfo);
        } else if (dataTypeInfo instanceof DateTimeFieldType) {
            result = map((DateTimeFieldType) dataTypeInfo);
        } else {
            throw new UnsupportedOperationException("DataType: " + dataTypeInfo.getClass() + " is nit supported");
        }

        return result;
    }

    private static DataType map(VarcharFieldType dataTypeInfo) {
        return new VarcharDataType(
                dataTypeInfo.getEncoding() == null ? null : dataTypeInfo.getEncoding().toString(),
                dataTypeInfo.isNullable(),
                dataTypeInfo.isMultiline()
        );
    }

    private static DataType map(IntegerFieldType dataTypeInfo) {
        return new IntegerDataType(
                dataTypeInfo.getEncoding() == null ? null : dataTypeInfo.getEncoding().toString(),
                dataTypeInfo.isNullable(),
                dataTypeInfo.getMinValue() == null ? null : Long.valueOf(dataTypeInfo.getMinValue().toString()),
                dataTypeInfo.getMaxValue() == null ? null : Long.valueOf(dataTypeInfo.getMaxValue().toString())
        );
    }

    private static DataType map(FloatFieldType dataTypeInfo) {
        return new FloatDataType(
                dataTypeInfo.getEncoding() == null ? null : dataTypeInfo.getEncoding().toString(),
                dataTypeInfo.isNullable(),
                dataTypeInfo.getMinValue() == null ? null : Double.valueOf(dataTypeInfo.getMinValue().toString()),
                dataTypeInfo.getMaxValue() == null ? null : Double.valueOf(dataTypeInfo.getMaxValue().toString())
        );
    }

    private static DataType map(Map<String, ClassDescriptor> descriptorsMap, ClassFieldType dataTypeInfo, ObjectArrayList<UniqueDescriptor> allDescriptors) {
        return new ClassDataType(
                dataTypeInfo.isNullable(),
                convertDescriptorRefsToRecordClassDescriptors(descriptorsMap, dataTypeInfo.getTypeDescriptors(), allDescriptors)
                );
    }

    private static DataType map(Map<String, ClassDescriptor> descriptorsMap, ArrayFieldType dataTypeInfo, ObjectArrayList<UniqueDescriptor> allDescriptors) {
        return new ArrayDataType(
                dataTypeInfo.isNullable(),
                map(descriptorsMap, dataTypeInfo.getElementType(), allDescriptors));
    }

    private static DataType map(Map<String, ClassDescriptor> descriptorsMap, EnumFieldType dataTypeInfo, ObjectArrayList<UniqueDescriptor> allDescriptors) {
        return new EnumDataType(
                dataTypeInfo.isNullable(),
                convertDescriptorRefToEnumClassDescriptor(
                        descriptorsMap,
                        dataTypeInfo.getTypeDescriptor(),
                        allDescriptors)
        );
    }

    private static DataType map(BooleanFieldType dataTypeInfo) {
        return new BooleanDataType(dataTypeInfo.isNullable());
    }

    private static DataType map(CharFieldType dataTypeInfo) {
        return new CharDataType(dataTypeInfo.isNullable());
    }

    private static DataType map(BinaryFieldType dataTypeInfo) {
        return new BinaryDataType(
                dataTypeInfo.isNullable(),
                dataTypeInfo.getMaxSize(),
                dataTypeInfo.getCompressionLevel()
        );
    }

    private static DataType map(TimeOfDayFieldType dataTypeInfo) {
        return new TimeOfDayDataType(dataTypeInfo.isNullable());
    }

    private static DataType map(DateTimeFieldType dataTypeInfo) {
        return new DateTimeDataType(dataTypeInfo.isNullable());
    }

    private static RecordClassDescriptor[] convertDescriptorRefsToRecordClassDescriptors(Map<String, ClassDescriptor> descriptorsMap, ObjectList<DescriptorRef> refs,
            ObjectArrayList<UniqueDescriptor> descriptorsInfo) {
        RecordClassDescriptor[] descriptors = new RecordClassDescriptor[refs.size()];
        for (int i = 0; i < refs.size(); i++) {
            descriptors[i] = convertDescriptorRefToRecordClassDescriptor(descriptorsMap, refs.get(i), descriptorsInfo);
        }

        return descriptors;
    }

    private static RecordClassDescriptor convertDescriptorRefToRecordClassDescriptor(Map<String, ClassDescriptor> descriptorsMap, DescriptorRef ref,
                                                                                     ObjectArrayList<UniqueDescriptor> descriptorsInfo) {
        String descriptorName = ref.getName().toString();

        if (descriptorsMap.containsKey(descriptorName)) {
            return (RecordClassDescriptor) descriptorsMap.get(descriptorName);
        }

        Optional<UniqueDescriptor> descriptorRef = descriptorsInfo.stream().filter(d -> descriptorName.equals(d.getName().toString()))
                .findAny();

        if (descriptorRef.isPresent()) {
            UniqueDescriptor info = descriptorRef.get();
            if (info instanceof TypeDescriptor) {
                return map(descriptorsMap, (TypeDescriptor) info, descriptorsInfo);
            } else {
                throw new IllegalArgumentException("Could not convert EnumDescriptor to RecordClassDescriptor with name:" + descriptorName);
            }
        } else {
            throw new IllegalStateException("Could not find RecordClassDescriptor from reference: " + descriptorName);
        }
    }

    private static EnumClassDescriptor convertDescriptorRefToEnumClassDescriptor(Map<String, ClassDescriptor> descriptorsMap, DescriptorRef ref,
                                                                          ObjectArrayList<UniqueDescriptor> descriptorsInfo) {
        String descriptorName = ref.getName().toString();

        if (descriptorsMap.containsKey(descriptorName)) {
            return (EnumClassDescriptor) descriptorsMap.get(descriptorName);
        }

        Optional<UniqueDescriptor> descriptorInfo = descriptorsInfo.stream().filter(d -> descriptorName.equals(d.getName().toString()))
                .findAny();

        if (descriptorInfo.isPresent()) {
            UniqueDescriptor info = descriptorInfo.get();
            if (info instanceof EnumDescriptor) {
                return map(descriptorsMap, (EnumDescriptor) info);
            } else {
                throw new IllegalArgumentException("Could not convert RecordClassDescriptor to EnumClassDescriptor with name:" + descriptorName);
            }
        } else {
            throw new IllegalStateException("Could not find EnumDescriptor from reference: " + descriptorName);
        }
    }
}
