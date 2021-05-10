package com.epam.deltix.qsrv.hf.tickdb.schema.migration;

import com.epam.deltix.qsrv.hf.pub.md.ArrayDataType;
import com.epam.deltix.qsrv.hf.pub.md.BinaryDataType;
import com.epam.deltix.qsrv.hf.pub.md.BooleanDataType;
import com.epam.deltix.qsrv.hf.pub.md.CharDataType;
import com.epam.deltix.qsrv.hf.pub.md.ClassDataType;
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.DateTimeDataType;
import com.epam.deltix.qsrv.hf.pub.md.EnumClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.EnumDataType;
import com.epam.deltix.qsrv.hf.pub.md.EnumValue;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.StaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.TimeOfDayDataType;
import com.epam.deltix.qsrv.hf.pub.md.VarcharDataType;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.timebase.messages.schema.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.collections.generated.ObjectList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MessageToDescriptorMapper {

    public static RecordClassSet convert(ObjectArrayList<ClassDescriptorInfo> descriptorsInfo) {
        List<RecordClassDescriptor> contentClasses = new ArrayList<>();
        ClassDescriptor[] descriptors = new ClassDescriptor[descriptorsInfo.size()];
        for (int i = 0; i < descriptorsInfo.size(); i++) {
            ClassDescriptorInfo info = descriptorsInfo.get(i);
            RecordClassDescriptor rcd = map((RecordClassDescriptorInfo) info, descriptorsInfo);
            if (info instanceof RecordClassDescriptorInfo) {
                if (((RecordClassDescriptorInfo) info).isContentClass()) {
                    contentClasses.add(rcd);
                }
                descriptors[i] = rcd;
            } else if (info instanceof EnumClassDescriptorInfo) {
                descriptors[i] = map((EnumClassDescriptorInfo) info);
            }
        }

        RecordClassSet classSet = new RecordClassSet();
        classSet.setClassDescriptors(descriptors);
        classSet.addContentClasses(contentClasses.toArray(new RecordClassDescriptor[contentClasses.size()]));

        return classSet;
    }

    public static RecordClassDescriptor map(com.epam.deltix.timebase.messages.schema.RecordClassDescriptorInfo descriptorInfo,
                                            ObjectArrayList<ClassDescriptorInfo> allDescriptors) {
        ObjectList<DataFieldInfo> dataFields = descriptorInfo.getDataFields();
        DataField[] fields = new DataField[dataFields.size()];
        for (int i = 0; i < dataFields.size(); i++) {
            fields[i] = map(dataFields.get(i), allDescriptors);
        }
        return new RecordClassDescriptor(
                descriptorInfo.getName() == null ? null : descriptorInfo.getName().toString(),
                descriptorInfo.getTitle() == null ? null : descriptorInfo.getTitle().toString(),
                descriptorInfo.isAbstract(),
                descriptorInfo.getParent() == null ? null : map(descriptorInfo.getParent(), allDescriptors),
                fields
        );
    }

    public static EnumClassDescriptor map(EnumClassDescriptorInfo enumDescriptorInfo) {
        ObjectList<EnumValueInfo> enumValues = enumDescriptorInfo.getValues();
        EnumValue[] values = new EnumValue[enumValues.size()];
        for (int i = 0; i < enumValues.size(); i++) {
            values[i] = map(enumValues.get(i));
        }
        return new EnumClassDescriptor(
                enumDescriptorInfo.getName() == null ? null : enumDescriptorInfo.getName().toString(),
                enumDescriptorInfo.getTitle() == null ? null : enumDescriptorInfo.getName().toString(),
                enumDescriptorInfo.isBitmask(),
                values);
    }

    public static DataField map(DataFieldInfo dataFieldInfo, ObjectArrayList<ClassDescriptorInfo> allDescriptors) {
        if (dataFieldInfo instanceof StaticDataFieldInfo) {
            return map((StaticDataFieldInfo) dataFieldInfo, allDescriptors);
        } else if (dataFieldInfo instanceof NonStaticDataFieldInfo) {
            return map((NonStaticDataFieldInfo) dataFieldInfo, allDescriptors);
        } else {
            throw new UnsupportedOperationException(dataFieldInfo.getClass() + " is not supported");
        }
    }

    public static EnumValue map(EnumValueInfo enumValueInfo) {
        return new EnumValue(
                enumValueInfo.getSymbol().toString(),
                enumValueInfo.getValue()
        );
    }

    private static StaticDataField map(StaticDataFieldInfo dataFieldInfo, ObjectArrayList<ClassDescriptorInfo> allDescriptors) {
        return new StaticDataField(
                dataFieldInfo.getName() == null ? null : dataFieldInfo.getName().toString(),
                dataFieldInfo.getTitle() == null ? null : dataFieldInfo.getTitle().toString(),
                map(dataFieldInfo.getDataType(), allDescriptors),
                dataFieldInfo.getStaticValue() == null ? null : dataFieldInfo.getStaticValue().toString()
        );
    }

    private static NonStaticDataField map(NonStaticDataFieldInfo dataFieldInfo, ObjectArrayList<ClassDescriptorInfo> allDescriptors) {
        return new NonStaticDataField(
                dataFieldInfo.getName() == null ? null : dataFieldInfo.getName().toString(),
                dataFieldInfo.getTitle() == null ? null : dataFieldInfo.getTitle().toString(),
                map(dataFieldInfo.getDataType(), allDescriptors)
        );
    }

    private static DataType map(DataTypeInfo dataTypeInfo, ObjectArrayList<ClassDescriptorInfo> allDescriptors) {
        DataType result;

        if (dataTypeInfo instanceof VarcharDataTypeInfo) {
            result = map((VarcharDataTypeInfo) dataTypeInfo);
        } else if (dataTypeInfo instanceof IntegerDataTypeInfo) {
            result = map((IntegerDataTypeInfo) dataTypeInfo);
        } else if (dataTypeInfo instanceof FloatDataTypeInfo) {
            result = map((FloatDataTypeInfo) dataTypeInfo);
        } else if (dataTypeInfo instanceof BooleanDataTypeInfo) {
            result = map((BooleanDataTypeInfo) dataTypeInfo);
        } else if (dataTypeInfo instanceof CharDataTypeInfo) {
            result = map((CharDataTypeInfo) dataTypeInfo);
        } else if (dataTypeInfo instanceof ClassDataTypeInfo) {
            result = map((ClassDataTypeInfo) dataTypeInfo, allDescriptors);
        } else if (dataTypeInfo instanceof EnumDataTypeInfo) {
            result = map((EnumDataTypeInfo) dataTypeInfo, allDescriptors);
        } else if (dataTypeInfo instanceof BinaryDataTypeInfo) {
            result = map((BinaryDataTypeInfo) dataTypeInfo);
        } else if (dataTypeInfo instanceof ArrayDataTypeInfo) {
            result = map((ArrayDataTypeInfo) dataTypeInfo, allDescriptors);
        } else if (dataTypeInfo instanceof TimeOfDayDataTypeInfo) {
            result = map((TimeOfDayDataTypeInfo) dataTypeInfo);
        } else if (dataTypeInfo instanceof DateTimeDataTypeInfo) {
            result = map((DateTimeDataTypeInfo) dataTypeInfo);
        } else {
            throw new UnsupportedOperationException("DataType: " + dataTypeInfo.getClass() + " is nit supported");
        }

        return result;
    }

    private static DataType map(VarcharDataTypeInfo dataTypeInfo) {
        return new VarcharDataType(
                dataTypeInfo.getEncoding() == null ? null : dataTypeInfo.getEncoding().toString(),
                dataTypeInfo.isNullable(),
                dataTypeInfo.isMultiline()
        );
    }

    private static DataType map(IntegerDataTypeInfo dataTypeInfo) {
        return new IntegerDataType(
                dataTypeInfo.getEncoding() == null ? null : dataTypeInfo.getEncoding().toString(),
                dataTypeInfo.isNullable(),
                dataTypeInfo.getMinValue() == null ? null : Long.valueOf(dataTypeInfo.getMinValue().toString()),
                dataTypeInfo.getMaxValue() == null ? null : Long.valueOf(dataTypeInfo.getMaxValue().toString())
        );
    }

    private static DataType map(FloatDataTypeInfo dataTypeInfo) {
        return new FloatDataType(
                dataTypeInfo.getEncoding() == null ? null : dataTypeInfo.getEncoding().toString(),
                dataTypeInfo.isNullable(),
                dataTypeInfo.getMinValue() == null ? null : Double.valueOf(dataTypeInfo.getMinValue().toString()),
                dataTypeInfo.getMaxValue() == null ? null : Double.valueOf(dataTypeInfo.getMaxValue().toString())
        );
    }

    private static DataType map(ClassDataTypeInfo dataTypeInfo, ObjectArrayList<ClassDescriptorInfo> allDescriptors) {
        return new ClassDataType(
                dataTypeInfo.isNullable(),
                convertDescriptorRefsToRecordClassDescriptors(dataTypeInfo.getTypeDescriptors(), allDescriptors)
                );
    }

    private static DataType map(ArrayDataTypeInfo dataTypeInfo, ObjectArrayList<ClassDescriptorInfo> allDescriptors) {
        return new ArrayDataType(
                dataTypeInfo.isNullable(),
                map(dataTypeInfo.getElementType(), allDescriptors));
    }

    private static DataType map(EnumDataTypeInfo dataTypeInfo, ObjectArrayList<ClassDescriptorInfo> allDescriptors) {
        return new EnumDataType(
                dataTypeInfo.isNullable(),
                convertDescriptorRefToEnumClassDescriptor(
                        dataTypeInfo.getTypeDescriptor(),
                        allDescriptors)
        );
    }

    private static DataType map(BooleanDataTypeInfo dataTypeInfo) {
        return new BooleanDataType(dataTypeInfo.isNullable());
    }

    private static DataType map(CharDataTypeInfo dataTypeInfo) {
        return new CharDataType(dataTypeInfo.isNullable());
    }

    private static DataType map(BinaryDataTypeInfo dataTypeInfo) {
        return new BinaryDataType(
                dataTypeInfo.isNullable(),
                dataTypeInfo.getMaxSize(),
                dataTypeInfo.getCompressionLevel()
        );
    }

    private static DataType map(TimeOfDayDataTypeInfo dataTypeInfo) {
        return new TimeOfDayDataType(dataTypeInfo.isNullable());
    }

    private static DataType map(DateTimeDataTypeInfo dataTypeInfo) {
        return new DateTimeDataType(dataTypeInfo.isNullable());
    }

    private static RecordClassDescriptor[] convertDescriptorRefsToRecordClassDescriptors(ObjectList<ClassDescriptorRefInfo> refs,
            ObjectArrayList<ClassDescriptorInfo> descriptorsInfo) {
        RecordClassDescriptor[] descriptors = new RecordClassDescriptor[refs.size()];
        for (int i = 0; i < refs.size(); i++) {
            descriptors[i] = convertDescriptorRefToRecordClassDescriptor(refs.get(i), descriptorsInfo);
        }

        return descriptors;
    }

    private static RecordClassDescriptor convertDescriptorRefToRecordClassDescriptor(ClassDescriptorRefInfo ref,
                                                                                     ObjectArrayList<ClassDescriptorInfo> descriptorsInfo) {
        String descriptorName = ref.getName().toString();

        Optional<ClassDescriptorInfo> descriptorInfo = descriptorsInfo.stream().filter(d -> descriptorName.equals(d.getName()))
                .findAny();

        if (descriptorInfo.isPresent()) {
            ClassDescriptorInfo info = descriptorInfo.get();
            if (info instanceof RecordClassDescriptorInfo) {
                return map((RecordClassDescriptorInfo) info, descriptorsInfo);
            } else {
                throw new IllegalArgumentException("Could not convert EnumDescriptor to RecordClassDescriptor with name:" + descriptorName);
            }
        } else {
            throw new IllegalStateException("Could not find RecordClassDescriptor from reference: " + descriptorName);
        }
    }

    private static EnumClassDescriptor convertDescriptorRefToEnumClassDescriptor(ClassDescriptorRefInfo ref,
                                                                          ObjectArrayList<ClassDescriptorInfo> descriptorsInfo) {
        String descriptorName = ref.getName().toString();

        Optional<ClassDescriptorInfo> descriptorInfo = descriptorsInfo.stream().filter(d -> descriptorName.equals(d.getName()))
                .findAny();

        if (descriptorInfo.isPresent()) {
            ClassDescriptorInfo info = descriptorInfo.get();
            if (info instanceof EnumClassDescriptorInfo) {
                return map((EnumClassDescriptorInfo) info);
            } else {
                throw new IllegalArgumentException("Could not convert RecordClassDescriptor to EnumClassDescriptor with name:" + descriptorName);
            }
        } else {
            throw new IllegalStateException("Could not find EnumDescriptor from reference: " + descriptorName);
        }
    }
}
