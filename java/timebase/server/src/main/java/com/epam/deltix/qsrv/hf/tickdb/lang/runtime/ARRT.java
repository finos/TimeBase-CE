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
package com.epam.deltix.qsrv.hf.tickdb.lang.runtime;

import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.qsrv.hf.codec.cg.StringBuilderPool;
import com.epam.deltix.qsrv.hf.pub.codec.AlphanumericCodec;
import com.epam.deltix.qsrv.hf.pub.md.CharDataType;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.Instance;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.InstancePool;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.containers.BaseInstanceArray;
import com.epam.deltix.util.BitUtil;
import com.epam.deltix.util.collections.generated.BooleanArrayList;
import com.epam.deltix.util.collections.generated.ByteArrayList;
import com.epam.deltix.util.collections.generated.CharacterArrayList;
import com.epam.deltix.util.collections.generated.DoubleArrayList;
import com.epam.deltix.util.collections.generated.FloatArrayList;
import com.epam.deltix.util.collections.generated.IntegerArrayList;
import com.epam.deltix.util.collections.generated.LongArrayList;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.collections.generated.ShortArrayList;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;

import java.util.List;

// todo: refactor this bunch of copy-paste code
public class ARRT {

    public static void encodeArray(List<?> list, MemoryDataOutput mdo) {
        if (list instanceof ByteArrayList) {
            encodeArray((ByteArrayList) list, mdo);
        } else if (list instanceof ShortArrayList) {
            encodeArray((ShortArrayList) list, mdo);
        } else if (list instanceof IntegerArrayList) {
            encodeArray((IntegerArrayList) list, mdo);
        } else if (list instanceof LongArrayList) {
            encodeArray((LongArrayList) list, mdo);
        } else if (list instanceof FloatArrayList) {
            encodeArray((FloatArrayList) list, mdo);
        } else if (list instanceof DoubleArrayList) {
            encodeArray((DoubleArrayList) list, mdo);
        } else if (list instanceof CharacterArrayList) {
            encodeArray((CharacterArrayList) list, mdo);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Encodes array without size and return overall size
     */
    public static int encodeArrayWithoutSize(List<?> list, MemoryDataOutput mdo) {
        if (list instanceof ByteArrayList) {
            return encodeArrayWithoutSize((ByteArrayList) list, mdo);
        } else if (list instanceof ShortArrayList) {
            return encodeArrayWithoutSize((ShortArrayList) list, mdo);
        } else if (list instanceof IntegerArrayList) {
            return encodeArrayWithoutSize((IntegerArrayList) list, mdo);
        } else if (list instanceof LongArrayList) {
            return encodeArrayWithoutSize((LongArrayList) list, mdo);
        } else if (list instanceof FloatArrayList) {
            return encodeArrayWithoutSize((FloatArrayList) list, mdo);
        } else if (list instanceof DoubleArrayList) {
            return encodeArrayWithoutSize((DoubleArrayList) list, mdo);
        } else if (list instanceof CharacterArrayList) {
            return encodeArrayWithoutSize((CharacterArrayList) list, mdo);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static void encodeArray(FloatArrayList list, MemoryDataOutput tempOut, MemoryDataOutput out) {
        tempOut.reset();
        MessageSizeCodec.write(list.size(), tempOut);
        for (int i = 0; i < list.size(); ++i) {
            tempOut.writeFloat(list.getFloat(i));
        }

        MessageSizeCodec.write(tempOut.getPosition(), out);
        out.write(tempOut.getBuffer(), 0, tempOut.getPosition());
    }

    public static void encodeArray(FloatArrayList list, MemoryDataOutput out) {
        int sizeOfSize = MessageSizeCodec.sizeOf(list.size());
        int sizeOfList = sizeOfSize + BitUtil.SIZE_OF_FLOAT * list.size();
        MessageSizeCodec.write(sizeOfList, out);
        MessageSizeCodec.write(list.size(), out);
        for (int i = 0; i < list.size(); ++i) {
            out.writeFloat(list.getFloat(i));
        }
    }

    public static int encodeArrayWithoutSize(FloatArrayList list, MemoryDataOutput out) {
        int sizeOfSize = MessageSizeCodec.sizeOf(list.size());
        int sizeOfList = sizeOfSize + BitUtil.SIZE_OF_FLOAT * list.size();
        MessageSizeCodec.write(list.size(), out);
        for (int i = 0; i < list.size(); ++i) {
            out.writeFloat(list.getFloat(i));
        }
        return sizeOfList;
    }

    public static void encodeArray(DoubleArrayList list, MemoryDataOutput tempOut, MemoryDataOutput out) {
        tempOut.reset();
        MessageSizeCodec.write(list.size(), tempOut);
        for (int i = 0; i < list.size(); ++i) {
            tempOut.writeDouble(list.getDouble(i));
        }

        MessageSizeCodec.write(tempOut.getPosition(), out);
        out.write(tempOut.getBuffer(), 0, tempOut.getPosition());
    }

    public static void encodeArray(DoubleArrayList list, MemoryDataOutput out) {
        int sizeOfSize = MessageSizeCodec.sizeOf(list.size());
        int sizeOfList = sizeOfSize + BitUtil.SIZE_OF_DOUBLE * list.size();
        MessageSizeCodec.write(sizeOfList, out);
        MessageSizeCodec.write(list.size(), out);
        for (int i = 0; i < list.size(); ++i) {
            out.writeDouble(list.getDouble(i));
        }
    }

    public static int encodeArrayWithoutSize(DoubleArrayList list, MemoryDataOutput out) {
        int sizeOfSize = MessageSizeCodec.sizeOf(list.size());
        int sizeOfList = sizeOfSize + BitUtil.SIZE_OF_DOUBLE * list.size();
        MessageSizeCodec.write(list.size(), out);
        for (int i = 0; i < list.size(); ++i) {
            out.writeDouble(list.getDouble(i));
        }
        return sizeOfList;
    }

    public static void encodeArray(ByteArrayList list, MemoryDataOutput tempOut, MemoryDataOutput out) {
        tempOut.reset();
        MessageSizeCodec.write(list.size(), tempOut);
        for (int i = 0; i < list.size(); ++i) {
            tempOut.writeByte(list.getByte(i));
        }

        MessageSizeCodec.write(tempOut.getPosition(), out);
        out.write(tempOut.getBuffer(), 0, tempOut.getPosition());
    }

    public static void encodeArray(ByteArrayList list, MemoryDataOutput out) {
        int sizeOfSize = MessageSizeCodec.sizeOf(list.size());
        int sizeOfList = sizeOfSize + BitUtil.SIZE_OF_BYTE * list.size();
        MessageSizeCodec.write(sizeOfList, out);
        MessageSizeCodec.write(list.size(), out);
        for (int i = 0; i < list.size(); ++i) {
            out.writeByte(list.getByte(i));
        }
    }

    public static int encodeArrayWithoutSize(ByteArrayList list, MemoryDataOutput out) {
        int sizeOfSize = MessageSizeCodec.sizeOf(list.size());
        int sizeOfList = sizeOfSize + BitUtil.SIZE_OF_BYTE * list.size();
        MessageSizeCodec.write(list.size(), out);
        for (int i = 0; i < list.size(); ++i) {
            out.writeByte(list.getByte(i));
        }
        return sizeOfList;
    }

    public static void encodeArray(ShortArrayList list, MemoryDataOutput tempOut, MemoryDataOutput out) {
        tempOut.reset();
        MessageSizeCodec.write(list.size(), tempOut);
        for (int i = 0; i < list.size(); ++i) {
            tempOut.writeShort(list.getShort(i));
        }

        MessageSizeCodec.write(tempOut.getPosition(), out);
        out.write(tempOut.getBuffer(), 0, tempOut.getPosition());
    }

    public static void encodeArray(ShortArrayList list, MemoryDataOutput out) {
        int sizeOfSize = MessageSizeCodec.sizeOf(list.size());
        int sizeOfList = sizeOfSize + BitUtil.SIZE_OF_SHORT * list.size();
        MessageSizeCodec.write(sizeOfList, out);
        MessageSizeCodec.write(list.size(), out);
        for (int i = 0; i < list.size(); ++i) {
            out.writeShort(list.getShort(i));
        }
    }

    public static int encodeArrayWithoutSize(ShortArrayList list, MemoryDataOutput out) {
        int sizeOfSize = MessageSizeCodec.sizeOf(list.size());
        int sizeOfList = sizeOfSize + BitUtil.SIZE_OF_SHORT * list.size();
        MessageSizeCodec.write(list.size(), out);
        for (int i = 0; i < list.size(); ++i) {
            out.writeShort(list.getShort(i));
        }
        return sizeOfList;
    }

    public static void encodeArray(IntegerArrayList list, MemoryDataOutput tempOut, MemoryDataOutput out) {
        tempOut.reset();
        MessageSizeCodec.write(list.size(), tempOut);
        for (int i = 0; i < list.size(); ++i) {
            tempOut.writeInt(list.getInteger(i));
        }

        MessageSizeCodec.write(tempOut.getPosition(), out);
        out.write(tempOut.getBuffer(), 0, tempOut.getPosition());
    }

    public static void encodeArray(IntegerArrayList list, MemoryDataOutput out) {
        int sizeOfSize = MessageSizeCodec.sizeOf(list.size());
        int sizeOfList = sizeOfSize + BitUtil.SIZE_OF_INT * list.size();
        MessageSizeCodec.write(sizeOfList, out);
        MessageSizeCodec.write(list.size(), out);
        for (int i = 0; i < list.size(); ++i) {
            out.writeInt(list.getInteger(i));
        }
    }

    public static int encodeArrayWithoutSize(IntegerArrayList list, MemoryDataOutput out) {
        int sizeOfSize = MessageSizeCodec.sizeOf(list.size());
        int sizeOfList = sizeOfSize + BitUtil.SIZE_OF_INT * list.size();
        MessageSizeCodec.write(list.size(), out);
        for (int i = 0; i < list.size(); ++i) {
            out.writeInt(list.getInteger(i));
        }
        return sizeOfList;
    }

    public static void encodeArray(LongArrayList list, MemoryDataOutput tempOut, MemoryDataOutput out) {
        tempOut.reset();
        MessageSizeCodec.write(list.size(), tempOut);
        for (int i = 0; i < list.size(); ++i) {
            tempOut.writeLong(list.getLong(i));
        }

        MessageSizeCodec.write(tempOut.getPosition(), out);
        out.write(tempOut.getBuffer(), 0, tempOut.getPosition());
    }

    public static void encodeArray(LongArrayList list, MemoryDataOutput out) {
        int sizeOfSize = MessageSizeCodec.sizeOf(list.size());
        int sizeOfList = sizeOfSize + BitUtil.SIZE_OF_LONG * list.size();
        MessageSizeCodec.write(sizeOfList, out);
        MessageSizeCodec.write(list.size(), out);
        for (int i = 0; i < list.size(); ++i) {
            out.writeLong(list.getLong(i));
        }
    }

    public static int encodeArrayWithoutSize(LongArrayList list, MemoryDataOutput out) {
        int sizeOfSize = MessageSizeCodec.sizeOf(list.size());
        int sizeOfList = sizeOfSize + BitUtil.SIZE_OF_LONG * list.size();
        MessageSizeCodec.write(list.size(), out);
        for (int i = 0; i < list.size(); ++i) {
            out.writeLong(list.getLong(i));
        }
        return sizeOfList;
    }

    public static int encodeArrayWithoutSize(ObjectArrayList<CharSequence> list, MemoryDataOutput out, AlphanumericCodec codec) {
        int startPosition = out.getPosition();
        MessageSizeCodec.write(list.size(), out);
        for (int i = 0; i < list.size(); ++i) {
            codec.writeCharSequence(list.get(i), out);
        }
        return out.getPosition() - startPosition;
    }

    public static int encodeArrayWithoutSize(LongArrayList list, MemoryDataOutput out, AlphanumericCodec codec) {
        int startPosition = out.getPosition();
        MessageSizeCodec.write(list.size(), out);
        for (int i = 0; i < list.size(); ++i) {
            codec.writeLong(list.getLong(i), out);
        }
        return out.getPosition() - startPosition;
    }

    public static void encodeArray(CharacterArrayList list, MemoryDataOutput tempOut, MemoryDataOutput out) {
        tempOut.reset();
        MessageSizeCodec.write(list.size(), tempOut);
        for (int i = 0; i < list.size(); ++i) {
            tempOut.writeChar(list.getCharacter(i));
        }

        MessageSizeCodec.write(tempOut.getPosition(), out);
        out.write(tempOut.getBuffer(), 0, tempOut.getPosition());
    }

    public static void encodeArray(CharacterArrayList list, MemoryDataOutput out) {
        int sizeOfSize = MessageSizeCodec.sizeOf(list.size());
        int sizeOfList = sizeOfSize + BitUtil.SIZE_OF_CHAR * list.size();
        MessageSizeCodec.write(sizeOfList, out);
        MessageSizeCodec.write(list.size(), out);
        for (int i = 0; i < list.size(); ++i) {
            out.writeChar(list.getCharacter(i));
        }
    }

    public static int encodeArrayWithoutSize(CharacterArrayList list, MemoryDataOutput out) {
        int sizeOfSize = MessageSizeCodec.sizeOf(list.size());
        int sizeOfList = sizeOfSize + BitUtil.SIZE_OF_CHAR * list.size();
        MessageSizeCodec.write(list.size(), out);
        for (int i = 0; i < list.size(); ++i) {
            out.writeChar(list.getCharacter(i));
        }
        return sizeOfList;
    }

    public static void encodeArray(ObjectArrayList<CharSequence> list, MemoryDataOutput tempOut, MemoryDataOutput out) {
        tempOut.reset();
        MessageSizeCodec.write(list.size(), tempOut);
        for (int i = 0; i < list.size(); ++i) {
            tempOut.writeString(list.get(i));
        }

        MessageSizeCodec.write(tempOut.getPosition(), out);
        out.write(tempOut.getBuffer(), 0, tempOut.getPosition());
    }

    public static int encodeArrayWithoutSize(ObjectArrayList<CharSequence> list, MemoryDataOutput out) {
        int start = out.getPosition();
        MessageSizeCodec.write(list.size(), out);
        for (int i = 0; i < list.size(); ++i) {
            out.writeString(list.get(i));
        }
        return out.getPosition() - start;
    }

    public static int encodeArrayInstanceWithoutSize(ObjectArrayList<Instance> list, MemoryDataOutput out) {
        int start = out.getPosition();
        MessageSizeCodec.write(list.size(), out);
        for (int i = 0; i < list.size(); ++i) {
            Instance instance = list.get(i);
            if (instance != null) {
                instance.encode(out);
            } else {
                MessageSizeCodec.write(0, out);
            }
        }
        return out.getPosition() - start;
    }


    public static void decodeArray(List<?> list, MemoryDataInput mdi) {
        if (list instanceof ByteArrayList) {
            decodeArray((ByteArrayList) list, mdi);
        } else if (list instanceof ShortArrayList) {
            decodeArray((ShortArrayList) list, mdi);
        } else if (list instanceof IntegerArrayList) {
            decodeArray((IntegerArrayList) list, mdi);
        } else if (list instanceof LongArrayList) {
            decodeArray((LongArrayList) list, mdi);
        } else if (list instanceof FloatArrayList) {
            decodeArray((FloatArrayList) list, mdi);
        } else if (list instanceof DoubleArrayList) {
            decodeArray((DoubleArrayList) list, mdi);
        } else if (list instanceof CharacterArrayList) {
            decodeArray((CharacterArrayList) list, mdi);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static void decodeArray(FloatArrayList list, MemoryDataInput mdi) {
        int length = MessageSizeCodec.read(mdi);
        for (int i = 0; i < length; ++i) {
            list.add(mdi.readFloat());
        }
    }

    public static void decodeArray(DoubleArrayList list, MemoryDataInput mdi) {
        int length = MessageSizeCodec.read(mdi);
        for (int i = 0; i < length; ++i) {
            list.add(mdi.readDouble());
        }
    }

    public static void decodeArray(ByteArrayList list, MemoryDataInput mdi) {
        int length = MessageSizeCodec.read(mdi);
        for (int i = 0; i < length; ++i) {
            list.add(mdi.readByte());
        }
    }

    public static void decodeArray(ShortArrayList list, MemoryDataInput mdi) {
        int length = MessageSizeCodec.read(mdi);
        for (int i = 0; i < length; ++i) {
            list.add(mdi.readShort());
        }
    }

    public static void decodeArray(IntegerArrayList list, MemoryDataInput mdi) {
        int length = MessageSizeCodec.read(mdi);
        for (int i = 0; i < length; ++i) {
            list.add(mdi.readInt());
        }
    }

    public static void decodeArray(LongArrayList list, MemoryDataInput mdi) {
        int length = MessageSizeCodec.read(mdi);
        for (int i = 0; i < length; ++i) {
            list.add(mdi.readLong());
        }
    }

    public static void decodeArray(LongArrayList list, MemoryDataInput mdi, AlphanumericCodec codec) {
        int length = MessageSizeCodec.read(mdi);
        for (int i = 0; i < length; ++i) {
            list.add(codec.readLong(mdi));
        }
    }

    public static void decodeArray(ObjectArrayList<CharSequence> list, MemoryDataInput mdi, AlphanumericCodec codec, StringBuilderPool pool) {
        int length = MessageSizeCodec.read(mdi);
        for (int i = 0; i < length; ++i) {
            StringBuilder sb = pool.borrow();
            sb.setLength(0);
            sb.append(codec.readCharSequence(mdi));
            list.add(sb);
        }
    }

    public static void decodeArray(BooleanArrayList list, MemoryDataInput mdi) {
        int length = MessageSizeCodec.read(mdi);
        for (int i = 0; i < length; ++i) {
            list.add(mdi.readBoolean());
        }
    }

    public static void decodeArray(CharacterArrayList list, MemoryDataInput mdi) {
        int length = MessageSizeCodec.read(mdi);
        for (int i = 0; i < length; ++i) {
            list.add(mdi.readChar());
        }
    }

    public static void decodeArray(ObjectArrayList<CharSequence> list, MemoryDataInput mdi, StringBuilderPool pool) {
        int length = MessageSizeCodec.read(mdi);
        for (int i = 0; i < length; ++i) {
            list.add(mdi.readStringBuilder(pool.borrow()));
        }
    }

    public static void decodeArray(ObjectArrayList<Instance> list, MemoryDataInput mdi, InstancePool pool) {
        int length = MessageSizeCodec.read(mdi);
        for (int i = 0; i < length; ++i) {
            Instance instance = pool.borrow();
            instance.decode(mdi);
            list.add(instance);
        }
    }

    // array indexer
    public static byte indexOf(ByteArrayList array, int i) {
        if (array == null) {
            return IntegerDataType.INT8_NULL;
        }

        final int index;
        if (i >= 0) {
            if ((index = i) >= array.size()) {
                return IntegerDataType.INT8_NULL;
            }
        } else {
            if ((index = array.size() + i) < 0) {
                return IntegerDataType.INT8_NULL;
            }
        }

        return array.getByte(index);
    }

    public static short indexOf(ShortArrayList array, int i) {
        if (array == null) {
            return IntegerDataType.INT16_NULL;
        }

        final int index;
        if (i >= 0) {
            if ((index = i) >= array.size()) {
                return IntegerDataType.INT16_NULL;
            }
        } else {
            if ((index = array.size() + i) < 0) {
                return IntegerDataType.INT16_NULL;
            }
        }

        return array.getShort(index);
    }

    public static int indexOf(IntegerArrayList array, int i) {
        if (array == null) {
            return IntegerDataType.INT32_NULL;
        }

        final int index;
        if (i >= 0) {
            if ((index = i) >= array.size()) {
                return IntegerDataType.INT32_NULL;
            }
        } else {
            if ((index = array.size() + i) < 0) {
                return IntegerDataType.INT32_NULL;
            }
        }

        return array.getInteger(index);
    }

    public static long indexOf(LongArrayList array, int i) {
        if (array == null) {
            return IntegerDataType.INT64_NULL;
        }

        final int index;
        if (i >= 0) {
            if ((index = i) >= array.size()) {
                return IntegerDataType.INT64_NULL;
            }
        } else {
            if ((index = array.size() + i) < 0) {
                return IntegerDataType.INT64_NULL;
            }
        }

        return array.getLong(index);
    }

    public static float indexOf(FloatArrayList array, int i) {
        if (array == null) {
            return FloatDataType.IEEE32_NULL;
        }

        final int index;
        if (i >= 0) {
            if ((index = i) >= array.size()) {
                return FloatDataType.IEEE32_NULL;
            }
        } else {
            if ((index = array.size() + i) < 0) {
                return FloatDataType.IEEE32_NULL;
            }
        }

        return array.getFloat(index);
    }

    public static double indexOf(DoubleArrayList array, int i) {
        if (array == null) {
            return FloatDataType.IEEE64_NULL;
        }

        final int index;
        if (i >= 0) {
            if ((index = i) >= array.size()) {
                return FloatDataType.IEEE64_NULL;
            }
        } else {
            if ((index = array.size() + i) < 0) {
                return FloatDataType.IEEE64_NULL;
            }
        }

        return array.getDouble(index);
    }

    public static char indexOf(CharacterArrayList array, int i) {
        if (array == null) {
            return CharDataType.NULL;
        }

        final int index;
        if (i >= 0) {
            if ((index = i) >= array.size()) {
                return CharDataType.NULL;
            }
        } else {
            if ((index = array.size() + i) < 0) {
                return CharDataType.NULL;
            }
        }

        return array.getCharacter(index);
    }

    public static <T> T indexOf(ObjectArrayList<T> array, int i) {
        if (array == null) {
            return null;
        }

        final int index;
        if (i >= 0) {
            if ((index = i) >= array.size()) {
                return null;
            }
        } else {
            if ((index = array.size() + i) < 0) {
                return null;
            }
        }

        return array.get(index);
    }

    // boolean array predicate
    public static boolean copyIf(ByteArrayList in, ByteArrayList out, ByteArrayList flags) {
        if (in == null || flags == null) {
            return false;
        }

        out.clear();
        for (int i = 0; i < in.size() && i < flags.size(); ++i) {
            if (flags.getByte(i) == 1) {
                out.add(in.getByte(i));
            }
        }

        return true;
    }

    public static boolean copyIf(ShortArrayList in, ShortArrayList out, ByteArrayList flags) {
        if (in == null || flags == null) {
            return false;
        }

        out.clear();
        for (int i = 0; i < in.size() && i < flags.size(); ++i) {
            if (flags.getByte(i) == 1) {
                out.add(in.getShort(i));
            }
        }

        return true;
    }

    public static boolean copyIf(IntegerArrayList in, IntegerArrayList out, ByteArrayList flags) {
        if (in == null || flags == null) {
            return false;
        }

        out.clear();
        for (int i = 0; i < in.size() && i < flags.size(); ++i) {
            if (flags.getByte(i) == 1) {
                out.add(in.getInteger(i));
            }
        }

        return true;
    }

    public static boolean copyIf(LongArrayList in, LongArrayList out, ByteArrayList flags) {
        if (in == null || flags == null) {
            return false;
        }

        out.clear();
        for (int i = 0; i < in.size() && i < flags.size(); ++i) {
            if (flags.getByte(i) == 1) {
                out.add(in.getLong(i));
            }
        }

        return true;
    }

    public static boolean copyIf(FloatArrayList in, FloatArrayList out, ByteArrayList flags) {
        if (in == null || flags == null) {
            return false;
        }

        out.clear();
        for (int i = 0; i < in.size() && i < flags.size(); ++i) {
            if (flags.getByte(i) == 1) {
                out.add(in.getFloat(i));
            }
        }

        return true;
    }

    public static boolean copyIf(DoubleArrayList in, DoubleArrayList out, ByteArrayList flags) {
        if (in == null || flags == null) {
            return false;
        }

        out.clear();
        for (int i = 0; i < in.size() && i < flags.size(); ++i) {
            if (flags.getByte(i) == 1) {
                out.add(in.getDouble(i));
            }
        }

        return true;
    }

    public static boolean copyIf(CharacterArrayList in, CharacterArrayList out, ByteArrayList flags) {
        if (in == null || flags == null) {
            return false;
        }

        out.clear();
        for (int i = 0; i < in.size() && i < flags.size(); ++i) {
            if (flags.getByte(i) == 1) {
                out.add(in.getCharacter(i));
            }
        }

        return true;
    }

    public static <T> boolean copyIf(ObjectArrayList<T> in, ObjectArrayList<T> out, ByteArrayList flags) {
        if (in == null || flags == null) {
            return false;
        }

        out.clear();
        for (int i = 0; i < in.size() && i < flags.size(); ++i) {
            if (flags.getByte(i) == 1) {
                out.add(in.get(i));
            }
        }

        return true;
    }

    // slice
    public static boolean slice(List<?> in, List<?> out, int from, int to, int step) {
        if (in == null || step == 0) {
            return false;
        }

        out.clear();
        if (step == IntegerDataType.INT32_NULL) {
            step = 1;
        }
        if (from == IntegerDataType.INT32_NULL) {
            if (step > 0) {
                from = 0;
            } else {
                from = in.size() - 1;
            }
        } else if (from < 0) {
            from = in.size() + from;
        }
        if (to == IntegerDataType.INT32_NULL) {
            if (step > 0) {
                to = in.size();
            } else {
                to = -1;
            }
        } else if (to < 0) {
            to = in.size() + to;
        }

        copyArray(in, out, from, to, step);

        return true;
    }

    private static <T> void copyArray(List<?> in, List<?> out, int from, int to, int step) {
        if (in instanceof ByteArrayList) {
            copyArray((ByteArrayList) in, (ByteArrayList) out, from, to, step);
        } else if (in instanceof ShortArrayList) {
            copyArray((ShortArrayList) in, (ShortArrayList) out, from, to, step);
        } else if (in instanceof IntegerArrayList) {
            copyArray((IntegerArrayList) in, (IntegerArrayList) out, from, to, step);
        } else if (in instanceof LongArrayList) {
            copyArray((LongArrayList) in, (LongArrayList) out, from, to, step);
        } else if (in instanceof FloatArrayList) {
            copyArray((FloatArrayList) in, (FloatArrayList) out, from, to, step);
        } else if (in instanceof DoubleArrayList) {
            copyArray((DoubleArrayList) in, (DoubleArrayList) out, from, to, step);
        } else if (in instanceof CharacterArrayList) {
            copyArray((CharacterArrayList) in, (CharacterArrayList) out, from, to, step);
        } else if (in instanceof ObjectArrayList) {
            copyObjectArray((ObjectArrayList) in, (ObjectArrayList) out, from, to, step);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static void copyArray(ByteArrayList in, ByteArrayList out, int from, int to, int step) {
        for (int i = from; (step > 0 ? i < to : i > to); i += step) {
            if (i >= 0 && i < in.size()) {
                out.add(in.getByte(i));
            }
        }
    }

    private static void copyArray(ShortArrayList in, ShortArrayList out, int from, int to, int step) {
        for (int i = from; (step > 0 ? i < to : i > to); i += step) {
            if (i >= 0 && i < in.size()) {
                out.add(in.getShort(i));
            }
        }
    }

    private static void copyArray(IntegerArrayList in, IntegerArrayList out, int from, int to, int step) {
        for (int i = from; (step > 0 ? i < to : i > to); i += step) {
            if (i >= 0 && i < in.size()) {
                out.add(in.getInteger(i));
            }
        }
    }

    private static void copyArray(LongArrayList in, LongArrayList out, int from, int to, int step) {
        for (int i = from; (step > 0 ? i < to : i > to); i += step) {
            if (i >= 0 && i < in.size()) {
                out.add(in.getLong(i));
            }
        }
    }

    private static void copyArray(FloatArrayList in, FloatArrayList out, int from, int to, int step) {
        for (int i = from; (step > 0 ? i < to : i > to); i += step) {
            if (i >= 0 && i < in.size()) {
                out.add(in.getFloat(i));
            }
        }
    }

    private static void copyArray(DoubleArrayList in, DoubleArrayList out, int from, int to, int step) {
        for (int i = from; (step > 0 ? i < to : i > to); i += step) {
            if (i >= 0 && i < in.size()) {
                out.add(in.getDouble(i));
            }
        }
    }

    private static void copyArray(CharacterArrayList in, CharacterArrayList out, int from, int to, int step) {
        for (int i = from; (step > 0 ? i < to : i > to); i += step) {
            if (i >= 0 && i < in.size()) {
                out.add(in.getCharacter(i));
            }
        }
    }

    private static <T> void copyObjectArray(ObjectArrayList<T> in, ObjectArrayList<T> out, int from, int to, int step) {
        for (int i = from; (step > 0 ? i < to : i > to); i += step) {
            if (i >= 0 && i < in.size()) {
                out.add(in.get(i));
            }
        }
    }

    public static int getMaxSize(BaseInstanceArray<?, ?>... array) {
        int max = IntegerDataType.INT32_NULL;
        for (int i = 0; i < array.length; ++i) {
            if (!array[i].isNull()) {
                max = Math.max(max, array[i].get().size());
            }
        }

        return max;
    }

    // todo: decode list of char sequence
}