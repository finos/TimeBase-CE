package com.epam.deltix.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.pub.codec.AlphanumericCodec;
import com.epam.deltix.qsrv.hf.pub.md.BooleanDataType;
import com.epam.deltix.qsrv.hf.pub.md.DateTimeDataType;
import com.epam.deltix.qsrv.hf.pub.md.TimeOfDayDataType;
import com.epam.deltix.qsrv.test.messages.TestEnum;
import com.epam.deltix.util.annotations.Bool;
import com.epam.deltix.util.annotations.TimeOfDay;
import com.epam.deltix.util.annotations.TimestampMs;
import com.epam.deltix.util.collections.generated.IntegerToObjectHashMap;

import javax.annotation.Nonnull;

public class FixedGenerator implements Generator {

    public static final byte BYTE_VALUE = 125;
    public static final short SHORT_VALUE = 14325;
    public static final char CHAR_VALUE = '&';
    public static final int INT_VALUE = -236326;
    public static final long LONG_VALUE = 2312721721721L;
    public static final float FLOAT_VALUE = 32324.4334f;
    public static final double DOUBLE_VALUE = -436346.34734734834;
    @Bool
    public static final byte BOOLEAN_VALUE = BooleanDataType.TRUE;
    public static final char CHAR_ALPHANUMERIC = 'Q';
    @TimeOfDay
    public static final int TIMEOFDAY_VALUE = TimeOfDayDataType.staticParse("23:34:42");
    @TimestampMs
    public static final long TIMESTAMP_VALUE = DateTimeDataType.staticParse("2021-03-12 06:12:42.100");

    private final IntegerToObjectHashMap<AlphanumericCodec> codecs = new IntegerToObjectHashMap<>();

    private final int listSize;
    private final int stringSize;

    private final byte byteValue;
    private final short shortValue;
    private final char charValue;
    private final int intValue;
    private final long longValue;
    private final float floatValue;
    private final double doubleValue;
    @Bool
    private final byte booleanValue;
    private final char charAlphanumeric;
    private final char charAscii;
    @TimeOfDay
    private final int timeOfDay;
    @TimestampMs
    private final long timestampMs;

    FixedGenerator(int listSize, int stringSize, byte byteValue, short shortValue, char charValue, int intValue,
                   long longValue, float floatValue, double doubleValue, @Bool byte booleanValue, char charAlphanumeric,
                   char charAscii, @TimeOfDay int timeOfDay, @TimestampMs long timestampMs, @Nonnull TestEnum testEnum) {
        this.listSize = listSize;
        this.stringSize = stringSize;
        this.byteValue = byteValue;
        this.shortValue = shortValue;
        this.charValue = charValue;
        this.intValue = intValue;
        this.longValue = longValue;
        this.floatValue = floatValue;
        this.doubleValue = doubleValue;
        this.booleanValue = booleanValue;
        this.charAlphanumeric = charAlphanumeric;
        this.charAscii = charAscii;
        this.timeOfDay = timeOfDay;
        this.timestampMs = timestampMs;
    }

    FixedGenerator(Generator generator) {
        this.listSize = generator.getListSize();
        this.stringSize = generator.getStringSize();
        this.byteValue = generator.nextByte();
        this.shortValue = generator.nextShort();
        this.charValue = generator.nextChar();
        this.intValue = generator.nextInt();
        this.longValue = generator.nextLong();
        this.floatValue = generator.nextFloat();
        this.doubleValue = generator.nextDouble();
        this.booleanValue = generator.nextBoolean();
        this.charAlphanumeric = generator.nextCharAlphaNumeric();
        this.charAscii = generator.nextCharAscii();
        this.timeOfDay = generator.nextTimeOfDay();
        this.timestampMs = generator.nextTimeOfDayNullable();
    }

    @Override
    public int getListSize() {
        return listSize;
    }

    @Override
    public int getStringSize() {
        return stringSize;
    }

    @Override
    public boolean returnNull() {
        return false;
    }

    @Override
    public byte nextByte() {
        return byteValue;
    }

    @Override
    public short nextShort() {
        return shortValue;
    }

    @Override
    public int nextInt() {
        return intValue;
    }

    @Override
    public long nextLong() {
        return longValue;
    }

    @Override
    public float nextFloat() {
        return floatValue;
    }

    @Override
    public double nextDouble() {
        return doubleValue;
    }

    @Bool
    @Override
    public byte nextBoolean() {
        return booleanValue;
    }

    @Override
    public char nextChar() {
        return charValue;
    }

    @Override
    public char nextCharAlphaNumeric() {
        return charAlphanumeric;
    }

    @Override
    public char nextCharAscii() {
        return charAscii;
    }

    @TimeOfDay
    @Override
    public int nextTimeOfDay() {
        return timeOfDay;
    }

    @TimestampMs
    @Override
    public long nextTimestampMs() {
        return timestampMs;
    }

    @Nonnull
    @Override
    public TestEnum nextEnum() {
        return TestEnum.ONE;
    }

    @Override
    public AlphanumericCodec getCodec(int size) {
        AlphanumericCodec codec = codecs.get(size, null);
        if (codec == null) {
            codec = new AlphanumericCodec(size);
            codecs.put(size, codec);
        }
        return codec;
    }
}
