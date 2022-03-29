package com.epam.deltix.qsrv.hf.tickdb.schema.migration;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.timebase.messages.schema.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

import java.util.Arrays;

public class DescriptorToMessageMapper {
    public static ObjectArrayList<UniqueDescriptor> convert(RecordClassSet recordClassSet) {
        return convert(recordClassSet, false);
    }

    /**
     * @param recordClassSet schema definition to serialize
     * @param addGuid if set to true then encoded RCD will have "guid" field preserved
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static ObjectArrayList<UniqueDescriptor> convert(RecordClassSet recordClassSet, boolean addGuid) {
        ObjectArrayList<UniqueDescriptor> descriptors = new ObjectArrayList<>();

        if (recordClassSet != null && recordClassSet.getClassDescriptors() != null) {
            synchronized (recordClassSet) {
                ClassDescriptor[] contentClasses = recordClassSet.getClassDescriptors();
                for (int i = 0; i < contentClasses.length; i++) {
                    ClassDescriptor contentClass = contentClasses[i];
                    if (contentClass instanceof RecordClassDescriptor) {
                        TypeDescriptor recordClassDescriptor = map((RecordClassDescriptor) contentClass, addGuid);

                        boolean isContentClass = recordClassSet.getContentClass(contentClass.getGuid()) != null;
                        recordClassDescriptor.setIsContentClass(isContentClass);
                        descriptors.add(recordClassDescriptor);
                    } else {
                        descriptors.add(DescriptorToMessageMapper.map((EnumClassDescriptor) contentClass));
                    }
                }
            }
        }

        return descriptors;
    }

    public static TypeDescriptor map(RecordClassDescriptor descriptorObject) {
        return map(descriptorObject, false);
    }

    public static TypeDescriptor map(RecordClassDescriptor descriptorObject, boolean addGuid) {
        TypeDescriptor result = new TypeDescriptor();
        result.setIsContentClass(false);
        if (addGuid) {
            result.setGuid(descriptorObject.getGuid());
        }
        copyNamedDescriptorProperties(descriptorObject, result);

        ObjectArrayList<Field> fieldMessages = new ObjectArrayList<>();
        result.setFields(fieldMessages);

        DataField[] fieldObjects = descriptorObject.getFields();
        Arrays.stream(fieldObjects)
                .forEach(item -> fieldMessages.add(map(item)));

        result.setIsAbstract(descriptorObject.isAbstract());

        if (descriptorObject.getParent() != null) {
            result.setParent(map(descriptorObject.getParent()));
        }

        return result;
    }

    public static EnumDescriptor map(EnumClassDescriptor descriptorObject) {
        EnumDescriptor result = new EnumDescriptor();
        result.setIsBitmask(false);
        copyNamedDescriptorProperties(descriptorObject, result);

        ObjectArrayList<EnumConstant> enumValues = new ObjectArrayList<>();
        result.setValues(enumValues);

        Arrays.stream(descriptorObject.getValues())
                .forEach(item -> enumValues.add(enumValueObjectToMessage(item)));

        return result;
    }

    public static Field map(DataField dataFieldObject) {
        if (dataFieldObject == null) {
            return null;
        }

        Field result;

        if (dataFieldObject instanceof StaticDataField) {
            result = new StaticField();
            copyStaticDescriptorProperties((StaticDataField) dataFieldObject, (StaticField)result);
        } else if (dataFieldObject instanceof NonStaticDataField) {
            result = new NonStaticField();
            copyNonStaticDescriptorProperties((NonStaticDataField) dataFieldObject, (NonStaticField) result);
        } else
            throw new UnsupportedOperationException(String.format("Unknown data field type %s", dataFieldObject.getClass().getName()));

        result.setType(dataTypeObjectToMessage(dataFieldObject.getType()));
        return result;
    }

    private static EnumConstant enumValueObjectToMessage(EnumValue enumValueObject) {
        EnumConstant result = new EnumConstant();
        result.setSymbol(enumValueObject.symbol);
        result.setValue((short) enumValueObject.value);

        return result;
    }

    private static FieldType dataTypeObjectToMessage(DataType dataTypeObject) {
        FieldType result;

        if (dataTypeObject instanceof ArrayDataType) {
            ArrayDataType arrayDataTypeObject = (ArrayDataType)dataTypeObject;
            ArrayFieldType arrayDataTypeMessage = new ArrayFieldType();
            arrayDataTypeMessage.setElementType(dataTypeObjectToMessage(arrayDataTypeObject.getElementDataType()));
            result = arrayDataTypeMessage;
        }
        else if (dataTypeObject instanceof BinaryDataType) {
            BinaryDataType binaryDataTypeObject = (BinaryDataType)dataTypeObject;
            BinaryFieldType binaryDataTypeMessage = new BinaryFieldType();
            binaryDataTypeMessage.setCompressionLevel((short) binaryDataTypeObject.getCompressionLevel());
            binaryDataTypeMessage.setMaxSize(binaryDataTypeObject.getMaxSize());
            result = binaryDataTypeMessage;
        }
        else if (dataTypeObject instanceof BooleanDataType) {
            result = new BooleanFieldType();
        }
        else if (dataTypeObject instanceof CharDataType) {
            result = new CharFieldType();
        }
        else if (dataTypeObject instanceof ClassDataType) {
            ClassDataType classTypeObject = (ClassDataType)dataTypeObject;
            ClassFieldType classDataTypeMessage = new ClassFieldType();
            ObjectArrayList<DescriptorRef> typeDescriptors = new ObjectArrayList<>();

            if (classTypeObject.isFixed()) {
                typeDescriptors.add(descriptorObjectToRef(classTypeObject.getFixedDescriptor()));
            } else {
                Arrays.stream(classTypeObject.getDescriptors())
                        .forEach(item -> typeDescriptors.add(descriptorObjectToRef(item)));
            }

            classDataTypeMessage.setTypeDescriptors(typeDescriptors);
            result = classDataTypeMessage;
        }
        else if (dataTypeObject instanceof DateTimeDataType) {
            result = new DateTimeFieldType();
        }
        else if (dataTypeObject instanceof EnumDataType) {
            EnumDataType enumDataTypeObject = (EnumDataType)dataTypeObject;
            EnumFieldType enumDataTypeMessage = new EnumFieldType();
            enumDataTypeMessage.setTypeDescriptor(descriptorObjectToRef(enumDataTypeObject.descriptor));
            result = enumDataTypeMessage;
        }
        else if (dataTypeObject instanceof FloatDataType) {
            FloatDataType floatDataTypeObject = (FloatDataType)dataTypeObject;
            FloatFieldType floatDataTypeMessage = new FloatFieldType();
            if (floatDataTypeObject.getMin() != null)
                floatDataTypeMessage.setMinValue(floatDataTypeObject.getMin().toString());
            if (floatDataTypeObject.getMax() != null)
                floatDataTypeMessage.setMaxValue(floatDataTypeObject.getMax().toString());
            floatDataTypeMessage.setScale(floatDataTypeMessage.getScale());
            floatDataTypeMessage.setEncoding(dataTypeObject.getEncoding());
            result = floatDataTypeMessage;
        }
        else if (dataTypeObject instanceof IntegerDataType) {
            IntegerDataType integerDataTypeObject = (IntegerDataType)dataTypeObject;
            FloatFieldType integerDataTypeMessage = new FloatFieldType();
            if (integerDataTypeObject.getMin() != null)
                integerDataTypeMessage.setMinValue(integerDataTypeObject.getMin().toString());
            if (integerDataTypeObject.getMax() != null)
                integerDataTypeMessage.setMaxValue(integerDataTypeObject.getMax().toString());
            result = integerDataTypeMessage;
        }
        else if (dataTypeObject instanceof TimeOfDayDataType) {
            result = new TimeOfDayFieldType();
        }
        else if (dataTypeObject instanceof VarcharDataType) {
            VarcharDataType varcharDataTypeObject = (VarcharDataType)dataTypeObject;
            VarcharFieldType varcharDataTypeMessage = new VarcharFieldType();
            varcharDataTypeMessage.setEncodingType(varcharDataTypeObject.getEncodingType());
            varcharDataTypeMessage.setIsMultiline(varcharDataTypeObject.isMultiLine());
            varcharDataTypeMessage.setLength(varcharDataTypeObject.getLength());
            result = varcharDataTypeMessage;
        }
        else {
            throw new UnsupportedOperationException(String.format("Unknown data type %s", dataTypeObject.getClass().getName()));
        }

        result.setEncoding(dataTypeObject.getEncoding());
        result.setIsNullable(dataTypeObject.isNullable());

        return result;
    }

    private static DescriptorRef descriptorObjectToRef(ClassDescriptor descriptorObject) {
        DescriptorRef result = new DescriptorRef();
        result.setName(descriptorObject.getName());
        return result;
    }

    private static void copyNamedDescriptorProperties(NamedDescriptor source, UniqueDescriptor destination) {
        destination.setName(source.getName());
        destination.setTitle(source.getTitle());
        destination.setDescription(source.getDescription());
    }

    private static void copyNonStaticDescriptorProperties(NonStaticDataField source, NonStaticField destination) {
        destination.setName(source.getName());
        destination.setTitle(source.getTitle());
        destination.setDescription(source.getDescription());
        destination.setRelativeTo(source.getRelativeTo());
        destination.setIsPrimaryKey(source.isPk());
    }

    private static void copyStaticDescriptorProperties(StaticDataField source, StaticField destination) {
        destination.setName(source.getName());
        destination.setTitle(source.getTitle());
        destination.setDescription(source.getDescription());
        destination.setStaticValue(source.getStaticValue());
    }
}
