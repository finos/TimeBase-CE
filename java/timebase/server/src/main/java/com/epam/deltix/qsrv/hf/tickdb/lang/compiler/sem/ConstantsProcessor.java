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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.CompiledArrayConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.CompiledConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.IllegalTypeCombinationException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.ArithmeticExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.ArithmeticFunction;
import com.epam.deltix.util.collections.generated.DoubleArrayList;
import com.epam.deltix.util.collections.generated.IntegerArrayList;
import com.epam.deltix.util.collections.generated.LongArrayList;

import static com.epam.deltix.dfp.Decimal64Utils.*;

class ConstantsProcessor {

    static CompiledConstant compute(ArithmeticExpression e, CompiledConstant left, CompiledConstant right) {
        DataType leftType = left.type;
        DataType rightType = right.type;
        if (leftType instanceof IntegerDataType && rightType instanceof IntegerDataType) {
            return compute(e.function, left, right, (IntegerDataType) leftType, (IntegerDataType) rightType);
        } else if (leftType instanceof FloatDataType && rightType instanceof FloatDataType) {
            return compute(e.function, left, right, (FloatDataType) leftType, (FloatDataType) leftType);
        } else if (DataTypeHelper.isTimestampAndInteger(leftType, rightType)) {
            return computeDateTimeAndInteger(e.function, left, right);
        } else if (DataTypeHelper.isTimestampAndTimestamp(leftType, rightType)) {
            return computeDateTimeAndDateTime(e.function, left, right);
        } else {
            throw new IllegalTypeCombinationException(e, leftType, rightType);
        }
    }

    static CompiledArrayConstant<?, ?, ?> compute(ArithmeticExpression e, CompiledArrayConstant<?, ?, ?> first,
                                                  CompiledConstant second, boolean swap) {
        DataType secondType = second.type;
        if (first.type.getElementDataType() instanceof DateTimeDataType) {
            if (secondType instanceof DateTimeDataType) {
                return CompiledArrayConstant.createLongArrayConstant(
                    compute(e.function, (LongArrayList) first.getList(), second.getLong(), swap)
                );
            } else {
                return CompiledArrayConstant.createDateArrayConstant(
                    compute(e.function, (LongArrayList) first.getList(), second.getLong(), swap)
                );
            }
        } else if (first.type.getElementDataType() instanceof IntegerDataType) {
            IntegerDataType dataType = (IntegerDataType) first.type.getElementDataType();
            if (secondType instanceof DateTimeDataType) {
                return CompiledArrayConstant.createDateArrayConstant(compute(e.function, (LongArrayList) first.getList(),
                        second.getLong(), swap));
            } else if (secondType instanceof IntegerDataType) {
                if (dataType.getNativeTypeSize() == 8) {
                    return CompiledArrayConstant.createLongArrayConstant(compute(e.function, (LongArrayList) first.getList(),
                            second.getLong(), swap));
                } else {
                    if (((IntegerDataType) secondType).getNativeTypeSize() == 8) {
                        return CompiledArrayConstant.createLongArrayConstant(compute(e.function,
                                (IntegerArrayList) first.getList(), second.getLong(), swap));
                    } else {
                        return CompiledArrayConstant.createIntegerArrayConstant(compute(e.function,
                                (IntegerArrayList) first.getList(), second.getInteger(), swap));
                    }
                }
            } else if (secondType instanceof FloatDataType) {
                if (dataType.getNativeTypeSize() == 8) {
                    if (((FloatDataType) secondType).isDecimal64()) {
                        return CompiledArrayConstant.createDecimalArrayConstant(computeDecimal(e.function,
                                (LongArrayList) first.getList(), second.getDecimalLong(), swap));
                    } else {
                        return CompiledArrayConstant.createDoubleArrayConstant(compute(e.function,
                                (LongArrayList) first.getList(), second.getDouble(), swap));
                    }
                } else {
                    if (((FloatDataType) secondType).isDecimal64()) {
                        return CompiledArrayConstant.createDecimalArrayConstant(computeDecimal(e.function,
                                (IntegerArrayList) first.getList(), second.getDecimalLong(), swap));
                    } else {
                        return CompiledArrayConstant.createDoubleArrayConstant(compute(e.function,
                                (IntegerArrayList) first.getList(), second.getDouble(), swap));
                    }
                }
            } else {
                throw new IllegalTypeCombinationException(e, first.type, secondType);
            }
        } else if (first.type.getElementDataType() instanceof FloatDataType) {
            FloatDataType dataType = (FloatDataType) first.type.getElementDataType();
            if (dataType.isDecimal64()) {
                @Decimal LongArrayList list = (LongArrayList) first.getList();
                if (secondType instanceof FloatDataType && !((FloatDataType) secondType).isDecimal64()) {
                    return CompiledArrayConstant.createDoubleArrayConstant(computeDecimal(e.function,
                            list, second.getDouble(), swap));
                } else {
                    return CompiledArrayConstant.createDecimalArrayConstant(computeDecimal(e.function,
                            list, second.getDecimalLong(), swap));
                }
            } else {
                DoubleArrayList list = (DoubleArrayList) first.getList();
                return CompiledArrayConstant.createDoubleArrayConstant(compute(e.function, list, second.getDouble(), swap));
            }
        } else {
            throw new IllegalTypeCombinationException(e, first.type, secondType);
        }
    }

    static CompiledArrayConstant<?, ?, ?> compute(ArithmeticExpression e, CompiledArrayConstant<?, ?, ?> left,
                                                  CompiledArrayConstant<?, ?, ?> right) {
        ArithmeticFunction function = e.function;
        DataType leftType = left.type.getElementDataType();
        DataType rightType = right.type.getElementDataType();
        if (leftType instanceof FloatDataType && rightType instanceof FloatDataType) {
            boolean leftDecimal = ((FloatDataType) leftType).isDecimal64();
            boolean rightDecimal = ((FloatDataType) rightType).isDecimal64();
            if (leftDecimal && rightDecimal) {
                return CompiledArrayConstant.createDecimalArrayConstant(computeDecimalDecimal(function,
                        (LongArrayList) left.getList(), (LongArrayList) right.getList()));
            } else if (leftDecimal) {
                return CompiledArrayConstant.createDoubleArrayConstant(computeDecimal(function,
                        (DoubleArrayList) right.getList(), (LongArrayList) left.getList(), true));
            } else if (rightDecimal) {
                return CompiledArrayConstant.createDoubleArrayConstant(computeDecimal(function,
                        (DoubleArrayList) left.getList(), (LongArrayList) right.getList(), false));
            } else {
                return CompiledArrayConstant.createDoubleArrayConstant(compute(function, (DoubleArrayList) left.getList(), 
                        (DoubleArrayList) right.getList()));
            }
        } else if (leftType instanceof FloatDataType && rightType instanceof IntegerDataType) {
            boolean leftDecimal = ((FloatDataType) leftType).isDecimal64();
            boolean rightLong = ((IntegerDataType) rightType).getNativeTypeSize() == 8;
            if (leftDecimal && rightLong) {
                return CompiledArrayConstant.createDecimalArrayConstant(computeDecimal(function, 
                        (LongArrayList) left.getList(), (LongArrayList) right.getList(), false));
            } else if (leftDecimal) {
                return CompiledArrayConstant.createDecimalArrayConstant(computeDecimal(function,
                        (LongArrayList) left.getList(), (IntegerArrayList) right.getList(), false));
            } else if (rightLong) {
                return CompiledArrayConstant.createDoubleArrayConstant(compute(function, (DoubleArrayList) left.getList(), 
                        (LongArrayList) right.getList(), false));
            } else {
                return CompiledArrayConstant.createDoubleArrayConstant(compute(function, (DoubleArrayList) left.getList(),
                        (IntegerArrayList) right.getList(), false));
            }
        } else if (leftType instanceof IntegerDataType && rightType instanceof FloatDataType) {
            boolean leftLong = ((IntegerDataType) leftType).getNativeTypeSize() == 8;
            boolean rightDecimal = ((FloatDataType) rightType).isDecimal64();
            if (rightDecimal && leftLong) {
                return CompiledArrayConstant.createDecimalArrayConstant(computeDecimal(function,
                        (LongArrayList) right.getList(), (LongArrayList) left.getList(), true));
            } else if (rightDecimal) {
                return CompiledArrayConstant.createDecimalArrayConstant(computeDecimal(function,
                        (LongArrayList) right.getList(), (IntegerArrayList) left.getList(), true));
            } else if (leftLong) {
                return CompiledArrayConstant.createDoubleArrayConstant(compute(function, (DoubleArrayList) right.getList(),
                        (LongArrayList) left.getList(), true));
            } else {
                return CompiledArrayConstant.createDoubleArrayConstant(compute(function, (DoubleArrayList) right.getList(),
                        (IntegerArrayList) left.getList(), true));
            }
        } else if (leftType instanceof IntegerDataType && rightType instanceof IntegerDataType) {
            boolean leftLong = ((IntegerDataType) leftType).getNativeTypeSize() == 8;
            boolean rightLong = ((IntegerDataType) rightType).getNativeTypeSize() == 8;
            if (leftLong && rightLong) {
                return CompiledArrayConstant.createLongArrayConstant(compute(function, (LongArrayList) left.getList(),
                        (LongArrayList) right.getList()));
            } else if (leftLong) {
                return CompiledArrayConstant.createLongArrayConstant(compute(function, (LongArrayList) left.getList(),
                        (IntegerArrayList) right.getList(), false));
            } else if (rightLong) {
                return CompiledArrayConstant.createLongArrayConstant(compute(function, (LongArrayList) right.getList(),
                        (IntegerArrayList) left.getList(), true));
            } else {
                return CompiledArrayConstant.createIntegerArrayConstant(compute(function, (IntegerArrayList) left.getList(),
                        (IntegerArrayList) right.getList()));
            }
        } else if (leftType instanceof IntegerDataType && rightType instanceof DateTimeDataType) {
            if (((IntegerDataType) leftType).getNativeTypeSize() == 8) {
                return CompiledArrayConstant.createDateArrayConstant(compute(function, (LongArrayList) left.getList(),
                        (LongArrayList) right.getList()));
            } else {
                return CompiledArrayConstant.createDateArrayConstant(compute(function, (LongArrayList) right.getList(),
                        (IntegerArrayList) left.getList(), true));
            }
        } else if (leftType instanceof DateTimeDataType && rightType instanceof IntegerDataType) {
            if (((IntegerDataType) rightType).getNativeTypeSize() == 8) {
                return CompiledArrayConstant.createDateArrayConstant(compute(function, (LongArrayList) left.getList(),
                        (LongArrayList) right.getList()));
            } else {
                return CompiledArrayConstant.createDateArrayConstant(compute(function, (LongArrayList) left.getList(),
                        (IntegerArrayList) right.getList(), false));
            }
        } else if (leftType instanceof DateTimeDataType && rightType instanceof DateTimeDataType) {
            return CompiledArrayConstant.createLongArrayConstant(
                compute(function, (LongArrayList) left.getList(), (LongArrayList) right.getList())
            );
        } else {
            throw new IllegalTypeCombinationException(e, left.type, right.type);
        }
    }

    static CompiledConstant compute(ArithmeticFunction function, CompiledConstant left, CompiledConstant right,
                                    IntegerDataType leftType, IntegerDataType rightType) {
        if (leftType.getNativeTypeSize() == 8 || rightType.getNativeTypeSize() == 8) {
            long a = left.getLong();
            long b = right.getLong();
            return new CompiledConstant(StandardTypes.INT64_CONTAINER.getType(false), compute(function, a, b));
        } else {
            int a = left.getInteger();
            int b = right.getInteger();
            return new CompiledConstant(StandardTypes.INT32_CONTAINER.getType(false), compute(function, a, b));
        }
    }

    static CompiledConstant computeDateTimeAndInteger(ArithmeticFunction function, CompiledConstant left, CompiledConstant right) {
        long a = left.getLong();
        long b = right.getLong();
        return new CompiledConstant(StandardTypes.DATE_TIME_CONTAINER.getType(false), compute(function, a, b));
    }

    static CompiledConstant computeDateTimeAndDateTime(ArithmeticFunction function, CompiledConstant left, CompiledConstant right) {
        long a = left.getLong();
        long b = right.getLong();
        return new CompiledConstant(StandardTypes.INT64_CONTAINER.getType(false), compute(function, a, b));
    }

    static CompiledConstant compute(ArithmeticFunction function, CompiledConstant left, CompiledConstant right,
                                    FloatDataType leftType, FloatDataType rightType) {
        if (leftType.isDecimal64() || rightType.isDecimal64()) {
            @Decimal long a = left.getDecimalLong();
            @Decimal long b = right.getDecimalLong();
            return new CompiledConstant(StandardTypes.DECIMAL64_CONTAINER.getType(false), computeDecimal(function, a, b), true);
        } else {
            double a = left.getDouble();
            double b = right.getDouble();
            return new CompiledConstant(StandardTypes.FLOAT64_CONTAINER.getType(false), compute(function, a, b));
        }
    }

    static int compute(ArithmeticFunction function, int a, int b, boolean swap) {
        switch (function) {
            case ADD:
                return a + b;
            case SUB:
                return swap ? b - a : a - b;
            case MUL:
                return a * b;
            case DIV:
                return swap ? b / a : a / b;
            case MOD:
                return swap ? b % a : a % b;
            default:
                throw new RuntimeException();
        }
    }

    static int compute(ArithmeticFunction function, int a, int b) {
        return compute(function, a, b, false);
    }

    static long compute(ArithmeticFunction function, long a, long b, boolean swap) {
        switch (function) {
            case ADD:
                return a + b;
            case SUB:
                return swap ? b - a : a - b;
            case MUL:
                return a * b;
            case DIV:
                return swap ? b / a : a / b;
            case MOD:
                return swap ? b % a : a % b;
            default:
                throw new RuntimeException();
        }
    }

    static long compute(ArithmeticFunction function, long a, long b) {
        return compute(function, a, b, false);
    }

    static double compute(ArithmeticFunction function, double a, double b, boolean swap) {
        switch (function) {
            case ADD:
                return a + b;
            case SUB:
                return swap ? b - a : a - b;
            case MUL:
                return a * b;
            case DIV:
                return swap ? b / a : a / b;
            default:
                throw new RuntimeException();
        }
    }

    static double compute(ArithmeticFunction function, double a, double b) {
        return compute(function, a, b, false);
    }

    @Decimal
    static long computeDecimal(ArithmeticFunction function, @Decimal long a, @Decimal long b, boolean swap) {
        switch (function) {
            case ADD:
                return add(a, b);
            case SUB:
                return swap ? subtract(b, a) : subtract(a, b);
            case MUL:
                return multiply(a, b);
            case DIV:
                return swap ? subtract(b, a) : divide(a, b);
            default:
                throw new RuntimeException();
        }
    }

    @Decimal
    static long computeDecimal(ArithmeticFunction function, @Decimal long a, @Decimal long b) {
        return computeDecimal(function, a, b, false);
    }

    static IntegerArrayList compute(ArithmeticFunction function, IntegerArrayList a, int b, boolean swap) {
        IntegerArrayList list = new IntegerArrayList(a.size());
        for (int i = 0; i < a.size(); i++) {
            list.add(compute(function, a.getInteger(i), b, swap));
        }
        return list;
    }

    static LongArrayList compute(ArithmeticFunction function, IntegerArrayList a, long b, boolean swap) {
        LongArrayList list = new LongArrayList(a.size());
        for (int i = 0; i < a.size(); i++) {
            list.add(compute(function, a.getInteger(i), b, swap));
        }
        return list;
    }

    static DoubleArrayList compute(ArithmeticFunction function, IntegerArrayList a, double b, boolean swap) {
        DoubleArrayList list = new DoubleArrayList(a.size());
        for (int i = 0; i < a.size(); i++) {
            list.add(compute(function, a.getInteger(i), b, swap));
        }
        return list;
    }

    @Decimal
    static LongArrayList computeDecimal(ArithmeticFunction function, IntegerArrayList a, @Decimal long b, boolean swap) {
        @Decimal LongArrayList list = new LongArrayList(a.size());
        for (int i = 0; i < a.size(); i++) {
            list.add(computeDecimal(function, Decimal64Utils.fromInt(a.getInteger(i)), b, swap));
        }
        return list;
    }

    static LongArrayList compute(ArithmeticFunction function, LongArrayList a, int b, boolean swap) {
        LongArrayList list = new LongArrayList(a.size());
        for (int i = 0; i < a.size(); i++) {
            list.add(compute(function, a.getLong(i), b, swap));
        }
        return list;
    }

    static LongArrayList compute(ArithmeticFunction function, LongArrayList a, long b, boolean swap) {
        LongArrayList list = new LongArrayList(a.size());
        for (int i = 0; i < a.size(); i++) {
            list.add(compute(function, a.getLong(i), b, swap));
        }
        return list;
    }

    static DoubleArrayList compute(ArithmeticFunction function, LongArrayList a, double b, boolean swap) {
        DoubleArrayList list = new DoubleArrayList(a.size());
        for (int i = 0; i < a.size(); i++) {
            list.add(compute(function, a.getLong(i), b, swap));
        }
        return list;
    }

    static DoubleArrayList compute(ArithmeticFunction function, DoubleArrayList a, double b, boolean swap) {
        DoubleArrayList list = new DoubleArrayList(a.size());
        for (int i = 0; i < a.size(); i++) {
            list.add(compute(function, a.getDouble(i), b, swap));
        }
        return list;
    }


    @Decimal
    static LongArrayList computeDecimal(ArithmeticFunction function, LongArrayList a, @Decimal long b, boolean swap) {
        @Decimal LongArrayList list = new LongArrayList(a.size());
        for (int i = 0; i < a.size(); i++) {
            list.add(computeDecimal(function, Decimal64Utils.fromLong(a.getLong(i)), b, swap));
        }
        return list;
    }

    static DoubleArrayList computeDecimal(ArithmeticFunction function, @Decimal LongArrayList a, double b, boolean swap) {
        DoubleArrayList list = new DoubleArrayList(a.size());
        for (int i = 0; i < a.size(); i++) {
            list.add(compute(function, Decimal64Utils.toDouble(a.getLong(i)), b, swap));
        }
        return list;
    }

    static DoubleArrayList compute(ArithmeticFunction function, DoubleArrayList a, DoubleArrayList b) {
        int size = Math.min(a.size(), b.size());
        DoubleArrayList list = new DoubleArrayList(size);
        for (int i = 0; i < size; i++) {
            list.add(compute(function, a.getDouble(i), b.getDouble(i)));
        }
        return list;
    }

    static DoubleArrayList computeDecimal(ArithmeticFunction function, DoubleArrayList a, @Decimal LongArrayList b, boolean swap) {
        int size = Math.min(a.size(), b.size());
        DoubleArrayList list = new DoubleArrayList(size);
        for (int i = 0; i < size; i++) {
            list.add(compute(function, a.getDouble(i), Decimal64Utils.toDouble(b.getLong(i)), swap));
        }
        return list;
    }

    static DoubleArrayList compute(ArithmeticFunction function, DoubleArrayList a, LongArrayList b, boolean swap) {
        int size = Math.min(a.size(), b.size());
        DoubleArrayList list = new DoubleArrayList(size);
        for (int i = 0; i < size; i++) {
            list.add(compute(function, a.getDouble(i), b.getLong(i), swap));
        }
        return list;
    }

    static DoubleArrayList compute(ArithmeticFunction function, DoubleArrayList a, IntegerArrayList b, boolean swap) {
        int size = Math.min(a.size(), b.size());
        DoubleArrayList list = new DoubleArrayList(size);
        for (int i = 0; i < size; i++) {
            list.add(compute(function, a.getDouble(i), b.getInteger(i), swap));
        }
        return list;
    }

    @Decimal
    static LongArrayList computeDecimalDecimal(ArithmeticFunction function, @Decimal LongArrayList a, @Decimal LongArrayList b) {
        int size = Math.min(a.size(), b.size());
        @Decimal LongArrayList list = new LongArrayList(size);
        for (int i = 0; i < size; i++) {
            list.add(computeDecimal(function, a.getLong(i), b.getLong(i)));
        }
        return list;
    }

    @Decimal
    static LongArrayList computeDecimal(ArithmeticFunction function, @Decimal LongArrayList a, LongArrayList b, boolean swap) {
        int size = Math.min(a.size(), b.size());
        @Decimal LongArrayList list = new LongArrayList(size);
        for (int i = 0; i < size; i++) {
            list.add(computeDecimal(function, a.getLong(i), Decimal64Utils.fromLong(b.getLong(i)), swap));
        }
        return list;
    }

    @Decimal
    static LongArrayList computeDecimal(ArithmeticFunction function, @Decimal LongArrayList a, IntegerArrayList b, boolean swap) {
        int size = Math.min(a.size(), b.size());
        @Decimal LongArrayList list = new LongArrayList(size);
        for (int i = 0; i < size; i++) {
            list.add(computeDecimal(function, a.getLong(i), Decimal64Utils.fromInt(b.getInteger(i))));
        }
        return list;
    }

    static LongArrayList compute(ArithmeticFunction function, LongArrayList a, LongArrayList b) {
        int size = Math.min(a.size(), b.size());
        LongArrayList list = new LongArrayList(size);
        for (int i = 0; i < size; i++) {
            list.add(compute(function, a.getLong(i), b.getLong(i)));
        }
        return list;
    }

    static LongArrayList compute(ArithmeticFunction function, LongArrayList a, IntegerArrayList b, boolean swap) {
        int size = Math.min(a.size(), b.size());
        LongArrayList list = new LongArrayList(size);
        for (int i = 0; i < size; i++) {
            list.add(compute(function, a.getLong(i), b.getInteger(i), swap));
        }
        return list;
    }

    static IntegerArrayList compute(ArithmeticFunction function, IntegerArrayList a, IntegerArrayList b) {
        int size = Math.min(a.size(), b.size());
        IntegerArrayList list = new IntegerArrayList(size);
        for (int i = 0; i < size; i++) {
            list.add(compute(function, a.getInteger(i), b.getInteger(i)));
        }
        return list;
    }

}