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
package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.functions;

import com.epam.deltix.computations.api.annotations.Function;
import com.epam.deltix.computations.api.annotations.FunctionsRepo;
import com.epam.deltix.computations.api.annotations.Result;
import com.epam.deltix.computations.api.annotations.Type;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.functions.StatefulFunctionDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.functions.StatelessFunctionDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.CompilerUtil;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.qql.Argument;
import com.epam.deltix.timebase.messages.qql.InitArgument;
import com.epam.deltix.timebase.messages.qql.StatefulFunctionMessage;
import com.epam.deltix.timebase.messages.qql.StatelessFunctionMessage;
import com.epam.deltix.timebase.messages.schema.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@FunctionsRepo
public class InternalFunctions {

    private static ObjectArrayList<StatefulFunctionMessage> statefulFunctions;
    private static ObjectArrayList<StatelessFunctionMessage> statelessFunctions;

    @Function("STREAMS")
    public static boolean streams(@DB DXTickDB db,
                                  @Type("ARRAY(OBJECT(com.epam.deltix.timebase.messages.schema.StreamMessage))?")
                                  @Nonnull @Result ObjectArrayList<StreamMessage> result) {
        final long currentTime = System.currentTimeMillis();
        for (DXTickStream stream : db.listStreams()) {
            StreamMessage msg = new StreamMessage();
            msg.setSymbol(stream.getKey());
            msg.setTimeStampMs(currentTime);
            msg.setKey(stream.getKey());
            msg.setDescription(stream.getDescription());
            Set<String> content = Arrays.stream(stream.getTypes())
                    .map(ClassDescriptor::getGuid)
                    .collect(Collectors.toSet());
            Map<String, TypeDescriptor> cache = new HashMap<>();
            Map<String, EnumDescriptor> enumCache = new HashMap<>();
            msg.setTopTypes(Arrays.stream(stream.getTypes())
                    .map(rcd -> convert(rcd, content, cache))
                    .collect(ObjectArrayList::new, ObjectArrayList::add, ObjectArrayList::addAll)
            );
            msg.setAllTypes(Arrays.stream(stream.getAllDescriptors())
                    .map(cd -> convert(cd, content, cache, enumCache))
                    .collect(ObjectArrayList::new, ObjectArrayList::add, ObjectArrayList::addAll)
            );
            result.add(msg);
        }
        return true;
    }

    @Function("SYMBOLS")
    public static boolean symbols(@Nonnull CharSequence streamKey, @DB DXTickDB db,
                                  @Type("ARRAY(VARCHAR?)?") @Nonnull @Result ObjectArrayList<CharSequence> result) {
        DXTickStream stream = db.getStream(streamKey.toString());
        if (stream == null)
            return false;
        result.clear();
        IdentityKey[] instrumentIdentities = stream.listEntities();
        for (int i = 0; i < instrumentIdentities.length; i++) {
            result.add(instrumentIdentities[i].getSymbol());
        }
        return true;
    }

    @Function("SPACES")
    public static boolean spaces(@Nonnull CharSequence streamKey, @DB DXTickDB db,
                                 @Type("ARRAY(VARCHAR?)?") @Nonnull @Result ObjectArrayList<CharSequence> result) {
        DXTickStream stream = db.getStream(streamKey.toString());
        if (stream == null)
            return false;
        result.clear();
        String[] spaces = stream.listSpaces();
        for (int i = 0; i < spaces.length; i++) {
            result.add(spaces[i]);
        }
        return true;
    }

    @Function("STATEFUL_FUNCTIONS")
    public static boolean statefulFunctions(@Type("ARRAY(OBJECT(com.epam.deltix.timebase.messages.qql.StatefulFunctionMessage))?")
                                            @Nonnull @Result ObjectArrayList<StatefulFunctionMessage> result) {
        for (int i = 0; i < statefulFunctions().size(); i++) {
            result.add(statefulFunctions().getObject(i));
        }
        return true;
    }

    @Function("STATELESS_FUNCTIONS")
    public static boolean statelessFunctions(@Type("ARRAY(OBJECT(com.epam.deltix.timebase.messages.qql.StatelessFunctionMessage))?")
                                             @Nonnull @Result ObjectArrayList<StatelessFunctionMessage> result) {
        for (int i = 0; i < statelessFunctions().size(); i++) {
            result.add(statelessFunctions().getObject(i));
        }
        return true;
    }

    private static ObjectArrayList<StatelessFunctionMessage> statelessFunctions() {
        if (statelessFunctions == null) {
            statelessFunctions = CompilerUtil.STDENV.getStatelessFunctions().stream()
                    .map(InternalFunctions::convert)
                    .collect(ObjectArrayList::new, ObjectArrayList::add, ObjectArrayList::addAll);
        }
        return statelessFunctions;
    }

    private static ObjectArrayList<StatefulFunctionMessage> statefulFunctions() {
        if (statefulFunctions == null) {
            statefulFunctions = CompilerUtil.STDENV.getStatefulFunctions().stream()
                    .map(InternalFunctions::convert)
                    .collect(ObjectArrayList::new, ObjectArrayList::add, ObjectArrayList::addAll);
        }
        return statefulFunctions;
    }

    private static UniqueDescriptor convert(ClassDescriptor cd, Set<String> content,
                                            Map<String, TypeDescriptor> cache,
                                            Map<String, EnumDescriptor> enumCache) {
        if (cd instanceof RecordClassDescriptor) {
            TypeDescriptor rcdInfo = cache.get(cd.getGuid());
            if (rcdInfo == null) {
                rcdInfo = convert((RecordClassDescriptor) cd, content, cache);
                cache.put(cd.getGuid(), rcdInfo);
            }
            return cache.computeIfAbsent(cd.getGuid(), key -> convert((RecordClassDescriptor) cd, content, cache));
        } else {
            return enumCache.computeIfAbsent(cd.getGuid(), key -> convert((EnumClassDescriptor) cd));
        }
    }

    private static EnumDescriptor convert(EnumClassDescriptor ecd) {
        EnumDescriptor result = new EnumDescriptor();
        copy(ecd, result);
        result.setIsBitmask(ecd.isBitmask());
        result.setValues(Arrays.stream(ecd.getValues())
                .map(InternalFunctions::convert)
                .collect(ObjectArrayList::new, ObjectArrayList::add, ObjectArrayList::addAll));
        return result;
    }

    private static TypeDescriptor convert(RecordClassDescriptor rcd, Set<String> content,
                                          Map<String, TypeDescriptor> cache) {
        TypeDescriptor resultInfo = cache.get(rcd.getGuid());
        if (resultInfo == null) {
            TypeDescriptor result = new TypeDescriptor();
            copy(rcd, result);
            result.setIsAbstract(rcd.isAbstract());
            result.setIsContentClass(content.contains(rcd.getGuid()));
            result.setFields(convert(rcd.getFields()));
            if (rcd.getParent() != null) {
                TypeDescriptor parentRcd = cache.get(rcd.getParent().getGuid());
                if (parentRcd == null) {
                    parentRcd = convert(rcd.getParent(), content, cache);
                    cache.put(rcd.getParent().getGuid(), parentRcd);
                }
                result.setParent(parentRcd);
            }
            cache.put(rcd.getGuid(), result);
            return result;
        } else {
            return resultInfo;
        }
    }

    private static void copy(ClassDescriptor from, UniqueDescriptor to) {
        to.setName(from.getName());
        to.setGuid(from.getGuid());
        to.setDescription(from.getDescription());
        to.setTitle(from.getTitle());
    }

    private static ObjectArrayList<Field> convert(DataField[] fields) {
        if (fields == null) {
            return new ObjectArrayList<>();
        }
        ObjectArrayList<Field> result = new ObjectArrayList<>(fields.length);
        for (DataField field : fields) {
            if (field instanceof NonStaticDataField) {
                result.add(convert((NonStaticDataField) field));
            } else {
                result.add(convert((StaticDataField) field));
            }
        }
        return result;
    }

    private static NonStaticField convert(NonStaticDataField field) {
        NonStaticField result = new NonStaticField();
        copy(field, result);
        result.setIsPrimaryKey(field.isPk());
        result.setRelativeTo(field.getRelativeTo());
        return result;
    }

    private static StaticField convert(StaticDataField field) {
        StaticField result = new StaticField();
        copy(field, result);
        result.setStaticValue(field.getStaticValue());
        return result;
    }

    private static void copy(DataField from, Field to) {
        to.setName(from.getName());
        to.setDescription(from.getDescription());
        to.setTitle(from.getTitle());
        to.setType(convert(from.getType()));
    }

    private static FieldType convert(DataType dataType) {
        if (dataType instanceof IntegerDataType) {
            IntegerFieldType result = new IntegerFieldType();
            copy(dataType, result);
            Number max = ((IntegerDataType) dataType).getMax();
            result.setMaxValue(max == null ? null : max.toString());
            Number min = ((IntegerDataType) dataType).getMin();
            result.setMinValue(min == null ? null : min.toString());
            return result;
        } else if (dataType instanceof FloatDataType) {
            FloatFieldType result = new FloatFieldType();
            copy(dataType, result);
            Number max = ((FloatDataType) dataType).getMax();
            result.setMaxValue(max == null ? null : max.toString());
            Number min = ((FloatDataType) dataType).getMin();
            result.setMinValue(min == null ? null : min.toString());
            result.setScale((short) ((FloatDataType) dataType).getScale());
            return result;
        } else if (dataType instanceof BooleanDataType) {
            BooleanFieldType result = new BooleanFieldType();
            copy(dataType, result);
            return result;
        } else if (dataType instanceof VarcharDataType) {
            VarcharFieldType result = new VarcharFieldType();
            copy(dataType, result);
            result.setEncodingType(((VarcharDataType) dataType).getEncodingType());
            result.setLength(((VarcharDataType) dataType).getLength());
            result.setIsMultiline(((VarcharDataType) dataType).isMultiLine());
            return result;
        } else if (dataType instanceof CharDataType) {
            CharFieldType result = new CharFieldType();
            copy(dataType, result);
            return result;
        } else if (dataType instanceof DateTimeDataType) {
            DateTimeFieldType result = new DateTimeFieldType();
            copy(dataType, result);
            return result;
        } else if (dataType instanceof TimeOfDayDataType) {
            TimeOfDayFieldType result = new TimeOfDayFieldType();
            copy(dataType, result);
            return result;
        } else if (dataType instanceof BinaryDataType) {
            BinaryFieldType result = new BinaryFieldType();
            copy(dataType, result);
            result.setCompressionLevel((short) ((BinaryDataType) dataType).getCompressionLevel());
            result.setMaxSize(((BinaryDataType) dataType).getMaxSize());
            return result;
        } else if (dataType instanceof ClassDataType) {
            ClassFieldType result = new ClassFieldType();
            copy(dataType, result);
            result.setTypeDescriptors(Arrays.stream(((ClassDataType) dataType).getDescriptors())
                    .map(rcd -> {
                        DescriptorRef ref = new DescriptorRef();
                        ref.setName(rcd.getName());
                        return ref;
                    })
                    .collect(ObjectArrayList::new, ObjectArrayList::add, ObjectArrayList::addAll)
            );
            return result;
        } else if (dataType instanceof ArrayDataType) {
            ArrayFieldType result = new ArrayFieldType();
            copy(dataType, result);
            result.setElementType(convert(((ArrayDataType) dataType).getElementDataType()));
            return result;
        } else if (dataType instanceof EnumDataType) {
            EnumFieldType result = new EnumFieldType();
            copy(dataType, result);
            DescriptorRef ref = new DescriptorRef();
            ref.setName(((EnumDataType) dataType).getDescriptor().getName());
            result.setTypeDescriptor(ref);
            return result;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static void copy(DataType from, FieldType to) {
        to.setEncoding(from.getEncoding());
        to.setIsNullable(from.isNullable());
    }

    private static EnumConstant convert(EnumValue value) {
        EnumConstant result = new EnumConstant();
        result.setSymbol(value.symbol);
        result.setValue((short) value.value);
        return result;
    }

    private static Argument convert(com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.functions.Argument arg) {
        Argument result = new Argument();
        result.setName(arg.getName());
        result.setDataType(convert(arg.getDataType()));
        return result;
    }

    private static InitArgument convert(com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.functions.InitArgument arg) {
        InitArgument result = new InitArgument();
        result.setName(arg.getName());
        result.setDataType(convert(arg.getDataType()));
        result.setDefaultValue(arg.getDefaultValue());
        return result;
    }

    private static StatefulFunctionMessage convert(StatefulFunctionDescriptor f) {
        StatefulFunctionMessage msg = new StatefulFunctionMessage();
        msg.setSymbol(f.id());
        msg.setTimeStampMs(System.currentTimeMillis());
        msg.setId(f.id());
        msg.setReturnType(convert(f.returnType()));
        msg.setArguments(f.args() != null ? f.args().stream()
                .map(InternalFunctions::convert)
                .collect(ObjectArrayList::new, ObjectArrayList::add, ObjectArrayList::addAll) : new ObjectArrayList<>()
        );
        msg.setInitArguments(f.initArgs() != null ? f.initArgs().stream()
                .map(InternalFunctions::convert)
                .collect(ObjectArrayList::new, ObjectArrayList::add, ObjectArrayList::addAll) : new ObjectArrayList<>()
        );
        return msg;
    }

    private static StatelessFunctionMessage convert(StatelessFunctionDescriptor f) {
        StatelessFunctionMessage msg = new StatelessFunctionMessage();
        msg.setSymbol(f.id());
        msg.setTimeStampMs(System.currentTimeMillis());
        msg.setId(f.id());
        msg.setReturnType(convert(f.returnType()));
        msg.setArguments(f.arguments() != null ? f.arguments().stream()
                .map(InternalFunctions::convert)
                .collect(ObjectArrayList::new, ObjectArrayList::add, ObjectArrayList::addAll) : new ObjectArrayList<>()
        );
        return msg;
    }

}