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
package com.epam.deltix.qsrv.hf.codec;

import com.epam.deltix.util.collections.generated.ByteArrayList;

/**
 * copy from deltix.util.collections.ByteArrayListUtils
 */
public class BinaryUtils {

    private static ThreadLocal<StringBuilder> threadLocalBuilder = new ThreadLocal<StringBuilder>() {
        @Override
        protected StringBuilder initialValue() {
            return new StringBuilder();
        }
    };

    private static ByteArrayList checkNull(ByteArrayList ar) {
        return (ar == null) ? new ByteArrayList() : ar;
    }

    private static StringBuilder checkNull(StringBuilder sb) {
        return (sb == null) ? new StringBuilder() : sb;
    }

    /**
     * Append x to binary array ar
     * @param ar binary array
     * @param x x
     * @return this
     */

    public static ByteArrayList append(ByteArrayList ar, short x) {
        ar = checkNull(ar);
        ar.add((byte)(x & 255));
        ar.add((byte)(x >>> 8));
        return ar;
    }

    /**
     * Assign binary array ar by x
     * @param ar binary array
     * @param x x
     * @return this
     */
    public static ByteArrayList assign(ByteArrayList ar, String x) {
        if (x == null) return null;
        ar = checkNull(ar);
        ar.clear();
        append(ar, x);
        return ar;
    }

    /**
     * Assign binary array ar by x
     * @param ar binary array
     * @param x x
     * @return this
     */
    public static ByteArrayList assign(ByteArrayList ar, CharSequence x) {
        if (x == null) return null;
        ar = checkNull(ar);
        ar.clear();
        append(ar, x);
        return ar;
    }

    /**
     * Append x to binary array ar
     * @param ar binary array
     * @param x x
     * @return this
     */
    public static ByteArrayList append(ByteArrayList ar, String x) {
        if (x == null) return null;
        ar = checkNull(ar);
        for (int i = 0; i < x.length(); ++i) {
            char ch = x.charAt(i);
            append(ar, (short) ch);
        }
        return ar;
    }

    /**
     * Append x to binary array ar
     * @param ar binary array
     * @param x x
     * @return this
     */
    public static ByteArrayList append(ByteArrayList ar, CharSequence x) {
        if (x == null) return null;
        ar = checkNull(ar);
        for (int i = 0; i < x.length(); ++i) {
            char ch = x.charAt(i);
            append(ar, (short) ch);
        }
        return ar;
    }

    /**
     * Convert binary array, started from offset to CharSequence
     * @param ar binary array
     * @param offset offset
     * @param builder result
     * @return builder.
     */
    public static StringBuilder toStringBuilder(ByteArrayList ar, int offset, StringBuilder builder) {
        if (ar == null) return null;
        builder = checkNull(builder);
        builder.setLength(0);
        for (int i = offset; i < ar.size(); i += 2) {
            builder.append((char) toShort(ar, i));
        }
        return builder;
    }

    /**
     * Convert binary array, started from offset to CharSequence
     * @param ar binary array
     * @param builder result
     * @return builder.
     */
    public static StringBuilder toStringBuilder(ByteArrayList ar, StringBuilder builder) {
        return toStringBuilder(ar, 0, builder);
    }

    /**
     * Convert binary array, started from offset to CharSequence
     * @param ar binary array
     * @return result.
     */
    public static StringBuilder toStringBuilder(ByteArrayList ar) {
        return toStringBuilder(ar, 0, threadLocalBuilder.get());
    }

    /**
     * Convert binary array, started from offset to short
     * @param ar binary array
     * @param offset offset
     * @return result of convertation
     */
    public static short toShort(ByteArrayList ar, int offset) {
        return (short) ((((short)(ar.getByte(offset + 1)) << 8) & (short)(0xff00)) |
                ((short) ar.getByte(offset)) & (short)(0xff));
    }
}
