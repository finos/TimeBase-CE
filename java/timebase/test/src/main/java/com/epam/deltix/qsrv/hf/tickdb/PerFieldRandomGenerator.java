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

package com.epam.deltix.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.pub.codec.AlphanumericCodec;
import com.epam.deltix.qsrv.hf.pub.md.BooleanDataType;
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;
import com.epam.deltix.qsrv.test.messages.AllListsMessage;
import com.epam.deltix.qsrv.test.messages.AllSimpleTypesMessage;
import com.epam.deltix.qsrv.test.messages.AllTypesMessage;
import com.epam.deltix.qsrv.test.messages.TestEnum;
import com.epam.deltix.util.annotations.Bool;
import com.epam.deltix.util.annotations.TimeOfDay;
import com.epam.deltix.util.annotations.TimestampMs;
import com.epam.deltix.util.collections.generated.IntegerToObjectHashMap;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

public class PerFieldRandomGenerator implements Generator {

    private final long seed;

    private Random random;

    private final Map<String, Random> randoms = new HashMap<>();
    private final int listSize;
    private final int stringSize;
    private final IntegerToObjectHashMap<AlphanumericCodec> codecs = new IntegerToObjectHashMap<>();

    public PerFieldRandomGenerator(int listSize) {
        this(listSize, 10);
    }

    public PerFieldRandomGenerator(int listSize, int stringSize) {
        this(System.currentTimeMillis(), listSize, stringSize);
    }

    public PerFieldRandomGenerator(long seed, int listSize, int stringSize) {
        this.seed = seed;
        this.listSize = listSize;
        this.stringSize = stringSize;
    }

    @Override
    public int getListSize() {
        return listSize;
    }

    @Override
    public int getStringSize() {
        return stringSize;
    }

    private Random random(String fieldName) {
        return randoms.computeIfAbsent(fieldName, k -> new Random(seed));
    }

    private void nextField(String filedName) {
        random = random(filedName);
    }

    private <T> T next(String field, Supplier<T> supplier) {
        nextField(field);
        return supplier.get();
    }

    @Override
    public boolean returnNull() {
        return random.nextBoolean();
    }

    @Override
    public byte nextByte() {
        return (byte)(TimebaseTypes.INT8_NULL + 1 + random.nextInt(255));
    }

    @Override
    public short nextShort() {
        return (short)(TimebaseTypes.INT16_NULL + 1 + random.nextInt(65535));
    }

    @Override
    public int nextInt() {
        return random.nextInt();
    }

    @Override
    public long nextLong() {
        return random.nextLong();
    }

    @Override
    public float nextFloat() {
        return random.nextFloat() * 1000;
    }

    @Override
    public double nextDouble() {
        return random.nextDouble() * 1000;
    }

    @Bool
    @Override
    public byte nextBoolean() {
        return random.nextBoolean() ? BooleanDataType.TRUE: BooleanDataType.FALSE;
    }

    @Override
    public char nextChar() {
        return (char) (random.nextInt('\uafff')); // skip surogate pairs
    }

    @Override
    public char nextCharAlphaNumeric() {
        return (char) (0x20 + random.nextInt(0x5F - 0x20 + 1));
    }

    @Override
    public char nextCharAscii() {
        return asciiChars[random.nextInt(asciiChars.length)];
    }

    @TimeOfDay
    @Override
    public int nextTimeOfDay() {
        return 0;
    }

    @TimestampMs
    @Override
    public long nextTimestampMs() {
        return System.currentTimeMillis() - random.nextInt(20 * 24 * 60 * 60 * 1000);
    }

    @Nonnull
    @Override
    public TestEnum nextEnum() {
        return TestEnum.values()[random.nextInt(TestEnum.values().length)];
    }

    private static final char[] asciiChars = ("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890!@#$%^&*()_+-=[]\\{}|" +
            ";':\",./<>?АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя").toCharArray();

    @Override
    public AlphanumericCodec getCodec(int size) {
        AlphanumericCodec codec = codecs.get(size, null);
        if (codec == null) {
            codec = new AlphanumericCodec(size);
            codecs.put(size, codec);
        }
        return codec;
    }

    @Override
    synchronized public void fillAllSimpleTypesMessage(AllSimpleTypesMessage message) {
        nextField("Bool");
        message.setBoolField(nextBoolean());
        nextField("BoolNullable");
        message.setBoolNullableField(nextBooleanNullable());

        nextField("Byte");
        message.setByteField(nextByte());
        nextField("ByteNullable");
        message.setByteNullableField(nextByteNullable());

        nextField("Binary");
        message.setBinaryField(nextBinaryArray());
        nextField("BinaryNullable");
        message.setBinaryNullableField(nextBinaryArrayNullable());

        nextField("Decimal");
        message.setDecimalField(nextDecimal());
        nextField("DecimalNullable");
        message.setDecimalNullableField(nextDecimalNullable());

        nextField("Double");
        message.setDoubleField(nextDouble());
        nextField("DoubleNullable");
        message.setDoubleNullableField(nextDoubleNullable());

        nextField("Float");
        message.setFloatField(nextFloat());
        nextField("FloatNullable");
        message.setFloatNullableField(nextFloatNullable());

        nextField("Short");
        message.setShortField(nextShort());
        nextField("ShortNullable");
        message.setShortNullableField(nextShortNullable());

        nextField("Int");
        message.setIntField(nextInt());
        nextField("IntNullable");
        message.setIntNullableField(nextIntNullable());

        nextField("Long");
        message.setLongField(nextLong());
        nextField("LongNullable");
        message.setLongNullableField(nextLongNullable());

        nextField("TextAlphaNumeric");
        message.setTextAlphaNumericField(nextAlphaNumeric(10));
        nextField("TextAlphaNumericNullable");
        message.setTextAlphaNumericNullableField(nextAlphaNumericNullable(10));

        nextField("Text");
        message.setTextField(nextCharSequence());
        nextField("TextNullable");
        message.setTextNullableField(nextCharSequenceNullable());

        nextField("AsciiText");
        message.setAsciiTextField(nextAsciiCharSequence());
        nextField("AsciiTextNullable");
        message.setAsciiTextNullableField(nextAsciiCharSequenceNullable());

        nextField("Timestamp");
        message.setTimestampField(nextTimestampMs());
        nextField("TimestampNullable");
        message.setTimestampNullableField(nextTimestampMsNullable());

        nextField("TimeOfDay");
        message.setTimeOfDayField(nextTimeOfDay());
        nextField("TimeOfDayNullable");
        message.setTimeOfDayNullableField(nextTimeOfDayNullable());

        nextField("Enum");
        message.setEnumField(nextEnum());
        nextField("EnumNullable");
        message.setEnumNullableField(nextEnumNullable());
    }

    @Override
    synchronized public void fillAllTypesMessage(AllTypesMessage message) {
        fillAllSimpleTypesMessage(message);

        nextField("BooleanList");
        message.setBooleanList(nextBooleanList());
        nextField("BooleanListOfNullable");
        message.setBooleanListOfNullable(nextBooleanListOfNullable());
        nextField("NullableBooleanList");
        message.setNullableBooleanList(nextNullableBooleanList());
        nextField("NullableBooleanListOfNullable");
        message.setNullableBooleanListOfNullable(nextNullableBooleanListOfNullable());

        nextField("ByteList");
        message.setByteList(nextByteList());
        nextField("ByteListOfNullable");
        message.setByteListOfNullable(nextByteListOfNullable());
        nextField("NullableByteList");
        message.setNullableByteList(nextNullableByteList());
        nextField("NullableByteListOfNullable");
        message.setNullableByteListOfNullable(nextNullableByteListOfNullable());

        nextField("DecimalList");
        message.setDecimalList(nextDecimalList());
        nextField("DecimalListOfNullable");
        message.setDecimalListOfNullable(nextDecimalListOfNullable());
        nextField("NullableDecimalList");
        message.setNullableDecimalList(nextNullableDecimalList());
        nextField("NullableDecimalListOfNullable");
        message.setNullableDecimalListOfNullable(nextNullableDecimalListOfNullable());

        nextField("DoubleList");
        message.setDoubleList(nextDoubleList());
        nextField("DoubleListOfNullable");
        message.setDoubleListOfNullable(nextDoubleListOfNullable());
        nextField("NullableDoubleList");
        message.setNullableDoubleList(nextNullableDoubleList());
        nextField("NullableDoubleListOfNullable");
        message.setNullableDoubleListOfNullable(nextNullableDoubleListOfNullable());

        nextField("FloatList");
        message.setFloatList(nextFloatList());
        nextField("FloatListOfNullable");
        message.setFloatListOfNullable(nextFloatListOfNullable());
        nextField("NullableFloatList");
        message.setNullableFloatList(nextNullableFloatList());
        nextField("NullableFloatListOfNullable");
        message.setNullableFloatListOfNullable(nextNullableFloatListOfNullable());

        nextField("IntList");
        message.setIntList(nextIntegerList());
        nextField("IntListOfNullable");
        message.setIntListOfNullable(nextIntegerListOfNullable());
        nextField("NullableIntList");
        message.setNullableIntList(nextNullableIntegerList());
        nextField("NullableIntListOfNullable");
        message.setNullableIntListOfNullable(nextNullableIntegerListOfNullable());

        nextField("ShortList");
        message.setShortList(nextShortList());
        nextField("ShortListOfNullable");
        message.setShortListOfNullable(nextShortListOfNullable());
        nextField("NullableShortList");
        message.setNullableShortList(nextNullableShortList());
        nextField("NullableShortListOfNullable");
        message.setNullableShortListOfNullable(nextNullableShortListOfNullable());

        nextField("LongList");
        message.setLongList(nextLongList());
        nextField("LongListOfNullable");
        message.setLongListOfNullable(nextLongListOfNullable());
        nextField("NullableLongList");
        message.setNullableLongList(nextNullableLongList());
        nextField("NullableLongListOfNullable");
        message.setNullableLongListOfNullable(nextNullableLongListOfNullable());

        nextField("TextList");
        message.setTextList(nextCharSequenceList());
        nextField("TextListOfNullable");
        message.setTextListOfNullable(nextCharSequenceListOfNullable());
        nextField("NullableTextList");
        message.setNullableTextList(nextNullableCharSequenceList());
        nextField("NullableTextListOfNullable");
        message.setNullableTextListOfNullable(nextNullableCharSequenceListOfNullable());

        nextField("AsciiTextList");
        message.setAsciiTextList(nextAsciiCharSequenceList());
        nextField("AsciiTextListOfNullable");
        message.setAsciiTextListOfNullable(nextAsciiCharSequenceListOfNullable());
        nextField("NullableAsciiTextList");
        message.setNullableAsciiTextList(nextNullableAsciiCharSequenceList());
        nextField("NullableAsciiTextListOfNullable");
        message.setNullableAsciiTextListOfNullable(nextNullableAsciiCharSequenceListOfNullable());

        nextField("AlphanumericList");
        message.setAlphanumericList(nextAlphaNumericList());
        nextField("AlphanumericListOfNullable");
        message.setAlphanumericListOfNullable(nextAlphaNumericListOfNullable());
        nextField("NullableAlphanumericList");
        message.setNullableAlphanumericList(nextNullableAlphaNumericList());
        nextField("NullableAlphanumericListOfNullable");
        message.setNullableAlphanumericListOfNullable(nextNullableAlphaNumericListOfNullable());

        nextField("TimeOfDayList");
        message.setTimeOfDayList(nextTimeOfDayList());
        nextField("TimeOfDayListOfNullable");
        message.setTimeOfDayListOfNullable(nextTimeOfDayListOfNullable());
        nextField("NullableTimeOfDayList");
        message.setNullableTimeOfDayList(nextNullableTimeOfDayList());
        nextField("NullableTimeOfDayListOfNullable");
        message.setNullableTimeOfDayListOfNullable(nextNullableTimeOfDayListOfNullable());

        nextField("TimestampList");
        message.setTimestampList(nextTimestampList());
        nextField("TimestampListOfNullable");
        message.setTimestampListOfNullable(nextTimestampListOfNullable());
        nextField("NullableTimestampList");
        message.setNullableTimestampList(nextNullableTimestampList());
        nextField("NullableTimestampListOfNullable");
        message.setNullableTimestampListOfNullable(nextNullableTimestampListOfNullable());

        nextField("EnumList");
        message.setEnumList(nextEnumList());
        nextField("EnumListOfNullable");
        message.setEnumListOfNullable(nextEnumListOfNullable());
        nextField("NullableEnumList");
        message.setNullableEnumList(nextNullableEnumList());
        nextField("NullableEnumListOfNullable");
        message.setNullableEnumListOfNullable(nextNullableEnumListOfNullable());

        message.setObject(nextSimpleMessage());
        message.setObjectsList(nextObjectsList());
        message.setObjectsListOfNullable(nextObjectsListOfNullable());
        message.setNullableObjectsList(nextNullableObjectsList());
        message.setNullableObjectsListOfNullable(nextNullableObjectsListOfNullable());
        message.setLists(nextAllListsMessage(new AllListsMessage()));
        message.setListOfLists(nextListOfLists());
    }

    @Override
    synchronized public AllListsMessage nextAllListsMessage(AllListsMessage message) {
        nextField("NestedBooleanList");
        message.setNestedBooleanList(nextBooleanList());
        nextField("NestedByteList");
        message.setNestedByteList(nextByteList());
        nextField("NestedDecimalList");
        message.setNestedDecimalList(nextDecimalList());
        nextField("NestedDoubleList");
        message.setNestedDoubleList(nextDoubleList());
        nextField("NestedFloatList");
        message.setNestedFloatList(nextFloatList());
        nextField("NestedIntList");
        message.setNestedIntList(nextIntegerList());
        nextField("NestedShortList");
        message.setNestedShortList(nextShortList());
        nextField("NestedLongList");
        message.setNestedLongList(nextLongList());
        nextField("NestedTextList");
        message.setNestedTextList(nextCharSequenceList());
        nextField("NestedAsciiTextList");
        message.setNestedAsciiTextList(nextAsciiCharSequenceList());
        nextField("NestedAlphanumericList");
        message.setNestedAlphanumericList(nextAlphaNumericList());
        nextField("NestedObjectsList");
        message.setNestedObjectsList(nextObjectsList());

        return message;
    }
}
