/*
 * Copyright 2023 EPAM Systems, Inc
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
import com.epam.deltix.qsrv.test.messages.TestEnum;
import com.epam.deltix.util.annotations.*;
import com.epam.deltix.util.collections.generated.IntegerToObjectHashMap;

import javax.annotation.Nonnull;
import java.util.Random;

public class RandomGenerator implements Generator {

    private final Random random;
    private final int listSize;
    private final int stringSize;
    private final IntegerToObjectHashMap<AlphanumericCodec> codecs = new IntegerToObjectHashMap<>();

    public RandomGenerator(int listSize) {
        this(listSize, 10);
    }

    public RandomGenerator(int listSize, int stringSize) {
        this(System.currentTimeMillis(), listSize, stringSize);
    }

    public RandomGenerator(long seed, int listSize, int stringSize) {
        this.random = new Random(seed);
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
        return (char) (random.nextInt('\uffff' + 1));
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
}
