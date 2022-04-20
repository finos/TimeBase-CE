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
import com.epam.deltix.qsrv.hf.pub.md.ArrayDataType;
import com.epam.deltix.qsrv.hf.pub.md.BooleanDataType;
import com.epam.deltix.qsrv.hf.pub.md.CharDataType;
import com.epam.deltix.qsrv.hf.pub.md.ClassDataType;
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.DateTimeDataType;
import com.epam.deltix.qsrv.hf.pub.md.EnumDataType;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.QueryDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.StandardTypes;
import com.epam.deltix.qsrv.hf.pub.md.StaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.TimeOfDayDataType;
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;
import com.epam.deltix.qsrv.hf.pub.md.VarcharDataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.functions.FirstFunctionDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.functions.FunctionDescriptorInfo;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.functions.FunctionInfoDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.functions.InitArgument;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.functions.OverloadedFunctionSet;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.functions.ReflectionUtils;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.functions.StatefulFunctionDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.functions.StatefulFunctionsSet;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.functions.StatelessFunctionDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.DuplicateNameException;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.IllegalDataTypeException;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.IllegalMessageSourceException;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.IllegalObjectException;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.IllegalStreamSelectorException;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.IllegalTypeCombinationException;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.NonStaticExpressionException;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.NumericTypeRequiredException;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.PredicateCompilationException;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.UnacceptableNullException;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.UnexpectedTypeException;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.UnknownIdentifierException;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.WrongArgTypesException;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.WrongNumArgsException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.AndExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.ArithmeticExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.ArrayJoin;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.ArraySlicingExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.AsExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.BetweenExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.BinaryLogicalOperation;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.CallExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.CallExpressionWithDict;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.CallExpressionWithInit;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.CastArrayTypeExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.CastObjectTypeExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.CastTypeIdExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.ComplexExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.EqualsExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Expression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.FieldAccessExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.FieldAccessorExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.FieldIdentifier;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Identifier;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.InExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.LikeExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.LimitExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.NamedExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.NamedObjectType;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.NotExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.NullCheckExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.OptionElement;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.OrExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.OrderRelation;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.PredicateExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.RelationExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.SelectExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.ThisObject;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.TypeCheckExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.TypeIdentifier;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.UnaryMinusExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.UnionExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants.ArrayConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants.BinConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants.BooleanArrayConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants.BooleanConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants.CharArrayConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants.CharConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants.Constant;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants.DateArrayConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants.DateConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants.FloatConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants.IntegerConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants.LongConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants.Null;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants.NumericArrayConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants.StringArrayConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants.StringConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants.TimeConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants.TimeIntervalConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.SelectionMode;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.containers.ByteInstanceArray;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
import com.epam.deltix.util.collections.generated.ByteArrayList;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.parsers.CompilationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler.KEYWORD_FIRST;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler.KEYWORD_HYBRID;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler.KEYWORD_LAST;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler.KEYWORD_LIVE;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler.KEYWORD_NOW;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler.KEYWORD_POSITION;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler.KEYWORD_REVERSE;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler.KEYWORD_SYMBOL;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler.KEYWORD_THIS;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler.KEYWORD_TIMESTAMP;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler.isCompatibleWithoutConversion;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler.lookUpVariable;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler.setUpEnv;

/**
 *
 */
public class QQLExpressionCompiler {
    private Environment env;
    private ClassMap classMap;

    public QQLExpressionCompiler(Environment env) {
        this.env = env;
        this.classMap = new ClassMap(env);
    }

    public CompiledExpression compile(Expression e, DataType expectedType) {
        CompiledExpression ce;

        if (e instanceof ComplexExpression)
            ce = compileComplexExpression((ComplexExpression) e, expectedType);
        else if (e instanceof FieldAccessExpression)
            ce = compileFieldAccessExpression((FieldAccessExpression) e);
        else if (e instanceof Identifier)
            ce = compileIdentifier((Identifier) e);
        else if (e instanceof Constant)
            ce = compileConstant((Constant) e, expectedType);
        else if (e instanceof ArrayConstant)
            ce = compileArrayConstant((ArrayConstant) e, expectedType);
        else
            throw new UnsupportedOperationException(e.getClass().getName());

        if (expectedType != null && !isCompatibleWithoutConversion(ce.type, expectedType))
            throw new UnexpectedTypeException(e, ce.type, expectedType);

        return (ce);
    }

    private static void checkType(
            DataType knownType,
            DataType expectedType,
            Expression e
    )
            throws UnexpectedTypeException {
        if (expectedType != null && !isCompatibleWithoutConversion(knownType, expectedType))
            throw new UnexpectedTypeException(e, knownType, expectedType);
    }

    private static void checkBooleanType(
            DataType expectedType,
            Expression e
    )
            throws UnexpectedTypeException {
        checkType(StandardTypes.CLEAN_BOOLEAN, expectedType, e);
    }

    private CompiledExpression compileComplexExpression(ComplexExpression e, DataType expectedType) {
        if (e instanceof UnionExpression) {
            return compileUnion((UnionExpression) e);
        }

        if (e instanceof SelectExpression)
            return (compileSelect((SelectExpression) e));

        if (e instanceof FieldAccessorExpression) {
            return compileFieldAccessorExpression((FieldAccessorExpression) e);
        }

        if (e instanceof AsExpression) {
            return compileCastAsExpression((AsExpression) e, expectedType);
        }

        if (e instanceof PredicateExpression) {
            return compileArrayPredicateExpression((PredicateExpression) e);
        }

        if (e instanceof RelationExpression)
            return (compileRelationExpression((RelationExpression) e, expectedType));

        if (e instanceof BetweenExpression)
            return (compileBetweenExpression((BetweenExpression) e, expectedType));

        if (e instanceof EqualsExpression)
            return (compileEqualsExpression((EqualsExpression) e, expectedType));

        if (e instanceof LikeExpression)
            return (compileLikeExpression((LikeExpression) e, expectedType));

        if (e instanceof ArithmeticExpression)
            return (compileArithmeticExpression((ArithmeticExpression) e, expectedType));

        if (e instanceof UnaryMinusExpression)
            return (compileUnaryMinusExpression((UnaryMinusExpression) e, expectedType));

        if (e instanceof AndExpression)
            return (compileAndExpression((AndExpression) e, expectedType));

        if (e instanceof OrExpression)
            return (compileOrExpression((OrExpression) e, expectedType));

        if (e instanceof NotExpression)
            return (compileNotExpression((NotExpression) e, expectedType));

        if (e instanceof NamedExpression)
            return (compileNamedExpression((NamedExpression) e, expectedType));

        if (e instanceof CallExpression)
            return (compileCallExpression((CallExpression) e, expectedType));

        if (e instanceof CallExpressionWithDict) {
            return compileCallExpression((CallExpressionWithDict) e, expectedType);
        }

        if (e instanceof CallExpressionWithInit) {
            return compileCallExpression((CallExpressionWithInit) e, expectedType);
        }

        if (e instanceof TypeCheckExpression)
            return (compileTypeCheckExpression((TypeCheckExpression) e, expectedType));

        if (e instanceof InExpression)
            return (compileInExpression((InExpression) e, expectedType));

        if (e instanceof NullCheckExpression)
            return (compileNullCheckExpression((NullCheckExpression) e, expectedType));

//        if (e instanceof SelectorExpression)
//            return (compileSelectorExpression ((SelectorExpression) e, expectedType));
//
//        if (e instanceof CastExpression)
//            return (compileCastExpression ((CastExpression) e, expectedType));

        throw new UnsupportedOperationException(e.getClass().getName());
    }

    private CompiledConstant compileConstant(Constant e, DataType expectedType) {
        if (e instanceof BooleanConstant)
            return (new CompiledConstant(StandardTypes.CLEAN_BOOLEAN, ((BooleanConstant) e).value));

        if (e instanceof IntegerConstant) {
            long value = ((IntegerConstant) e).value;

            if (expectedType instanceof FloatDataType)
                return (new CompiledConstant(StandardTypes.CLEAN_DECIMAL, Long.toString(value)));
            else if (expectedType instanceof IntegerDataType)
                return (new CompiledConstant(expectedType, value));

            if (value > Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                return new CompiledConstant(StandardTypes.INT32_CONTAINER.getType(false), value);
            } else {
                return new CompiledConstant(StandardTypes.INT64_CONTAINER.getType(false), value);
            }
        }

        if (e instanceof LongConstant) {
            long value = ((LongConstant) e).value;

            if (expectedType instanceof FloatDataType)
                return (new CompiledConstant(StandardTypes.CLEAN_DECIMAL, Long.toString(value)));
            else if (expectedType instanceof IntegerDataType)
                return (new CompiledConstant(expectedType, value));
            return new CompiledConstant(StandardTypes.INT64_CONTAINER.getType(false), value);
        }

        if (e instanceof FloatConstant) {
            if (!(expectedType instanceof FloatDataType)) {
                if (((FloatConstant) e).isDecimal64()) {
                    return new CompiledConstant(StandardTypes.CLEAN_DECIMAL, ((FloatConstant) e).toFloatString());
                } else {
                    return new CompiledConstant(StandardTypes.CLEAN_FLOAT, ((FloatConstant) e).toFloatString());
                }
            } else if (((FloatDataType) expectedType).isDecimal64()) {
                return new CompiledConstant(StandardTypes.CLEAN_DECIMAL, ((FloatConstant) e).toFloatString());
            } else {
                return new CompiledConstant(StandardTypes.CLEAN_FLOAT, ((FloatConstant) e).toFloatString());
            }
        }

        if (e instanceof StringConstant)
            return (new CompiledConstant(StandardTypes.CLEAN_VARCHAR, ((StringConstant) e).value));

        if (e instanceof DateConstant)
            return (
                    new CompiledConstant(
                            StandardTypes.CLEAN_TIMESTAMP,
                            ((DateConstant) e).nanoseconds / 1000000
                    )
            );

        if (e instanceof TimeConstant)
            return (
                    new CompiledConstant(
                            StandardTypes.CLEAN_TIMEOFDAY,
                            (int) (((TimeConstant) e).nanoseconds / 1000000)
                    )
            );

        if (e instanceof BinConstant)
            return (
                    new CompiledConstant(
                            StandardTypes.CLEAN_BINARY,
                            ((BinConstant) e).bytes
                    )
            );

        if (e instanceof CharConstant)
            return (
                    new CompiledConstant(
                            StandardTypes.CLEAN_CHAR,
                            ((CharConstant) e).ch
                    )
            );

        if (e instanceof TimeIntervalConstant) {
            return new CompiledConstant(StandardTypes.INT64_CONTAINER.getType(false), ((TimeIntervalConstant) e).getTimeStampMs());
        }

        if (e instanceof Null) {
            if (expectedType != null && !expectedType.isNullable())
                throw new UnacceptableNullException((Null) e);

            return (new CompiledConstant(expectedType, null));
        }

        throw new UnsupportedOperationException(e.getClass().getName());
    }

    private CompiledArrayConstant<?, ?, ?> compileArrayConstant(ArrayConstant arrayConstant, DataType expectedType) {
        CompiledConstant[] array = arrayConstant.getExpressions().stream()
                .map(e -> (CompiledConstant) compile(e, null))
                .toArray(CompiledConstant[]::new);
        if (arrayConstant instanceof NumericArrayConstant) {
            return compileArrayConstant((NumericArrayConstant) arrayConstant, expectedType, array);
        } else if (arrayConstant instanceof StringArrayConstant) {
            return compileStringArrayConstant(expectedType, array);
        } else if (arrayConstant instanceof BooleanArrayConstant) {
            return compileBooleanArrayConstant(expectedType, array);
        } else if (arrayConstant instanceof CharArrayConstant) {
            return compileCharArrayConstant(expectedType, array);
        } else if (arrayConstant instanceof DateArrayConstant) {
            return compileDateArrayConstant(expectedType, array);
        }

        throw new UnsupportedOperationException(arrayConstant.getClass().getName());
    }

    private CompiledArrayConstant<?, ?, ?> compileArrayConstant(NumericArrayConstant arrayConstant,
                                                                DataType expectedType, CompiledConstant[] constants) {
        int intSize = 1;
        boolean isDecimal = true;
        boolean isFloat = false;
        boolean nullable = false;
        for (CompiledConstant e : constants) {
            if (e.type == null || e.value == null) {
                nullable = true;
                continue;
            }
            if (!(e.type instanceof IntegerDataType) && !(e.type instanceof FloatDataType)) {
                throw new NumericTypeRequiredException(arrayConstant, e.type);
            }
            if (e.type instanceof IntegerDataType) {
                intSize = Math.max(intSize, ((IntegerDataType) e.type).getNativeTypeSize());
            } else {
                isFloat = true;
                if (!((FloatDataType) e.type).isDecimal64()) {
                    isDecimal = false;
                }
            }
        }
        if (isFloat) {
            return CompiledArrayConstant.createFloatArrayConstant(isDecimal, constants, nullable);
        } else {
            return CompiledArrayConstant.createIntegerArrayConstant(intSize, constants, nullable);
        }
    }

    private CompiledArrayConstant<Byte, ByteArrayList, ByteInstanceArray> compileBooleanArrayConstant(
            DataType expectedType, CompiledConstant[] constants
    ) {
        boolean nullable = false;
        for (CompiledConstant constant : constants) {
            if (constant.getValue() == null || constant.type == null) {
                nullable = true;
            }
        }
        return CompiledArrayConstant.createBooleanArrayConstant(constants, nullable);
    }

    private CompiledArrayConstant<?, ?, ?> compileStringArrayConstant(DataType expectedType, CompiledConstant[] constants) {
        boolean nullable = false;
        for (CompiledConstant constant : constants) {
            if (constant.getValue() == null || constant.type == null) {
                nullable = true;
            }
        }
        return CompiledArrayConstant.createStringArrayConstant(constants, nullable);
    }

    private CompiledArrayConstant<?, ?, ?> compileCharArrayConstant(DataType expectedType, CompiledConstant[] constants) {
        boolean nullable = false;
        for (CompiledConstant constant : constants) {
            if (constant.getValue() == null || constant.type == null) {
                nullable = true;
            }
        }
        return CompiledArrayConstant.createCharArrayConstant(constants, nullable);
    }

    private CompiledArrayConstant<?, ?, ?> compileDateArrayConstant(DataType expectedType, CompiledConstant[] constants) {
        boolean nullable = false;
        for (CompiledConstant constant : constants) {
            if (constant.getValue() == null || constant.type == null) {
                nullable = true;
            }
        }
        return CompiledArrayConstant.createDateArrayConstant(constants, nullable);
    }

    private CompiledExpression convertIfNecessary(
            CompiledExpression x,
            CompiledExpression other
    ) {
        DataType xt = x.type;
        DataType ot = other.type;

        if (xt instanceof IntegerDataType && ot instanceof FloatDataType) {

            if (x instanceof CompiledConstant) {
                CompiledConstant c = (CompiledConstant) x;

                if (((FloatDataType) ot).isDecimal64()) {
                    return new CompiledConstant(StandardTypes.CLEAN_DECIMAL, c.getString());
                } else {
                    return new CompiledConstant(StandardTypes.CLEAN_FLOAT, c.getString());
                }
            }

            if (((FloatDataType) ot).isDecimal64()) {
                return new SimpleFunction(SimpleFunctionCode.INTEGER_TO_DECIMAL, x);
            } else {
                return (new SimpleFunction(SimpleFunctionCode.INTEGER_TO_FLOAT, x));
            }
        } else if (xt instanceof FloatDataType && ot instanceof FloatDataType) {
            if (x instanceof CompiledConstant) {
                if (!((FloatDataType) xt).isDecimal64() && ((FloatDataType) ot).isDecimal64()) {
                    return new CompiledConstant(StandardTypes.CLEAN_DECIMAL, ((CompiledConstant) x).getString());
                } else if (((FloatDataType) xt).isDecimal64() && !((FloatDataType) ot).isDecimal64()) {
                    return new CompiledConstant(StandardTypes.CLEAN_FLOAT, ((CompiledConstant) x).getString());
                }
            } else if (!(other instanceof CompiledConstant) && ((FloatDataType) xt).isDecimal64()
                    && !((FloatDataType) ot).isDecimal64()) {
                return new SimpleFunction(SimpleFunctionCode.DECIMAL_TO_FLOAT, x);
            }
        }

        return (x);
    }

    private DataType intersectTypes(Expression e, DataType a, DataType b) {
        if (a == null)
            return (b);

        if (b == null)
            return (a);

        checkType(b, a, e);

        return (a);
    }

    private CompiledExpression compileNamedExpression(NamedExpression e, DataType expectedType) {
        String name = e.name;

        switch (name) {
            case KEYWORD_TIMESTAMP:
                expectedType = intersectTypes(e, expectedType, StandardTypes.NULLABLE_TIMESTAMP);
                break;

            case KEYWORD_SYMBOL:
                expectedType = intersectTypes(e, expectedType, StandardTypes.CLEAN_VARCHAR);
                break;
//
//            case KEYWORD_TYPE:
//                throw new UnsupportedOperationException("type initializer");
//                //expectedType = intersectTypes (e, expectedType, ?);
        }

        // unbind alias name to prevent recursive compilation
        // todo: refactor this
        boolean removed = ((EnvironmentFrame) env).unbind(NamedObjectType.VARIABLE, e.name);
        try {
            CompiledExpression ret = compile(e.getArgument(), expectedType);
            ret.name = name;
            return (ret);
        } finally {
            if (removed) {
                ((EnvironmentFrame) env).bindNoDup(NamedObjectType.VARIABLE, e.name, e.location, e);
            }
        }
    }

    private CompiledExpression compileBetweenExpression(BetweenExpression e, DataType expectedType) {
        checkBooleanType(expectedType, e);

        CompiledExpression arg = compile(e.args[0], null);
        CompiledExpression min = compile(e.args[1], null);
        CompiledExpression max = compile(e.args[2], null);

        return (
                processAnd(
                        processRelation(e, OrderRelation.GE, arg, min),
                        processRelation(e, OrderRelation.LE, arg, max)
                )
        );
    }

    private CompiledExpression<?> processRelation(Expression e, OrderRelation relation, CompiledExpression<?> left,
                                                  CompiledExpression<?> right) {
        left = convertIfNecessary(left, right);
        right = convertIfNecessary(right, left);

        if (left instanceof CompiledConstant && right instanceof CompiledConstant)
            return (computeRelationExpression(e, relation, (CompiledConstant) left, (CompiledConstant) right));

        return new ComparisonOperation(relation, left, right);
    }

    private CompiledExpression<?> compileRelationExpression(RelationExpression e, DataType expectedType) {
        CompiledExpression<?> left = compile(e.getLeft(), null);
        CompiledExpression<?> right = compile(e.getRight(), null);

        ComparisonOperation.validate(e, left, right);

        return (processRelation(e, e.relation, left, right));
    }

    private CompiledConstant computeRelationExpression(
            Expression e,
            OrderRelation relation,
            CompiledConstant left,
            CompiledConstant right
    ) {
        DataType leftType = left.type;
        DataType rightType = right.type;
        boolean ret;

        if (leftType instanceof IntegerDataType && rightType instanceof IntegerDataType ||
                leftType instanceof DateTimeDataType && rightType instanceof DateTimeDataType ||
                leftType instanceof TimeOfDayDataType && rightType instanceof TimeOfDayDataType) {
            long a = left.getLong();
            long b = right.getLong();

            switch (relation) {
                case GT:
                    ret = a > b;
                    break;
                case GE:
                    ret = a >= b;
                    break;
                case LE:
                    ret = a <= b;
                    break;
                case LT:
                    ret = a < b;
                    break;
                default:
                    throw new RuntimeException();
            }
        } else if (leftType instanceof FloatDataType && rightType instanceof FloatDataType) {
            // assuming both are DECIMAL
            @Decimal long a = left.getLong();
            @Decimal long b = left.getLong();

            switch (relation) {
                case GT:
                    ret = Decimal64Utils.isGreater(a, b);
                    break;
                case GE:
                    ret = Decimal64Utils.isGreaterOrEqual(a, b);
                    break;
                case LE:
                    ret = Decimal64Utils.isLessOrEqual(a, b);
                    break;
                case LT:
                    ret = Decimal64Utils.isLess(a, b);
                    break;
                default:
                    throw new RuntimeException();
            }
        } else if (leftType instanceof VarcharDataType && rightType instanceof VarcharDataType) {
            String a = (String) left.value;
            String b = (String) right.value;

            switch (relation) {
                case GT:
                    ret = a.compareTo(b) > 0;
                    break;
                case GE:
                    ret = a.compareTo(b) >= 0;
                    break;
                case LE:
                    ret = a.compareTo(b) <= 0;
                    break;
                case LT:
                    ret = a.compareTo(b) < 0;
                    break;
                default:
                    throw new RuntimeException();
            }
        } else if (leftType instanceof CharDataType && rightType instanceof CharDataType) {
            char a = left.getChar();
            char b = right.getChar();

            switch (relation) {
                case GT:
                    ret = a > b;
                    break;
                case GE:
                    ret = a >= b;
                    break;
                case LE:
                    ret = a <= b;
                    break;
                case LT:
                    ret = a < b;
                    break;
                default:
                    throw new RuntimeException();
            }
        } else
            throw new IllegalTypeCombinationException(e, leftType, rightType);

        return (new CompiledConstant(StandardTypes.CLEAN_BOOLEAN, ret));
    }

    private CompiledExpression<?> compileEqualsExpression(EqualsExpression e, DataType expectedType) {
        CompiledExpression<?> left = compile(e.getLeft(), null);
        CompiledExpression<?> right = compile(e.getRight(), null);

        left = convertIfNecessary(left, right);
        right = convertIfNecessary(right, left);

        if (e.isStrict()) {
            return compileStrictEqualityTest(e, left, right, expectedType);
        } else {
            return (compileEqualityTest(e, e.isEqual, left, right));
        }
    }

    private CompiledExpression<?> compileStrictEqualityTest(EqualsExpression e, CompiledExpression<?> left,
                                                            CompiledExpression<?> right, DataType expectedType) {
        checkBooleanType(expectedType, e);
        if (QQLPostProcessingPatterns.isNull(left)) {
            return (processNullCheckExpression(right, e.isEqual()));
        } else if (QQLPostProcessingPatterns.isNull(right)) {
            return (processNullCheckExpression(left, e.isEqual()));
        }

        return new StrictEqualityCheckOperation(e.isEqual(), left, right);
    }

    private CompiledExpression<?> compileEqualityTest(Expression e, boolean positive, CompiledExpression<?> left,
                                                      CompiledExpression<?> right) {
        if (QQLPostProcessingPatterns.isNull(left)) {
            return processNullCheckExpression(right, positive);
        } else if (QQLPostProcessingPatterns.isNull(right)) {
            return processNullCheckExpression(left, positive);
        }

        EqualityCheckOperation.validate(e, left.type, right.type);

        if (left instanceof CompiledConstant && right instanceof CompiledConstant) {
            return computeEqualityTest(e, positive, (CompiledConstant) left, (CompiledConstant) right);
        }
        return new EqualityCheckOperation(positive, left, right);
    }


    private CompiledConstant computeEqualityTest(
            Expression e,
            boolean positive,
            CompiledConstant left,
            CompiledConstant right
    ) {
        DataType leftType = left.type;
        DataType rightType = right.type;
        boolean ret;

        if (left.isNull())
            if (right.isNull())
                ret = true;
            else
                ret = false;
        else if (right.isNull())
            ret = false;
        else if (leftType instanceof IntegerDataType && rightType instanceof IntegerDataType ||
                leftType instanceof DateTimeDataType && rightType instanceof DateTimeDataType ||
                leftType instanceof TimeOfDayDataType && rightType instanceof TimeOfDayDataType)
            ret = positive == (left.getLong() == right.getLong());
        else if (leftType instanceof FloatDataType && rightType instanceof FloatDataType) {
            if (!((FloatDataType) leftType).isDecimal64() || !((FloatDataType) rightType).isDecimal64()) {
                ret = positive == (left.getDouble() == right.getDouble());
            } else {
                ret = positive == (Decimal64Utils.isEqual(left.getLong(), right.getLong()));
            }
        } else if (leftType instanceof VarcharDataType && rightType instanceof VarcharDataType)
            ret = positive == (left.getString().equals(right.getString()));
        else if (leftType instanceof CharDataType && rightType instanceof CharDataType)
            ret = positive == (left.getChar() == right.getChar());
        else if (leftType instanceof BooleanDataType && rightType instanceof BooleanDataType)
            ret = positive == (left.getBoolean() == right.getBoolean());
        else
            throw new IllegalTypeCombinationException(e, leftType, rightType);

        return (new CompiledConstant(StandardTypes.CLEAN_BOOLEAN, ret));
    }

    private CompiledExpression compileLikeExpression(LikeExpression e, DataType expectedType) {
        checkBooleanType(expectedType, e);

        CompiledExpression left = compile(e.getLeft(), null);
        CompiledExpression right = compile(e.getRight(), null);

        left = convertIfNecessary(left, right);
        right = convertIfNecessary(right, left);

        DataType leftType = left.type;
        DataType rightType = right.type;

        if (left instanceof CompiledConstant && right instanceof CompiledConstant) {
            return (computeLikeExpression(e, (CompiledConstant) left, (CompiledConstant) right));
        } else if (QQLPostProcessingPatterns.isNull(left))
            return (new CompiledConstant(StandardTypes.CLEAN_BOOLEAN, false));
        else if (QQLPostProcessingPatterns.isNull(right))
            return (new CompiledConstant(StandardTypes.CLEAN_BOOLEAN, false));

        SimpleFunctionCode f = null;
        if (leftType instanceof VarcharDataType && rightType instanceof VarcharDataType)
            f = e.isNegative ? SimpleFunctionCode.VARCHAR_NLIKE : SimpleFunctionCode.VARCHAR_LIKE;

        if (f == null)
            throw new IllegalTypeCombinationException(e, leftType, rightType);

        return (new SimpleFunction(f, left, right));
    }

    private CompiledConstant computeLikeExpression(LikeExpression e, CompiledConstant left, CompiledConstant right) {
        boolean result;

        DataType leftType = left.type;
        DataType rightType = right.type;
        if (leftType instanceof VarcharDataType && rightType instanceof VarcharDataType) {
            result = StringUtils.wildcardMatch(left.getString(), right.getString(), false);
        } else
            throw new IllegalTypeCombinationException(e, leftType, rightType);

        return new CompiledConstant(StandardTypes.CLEAN_BOOLEAN, result);
    }

    private CompiledExpression<?> compileArithmeticExpression(ArithmeticExpression e, DataType expectedType) {

        CompiledExpression<?> left = compile(e.getLeft(), null);
        CompiledExpression<?> right = compile(e.getRight(), null);

        ArithmeticOperation.validateArgs(e, left, right);

        left = convertIfNecessary(left, right);
        right = convertIfNecessary(right, left);

        if (left instanceof CompiledConstant && right instanceof CompiledConstant) {
            return ConstantsProcessor.compute(e, (CompiledConstant) left, (CompiledConstant) right);
        } else if (left instanceof CompiledArrayConstant && right instanceof CompiledConstant) {
            return ConstantsProcessor.compute(e, (CompiledArrayConstant<?, ?, ?>) left, (CompiledConstant) right, false);
        } else if (right instanceof CompiledArrayConstant && left instanceof CompiledConstant) {
            return ConstantsProcessor.compute(e, (CompiledArrayConstant<?, ?, ?>) right, (CompiledConstant) left, true);
        } else if (left instanceof CompiledArrayConstant && right instanceof CompiledArrayConstant) {
            return ConstantsProcessor.compute(e, (CompiledArrayConstant<?, ?, ?>) left, (CompiledArrayConstant<?, ?, ?>) right);
        }

        return new ArithmeticOperation(e.function, left, right);
    }

    private CompiledExpression<?> compileUnaryMinusExpression(UnaryMinusExpression e, DataType expectedType) {
        CompiledExpression<?> arg = compile(e.getArgument(), null);

        NegateOperation.validate(e, arg);

        if (arg instanceof CompiledConstant)
            return (computeUnaryMinusExpression(e, (CompiledConstant) arg));

        return new NegateOperation(arg, arg.type);
    }

    private CompiledConstant computeUnaryMinusExpression(
            UnaryMinusExpression e,
            CompiledConstant arg
    ) {
        DataType argType = arg.type;

        if (argType instanceof IntegerDataType) {
            long a = arg.getLong();

            return (new CompiledConstant(StandardTypes.CLEAN_INTEGER, -a));
        } else if (argType instanceof FloatDataType) {
            if (((FloatDataType) argType).isDecimal64()) {
                @Decimal long a = arg.getLong();
                return new CompiledConstant(StandardTypes.CLEAN_DECIMAL, Decimal64Utils.toString(Decimal64Utils.negate(a)));
            } else {
                double a = arg.getDouble();
                return (new CompiledConstant(StandardTypes.CLEAN_FLOAT, Double.toString(-a)));
            }
        } else
            throw new UnexpectedTypeException(e, argType, StandardTypes.CLEAN_FLOAT);
    }

    private CompiledExpression<?> compileAndExpression(AndExpression e, DataType expectedType) {
        CompiledExpression<?> left = compile(e.getLeft(), null);
        CompiledExpression<?> right = compile(e.getRight(), null);

        LogicalOperation.validate(e, left, right);

        return (processAnd(left, right));
    }

    private CompiledExpression<?> processAnd(CompiledExpression<?> left, CompiledExpression<?> right) {
        if (left instanceof CompiledConstant && right instanceof CompiledConstant) {
            CompiledConstant cleft = (CompiledConstant) left;
            CompiledConstant cright = (CompiledConstant) right;

            return (CompiledConstant.trueOrFalse(cleft.getBoolean() && cright.getBoolean()));
        }

        return new LogicalOperation(BinaryLogicalOperation.AND, left, right);
    }

    private CompiledExpression<?> compileOrExpression(OrExpression e, DataType expectedType) {
        CompiledExpression<?> left = compile(e.getLeft(), null);
        CompiledExpression<?> right = compile(e.getRight(), null);

        LogicalOperation.validate(e, left, right);

        return (processOr(left, right));
    }

    private CompiledExpression<?> processOr(CompiledExpression<?> left, CompiledExpression<?> right) {
        if (left instanceof CompiledConstant && right instanceof CompiledConstant) {
            CompiledConstant cleft = (CompiledConstant) left;
            CompiledConstant cright = (CompiledConstant) right;
            return (CompiledConstant.trueOrFalse(cleft.getBoolean() || cright.getBoolean()));
        }

        return new LogicalOperation(BinaryLogicalOperation.OR, left, right);
    }

    private CompiledExpression<?> compileNotExpression(NotExpression e, DataType expectedType) {
        CompiledExpression<?> arg = compile(e.getArgument(), null);

        NotOperation.validate(e, arg);

        if (arg instanceof CompiledConstant) {
            CompiledConstant carg = (CompiledConstant) arg;
            return (CompiledConstant.trueOrFalse(!carg.getBoolean()));
        }

        return new NotOperation(arg);
    }

    private CompiledExpression compileNullCheckExpression(
            NullCheckExpression e,
            DataType expectedType
    ) {
        checkBooleanType(expectedType, e);

        CompiledExpression arg = compile(e.getArgument(), null);

        return (processNullCheckExpression(arg, e.checkIsNull));
    }

    private CompiledExpression processNullCheckExpression(
            CompiledExpression arg,
            boolean positive
    ) {
        DataType argType = arg.type;

        if (!argType.isNullable())
            return (CompiledConstant.trueOrFalse(!positive));

        if (arg instanceof CompiledConstant) {
            CompiledConstant carg = (CompiledConstant) arg;

            if (carg.isNull())
                return (carg);

            return (CompiledConstant.trueOrFalse(carg.isNull() == positive));
        }

        SimpleFunctionCode code =
                positive ?
                        SimpleFunctionCode.IS_NULL :
                        SimpleFunctionCode.IS_NOT_NULL;

        return (new SimpleFunction(code, arg));
    }

    //  SIDE EFFECT env=...
    private void setUpQueryEnv(CompiledQuery q) {
        Set<ClassDescriptor> cds = new HashSet<>();

        q.getAllTypes(cds);

        for (ClassDescriptor cd : cds)
            classMap.register(cd);

        RecordClassDescriptor[] types = q.getConcreteOutputTypes();

        EnvironmentFrame selectorEnv = new EnvironmentFrame(env);

        selectorEnv.bind(NamedObjectType.VARIABLE, KEYWORD_THIS, new ThisRef(new ClassDataType(false, types)));

        for (ClassDescriptor cd : classMap.getAllDescriptors())
            setUpEnv(selectorEnv, cd);

        selectorEnv.setTopTypes(types);

        env = selectorEnv;
    }

    private void setUpPredicateEnv(DataType type) {
        RecordClassDescriptor[] descriptors = new RecordClassDescriptor[0];
        if (type instanceof ClassDataType) {
            ClassDataType classDataType = (ClassDataType) type;
            descriptors = classDataType.getDescriptors();
        }

        EnvironmentFrame predicateEnv = new EnvironmentFrame(env);
        predicateEnv.bind(NamedObjectType.VARIABLE, KEYWORD_THIS, new PredicateIterator(type));
        Set<String> boundFields = new HashSet<>();
        for (int i = 0; i < descriptors.length; ++i) {
            for (DataField f : QQLCompilerUtils.collectFields(descriptors[i], true)) {
                if (!boundFields.contains((f.getName().toUpperCase()))) {
                    predicateEnv.bind(
                        NamedObjectType.VARIABLE, f.getName(), new PredicateFieldRef(f)
                    );
                    boundFields.add(f.getName().toUpperCase());
                }
            }
        }
        predicateEnv.setTopTypes(descriptors);

        env = predicateEnv;
    }

    private void setUpAliasEnv(List<Expression> expressions) {
        EnvironmentFrame aliasEnv = new EnvironmentFrame(env);
        expressions.forEach(e -> compileAliases(aliasEnv, e));
        env = aliasEnv;
    }

    private void compileAliases(EnvironmentFrame env, Expression expression) {
        if (expression instanceof ComplexExpression) {
            ComplexExpression complexExpression = (ComplexExpression) expression;
            for (int i = 0; i < complexExpression.args.length; ++i) {
                compileAliases(env, complexExpression.args[i]);
            }
        }

        NamedExpression namedExpression = getAlias(expression);
        if (namedExpression != null) {
            env.bindNoDup(
                NamedObjectType.VARIABLE, namedExpression.name, namedExpression.location, namedExpression
            );
        }
    }

    private NamedExpression getAlias(Expression expression) {
        if (expression instanceof NamedExpression) {
            return (NamedExpression) expression;
        }

        if (expression instanceof AsExpression) {
            return getNamedExpression((AsExpression) expression);
        }

        return null;
    }

    //  SIDE EFFECT env=...
    public void setUpClassSetEnv(RecordClassDescriptor... types) {
        for (RecordClassDescriptor cd : types)
            classMap.register(cd);

        EnvironmentFrame selectorEnv = new EnvironmentFrame(env);

        selectorEnv.bind(NamedObjectType.VARIABLE, KEYWORD_THIS, new ThisRef(new ClassDataType(false, types)));

        for (ClassDescriptor cd : classMap.getAllDescriptors())
            setUpEnv(selectorEnv, cd);

        env = selectorEnv;
    }

    @SuppressWarnings("ConvertToStringSwitch")
    private TupleConstructor createAnonymousTuple(
            Expression[] origExpressions,
            CompiledExpression[] args,
            boolean clearTimeAndIdentity,
            TypeIdentifier typeId
    ) {
        int n = args.length;
        ArrayList<DataField> fields = new ArrayList<>(n);
        ArrayList<CompiledExpression<?>> nsInits = new ArrayList<>(n);
        CompiledExpression tsInit = null;
        CompiledExpression symbolInit = null;

        HashSet<String> namesInUse = new HashSet<>();
        Map<RecordClassDescriptor, List<CompiledExpression<?>>> typeToInitializers = new LinkedHashMap<>();

        for (int ii = 0; ii < n; ii++) {
            CompiledExpression e = args[ii];
            Expression oe = origExpressions[ii];
            DataType type = e.type;
            String name = e.name;

            // no switch! - could be null
            if (KEYWORD_TIMESTAMP.equals(name)) {
                if (tsInit != null)
                    throw new DuplicateNameException(oe, name);

                tsInit = e;
            } else if (KEYWORD_SYMBOL.equals(name)) {
                if (symbolInit != null)
                    throw new DuplicateNameException(oe, name);

                symbolInit = e;
            } else {
                if (isNamedExpression(oe)) {
                    if (!namesInUse.add(name.toUpperCase()))
                        throw new DuplicateNameException(oe, name);
                } else {
                    // name was implied; make unambiguous
                    if (name == null || !namesInUse.add(name.toUpperCase())) {
                        if (name == null)
                            name = "$";

                        for (int jj = 1; ; jj++) {
                            String test = name + jj;

                            if (namesInUse.add(test.toUpperCase())) {
                                name = test;
                                break;
                            }
                        }
                    }
                }

                if (e instanceof CompiledConstant) {
                    CompiledConstant c = (CompiledConstant) e;

                    if (type == null) {
                        assert c.isNull();
                        type = StandardTypes.NULLABLE_VARCHAR;
                    }

                    fields.add(new StaticDataField(name, name, type, type.toString(c.getValue())));
                } else {
                    fields.add(new NonStaticDataField(name, name, type));
                    nsInits.add(e);
                }

            }
        }

        if (clearTimeAndIdentity) {
            if (tsInit == null)
                tsInit = new CompiledConstant(StandardTypes.NULLABLE_TIMESTAMP, null);

            if (symbolInit == null)
                symbolInit = new CompiledConstant(StandardTypes.CLEAN_VARCHAR, "");

//            if (typeInit == null)
//                typeInit = new CompiledConstant(StdEnvironment.INSTR_TYPE_ENUM, InstrumentType.SYSTEM.ordinal());
        }

        String typeName = typeId != null ? typeId.typeName : null;
        RecordClassDescriptor rcd = new RecordClassDescriptor(
            typeName, null, false, null,
            fields.toArray(new DataField[fields.size()])
        );
        typeToInitializers.put(rcd, new ArrayList<>(nsInits));

        ClassDataType type = new ClassDataType(false, rcd);

        return new TupleConstructor(
            type, tsInit, symbolInit,
            typeToInitializers,
            nsInits.toArray(new CompiledExpression[nsInits.size()])
        );
    }

    private CompiledExpression<?> compileUnion(UnionExpression e) {
        LimitExpression limitExpression = e.limit;
        Expression[] subSelects = QQLCompilerUtils.flattenUnion(e);
        CompiledQuery[] compiledQueries = new CompiledQuery[subSelects.length];
        for (int i = 0; i < subSelects.length; ++i) {
            if (subSelects[i] instanceof SelectExpression) {
                compiledQueries[i] = compileSelect((SelectExpression) subSelects[i]);
            } else {
                throw new CompilationException("Invalid Select expression type", subSelects[i]);
            }
        }

        Map<String, RecordClassDescriptor> outputTypes = UnionTypesBuilder.buildType(compiledQueries);

        boolean forward = compiledQueries[0].isForward();
        for (int i = 1; i < compiledQueries.length; ++i) {
            if (compiledQueries[i].isForward() != forward) {
                throw new CompilationException("Can't union queries with different reverse type", e);
            }
        }

        SelectLimit limit = (limitExpression != null ? compileLimit(limitExpression) : null);

        return new CompiledUnion(
            new QueryDataType(true, new ClassDataType(
                true, outputTypes.values().toArray(new RecordClassDescriptor[0])
            )),
            forward, limit, compiledQueries
        );
    }

    private CompiledQuery compileSelect(SelectExpression e) {
        Expression src = e.getSource();
        Expression fe = e.getFilter();
        Expression[] with = e.getWithExpressions();
        ArrayJoin arrayJoinExpression = e.getArrayJoin();
        Expression[] selectors = e.getSelectors();
        boolean selectFirst = false;
        boolean selectLast = false;
        boolean selectCurrent = false;
        boolean someFormOfSelectStar = false;

        if (selectors.length == 1) {
            Expression se = selectors[0];

            if (QQLPreProcessingPatterns.isThis(se)) {
                selectCurrent = true;
                someFormOfSelectStar = true;
            } else if (QQLPreProcessingPatterns.isFirstThis(se)) {
                selectFirst = true;
                someFormOfSelectStar = true;
            } else if (QQLPreProcessingPatterns.isLastThis(se)) {
                selectLast = true;
                someFormOfSelectStar = true;
            }
        }

        CompiledQuery q;

        if (src == null)
            q = new SingleMessageSource();
        else {
            CompiledExpression csrc = compile(src, null);

            if (csrc != null && !(csrc instanceof CompiledQuery))
                throw new IllegalMessageSourceException(src);

            q = (CompiledQuery) csrc;
        }

        if (someFormOfSelectStar && e.typeId != null) {
            RecordClassDescriptor[] descriptors = q.getConcreteOutputTypes();
            if (descriptors.length > 1) {
                throw new CompilationException("Can't use TYPE with polymorphic output", e.typeId);
            }

            someFormOfSelectStar = false;
            long location = selectors[0].location;
            selectors = QQLCompilerUtils.collectFields(descriptors[0]).stream()
                .map(f -> new Identifier(location, f.getName()))
                .toArray(Expression[]::new);
        }

        if (!selectCurrent || fe != null || arrayJoinExpression != null || e.getOverExpression() != null || e.typeId != null || e.getLimit() != null || e.groupBy != null) {
            Environment saveEnv = env;

            CompiledExpression cond = null;
            TupleConstructor compiledSelector = null;
            TimestampLimits tslimits = null;
            SymbolLimits symbolLimits = null;
            GroupBySpec groupBy;

            try {
                setUpQueryEnv(q);

                // compile
                List<Expression> allExpressions = new ArrayList<>();
                if (with != null) {
                    allExpressions.addAll(Arrays.asList(with));
                }
                allExpressions.add(fe);
                allExpressions.addAll(Arrays.asList(selectors));
                if (e.groupBy != null && !isEntityGroupBy(e.groupBy)) {
                    allExpressions.addAll(Arrays.asList(e.groupBy));
                }
                setUpAliasEnv(allExpressions);

                // compile array join
                Map<CompiledExpression<DataType>, Expression> compiledArrayJoins = null;
                if (arrayJoinExpression != null) {
                    compiledArrayJoins = compileArrayJoinExpression(arrayJoinExpression);
                    compiledArrayJoins.forEach(this::validateArrayJoin);
                }

                // compile filter
                if (fe != null) {
                    cond = compile(fe, StandardTypes.NULLABLE_BOOLEAN);

                    List<CompiledExpression> flatCond =
                        QQLPostProcessingPatterns.flattenConjunction(cond);

                    tslimits =
                        QQLPostProcessingPatterns.adjustTimestampLimits(flatCond, e.getEndTime());

                    symbolLimits = QQLPostProcessingPatterns.symbolLimits(cond);

                    cond = QQLPostProcessingPatterns.reconstructConjunction(flatCond);
                } else {
                    tslimits = QQLPostProcessingPatterns.adjustTimestampLimits(Collections.emptyList(), e.getEndTime());
                }

                // compile selectors
                if (!someFormOfSelectStar) {
                    List<Expression> outSelectors = buildSelectorsList(selectors);
                    List<CompiledExpression> compiledSelectors = outSelectors.stream()
                            .map(s -> compile(s, null))
                            .collect(Collectors.toList());

                    if (outSelectors.size() == 1 && (compiledSelectors.get(0) instanceof TupleConstructor)) {
                        compiledSelector = (TupleConstructor) compiledSelectors.get(0);
                    } else {
                        compiledSelector = createAnonymousTuple(
                                outSelectors.toArray(new Expression[0]),
                                compiledSelectors.toArray(new CompiledExpression[0]),
                                e.isDistinct(),
                                e.typeId
                        );
                    }
                } else if (arrayJoinExpression != null) {
                    compiledSelector = new QQLTupleBuilder(this)
                            .createTupleWithArrayJoins(q, compiledArrayJoins, e.isDistinct());
                }

                groupBy = compileGroupBy(e.groupBy);
            } finally {
                env = saveEnv;
                classMap = new ClassMap(env);
            }

            boolean aggregate = !e.isRunning() &&
                ((compiledSelector != null && compiledSelector.impliesAggregation()) || groupBy != null);

            CompiledFilter.RunningFilter runningFilter =
                    selectFirst ?
                            CompiledFilter.RunningFilter.FIRST_ONLY :
                            e.isDistinct() ?
                                    CompiledFilter.RunningFilter.DISTINCT :
                                    CompiledFilter.RunningFilter.NONE;

            SelectLimit limit = e.getLimit() != null ? compileLimit(e.getLimit()) : null;
            q = new CompiledFilter(
                q,
                QQLCompilerUtils.addQueryStatusType(
                    compiledSelector == null ?
                        q.type :
                        new QueryDataType(false, (ClassDataType) compiledSelector.type),
                    groupBy
                ),
                cond,
                runningFilter,
                aggregate,
                e.isRunning(),
                groupBy,
                compiledSelector,
                tslimits,
                symbolLimits,
                limit,
                e.getOverExpression()
            );
            ((CompiledFilter) q).someFormOfSelectStar = someFormOfSelectStar;
        }

        return (q);
    }

    private void validateArrayJoin(CompiledExpression compiledExpression, Expression expression) {
        if (compiledExpression instanceof CompiledComplexExpression) {
            CompiledComplexExpression complexExpression = (CompiledComplexExpression) compiledExpression;
            for (int i = 0; i < complexExpression.args.length; ++i) {
                if (complexExpression.args[i] instanceof ArrayJoinElement) {
                    throw new CompilationException("Array join can't depend on another array join", expression.location);
                } else {
                    validateArrayJoin(complexExpression.args[i], expression);
                }
            }
        }
    }

    private GroupBySpec compileGroupBy(Expression[] groups) {
        if (groups != null && groups.length > 0) {
            if (isEntityGroupBy(groups)) {
                return new GroupByEntity();
            }

            CompiledExpression<?>[] expressions = new CompiledExpression[groups.length];
            for (int i = 0; i < groups.length; ++i) {
                expressions[i] = compile(groups[i], null);
                if (expressions[i].impliesAggregation()) {
                    throw new CompilationException("Can't use aggregation expressions in group by", groups[i]);
                }
                if (expressions[i].type instanceof ClassDataType) {
                    throw new CompilationException("Invalid aggregation expression type", groups[i]);
                }
                if (expressions[i].type instanceof ArrayDataType) {
                    throw new CompilationException("Invalid aggregation expression type", groups[i]);
                }
                if (expressions[i].type instanceof FloatDataType) {
                    throw new CompilationException("Invalid aggregation expression type", groups[i]);
                }
                if (expressions[i].type instanceof QueryDataType) {
                    throw new CompilationException("Invalid aggregation expression type", groups[i]);
                }
            }
            return new GroupByExpressions(expressions);
        }

        return null;
    }

    private boolean isEntityGroupBy(Expression[] groups) {
        if (groups != null && groups.length == 1) {
            if (groups[0] instanceof Identifier && KEYWORD_SYMBOL.equalsIgnoreCase(((Identifier) groups[0]).id)) {
                return true;
            }
        }

        return false;
    }

    private SelectLimit compileLimit(LimitExpression limitExpression) {
        long limit = 0;
        long offset = 0;

        limit = compileLimitOffset(limitExpression.limit);
        if (limitExpression.offset != null) {
            offset = compileLimitOffset(limitExpression.offset);
        }

        return new SelectLimit(limit, offset);
    }

    private long compileLimitOffset(Expression expression) {
        CompiledExpression<?> compiledExpression = compile(expression, null);
        if (compiledExpression instanceof CompiledConstant) {
            if (compiledExpression.type instanceof IntegerDataType) {
                return  ((CompiledConstant) compiledExpression).getLong();
            } else {
                throw new CompilationException("Expression should have integer type", expression);
            }
        } else {
            throw new CompilationException("Expression should be constant", expression);
        }
    }

    private Map<CompiledExpression<DataType>, Expression> compileArrayJoinExpression(ArrayJoin arrayJoinExpression) {
        Map<CompiledExpression<DataType>, Expression> compiledArrayJoins = new HashMap<>();
        for (int i = 0; i < arrayJoinExpression.args.length; ++i) {
            CompiledExpression<DataType> compiledArrayJoin = compile(arrayJoinExpression.args[i], null);
            if (compiledArrayJoin.type instanceof ArrayDataType) {
                ArrayJoinElement arrayJoinElement = new ArrayJoinElement(
                    compiledArrayJoin,
                    compiledArrayJoin.name != null ? compiledArrayJoin.name : ("$arrayjoin$" + i),
                    arrayJoinExpression.left
                );
                compiledArrayJoins.put(arrayJoinElement, arrayJoinExpression.args[i]);
                ((EnvironmentFrame) env).bindNoDup(
                    NamedObjectType.VARIABLE, arrayJoinElement.name, arrayJoinExpression.args[i].location, arrayJoinElement
                );
            } else {
                throw new IllegalDataTypeException(arrayJoinExpression, compiledArrayJoin.type, StandardTypes.ARR);
            }
        }

        return compiledArrayJoins;
    }

    private List<Expression> buildSelectorsList(Expression[] selectors) {
        List<Expression> outSelectors = new ArrayList<>();
        for (int i = 0; i < selectors.length; ++i) {
            if (selectors[i] instanceof ThisObject) {
                outSelectors.addAll(buildThisSelector((ThisObject) selectors[i]));
            } else {
                outSelectors.add(selectors[i]);
            }
        }

        return outSelectors;
    }

    private List<Expression> buildThisSelector(ThisObject selector) {
        class FieldType {
            private DataField field;
            private Set<TypeIdentifier> types = new LinkedHashSet<>();

            FieldType(DataField field, TypeIdentifier typeIdentifier) {
                this.field = field;
                this.types.add(typeIdentifier);
            }

            Expression makeExpression(ThisObject selector, String selectorName, boolean disambiguate, int i) {
                return disambiguate ?
                    new NamedExpression(
                        selector.location,
                        new FieldAccessorExpression(
                            new AsExpression(
                                selector.location,
                                selector.parent,
                                new CastObjectTypeExpression(
                                    selector.location,
                                    types.stream()
                                        .map(t -> new CastTypeIdExpression(selector.location, t))
                                        .collect(Collectors.toList()),
                                    true
                                )
                            ),
                            new FieldIdentifier(selector.location, field.getName())
                        ),
                        selectorName + "." + field.getName() + (i > 0 ? String.valueOf(i) : "")
                    ) :
                    new FieldAccessorExpression(
                        selector.parent, new FieldIdentifier(selector.location, field.getName())
                    );
            }
        }

        List<Expression> outSelectors = new ArrayList<>();
        CompiledExpression compiled = compile(selector.parent, null);
        if (compiled.type instanceof ClassDataType) {
            ClassDataType type = (ClassDataType) compiled.type;
            RecordClassDescriptor[] descriptors = type.getDescriptors();
            Map<String, List<FieldType>> fieldTypesMap = new LinkedHashMap<>();
            for (int i = 0; i < descriptors.length; ++i) {
                TypeIdentifier typeIdentifier = new TypeIdentifier(descriptors[i].getName());
                for (DataField field : QQLCompilerUtils.collectFields(descriptors[i])) {
                    if (field instanceof NonStaticDataField) {
                        List<FieldType> fieldTypes = fieldTypesMap.computeIfAbsent(field.getName(), k -> new ArrayList<>());
                        boolean addNewType = true;
                        for (FieldType fieldType : fieldTypes) {
                            try {
                                QQLCompilerUtils.checkTypesAreEqual(
                                        selector.parent,
                                        new DataType[]{field.getType(), fieldType.field.getType()}
                                );
                                fieldType.types.add(typeIdentifier);
                                addNewType = false;
                            } catch (Throwable t) {
                            }
                        }

                        if (addNewType) {
                            fieldTypes.add(new FieldType(field, typeIdentifier));
                        }
                    }
                }
            }

            fieldTypesMap.forEach((name, fieldTypes) -> {
                for (int i = 0; i < fieldTypes.size(); ++i) {
                    FieldType fieldType = fieldTypes.get(i);
                    String compiledName = compiled.name != null ? compiled.name : compiled.toString();
                    outSelectors.add(fieldType.makeExpression(selector, compiledName, fieldTypes.size() > 1, i));
                }
            });
        } else {
            throw new IllegalDataTypeException(selector.parent, compiled.type, StandardTypes.CLASS);
        }

        return outSelectors;
    }

    private CompiledExpression compileIdentifier(Identifier id) {
        final String text = id.id;

        if (text.equals(KEYWORD_SYMBOL))
            return (new SymbolSelector());

        if (text.equals(KEYWORD_TIMESTAMP))
            return (new TimestampSelector());

        Object obj = lookUpVariable(env, id);

        if (obj instanceof TickStream)
            return (compileStreamSelector((TickStream) obj));

        if (obj instanceof DataFieldRef)
            return (compileFieldSelector(id, (DataFieldRef) obj));

        if (obj instanceof PredicateFieldRef) {
            return compileFieldAccessorExpression(
                new FieldAccessorExpression(
                    id.location, thisIdentifier(), new FieldIdentifier(id.location, ((PredicateFieldRef) obj).field.getName())
                )
            );
        }

        if (obj instanceof EnumValueRef)
            return (compileEnumValueRef((EnumValueRef) obj));

        if (obj instanceof ParamRef)
            return (new ParamAccess((ParamRef) obj));

        if (obj instanceof ThisRef) {
            return (new ThisSelector(((ThisRef) obj).type));
        }

        if (obj instanceof PredicateIterator) {
            return (PredicateIterator) obj;
        }

        if (obj instanceof ArrayJoinElement) {
            return (ArrayJoinElement) obj;
        }

        if (obj instanceof NamedExpression) {
            return compileNamedExpression((NamedExpression) obj, null);
        }

        throw new IllegalObjectException(id, obj);
    }

    private CompiledExpression compileFieldSelector(Expression e, DataFieldRef dfr) {
        DataField df = dfr.field;

        RecordClassDescriptor[] concreteTypes = env.getTopTypes();
        if (concreteTypes != null && !QQLCompilerUtils.findType(concreteTypes, dfr.parent)) {
            throw new UnknownIdentifierException(dfr.field.getName(), e.location);
        }

        if (df instanceof NonStaticDataField)
            return (new FieldSelector(dfr));

        StaticDataField sdf = (StaticDataField) df;
        DataType dt = sdf.getType();

        return (new CompiledConstant(dt, sdf.getBoxedStaticValue(), sdf.getName()));
    }

    private CompiledExpression<DataType> compileFieldAccessor(
            FieldAccessorExpression e, DataFieldRef[] dfr, CompiledExpression<DataType> parent
    ) {
        if (parent == null && dfr.length == 1) {
            return compileFieldSelector(e, dfr[0]);
        }

        List<DataType> types = Arrays.stream(dfr).map(d -> d.field.getType()).collect(Collectors.toList());
        DataType outputType = types.get(0);
        if (types.size() > 1) {
            outputType = QQLCompilerUtils.combinePolymorphicTypes(types);
        }

        DataType slicedType = null;
        if (parent != null && parent.type instanceof ArrayDataType) {
            slicedType = outputType;
            outputType = QQLCompilerUtils.makeSlicedType(outputType);
        }

        if (e.fetchNulls && !(outputType instanceof ArrayDataType)) {
            throw new CompilationException("Operator .? can be applied only for arrays", e);
        }

        return new FieldAccessor(dfr, outputType, parent, slicedType, e.fetchNulls);
    }

    private CompiledExpression compileEnumValueRef(EnumValueRef evr) {
        return (
                new CompiledConstant(
                        new EnumDataType(false, evr.parent),
                        new Long(evr.field.value),
                        evr.field.symbol
                )
        );
    }

    private StreamSelector compileStreamSelector(TickStream s) {
        return (new StreamSelector(s));
    }

//    private CompiledExpression      compileSelectorExpression (
//        SelectorExpression              e,
//        DataType                        expectedType
//    )
//    {
//        Expression                      arg = e.getInput ();
//        CompiledExpression              carg = compile (arg, null);
//        DataType                        argType = carg.type;
//
//        if (!(argType instanceof ClassDataType))
//            throw new ClassTypeExpectedException (arg, argType);
//
//        ClassDataType                   cargType = (ClassDataType) argType;
//
//        if (!cargType.isFixed ())
//            throw new PolymorphicTypeException (arg, cargType);
//
//        RecordClassDescriptor           rcd = cargType.getFixedDescriptor ();
//
//        DataField                       field = rcd.getField (e.fieldId);
//
//        if (field == null)
//            throw new UnknownIdentifierException (e.fieldId, e.fieldIdLocation);
//
//        DataFieldRef                    dfr = new DataFieldRef (rcd, field);
//
//        return (new FieldSelector (dfr));
//    }
//

    private CompiledExpression compileTypeCheckExpression(TypeCheckExpression e, DataType expectedType) {
        checkBooleanType(expectedType, e);

        CompiledExpression carg = compile(e.getArgument(), null);
        ClassMap.ClassInfo ci = classMap.lookUpClass(e.typeId);

        if (!(carg.type instanceof ClassDataType)) {
            throw new IllegalDataTypeException(e.getArgument(), carg.type, StandardTypes.CLASS);
        }

        return (new TypeCheck(carg, ci.cd));
    }

//    private CompiledExpression      compileCastExpression (
//        CastExpression                  e,
//        DataType                        expectedType
//    )
//    {
//        CompiledExpression              carg = compile (e.getInput (), null);
//        TypeIdResolver                  resolver = new TypeIdResolver (e, carg, e.typeId);
//        ... use TypeIdResolver ...
//
//        ClassDataType                   outputType =
//            new ClassDataType (cargType.isNullable (), outputCD);
//
//        return (new TypeCast (carg, outputType));
//    }

    private CompiledExpression compileFieldAccessExpression(
            FieldAccessExpression e
    ) {

        ClassMap.ClassInfo ci = classMap.lookUpClass(e.typeId);

        if (ci instanceof ClassMap.RecordClassInfo) {
            ClassMap.RecordClassInfo rci = (ClassMap.RecordClassInfo) ci;
            DataFieldRef dfr = rci.lookUpField(e.fieldId);

            return (compileFieldSelector(e, dfr));
        }

        if (ci instanceof ClassMap.EnumClassInfo) {
            ClassMap.EnumClassInfo eci = (ClassMap.EnumClassInfo) ci;
            EnumValueRef evr = eci.lookUpValue(e.fieldId);

            return (compileEnumValueRef(evr));
        }

        throw new RuntimeException(ci.toString());
    }

    private CompiledExpression<DataType> compileCastAsExpression(AsExpression e, DataType expectedType) {
        NamedExpression namedExpression = getNamedExpression(e);
        if (namedExpression != null) {
            return compileNamedExpression(namedExpression, expectedType);
        }

        return compileCastExpression(e);
    }

    private NamedExpression getNamedExpression(AsExpression e) {
        if (e.castType instanceof CastTypeIdExpression) {
            CastTypeIdExpression castTypeId = (CastTypeIdExpression) e.castType;
            try {
                lookUpType(castTypeId);
            } catch (Throwable t) {
                return new NamedExpression(e.location, e.expression, castTypeId.typeId.typeName);
            }
        }

        return null;
    }

    private boolean isNamedExpression(Expression e) {
        if (e instanceof NamedExpression) {
            return true;
        }

        if (e instanceof AsExpression) {
            return getNamedExpression((AsExpression) e) != null;
        }

        return false;
    }

    private CompiledExpression<DataType> compileCastExpression(AsExpression e) {
        CompiledExpression<DataType> parent = compile(e.expression, null);

        DataType castDataType = getCastDataType(e, parent.type);

        if (castDataType == null) {
            throw new CompilationException("Unknown target data type.", e);
        }

        if (parent.type instanceof ArrayDataType && castDataType instanceof ArrayDataType) {
            DataType parentElementType = ((ArrayDataType) parent.type).getElementDataType();
            DataType castElementType = ((ArrayDataType) castDataType).getElementDataType();
            if (parentElementType instanceof ClassDataType && castElementType instanceof ClassDataType) {
                if (isCastRequired((ClassDataType) parentElementType, (ClassDataType) castElementType)) {
                    return new CastArrayClassType(parent, (ArrayDataType) parent.type, castDataType);
                } else {
                    return parent;
                }
            }

            NumericType sourceNumeric = getNumericType(parentElementType);
            NumericType numeric = getNumericType(castElementType);
            if (sourceNumeric != null && numeric != null) {
                return new CastPrimitiveType(parent, parent.type, castDataType, sourceNumeric, numeric, true);
            }
        }

        if (parent.type instanceof ClassDataType && castDataType instanceof ClassDataType) {
            if (isCastRequired((ClassDataType) parent.type, (ClassDataType) castDataType)) {
                return new CastClassType(parent, (ClassDataType) parent.type, castDataType);
            } else {
                return parent;
            }
        }

        NumericType sourceNumeric = getNumericType(parent.type);
        NumericType numeric = getNumericType(castDataType);
        if (sourceNumeric != null && numeric != null) {
            return new CastPrimitiveType(parent, parent.type, castDataType, sourceNumeric, numeric, false);
        }

        throw new CompilationException("Can't cast " + parent.type.getBaseName() + " to " + castDataType.getBaseName(), e);
    }

    private NumericType getNumericType(DataType type) {
        if (type instanceof IntegerDataType) {
            int size = ((IntegerDataType) type).getNativeTypeSize();
            switch (size) {
                case 1:
                    return NumericType.Int8;
                case 2:
                    return NumericType.Int16;
                case 4:
                    return NumericType.Int32;
                case 8:
                    return NumericType.Int64;
            }
        } else if (type instanceof FloatDataType) {
            if (((FloatDataType) type).isDecimal64()) {
                return NumericType.Decimal64;
            } else if (((FloatDataType) type).isFloat()) {
                return NumericType.Float32;
            } else {
                return NumericType.Float64;
            }
        } else if (type instanceof BooleanDataType) {
            return NumericType.Int8;
        } else if (type instanceof CharDataType) {
            return NumericType.Char;
        }

        return null;
    }

    private boolean isCastRequired(ClassDataType sourceType, ClassDataType type) {
        return !Arrays.equals(sourceType.getDescriptors(), type.getDescriptors());
    }

    private DataType getCastDataType(AsExpression e, DataType parentType) {
        if (e.castType instanceof CastTypeIdExpression) {
            return compileCastTypeIdExpression((CastTypeIdExpression) e.castType, parentType.isNullable());
        } else if (e.castType instanceof CastObjectTypeExpression) {
            CastObjectTypeExpression castObjectType = (CastObjectTypeExpression) e.castType;
            return new ClassDataType(
                true,
                collectDescriptors(castObjectType.typeIdList)
            );
        } else if (e.castType instanceof CastArrayTypeExpression) {
            CastArrayTypeExpression castArrayType = (CastArrayTypeExpression) e.castType;
            List<CastTypeIdExpression> castTypeIds = castArrayType.typeIdList;

            if (castTypeIds.size() > 1) {
                return new ArrayDataType(
                    parentType.isNullable(),
                    new ClassDataType(true, collectDescriptors(castTypeIds))
                );
            } else {
                return new ArrayDataType(
                    parentType.isNullable(),
                    compileCastTypeIdExpression(castTypeIds.get(0), parentType.isNullable())
                );
            }
        }

        return null;
    }

    private Object lookUpType(CastTypeIdExpression castTypeId) {
        try {
            Object type = ReflectionUtils.forName(castTypeId.typeId.typeName.trim().toUpperCase());
            if (type == null) {
                throw new RuntimeException();
            }
            return type;
        } catch (Throwable t) {
            return classMap.lookUpClass(castTypeId.typeId);
        }
    }

    private DataType compileCastTypeIdExpression(CastTypeIdExpression castTypeId, boolean nullable) {
        Object type = lookUpType(castTypeId);
        if (type instanceof DataType) {
            return ((DataType) type).nullableInstance(nullable);
        } else if (type instanceof ClassMap.RecordClassInfo) {
            return new ClassDataType(true, ((ClassMap.RecordClassInfo) type).cd);
        } else if (type instanceof ClassMap.EnumClassInfo) {
            return new EnumDataType(true, ((ClassMap.EnumClassInfo) type).cd);
        } else {
            throw new CompilationException("Unknown type: " + castTypeId.typeId, castTypeId);
        }
    }

    private RecordClassDescriptor[] collectDescriptors(List<CastTypeIdExpression> castTypeIds) {
        List<RecordClassDescriptor> descriptors = new ArrayList<>();
        for (int i = 0; i < castTypeIds.size(); ++i) {
            CastTypeIdExpression castTypeId = castTypeIds.get(i);
            Object type = lookUpType(castTypeId);
            if (type instanceof ClassMap.RecordClassInfo) {
                RecordClassDescriptor descriptor = ((ClassMap.RecordClassInfo) type).cd;
                if (descriptors.contains(descriptor)) {
                    throw new CompilationException("Duplicate descriptor", castTypeId);
                } else {
                    descriptors.add(descriptor);
                }
            } else {
                throw new CompilationException("Invalid object type: " + castTypeId.typeId, castTypeId);
            }
        }

        return descriptors.toArray(new RecordClassDescriptor[0]);
    }

    private CompiledExpression<DataType> compileFieldAccessorExpression(FieldAccessorExpression e) {
        CompiledExpression<DataType> parent = null;
        List<String> parentTypes = new ArrayList<>();

        if (e.parent != null) {
            parent = compile(e.parent, null);
        }

        List<ClassMap.ClassInfo<?>> cis = new ArrayList<>();
        if (parentTypes.size() > 0) {
            for (int i = 0; i < parentTypes.size(); ++i) {
                cis.add(classMap.lookUpClass(parentTypes.get(i)));
            }
        } else if (parent != null) {
            DataType type = parent.type;

            if (type instanceof ArrayDataType) {
                type = ((ArrayDataType) type).getElementDataType();
            }

            if (type instanceof ClassDataType) {
                ClassDataType classType = (ClassDataType) type;
                for (RecordClassDescriptor rcd : classType.getDescriptors()) {
                    cis.add(classMap.lookUpClass(rcd.getName()));
                }
            } else {
                throw new IllegalDataTypeException(e, type, StandardTypes.CLASS);
            }
        } else {
            return compileIdentifier(new Identifier(e.location, e.identifier.fieldName));
        }

        List<DataFieldRef> dfrs = new ArrayList<>();
        for (ClassMap.ClassInfo<?> ci : cis) {
            if (ci instanceof ClassMap.RecordClassInfo) {
                DataFieldRef newDfr;
                try {
                    newDfr = ((ClassMap.RecordClassInfo) ci).lookUpField(e.identifier);
                } catch (Throwable t) {
                    continue;
                }

                if (!dfrs.contains(newDfr)) {
                    dfrs.add(newDfr);
                }
            } else {
                throw new RuntimeException("Type is not RecordClassInfo: " + ci);
            }
        }

        if (dfrs.size() == 0) {
            throw new UnknownIdentifierException(e.identifier.fieldName, e.location);
        }

        QQLCompilerUtils.checkTypesCompatibility(e, dfrs);

        return compileFieldAccessor(e, dfrs.toArray(new DataFieldRef[0]), parent);
    }

    private CompiledExpression<DataType> compileArrayPredicateExpression(PredicateExpression e) {
        CompiledExpression<DataType> compiledSelector = compile(e.selectorExpression, null);

        if (compiledSelector.type instanceof ArrayDataType) {
            if (e.predicateExpression instanceof ArraySlicingExpression) {
                return compileArraySliceExpression(e, compiledSelector, (ArraySlicingExpression) e.predicateExpression);
            }

            if (QQLCompilerUtils.containsThis(e.predicateExpression)) {
                return compilePredicateWithIterator(compiledSelector, e.predicateExpression);
            }

            ArrayDataType arrayDataType = (ArrayDataType) compiledSelector.type;
            DataType type = detectTypeOfPredicate(compiledSelector, e.predicateExpression);

            if (type instanceof BooleanDataType) {
                CompiledExpression<DataType> compiledPredicate = compileWithContext(
                        e.predicateExpression, arrayDataType.getElementDataType()
                );

                return new ArrayPredicate(compiledSelector, compiledPredicate);
            } else if (type instanceof IntegerDataType) {
                CompiledExpression<DataType> compiledPredicate = compile(e.predicateExpression, null);
                return new ArrayIndexer(compiledSelector, compiledPredicate);
            } else if (type instanceof ArrayDataType && ((ArrayDataType) type).getElementDataType() instanceof BooleanDataType) {
                CompiledExpression<DataType> compiledPredicate = compile(e.predicateExpression, null);
                return new ArrayBooleanIndexer(compiledSelector, compiledPredicate);
            } else if (type instanceof ArrayDataType && ((ArrayDataType) type).getElementDataType() instanceof IntegerDataType) {
                CompiledExpression<DataType> compiledPredicate = compile(e.predicateExpression, null);
                return new ArrayIntegerIndexer(compiledSelector, compiledPredicate);
            }

            throw new IllegalDataTypeException(e, type, StandardTypes.CLEAN_BOOLEAN, StandardTypes.CLEAN_INTEGER);
        } else {
            return compilePredicateWithIterator(compiledSelector, e.predicateExpression);
        }
    }

    private DataType detectTypeOfPredicate(
            CompiledExpression<DataType> compiledSelector, Expression predicateExpression
    ) {
        DataType type;
        try {
            type = compile(predicateExpression, null).type;
            if (type instanceof IntegerDataType) {
                return type;
            } else if (type instanceof ArrayDataType && ((ArrayDataType) type).getElementDataType() instanceof BooleanDataType) {
                return type;
            } else if (type instanceof ArrayDataType && ((ArrayDataType) type).getElementDataType() instanceof IntegerDataType) {
                return type;
            } else {
                throw new RuntimeException();
            }
        } catch (Throwable t) {
            try {
                ArrayDataType arrayDataType = ((ArrayDataType) compiledSelector.type);
                type = compileWithContext(predicateExpression, arrayDataType.getElementDataType()).type;
                if (type instanceof BooleanDataType) {
                    return type;
                }
            } catch (Throwable tt) {
                throw tt;
            }
        }

        throw new IllegalDataTypeException(predicateExpression, type, StandardTypes.CLEAN_BOOLEAN);
    }

    private CompiledExpression<DataType> compilePredicateWithIterator(
            CompiledExpression<DataType> compiledSelector, Expression predicateExpression
    ) {
        DataType iteratorType;
        if (compiledSelector.type instanceof ArrayDataType) {
            iteratorType = ((ArrayDataType) compiledSelector.type).getElementDataType();
        } else {
            iteratorType = compiledSelector.type;
        }
        CompiledExpression<DataType> compiledPredicate = compileWithContext(predicateExpression, iteratorType);

        if (compiledPredicate.type instanceof BooleanDataType) {
            if (compiledSelector.type instanceof ArrayDataType) {
                return new ArrayPredicate(compiledSelector, compiledPredicate);
            } else {
                return new Predicate(compiledSelector, compiledPredicate);
            }
        } else {
            throw new IllegalDataTypeException(predicateExpression, compiledPredicate.type, StandardTypes.CLEAN_BOOLEAN);
        }
    }

    private CompiledExpression<DataType> compileArraySliceExpression(
            PredicateExpression e, CompiledExpression<DataType> selector, ArraySlicingExpression arraySlicingExpression
    ) {
        CompiledExpression<DataType> compiledFrom = null;
        if (arraySlicingExpression.args[0] != null) {
            compiledFrom = compile(arraySlicingExpression.args[0], null);
            if (!(compiledFrom.type instanceof IntegerDataType)) {
                throw new IllegalDataTypeException(e, compiledFrom.type, StandardTypes.CLEAN_INTEGER);
            }
        }

        CompiledExpression<DataType> compiledTo = null;
        if (arraySlicingExpression.args[1] != null) {
            compiledTo = compile(arraySlicingExpression.args[1], null);
            if (!(compiledTo.type instanceof IntegerDataType)) {
                throw new IllegalDataTypeException(e, compiledTo.type, StandardTypes.CLEAN_INTEGER);
            }
        }

        CompiledExpression<DataType> compiledStep = null;
        if (arraySlicingExpression.args[2] != null) {
            compiledStep = compile(arraySlicingExpression.args[2], null);
            if (!(compiledStep.type instanceof IntegerDataType)) {
                throw new IllegalDataTypeException(e, compiledStep.type, StandardTypes.CLEAN_INTEGER);
            }
        }

        return new ArraySlice(
                selector, compiledFrom, compiledTo, compiledStep
        );
    }

    private PredicateFunction compilePredicateFunction(Expression e, PredicateFunction.FunctionName functionName) {
        if (!isPredicateContext()) {
            throw new CompilationException("Can't use predicate function outside predicate expression.", e);
        }

        return new PredicateFunction(getPredicateContext(), functionName);
    }

    private CompiledExpression<DataType> compileWithContext(Expression e, DataType contextType) {
        Environment savedEnv = env;
        setUpPredicateEnv(contextType);
        try {
            return compile(e, null);
        } catch (CompilationException ex) {
            throw new PredicateCompilationException(ex, e);
        } catch (RuntimeException ex) {
            throw new PredicateCompilationException(ex, e);
        } finally {
            env = savedEnv;
        }
    }

    private boolean isPredicateContext() {
        return getPredicateContext() != null;
    }

    private PredicateIterator getPredicateContext() {
        try {
            Object obj = lookUpVariable(env, thisIdentifier());
            if (obj instanceof PredicateIterator) {
                return (PredicateIterator) obj;
            }
        } catch (Throwable t) {
        }

        return null;
    }

    private Identifier thisIdentifier() {
        return new Identifier(KEYWORD_THIS);
    }

    private CompiledExpression compileCallExpression(
            CallExpression e,
            DataType expectedType
    ) {
        Object func = env.lookUp(NamedObjectType.FUNCTION, e.name, e.location);

        if (func == KEYWORD_LAST) {
            if (isPredicateContext()) {
                return compilePredicateFunction(e, PredicateFunction.FunctionName.LAST);
            } else {
                return (compileLast(e, expectedType));
            }
        }

        if (func == KEYWORD_REVERSE)
            return (compileModalStreamSelector(e, expectedType, SelectionMode.REVERSE));

        if (func == KEYWORD_LIVE)
            return (compileModalStreamSelector(e, expectedType, SelectionMode.LIVE));

        if (func == KEYWORD_HYBRID)
            return (compileModalStreamSelector(e, expectedType, SelectionMode.HYBRID));

        if (func == KEYWORD_POSITION) {
            return compilePredicateFunction(e, PredicateFunction.FunctionName.POSITION);
        }

        if (func == KEYWORD_NOW) {
            return new CompiledConstant(TimebaseTypes.DATE_TIME_CONTAINER.getType(false), System.currentTimeMillis());
        }

        int numArgs = e.args.length;

        OverloadedFunctionSet ofd = (OverloadedFunctionSet) func;

        CompiledExpression<?>[] args = new CompiledExpression[numArgs];
        FunctionDescriptorInfo fd;

        DataType[] signature = ofd.getSignature(numArgs);

        if (signature == null)
            throw new WrongNumArgsException(e, numArgs);

        DataType[] actualArgTypes = new DataType[numArgs];

        for (int ii = 0; ii < numArgs; ii++) {
            CompiledExpression<?> arg = compile(e.args[ii], null);

            args[ii] = arg;
            actualArgTypes[ii] = arg.type;
        }

        fd = ofd.getDescriptor(actualArgTypes);

        if (fd == null)
            throw new WrongArgTypesException(e, actualArgTypes);

        if (fd.returnType() instanceof ClassDataType) {
            ClassDataType cdt = (ClassDataType) fd.returnType();
            for (int i = 0; i < cdt.getDescriptors().length; i++) {
                classMap.register(cdt.getDescriptors()[i]);
            }
        }
        if (fd.returnType() instanceof ArrayDataType &&
                ((ArrayDataType) fd.returnType()).getElementDataType() instanceof ClassDataType) {
            ClassDataType cdt = (ClassDataType) ((ArrayDataType) fd.returnType()).getElementDataType();
            for (int i = 0; i < cdt.getDescriptors().length; i++) {
                classMap.register(cdt.getDescriptors()[i]);
            }
        }

        if (fd instanceof StatelessFunctionDescriptor) {
            return new PluginSimpleFunction((StatelessFunctionDescriptor) fd, args);
        } else if (fd instanceof FunctionInfoDescriptor) {
            return (new PluginFunction((FunctionInfoDescriptor) fd, args));
        }
        throw new UnsupportedOperationException();
    }

    private CompiledExpression<?> compileCallExpression(CallExpressionWithInit e, DataType expectedType) {
        StatefulFunctionsSet func = getContextSet(e.getName(), e.location);

        CompiledExpression<?>[] args = new CompiledExpression[e.getNonInitArgsLength()];
        DataType[] argTypes = new DataType[e.getNonInitArgsLength()];
        CompiledExpression<?>[] initArgs = new CompiledExpression[e.getInitArgsLength()];
        DataType[] initArgTypes = new DataType[e.getInitArgsLength()];

        for (int i = 0; i < e.getNonInitArgsLength(); i++) {
            args[i] = compile(e.getNonInitArgs()[i], null);
            argTypes[i] = args[i].type;
        }

        for (int i = 0; i < e.getInitArgsLength(); i++) {
            initArgs[i] = compile(e.getInitArgs()[i], null);
            initArgTypes[i] = initArgs[i].type;
        }

        StatefulFunctionDescriptor function = func.getDescriptor(argTypes, initArgTypes);

        if (function == null)
            throw new WrongArgTypesException(e, argTypes);

        if (function.returnType() instanceof ClassDataType) {
            classMap.register(((ClassDataType) function.returnType()).getFixedDescriptor());
        }

        return new PluginStatefulFunction(function, initArgs, args);
    }

    private StatefulFunctionsSet getContextSet(String name, long location) {
        return (StatefulFunctionsSet) env.lookUp(NamedObjectType.STATEFUL_FUNCTION, name, location);
    }

    private static boolean isSpecialLast(CallExpressionWithDict e) {
        return e.getName().equals(KEYWORD_LAST) && e.getDict().isEmpty() && e.getNonInitArgs().length == 1;
    }

    private static boolean isSpecialFirst(CallExpressionWithDict e) {
        return e.getName().equals(KEYWORD_FIRST) && e.getDict().isEmpty() && e.getNonInitArgs().length == 1;
    }

    // select total/running
    private CompiledExpression<?> compileCallExpression(CallExpressionWithDict e, DataType expectedType) {
        if (isSpecialLast(e)) {
            return compileLast(e, expectedType);
        } else if (isSpecialFirst(e)) {
            return compileFirst(e, expectedType);
        }

        StatefulFunctionsSet func = getContextSet(e.getName(), e.location);

        CompiledExpression<?>[] args = new CompiledExpression[e.getNonInitArgsLength()];
        DataType[] argTypes = new DataType[e.getNonInitArgsLength()];
        Map<String, CompiledExpression<?>> initArgs = new HashMap<>();
        Map<String, DataType> initArgTypes = new HashMap<>();

        for (int i = 0; i < e.getNonInitArgs().length; i++) {
            args[i] = compile(e.getNonInitArgs()[i], null);
            argTypes[i] = args[i].type;
        }

        e.getDict().forEach((k, v) -> {
            CompiledExpression<?> ce = compile(v, null);
            initArgs.put(k, ce);
            initArgTypes.put(k, ce.type);
        });

        StatefulFunctionDescriptor function = func.getDescriptor(argTypes, initArgTypes);

        if (function == null)
            throw new WrongArgTypesException(e, argTypes);

        if (function.returnType() instanceof ClassDataType) {
            classMap.register(((ClassDataType) function.returnType()).getFixedDescriptor());
        }

        CompiledExpression<?>[] actualInitArgs = new CompiledExpression[function.initArgs().size()];
        for (int i = 0; i < function.initArgs().size(); i++) {
            InitArgument arg = function.initArgs().get(i);
            CompiledExpression<?> ce = initArgs.get(arg.getName());
            if (ce == null) {
                ce = compile(arg.getDefaultValueAsConstant(), arg.getDataType());
            }
            actualInitArgs[i] = ce;
        }

        return new PluginStatefulFunction(function, actualInitArgs, args);
    }

    private static boolean areConstants(CompiledExpression<?>[] args) {
        return Arrays.stream(args).allMatch(arg -> arg instanceof CompiledConstant);
    }

    private CompiledExpression<?> compileLast(
            CallExpression e,
            DataType expectedType
    ) {
        int numArgs = e.args.length;
        if (numArgs != 1)
            throw new WrongNumArgsException(e, numArgs);

        CompiledExpression<?> arg = compile(e.args[0], expectedType);
        arg.impliesAggregation = true;
        return arg;
    }

    private CompiledExpression<?> compileLast(
            CallExpressionWithDict e,
            DataType expectedType
    ) {
        CompiledExpression<?> arg = compile(e.args[0], expectedType);
        arg.impliesAggregation = true;
        return arg;
    }

    private CompiledExpression<?> compileFirst(
            CallExpressionWithDict e,
            DataType expectedType
    ) {
        CompiledExpression<?> arg = compile(e.args[0], expectedType);
        return new PluginStatefulFunction(new FirstFunctionDescriptor(arg.type), new CompiledExpression[0], new CompiledExpression[]{arg});
    }

    private CompiledExpression compileModalStreamSelector(
            CallExpression e,
            DataType expectedType,
            SelectionMode mode
    ) {
        int numArgs = e.args.length;

        if (numArgs != 1)
            throw new WrongNumArgsException(e, numArgs);

        Expression earg = e.getArgument();
        CompiledExpression arg = compile(earg, expectedType);

        if (!(arg instanceof StreamSelector))
            throw new IllegalStreamSelectorException(earg);

        StreamSelector ss = (StreamSelector) arg;

        if (ss.mode != SelectionMode.NORMAL)
            throw new IllegalStreamSelectorException(earg);

        return (new StreamSelector(ss, mode));
    }

    private CompiledExpression compileInExpression(InExpression e, DataType expectedType) {
        checkBooleanType(expectedType, e);

        CompiledExpression carg = compile(e.getArgument(), null);
        int numTests = e.getNumTests();

        if (numTests == 0)  // allowed in QQL
            return (CompiledConstant.B_False);

        ArrayList<CompiledExpression> constants = new ArrayList<CompiledExpression>();
        ArrayList<CompiledExpression> other = new ArrayList<CompiledExpression>();

        for (int ii = 0; ii < numTests; ii++) {
            CompiledExpression compiled = compile(e.getTest(ii), carg.type);

            if (compiled instanceof CompiledConstant)
                constants.add(compiled);
            else
                other.add(compiled);
        }

        for (int ii = 0; ii < constants.size(); ii++)
            carg = convertIfNecessary(carg, constants.get(ii));
        for (int ii = 0; ii < other.size(); ii++)
            carg = convertIfNecessary(carg, other.get(ii));

        for (int ii = 0; ii < constants.size(); ii++)
            constants.set(ii, convertIfNecessary(constants.get(ii), carg));
        for (int ii = 0; ii < other.size(); ii++)
            other.set(ii, convertIfNecessary(other.get(ii), carg));

//        if (left instanceof CompiledConstant && right instanceof CompiledConstant)
//            return (computeEqualsExpression (e, (CompiledConstant) left, (CompiledConstant) right));

        CompiledExpression ret = null;

        for (int ii = 0; ii < other.size(); ii++) {
            CompiledExpression x = compileEqualityTest(e, e.positive, carg, other.get(ii));

            ret = ret == null ? x : e.positive ? processOr(ret, x) : processAnd(ret, x);
        }

        // adding argument into constants
        constants.add(0, carg);

        ConnectiveExpression expr = new ConnectiveExpression(!e.positive,
                StandardTypes.CLEAN_BOOLEAN, constants.toArray(new CompiledExpression[constants.size()]));

        if (ret == null) {
            ret = expr;
        } else if (constants.size() > 0) {
            ret = e.positive ? processOr(ret, expr) : processAnd(ret, expr);
        }

        return ret;
    }

    //
    //  STATIC COMPUTE
    //
    public long computeStaticInt(Expression value) {
        return (computeStatic(value, StandardTypes.CLEAN_INTEGER).getLong());
    }

    public double computeStaticFloat(Expression value) {
        return (computeStatic(value, StandardTypes.CLEAN_FLOAT).getDouble());
    }

    public Long computeStaticIntOrStar(Expression value) {
        if (value == null || QQLPreProcessingPatterns.isThis(value))
            return (null);

        return (computeStaticInt(value));
    }

    public Double computeStaticFloatOrStar(Expression value) {
        if (value == null || QQLPreProcessingPatterns.isThis(value))
            return (null);

        return (computeStaticFloat(value));
    }

    public CompiledConstant computeStatic(Expression value, DataType type) {
        CompiledExpression e = compile(value, type);

        if (e instanceof CompiledConstant)
            return ((CompiledConstant) e);

        throw new NonStaticExpressionException(value);
    }

    @SuppressWarnings("unchecked")
    public void processOptions(
            OptionProcessor[] processors,
            OptionElement[] opts,
            Object target
    ) {
        if (opts == null)
            return;

        for (OptionElement opt : opts) {
            OptionProcessor found = null;
            Identifier id = opt.id;

            for (OptionProcessor p : processors) {
                if (id.id.equalsIgnoreCase(p.key)) {
                    found = p;
                    break;
                }
            }

            if (found == null)
                throw new UnknownIdentifierException(id);

            CompiledConstant value;

            if (opt.value == null)
                value = null;
            else
                value = computeStatic(opt.value, found.valueType);

            found.process(opt, value, target);
        }
    }
}