package com.epam.deltix.qsrv.hf.tickdb;

import com.epam.deltix.containers.BinaryArray;
import com.epam.deltix.containers.BinaryAsciiString;
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.codec.AlphanumericCodec;
import com.epam.deltix.qsrv.test.messages.*;
import com.epam.deltix.util.annotations.Alphanumeric;
import com.epam.deltix.util.annotations.Bool;
import com.epam.deltix.util.annotations.TimeOfDay;
import com.epam.deltix.util.annotations.TimestampMs;
import com.epam.deltix.util.collections.generated.*;

import javax.annotation.Nonnull;

import static com.epam.deltix.qsrv.hf.tickdb.FixedGenerator.*;

public interface Generator {

    static Generator createFixed(int listSize, int stringSize) {
        return new FixedGenerator(
                listSize,
                stringSize,
                BYTE_VALUE,
                SHORT_VALUE,
                CHAR_VALUE,
                INT_VALUE,
                LONG_VALUE,
                FLOAT_VALUE,
                DOUBLE_VALUE,
                BOOLEAN_VALUE,
                CHAR_ALPHANUMERIC,
                CHAR_ALPHANUMERIC,
                TIMEOFDAY_VALUE,
                TIMESTAMP_VALUE,
                TestEnum.ONE
        );
    }

    static Generator createFixedRandom(int listSize, int stringSize) {
        return new FixedGenerator(new RandomGenerator(listSize, stringSize));
    }

    static Generator createFixedRandom(long seed, int listSize, int stringSize) {
        return new FixedGenerator(new RandomGenerator(seed, listSize, stringSize));
    }

    static Generator createRandom(int listSize, int stringSize) {
        return new RandomGenerator(listSize, stringSize);
    }

    static Generator createRandom(long seed, int listSize, int stringSize) {
        return new RandomGenerator(seed, listSize, stringSize);
    }

    // parameters

    int getListSize();

    int getStringSize();

    boolean returnNull();

    // data

    byte nextByte();

    short nextShort();

    int nextInt();

    long nextLong();

    float nextFloat();

    double nextDouble();

    @Bool
    byte nextBoolean();

    char nextChar();

    char nextCharAlphaNumeric();

    char nextCharAscii();

    @TimeOfDay
    int nextTimeOfDay();

    @TimestampMs
    long nextTimestampMs();

    @Nonnull
    TestEnum nextEnum();

    @Decimal
    default long nextDecimal() {
        return Decimal64Utils.fromDouble(nextDouble());
    }

    @TimeOfDay
    default int nextTimeOfDayNullable() {
        return returnNull() ? TimebaseTypes.TIMEOFDAY_NULL: nextTimeOfDay();
    }

    @TimestampMs
    default long nextTimestampMsNullable() {
        return returnNull() ? TimebaseTypes.DATETIME_NULL: nextTimestampMs();
    }

    @Decimal
    default long nextDecimalNullable() {
        return returnNull() ? TimebaseTypes.DECIMAL64_NULL: Decimal64Utils.fromDouble(nextDouble());
    }

    default byte nextByteNullable() {
        return returnNull() ? TimebaseTypes.INT8_NULL : nextByte();
    }

    default short nextShortNullable() {
        return returnNull() ? TimebaseTypes.INT16_NULL : nextShort();
    }

    default int nextIntNullable() {
        return returnNull() ? TimebaseTypes.INT32_NULL : nextInt();
    }

    default long nextLongNullable() {
        return returnNull() ? TimebaseTypes.INT64_NULL : nextLong();
    }

    default float nextFloatNullable() {
        return returnNull() ? TimebaseTypes.FLOAT32_NULL : nextFloat();
    }

    default double nextDoubleNullable() {
        return returnNull() ? TimebaseTypes.FLOAT64_NULL : nextDouble();
    }

    @Bool
    default byte nextBooleanNullable() {
        return returnNull() ? TimebaseTypes.BOOLEAN_NULL : nextBoolean();
    }

    default char nextCharNullable() {
        return returnNull() ? TimebaseTypes.CHAR_NULL : nextChar();
    }

    default char nextCharAlphaNumericNullable() {
        return returnNull() ? TimebaseTypes.CHAR_NULL : nextCharAlphaNumeric();
    }

    default char nextCharAsciiNullable() {
        return returnNull() ? TimebaseTypes.CHAR_NULL: nextCharAscii();
    }

    default long nextAlphaNumeric(int size) {
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append(nextCharAlphaNumeric());
        }
        return getCodec(size).encodeToLong(sb);
    }

    default long nextAlphaNumericNullable(int size) {
        return returnNull() ? TimebaseTypes.ALPHANUMERIC_NULL: nextAlphaNumeric(size);
    }

    default CharSequence nextCharSequence() {
        StringBuilder sb = new StringBuilder(getStringSize());
        for (int i = 0; i < getStringSize(); i++) {
            sb.append(nextChar());
        }
        return sb;
    }

    default CharSequence nextCharSequenceNullable() {
        return returnNull() ? null: nextCharSequence();
    }

    default CharSequence nextAsciiCharSequence() {
        BinaryAsciiString sb = new BinaryAsciiString(getStringSize());
        for (int i = 0; i < getStringSize(); i++)
            sb.append(nextCharAscii());
        return sb;
    }

    default CharSequence nextAsciiCharSequenceNullable() {
        return returnNull() ? null: nextAsciiCharSequence();
    }

    default TestEnum nextEnumNullable() {
        return returnNull() ? null: nextEnum();
    }

    default byte[] nextBinary() {
        byte[] bytes = new byte[getListSize()];
        for (int i = 0; i < getListSize(); i++) {
            bytes[i] = nextByte();
        }
        return bytes;
    }

    default BinaryArray nextBinaryArray() {
        return new BinaryArray(nextBinary());
    }

    default BinaryArray nextBinaryArrayNullable() {
        return returnNull() ? null: new BinaryArray(nextBinary());
    }

    default ByteArrayList nextByteList() {
        ByteArrayList list = new ByteArrayList(getListSize());
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextByte());;
        }
        return list;
    }

    default ByteArrayList nextByteListOfNullable() {
        ByteArrayList list = new ByteArrayList(getListSize());
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextByteNullable());;
        }
        return list;
    }

    default ByteArrayList nextNullableByteList() {
        return returnNull() ? null: nextByteList();
    }

    default ByteArrayList nextNullableByteListOfNullable() {
        return returnNull() ? null: nextByteListOfNullable();
    }

    default ShortArrayList nextShortList() {
        ShortArrayList list = new ShortArrayList(getListSize());
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextShort());;
        }
        return list;
    }

    default ShortArrayList nextShortListOfNullable() {
        ShortArrayList list = new ShortArrayList(getListSize());
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextShortNullable());;
        }
        return list;
    }

    default ShortArrayList nextNullableShortList() {
        return returnNull() ? null: nextShortList();
    }

    default ShortArrayList nextNullableShortListOfNullable() {
        return returnNull() ? null: nextShortListOfNullable();
    }

    default IntegerArrayList nextIntegerList() {
        IntegerArrayList list = new IntegerArrayList(getListSize());
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextInt());;
        }
        return list;
    }

    default IntegerArrayList nextIntegerListOfNullable() {
        IntegerArrayList list = new IntegerArrayList(getListSize());
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextIntNullable());;
        }
        return list;
    }

    default IntegerArrayList nextNullableIntegerList() {
        return returnNull() ? null: nextIntegerList();
    }

    default IntegerArrayList nextNullableIntegerListOfNullable() {
        return returnNull() ? null: nextIntegerListOfNullable();
    }

    @TimeOfDay
    default IntegerArrayList nextTimeOfDayList() {
        IntegerArrayList list = new IntegerArrayList(getListSize());
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextTimeOfDay());;
        }
        return list;
    }

    @TimeOfDay
    default IntegerArrayList nextTimeOfDayListOfNullable() {
        IntegerArrayList list = new IntegerArrayList(getListSize());
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextTimeOfDayNullable());;
        }
        return list;
    }

    @TimeOfDay
    default IntegerArrayList nextNullableTimeOfDayList() {
        return returnNull() ? null: nextTimeOfDayList();
    }

    @TimeOfDay
    default IntegerArrayList nextNullableTimeOfDayListOfNullable() {
        return returnNull() ? null: nextTimeOfDayListOfNullable();
    }

    default LongArrayList nextLongList() {
        LongArrayList list = new LongArrayList(getListSize());
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextLong());;
        }
        return list;
    }

    default LongArrayList nextLongListOfNullable() {
        LongArrayList list = new LongArrayList(getListSize());
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextLongNullable());;
        }
        return list;
    }

    default LongArrayList nextNullableLongList() {
        return returnNull() ? null: nextLongList();
    }

    default LongArrayList nextNullableLongListOfNullable() {
        return returnNull() ? null: nextLongListOfNullable();
    }

    @TimestampMs
    default LongArrayList nextTimestampList() {
        LongArrayList list = new LongArrayList(getListSize());
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextTimestampMs());;
        }
        return list;
    }

    @TimestampMs
    default LongArrayList nextTimestampListOfNullable() {
        LongArrayList list = new LongArrayList(getListSize());
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextTimestampMsNullable());;
        }
        return list;
    }

    @TimestampMs
    default LongArrayList nextNullableTimestampList() {
        return returnNull() ? null: nextTimestampList();
    }

    @TimestampMs
    default LongArrayList nextNullableTimestampListOfNullable() {
        return returnNull() ? null: nextTimestampListOfNullable();
    }

    default ObjectArrayList<TestEnum> nextEnumList() {
        ObjectArrayList<TestEnum> list = new ObjectArrayList<>(getListSize());
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextEnum());;
        }
        return list;
    }

    default ObjectArrayList<TestEnum> nextEnumListOfNullable() {
        ObjectArrayList<TestEnum> list = new ObjectArrayList<>(getListSize());
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextEnumNullable());;
        }
        return list;
    }

    default ObjectArrayList<TestEnum> nextNullableEnumList() {
        return returnNull() ? null: nextEnumList();
    }

    default ObjectArrayList<TestEnum> nextNullableEnumListOfNullable() {
        return returnNull() ? null: nextEnumListOfNullable();
    }

    default FloatArrayList nextFloatList() {
        FloatArrayList list = new FloatArrayList(getListSize());
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextFloat());;
        }
        return list;
    }

    default FloatArrayList nextFloatListOfNullable() {
        FloatArrayList list = new FloatArrayList(getListSize());
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextFloatNullable());;
        }
        return list;
    }

    default FloatArrayList nextNullableFloatList() {
        return returnNull() ? null: nextFloatList();
    }

    default FloatArrayList nextNullableFloatListOfNullable() {
        return returnNull() ? null: nextFloatListOfNullable();
    }

    default DoubleArrayList nextDoubleList() {
        DoubleArrayList list = new DoubleArrayList(getListSize());
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextDouble());;
        }
        return list;
    }

    default DoubleArrayList nextDoubleListOfNullable() {
        DoubleArrayList list = new DoubleArrayList(getListSize());
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextDoubleNullable());;
        }
        return list;
    }

    default DoubleArrayList nextNullableDoubleList() {
        return returnNull() ? null: nextDoubleList();
    }

    default DoubleArrayList nextNullableDoubleListOfNullable() {
        return returnNull() ? null: nextDoubleListOfNullable();
    }

    @Decimal
    default LongArrayList nextDecimalList() {
        LongArrayList list = new LongArrayList(getListSize());
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextDecimal());;
        }
        return list;
    }

    @Decimal
    default LongArrayList nextDecimalListOfNullable() {
        LongArrayList list = new LongArrayList(getListSize());
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextDecimalNullable());;
        }
        return list;
    }

    @Decimal
    default LongArrayList nextNullableDecimalList() {
        return returnNull() ? null: nextDecimalList();
    }

    @Decimal
    default LongArrayList nextNullableDecimalListOfNullable() {
        return returnNull() ? null: nextDecimalListOfNullable();
    }

    @Bool
    default ByteArrayList nextBooleanList() {
        ByteArrayList list = new ByteArrayList(getListSize());
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextBoolean());
        }
        return list;
    }

    @Bool
    default ByteArrayList nextBooleanListOfNullable() {
        ByteArrayList list = new ByteArrayList(getListSize());
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextBooleanNullable());
        }
        return list;
    }

    @Bool
    default ByteArrayList nextNullableBooleanList() {
        return returnNull() ? null: nextBooleanList();
    }

    @Bool
    default ByteArrayList nextNullableBooleanListOfNullable() {
        return returnNull() ? null: nextBooleanListOfNullable();
    }

    default ObjectArrayList<CharSequence> nextCharSequenceList() {
        ObjectArrayList<CharSequence> list = new ObjectArrayList<>();
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextCharSequence());
        }
        return list;
    }

    default ObjectArrayList<CharSequence> nextCharSequenceListOfNullable() {
        ObjectArrayList<CharSequence> list = new ObjectArrayList<>();
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextCharSequenceNullable());
        }
        return list;
    }

    default ObjectArrayList<CharSequence> nextNullableCharSequenceList() {
        return returnNull() ? null: nextCharSequenceList();
    }

    default ObjectArrayList<CharSequence> nextNullableCharSequenceListOfNullable() {
        return returnNull() ? null: nextCharSequenceListOfNullable();
    }

    default ObjectArrayList<CharSequence> nextAsciiCharSequenceList() {
        ObjectArrayList<CharSequence> list = new ObjectArrayList<>();
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextAsciiCharSequence());
        }
        return list;
    }

    default ObjectArrayList<CharSequence> nextAsciiCharSequenceListOfNullable() {
        ObjectArrayList<CharSequence> list = new ObjectArrayList<>();
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextAsciiCharSequenceNullable());
        }
        return list;
    }

    default ObjectArrayList<CharSequence> nextNullableAsciiCharSequenceList() {
        return returnNull() ? null: nextAsciiCharSequenceList();
    }

    default ObjectArrayList<CharSequence> nextNullableAsciiCharSequenceListOfNullable() {
        return returnNull() ? null: nextAsciiCharSequenceListOfNullable();
    }

    @Alphanumeric
    default LongArrayList nextAlphaNumericList() {
        LongArrayList list = new LongArrayList();
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextAlphaNumeric(10));
        }
        return list;
    }

    @Alphanumeric
    default LongArrayList nextAlphaNumericListOfNullable() {
        LongArrayList list = new LongArrayList();
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextAlphaNumericNullable(10));
        }
        return list;
    }

    @Alphanumeric
    default LongArrayList nextNullableAlphaNumericList() {
        return returnNull() ? null: nextAlphaNumericList();
    }

    @Alphanumeric
    default LongArrayList nextNullableAlphaNumericListOfNullable() {
        return returnNull() ? null: nextAlphaNumericListOfNullable();
    }

    default ObjectArrayList<AllListsMessageInfo> nextListOfLists() {
        ObjectArrayList<AllListsMessageInfo> lists = new ObjectArrayList<>();
        for (int i = 0; i < getListSize(); ++i) {
            lists.add(nextAllListsMessage(new AllListsMessage()));
        }

        return lists;
    }

    default ObjectArrayList<AllSimpleTypesMessageInfo> nextObjectsList() {
        ObjectArrayList<AllSimpleTypesMessageInfo> list = new ObjectArrayList<>(getListSize());
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextSimpleMessage());
        }
        return list;
    }

    default ObjectArrayList<AllSimpleTypesMessageInfo> nextObjectsListOfNullable() {
        ObjectArrayList<AllSimpleTypesMessageInfo> list = new ObjectArrayList<>(getListSize());
        for (int i = 0; i < getListSize(); i++) {
            list.add(nextSimpleMessageNullable());
        }
        return list;
    }

    default ObjectArrayList<AllSimpleTypesMessageInfo> nextNullableObjectsList() {
        return returnNull() ? null: nextObjectsList();
    }

    default ObjectArrayList<AllSimpleTypesMessageInfo> nextNullableObjectsListOfNullable() {
        return returnNull() ? null: nextObjectsListOfNullable();
    }

    default void fillAllSimpleTypesMessage(AllSimpleTypesMessage message) {
        message.setBoolField(nextBoolean());
        message.setBoolNullableField(nextBooleanNullable());

        message.setByteField(nextByte());
        message.setByteNullableField(nextByteNullable());

        message.setBinaryField(nextBinaryArray());
        message.setBinaryNullableField(nextBinaryArrayNullable());

        message.setDecimalField(nextDecimal());
        message.setDecimalNullableField(nextDecimalNullable());

        message.setDoubleField(nextDouble());
        message.setDoubleNullableField(nextDoubleNullable());

        message.setFloatField(nextFloat());
        message.setFloatNullableField(nextFloatNullable());

        message.setShortField(nextShort());
        message.setShortNullableField(nextShortNullable());

        message.setIntField(nextInt());
        message.setIntNullableField(nextIntNullable());

        message.setLongField(nextLong());
        message.setLongNullableField(nextLongNullable());

        message.setTextAlphaNumericField(nextAlphaNumeric(10));
        message.setTextAlphaNumericNullableField(nextAlphaNumericNullable(10));

        message.setTextField(nextCharSequence());
        message.setTextNullableField(nextCharSequenceNullable());

        message.setAsciiTextField(nextAsciiCharSequence());
        message.setAsciiTextNullableField(nextAsciiCharSequenceNullable());

        message.setTimestampField(nextTimestampMs());
        message.setTimestampNullableField(nextTimestampMsNullable());

        message.setTimeOfDayField(nextTimeOfDay());
        message.setTimeOfDayNullableField(nextTimeOfDayNullable());

        message.setEnumField(nextEnum());
        message.setEnumNullableField(nextEnumNullable());
    }

    default void fillAllTypesMessage(AllTypesMessage message) {
        fillAllSimpleTypesMessage(message);

        message.setObject(nextSimpleMessage());

        message.setBooleanList(nextBooleanList());
        message.setBooleanListOfNullable(nextBooleanListOfNullable());
        message.setNullableBooleanList(nextNullableBooleanList());
        message.setNullableBooleanListOfNullable(nextNullableBooleanListOfNullable());

        message.setByteList(nextByteList());
        message.setByteListOfNullable(nextByteListOfNullable());
        message.setNullableByteList(nextNullableByteList());
        message.setNullableByteListOfNullable(nextNullableByteListOfNullable());

        message.setDecimalList(nextDecimalList());
        message.setDecimalListOfNullable(nextDecimalListOfNullable());
        message.setNullableDecimalList(nextNullableDecimalList());
        message.setNullableDecimalListOfNullable(nextNullableDecimalListOfNullable());

        message.setDoubleList(nextDoubleList());
        message.setDoubleListOfNullable(nextDoubleListOfNullable());
        message.setNullableDoubleList(nextNullableDoubleList());
        message.setNullableDoubleListOfNullable(nextNullableDoubleListOfNullable());

        message.setFloatList(nextFloatList());
        message.setFloatListOfNullable(nextFloatListOfNullable());
        message.setNullableFloatList(nextNullableFloatList());
        message.setNullableFloatListOfNullable(nextNullableFloatListOfNullable());

        message.setIntList(nextIntegerList());
        message.setIntListOfNullable(nextIntegerListOfNullable());
        message.setNullableIntList(nextNullableIntegerList());
        message.setNullableIntListOfNullable(nextNullableIntegerListOfNullable());

        message.setShortList(nextShortList());
        message.setShortListOfNullable(nextShortListOfNullable());
        message.setNullableShortList(nextNullableShortList());
        message.setNullableShortListOfNullable(nextNullableShortListOfNullable());

        message.setLongList(nextLongList());
        message.setLongListOfNullable(nextLongListOfNullable());
        message.setNullableLongList(nextNullableLongList());
        message.setNullableLongListOfNullable(nextNullableLongListOfNullable());

        message.setObjectsList(nextObjectsList());
        message.setObjectsListOfNullable(nextObjectsListOfNullable());
        message.setNullableObjectsList(nextNullableObjectsList());
        message.setNullableObjectsListOfNullable(nextNullableObjectsListOfNullable());

        message.setTextList(nextCharSequenceList());
        message.setTextListOfNullable(nextCharSequenceListOfNullable());
        message.setNullableTextList(nextNullableCharSequenceList());
        message.setNullableTextListOfNullable(nextNullableCharSequenceListOfNullable());

        message.setAsciiTextList(nextAsciiCharSequenceList());
        message.setAsciiTextListOfNullable(nextAsciiCharSequenceListOfNullable());
        message.setNullableAsciiTextList(nextNullableAsciiCharSequenceList());
        message.setNullableAsciiTextListOfNullable(nextNullableAsciiCharSequenceListOfNullable());

        message.setAlphanumericList(nextAlphaNumericList());
        message.setAlphanumericListOfNullable(nextAlphaNumericListOfNullable());
        message.setNullableAlphanumericList(nextNullableAlphaNumericList());
        message.setNullableAlphanumericListOfNullable(nextNullableAlphaNumericListOfNullable());

        message.setLists(nextAllListsMessage(new AllListsMessage()));
        message.setListOfLists(nextListOfLists());

        message.setTimeOfDayList(nextTimeOfDayList());
        message.setTimeOfDayListOfNullable(nextTimeOfDayListOfNullable());
        message.setNullableTimeOfDayList(nextNullableTimeOfDayList());
        message.setNullableTimeOfDayListOfNullable(nextNullableTimeOfDayListOfNullable());

        message.setTimestampList(nextTimestampList());
        message.setTimestampListOfNullable(nextTimestampListOfNullable());
        message.setNullableTimestampList(nextNullableTimestampList());
        message.setNullableTimestampListOfNullable(nextNullableTimestampListOfNullable());

        message.setEnumList(nextEnumList());
        message.setEnumListOfNullable(nextEnumListOfNullable());
        message.setNullableEnumList(nextNullableEnumList());
        message.setNullableEnumListOfNullable(nextNullableEnumListOfNullable());
    }

    default AllListsMessage nextAllListsMessage(AllListsMessage message) {
        message.setNestedObjectsList(nextObjectsList());
        message.setNestedBooleanList(nextBooleanList());
        message.setNestedByteList(nextByteList());
        message.setNestedDecimalList(nextDecimalList());
        message.setNestedDoubleList(nextDoubleList());
        message.setNestedFloatList(nextFloatList());
        message.setNestedIntList(nextIntegerList());
        message.setNestedShortList(nextShortList());
        message.setNestedLongList(nextLongList());
        message.setNestedTextList(nextCharSequenceList());
        message.setNestedAsciiTextList(nextAsciiCharSequenceList());
        message.setNestedAlphanumericList(nextAlphaNumericList());

        return message;
    }

    default AllSimpleTypesMessage nextSimpleMessage() {
        AllSimpleTypesMessage message = new AllSimpleTypesMessage();
        fillAllSimpleTypesMessage(message);
        return message;
    }

    default AllSimpleTypesMessage nextSimpleMessageNullable() {
        return returnNull() ? null: nextSimpleMessage();
    }

    default AllTypesMessage nextMessage() {
        AllTypesMessage message = new AllTypesMessage();
        fillAllTypesMessage(message);
        return message;
    }

    default AllTypesMessage nextMessageNullable() {
        return returnNull() ? null: nextMessage();
    }

    default AlphanumericCodec getCodec(int size) {
        return new AlphanumericCodec(size);
    }
}
