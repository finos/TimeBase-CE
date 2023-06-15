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
package com.epam.deltix.test.qsrv.hf.tickdb.qql;

import com.epam.deltix.containers.AlphanumericUtils;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.Attribute;
import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.entries.*;
import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.orders.*;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.collections.generated.*;

import java.util.List;
import java.util.function.Function;

public class QueryResults {

    public static class FloatQuery extends InstrumentMessage {
        public float res1;

        @Override
        public String toString() {
            return String.valueOf(res1);
        }
    }

    public static class FloatFloatQuery extends InstrumentMessage {
        public float res1;
        public float res2;

        @Override
        public String toString() {
            return String.valueOf(res1) + ", " + String.valueOf(res2);
        }
    }

    public static class IntegerQuery extends InstrumentMessage {
        public int res1;

        @Override
        public String toString() {
            return String.valueOf(res1);
        }
    }

    public static class DoubleQuery extends InstrumentMessage {
        public double res1;

        @Override
        public String toString() {
            return String.valueOf(res1);
        }
    }

    public static class DoubleDoubleQuery extends InstrumentMessage {
        public double res1;
        public double res2;

        @Override
        public String toString() {
            return res1 + ", " + res2;
        }
    }

    public static class VarcharQuery extends InstrumentMessage {
        public String res1;

        @Override
        public String toString() {
            return String.valueOf(res1);
        }
    }

    public static class VarcharFloatQuery extends InstrumentMessage {
        public String res1;
        public float res2;

        @Override
        public String toString() {
            return res1 + ", " + res2;
        }
    }

    public static class VarcharIntegerQuery extends InstrumentMessage {
        public String res1;
        public int res2;

        @Override
        public String toString() {
            return res1 + ", " + res2;
        }
    }

    public static class FloatArrayQuery extends InstrumentMessage {
        public FloatArrayList res1;

        @Override
        public String toString() {
            return writeList("res1", res1);
        }
    }

    public static class BoolArrayQuery extends InstrumentMessage {
        public ByteArrayList res1;

        @Override
        public String toString() {
            return writeList("res1", res1);
        }
    }

    public static class ByteArrayQuery extends InstrumentMessage {
        public ByteArrayList res1;

        @Override
        public String toString() {
            return writeList("res1", res1);
        }
    }

    public static class ShortArrayQuery extends InstrumentMessage {
        public ShortArrayList res1;

        @Override
        public String toString() {
            return writeList("res1", res1);
        }
    }

    public static class DoubleArrayQuery extends InstrumentMessage {
        public DoubleArrayList res1;

        @Override
        public String toString() {
            return writeList("res1", res1);
        }
    }

    public static class IntegerArrayQuery extends InstrumentMessage {
        public IntegerArrayList res1;

        @Override
        public String toString() {
            return writeList("res1", res1);
        }
    }

    public static class LongArrayQuery extends InstrumentMessage {
        public LongArrayList res1;

        @Override
        public String toString() {
            return writeList("res1", res1);
        }
    }

    public static class DecimalArrayQuery extends InstrumentMessage {
        public LongArrayList res1;

        @Override
        public String toString() {
            return writeList("res1", res1, Decimal64Utils::toString);
        }
    }

    public static class AlphanumericArrayQuery extends InstrumentMessage {
        public LongArrayList res1;

        @Override
        public String toString() {
            return writeList("res1", res1, AlphanumericUtils::toString);
        }
    }

    public static class VarcharArrayQuery extends InstrumentMessage {
        public ObjectArrayList<CharSequence> res1;

        @Override
        public String toString() {
            return writeList("res1", res1);
        }
    }

    public static class OrderQuery extends InstrumentMessage {
        public Order res1;

        @Override
        public String toString() {
            return res1 != null ? res1.toString() : "NULL";
        }
    }

    public static class EntryArrayQuery extends InstrumentMessage {
        public ObjectArrayList<PackageEntry> res1;

        @Override
        public String toString() {
            return writeList("res1", res1);
        }
    }

    public static class AttributesArrayQuery extends InstrumentMessage {
        public ObjectArrayList<Attribute> res1;

        @Override
        public String toString() {
            return writeList("res1", res1);
        }
    }

    public static class IdQuery extends InstrumentMessage {
        public Id res1;

        @Override
        public String toString() {
            return res1 != null ? res1.toString() : "NULL";
        }
    }

    public static class OrderInfoQuery extends InstrumentMessage {
        public OrderInfo res1;

        @Override
        public String toString() {
            return res1 != null ? res1.toString() : "NULL";
        }
    }

    public static class OrderInfoOrderInfoQuery extends InstrumentMessage {
        public OrderInfo res1;
        public OrderInfo res2;

        @Override
        public String toString() {
            return (res1 != null ? res1.toString() : "NULL") + " | " +
                (res2 != null ? res2.toString() : "NULL");
        }
    }

    public static class ExecutionQuery extends InstrumentMessage {
        public Execution res1;

        @Override
        public String toString() {
            return res1 != null ? res1.toString() : "NULL";
        }
    }

    public static class ExecutedInfoQuery extends InstrumentMessage {
        public ExecutedInfo res1;

        @Override
        public String toString() {
            return res1 != null ? res1.toString() : "NULL";
        }
    }

    public static class ExecutedInfoHistoryQuery extends InstrumentMessage {
        public ObjectArrayList<ExecutedInfo> res1;

        @Override
        public String toString() {
            return writeList("res1", res1);
        }
    }

    public static class EnumActionByteArrayQuery extends InstrumentMessage {
        public ByteArrayList res1;

//        @Override
//        public String toString() {
//            return writeList("res1", res1, (v) -> QuoteUpdateAction.values()[v].toString());
//        }
    }

    public static class EnumQuoteSideByteArrayQuery extends InstrumentMessage {
        public ByteArrayList res1;

        @Override
        public String toString() {
            return writeList("res1", res1, (v) -> QuoteSide.values()[v].toString());
        }
    }

    public static class FloatFloatVarcharIntegerFloatQuery extends InstrumentMessage {
        public float res1;
        public float res2;
        public String res3;
        public int res4;
        public float res5;

        @Override
        public String toString() {
            return res1 + ", " + res2 + ", " + res3 + ", " + res4 + ", " + res5;
        }
    }

    public static class DecimalDecimalIntegerEnumQuoteSideByteArrayQuery extends InstrumentMessage {
        public LongArrayList res1;
        public LongArrayList res2;
        public IntegerArrayList res3;
        public ByteArrayList res4;

        @Override
        public String toString() {
            return
                writeList("res1", res1, Decimal64Utils::toString) + " | \n" +
                writeList("res2", res2, Decimal64Utils::toString) + " | \n" +
                writeList("res3", res3) + " | \n" +
                writeList("res4", res4, (v) -> QuoteSide.values()[v].toString());
        }
    }

    public static class FloatFloatArrayQuery extends InstrumentMessage {
        public FloatArrayList res1;
        public FloatArrayList res2;

        @Override
        public String toString() {
            return writeList("res1", res1) + " | "
                + writeList("res2", res2);
        }
    }

    public static class FloatFloatIntegerVarcharArrayQuery extends InstrumentMessage {
        public FloatArrayList res1;
        public FloatArrayList res2;
        public IntegerArrayList res3;
        public ObjectArrayList<CharSequence> res4;

        @Override
        public String toString() {
            return writeList("res1", res1) + " | "
                + writeList("res2", res2) + " | "
                + writeList("res3", res3) + " | "
                + writeList("res4", res4);
        }
    }

    public static class DoubleDoubleArrayQuery extends InstrumentMessage {
        public DoubleArrayList res1;
        public DoubleArrayList res2;

        @Override
        public String toString() {
            return writeList("res1", res1) + " | "
                + writeList("res2", res2);
        }
    }

    public static class FloatVarcharArrayQuery extends InstrumentMessage {
        public FloatArrayList res1;
        public ObjectArrayList<CharSequence> res2;

        @Override
        public String toString() {
            return writeList("res1", res1) + " | "
                + writeList("res2", res2);
        }
    }

    public static class DoubleVarcharArrayQuery extends InstrumentMessage {
        public DoubleArrayList res1;
        public ObjectArrayList<CharSequence> res2;

        @Override
        public String toString() {
            return writeList("res1", res1) + " | "
                + writeList("res2", res2);
        }
    }

    public static class ExecutionsArrayQuery extends InstrumentMessage {
        public ObjectArrayList<Execution> res1;

        @Override
        public String toString() {
            return writeList("res1", res1);
        }
    }

    public static class FloatExecutionsArrayQuery extends InstrumentMessage {
        public FloatArrayList res1;
        public ObjectArrayList<Execution> res2;

        @Override
        public String toString() {
            return writeList("res1", res1) + " | \n"
                + writeList("res2", res2);
        }
    }

    public static class FloatExecutionsAttributesArrayQuery extends InstrumentMessage {
        public FloatArrayList res1;
        public ObjectArrayList<Execution> res2;
        public ObjectArrayList<Attribute> res3;

        @Override
        public String toString() {
            return writeList("res1", res1) + " | \n"
                + writeList("res2", res2) + " | \n"
                + writeList("res3", res3);
        }
    }

    public static class IntegerExecutionsAttributesArrayQuery extends InstrumentMessage {
        public IntegerArrayList res1;
        public ObjectArrayList<Execution> res2;
        public ObjectArrayList<Attribute> res3;

        @Override
        public String toString() {
            return writeList("res1", res1) + " | \n"
                + writeList("res2", res2) + " | \n"
                + writeList("res3", res3);
        }
    }

    public static class FloatExecutionsInfoArrayQuery extends InstrumentMessage {
        public FloatArrayList res1;
        public ObjectArrayList<ExecutionInfo> res2;
        public ObjectArrayList<Execution> res3;
        public Order res4;

        @Override
        public String toString() {
            return writeList("res1", res1) + " | \n"
                + writeList("res2", res2) + " | \n"
                + writeList("res3", res3) + " | \n"
                + res4;
        }
    }

    public static class EntryQuery extends InstrumentMessage {
        public PackageEntry res1;

        @Override
        public String toString() {
            return writeObject("res1", res1);
        }
    }

    public static class EntriesPackageTypeQuery extends InstrumentMessage {
        public PackageEntry entries;
        public PackageType packageType;

        @Override
        public String toString() {
            return writeObject("entries", entries) + " | " +
                writeObject("packageType", packageType);
        }
    }

    public static class EntriesPackageTypeEntryQuery extends InstrumentMessage {
        public ObjectArrayList<PackageEntry> entries;
        public PackageType packageType;
        public PackageEntry entry;

        @Override
        public String toString() {
            return writeList("entries", entries) + " | " +
                writeObject("packageType", packageType) + " | " +
                writeObject("entry", entry);
        }
    }

    public static class EntryIntegerQuery extends InstrumentMessage {
        public PackageEntry res1;
        public int res2;

        @Override
        public String toString() {
            return writeObject("res1", res1) + " | res2: " + res2;
        }
    }

    public static class EntriesIntegerQuery extends InstrumentMessage {
        public ObjectArrayList<PackageEntry> res1;
        public int res2;

        @Override
        public String toString() {
            return writeList("res1", res1) + " | res2: " + res2;
        }
    }

    public static class EntryOrderQuery extends InstrumentMessage {
        public PackageEntry res1;
        public Order res2;

        @Override
        public String toString() {
            return writeObject("res1", res1) + " | " +
                writeObject("res2", res2);
        }
    }

    public static class TestQueryResult1 extends InstrumentMessage {
        public ObjectArrayList<PackageEntry> res0;
        public IntegerArrayList res1;
        public FloatArrayList res2;
        public FloatArrayList res3;
        public IntegerArrayList res4;
        public ObjectArrayList<PackageEntry> res5;
        public ObjectArrayList<PackageEntry> res6;

        @Override
        public String toString() {
            return writeList("res0", res0) + " | " +
                writeList("res1", res1) + " | " +
                writeList("res2", res2) + " | " +
                writeList("res3", res3) + " | " +
                writeList("res4", res4) + " | " +
                writeList("res5", res5) + " | " +
                writeList("res6", res6);
        }
    }

    static <T> String writeObject(String name, T obj) {
        return name + ": " + (obj != null ? obj.toString() : "NULL");
    }

    static <T> String writeList(String name, List<T> list) {
        return writeList(name, list, Object::toString);
    }

    static <T> String writeList(String name, List<T> list, Function<T, String> toString) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" : ");
        if (list != null) {
            sb.append(list.size()).append(" ");
            sb.append("[");
            for (int i = 0; i < list.size(); ++i) {
                if (i > 0) {
                    sb.append(", ");
                }
                if (list.get(i) == null) {
                    sb.append("[NULL]");
                } else {
                    sb.append(toString.apply(list.get(i)));
                }
            }
            sb.append("]");
        } else {
            sb.append("NULL");
        }

        return sb.toString();
    }
}
