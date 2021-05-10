package com.epam.deltix.qsrv.hf.tickdb.schema.migration;

import com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.StaticDataField;
import com.epam.deltix.timebase.messages.schema.DataFieldInfo;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

import java.util.Arrays;

public class DescriptorToMessageMapper {

    public static com.epam.deltix.timebase.messages.schema.RecordClassDescriptor map(com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor descriptorObject) {
        com.epam.deltix.timebase.messages.schema.RecordClassDescriptor result = new com.epam.deltix.timebase.messages.schema.RecordClassDescriptor();
        copyNamedDescriptorProperties(descriptorObject, result);

        ObjectArrayList<DataFieldInfo> fieldMessages = new ObjectArrayList<>();
        result.setDataFields(fieldMessages);

        com.epam.deltix.qsrv.hf.pub.md.DataField[] fieldObjects = descriptorObject.getFields();
        Arrays.stream(fieldObjects)
                .forEach(item -> fieldMessages.add(map(item)));

        result.setIsAbstract(descriptorObject.isAbstract());

        if (descriptorObject.getParent() != null) {
            result.setParent(map(descriptorObject.getParent()));
        }

        return result;
    }

    public static  com.epam.deltix.timebase.messages.schema.EnumClassDescriptor map(com.epam.deltix.qsrv.hf.pub.md.EnumClassDescriptor descriptorObject) {
         com.epam.deltix.timebase.messages.schema.EnumClassDescriptor result = new com.epam.deltix.timebase.messages.schema.EnumClassDescriptor();
        copyNamedDescriptorProperties(descriptorObject, result);

        ObjectArrayList<com.epam.deltix.timebase.messages.schema.EnumValueInfo> enumValues = new ObjectArrayList<>();
        result.setValues(enumValues);

        Arrays.stream(descriptorObject.getValues())
                .forEach(item -> enumValues.add(enumValueObjectToMessage(item)));

        return result;
    }

    public static  com.epam.deltix.timebase.messages.schema.DataField map(com.epam.deltix.qsrv.hf.pub.md.DataField dataFieldObject) {
        if (dataFieldObject == null) {
            return null;
        }

         com.epam.deltix.timebase.messages.schema.DataField result;

        if (dataFieldObject instanceof com.epam.deltix.qsrv.hf.pub.md.StaticDataField) {
            result = new com.epam.deltix.timebase.messages.schema.StaticDataField();
            copyStaticDescriptorProperties((StaticDataField) dataFieldObject, (com.epam.deltix.timebase.messages.schema.StaticDataField) result);
        } else if (dataFieldObject instanceof com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField) {
            result = new com.epam.deltix.timebase.messages.schema.NonStaticDataField();
            copyNonStaticDescriptorProperties((NonStaticDataField) dataFieldObject, (com.epam.deltix.timebase.messages.schema.NonStaticDataField) result);
        } else
            throw new UnsupportedOperationException(String.format("Unknown data field type %s", dataFieldObject.getClass().getName()));

        result.setDataType(dataTypeObjectToMessage(dataFieldObject.getType()));
        return result;
    }

    private static  com.epam.deltix.timebase.messages.schema.EnumValue enumValueObjectToMessage(com.epam.deltix.qsrv.hf.pub.md.EnumValue enumValueObject) {
         com.epam.deltix.timebase.messages.schema.EnumValue result = new com.epam.deltix.timebase.messages.schema.EnumValue();
        result.setSymbol(enumValueObject.symbol);
        result.setValue((short) enumValueObject.value);

        return result;
    }

    private static com.epam.deltix.timebase.messages.schema.DataType dataTypeObjectToMessage(com.epam.deltix.qsrv.hf.pub.md.DataType dataTypeObject) {
        com.epam.deltix.timebase.messages.schema.DataType result;

        if (dataTypeObject instanceof com.epam.deltix.qsrv.hf.pub.md.ArrayDataType) {
            com.epam.deltix.qsrv.hf.pub.md.ArrayDataType arrayDataTypeObject = (com.epam.deltix.qsrv.hf.pub.md.ArrayDataType)dataTypeObject;
             com.epam.deltix.timebase.messages.schema.ArrayDataType arrayDataTypeMessage = new com.epam.deltix.timebase.messages.schema.ArrayDataType();
            arrayDataTypeMessage.setElementType(dataTypeObjectToMessage(arrayDataTypeObject.getElementDataType()));
            result = arrayDataTypeMessage;
        }
        else if (dataTypeObject instanceof com.epam.deltix.qsrv.hf.pub.md.BinaryDataType) {
            com.epam.deltix.qsrv.hf.pub.md.BinaryDataType binaryDataTypeObject = (com.epam.deltix.qsrv.hf.pub.md.BinaryDataType)dataTypeObject;
             com.epam.deltix.timebase.messages.schema.BinaryDataType binaryDataTypeMessage = new com.epam.deltix.timebase.messages.schema.BinaryDataType();
            binaryDataTypeMessage.setCompressionLevel((short) binaryDataTypeObject.getCompressionLevel());
            binaryDataTypeMessage.setMaxSize(binaryDataTypeObject.getMaxSize());
            result = binaryDataTypeMessage;
        }
        else if (dataTypeObject instanceof com.epam.deltix.qsrv.hf.pub.md.BooleanDataType) {
            result = new com.epam.deltix.timebase.messages.schema.BooleanDataType();
        }
        else if (dataTypeObject instanceof com.epam.deltix.qsrv.hf.pub.md.CharDataType) {
            result = new com.epam.deltix.timebase.messages.schema.CharDataType();
        }
        else if (dataTypeObject instanceof com.epam.deltix.qsrv.hf.pub.md.ClassDataType) {
            com.epam.deltix.qsrv.hf.pub.md.ClassDataType classTypeObject = (com.epam.deltix.qsrv.hf.pub.md.ClassDataType)dataTypeObject;
             com.epam.deltix.timebase.messages.schema.ClassDataType classDataTypeMessage = new com.epam.deltix.timebase.messages.schema.ClassDataType();
            ObjectArrayList<com.epam.deltix.timebase.messages.schema.ClassDescriptorRefInfo> typeDescriptors = new ObjectArrayList<>();

            if (classTypeObject.isFixed()) {
                typeDescriptors.add(descriptorObjectToRef(classTypeObject.getFixedDescriptor()));
            } else {
                Arrays.stream(classTypeObject.getDescriptors())
                        .forEach(item -> typeDescriptors.add(descriptorObjectToRef(item)));
            }

            classDataTypeMessage.setTypeDescriptors(typeDescriptors);
            result = classDataTypeMessage;
        }
        else if (dataTypeObject instanceof com.epam.deltix.qsrv.hf.pub.md.DateTimeDataType) {
            result = new com.epam.deltix.timebase.messages.schema.DateTimeDataType();
        }
        else if (dataTypeObject instanceof com.epam.deltix.qsrv.hf.pub.md.EnumDataType) {
            com.epam.deltix.qsrv.hf.pub.md.EnumDataType enumDataTypeObject = (com.epam.deltix.qsrv.hf.pub.md.EnumDataType)dataTypeObject;
             com.epam.deltix.timebase.messages.schema.EnumDataType enumDataTypeMessage = new com.epam.deltix.timebase.messages.schema.EnumDataType();
            enumDataTypeMessage.setTypeDescriptor(descriptorObjectToRef(enumDataTypeObject.descriptor));
            result = enumDataTypeMessage;
        }
        else if (dataTypeObject instanceof com.epam.deltix.qsrv.hf.pub.md.FloatDataType) {
            com.epam.deltix.qsrv.hf.pub.md.FloatDataType floatDataTypeObject = (com.epam.deltix.qsrv.hf.pub.md.FloatDataType)dataTypeObject;
             com.epam.deltix.timebase.messages.schema.FloatDataType floatDataTypeMessage = new com.epam.deltix.timebase.messages.schema.FloatDataType();
            if (floatDataTypeObject.getMin() != null)
                floatDataTypeMessage.setMinValue(floatDataTypeObject.getMin().toString());
            if (floatDataTypeObject.getMax() != null)
                floatDataTypeMessage.setMaxValue(floatDataTypeObject.getMax().toString());
            floatDataTypeMessage.setScale(floatDataTypeMessage.getScale());
            floatDataTypeMessage.setEncoding(dataTypeObject.getEncoding());
            result = floatDataTypeMessage;
        }
        else if (dataTypeObject instanceof com.epam.deltix.qsrv.hf.pub.md.IntegerDataType) {
            com.epam.deltix.qsrv.hf.pub.md.IntegerDataType integerDataTypeObject = (com.epam.deltix.qsrv.hf.pub.md.IntegerDataType)dataTypeObject;
             com.epam.deltix.timebase.messages.schema.IntegerDataType integerDataTypeMessage = new com.epam.deltix.timebase.messages.schema.IntegerDataType();
            if (integerDataTypeObject.getMin() != null)
                integerDataTypeMessage.setMinValue(integerDataTypeObject.getMin().toString());
            if (integerDataTypeObject.getMax() != null)
                integerDataTypeMessage.setMaxValue(integerDataTypeObject.getMax().toString());
            result = integerDataTypeMessage;
        }
        else if (dataTypeObject instanceof com.epam.deltix.qsrv.hf.pub.md.TimeOfDayDataType) {
            result = new com.epam.deltix.timebase.messages.schema.TimeOfDayDataType();
        }
        else if (dataTypeObject instanceof com.epam.deltix.qsrv.hf.pub.md.VarcharDataType) {
            com.epam.deltix.qsrv.hf.pub.md.VarcharDataType varcharDataTypeObject = (com.epam.deltix.qsrv.hf.pub.md.VarcharDataType)dataTypeObject;
             com.epam.deltix.timebase.messages.schema.VarcharDataType varcharDataTypeMessage = new com.epam.deltix.timebase.messages.schema.VarcharDataType();
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

    private static  com.epam.deltix.timebase.messages.schema.ClassDescriptorRef descriptorObjectToRef(com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor descriptorObject) {
        com.epam.deltix.timebase.messages.schema.ClassDescriptorRef result = new com.epam.deltix.timebase.messages.schema.ClassDescriptorRef();
        result.setName(descriptorObject.getName());

        return result;
    }

    private static void copyNamedDescriptorProperties(com.epam.deltix.qsrv.hf.pub.md.NamedDescriptor source,
                                                      com.epam.deltix.timebase.messages.schema.NamedDescriptor destination) {
        destination.setName(source.getName());
        destination.setTitle(source.getTitle());
        destination.setDescription(source.getDescription());
    }

    private static void copyNonStaticDescriptorProperties(NonStaticDataField source, com.epam.deltix.timebase.messages.schema.NonStaticDataField destination) {
        destination.setName(source.getName());
        destination.setTitle(source.getTitle());
        destination.setDescription(source.getDescription());
        destination.setRelativeTo(source.getRelativeTo());
        if (source.isPk()) {
            destination.setIsPrimaryKey(source.isPk());
        }
    }

    private static void copyStaticDescriptorProperties(StaticDataField source,  com.epam.deltix.timebase.messages.schema.StaticDataField destination) {
        destination.setName(source.getName());
        destination.setTitle(source.getTitle());
        destination.setDescription(source.getDescription());
        destination.setStaticValue(source.getStaticValue());
    }
}
