package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.SelectionMode;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;
import com.epam.deltix.util.lang.StringUtils;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler.*;

import java.util.*;

/**
 *
 */
public class QQLExpressionCompiler {
    private Environment                     env;
    private final ClassMap                  classMap;
    
    public QQLExpressionCompiler (Environment env) {
        this.env = env;
        this.classMap = new ClassMap (env);
    }

    public CompiledExpression       compile (Expression e, DataType expectedType) {
        CompiledExpression              ce;

        if (e instanceof ComplexExpression)
            ce = compileComplexExpression ((ComplexExpression) e, expectedType);
        else if (e instanceof FieldAccessExpression)
            ce = compileFieldAccessExpression ((FieldAccessExpression) e);
        else if (e instanceof Identifier)
            ce = compileIdentifier ((Identifier) e);
        else if (e instanceof Constant)
            ce = compileConstant ((Constant) e, expectedType);
        else
            throw new UnsupportedOperationException (e.getClass ().getName ());

        if (expectedType != null && !isCompatibleWithoutConversion (ce.type, expectedType))
            throw new UnexpectedTypeException (e, expectedType, ce.type);

        return (ce);
    }
    
    private static void             checkType (
        DataType                        knownType,
        DataType                        expectedType,
        Expression                      e
    )
        throws UnexpectedTypeException
    {
        if (expectedType != null && !isCompatibleWithoutConversion (knownType, expectedType))
            throw new UnexpectedTypeException (e, expectedType, knownType);
    }

    private static void             checkBooleanType (
        DataType                        expectedType,
        Expression                      e
    )
        throws UnexpectedTypeException
    {
        checkType (StandardTypes.CLEAN_BOOLEAN, expectedType, e);        
    }

    private CompiledExpression      compileComplexExpression (ComplexExpression e, DataType expectedType) {
        if (e instanceof SelectExpression)
            return (compileSelect ((SelectExpression) e));

        if (e instanceof RelationExpression)
            return (compileRelationExpression ((RelationExpression) e, expectedType));
        
        if (e instanceof BetweenExpression)
            return (compileBetweenExpression ((BetweenExpression) e, expectedType));
        
        if (e instanceof EqualsExpression)
            return (compileEqualsExpression ((EqualsExpression) e, expectedType));

        if (e instanceof LikeExpression)
            return (compileLikeExpression ((LikeExpression) e, expectedType));

        if (e instanceof ArithmeticExpression)
            return (compileArithmeticExpression ((ArithmeticExpression) e, expectedType));

        if (e instanceof UnaryMinusExpression)
            return (compileUnaryMinusExpression ((UnaryMinusExpression) e, expectedType));
        
        if (e instanceof AndExpression)
            return (compileAndExpression ((AndExpression) e, expectedType));

        if (e instanceof OrExpression)
            return (compileOrExpression ((OrExpression) e, expectedType));

        if (e instanceof NotExpression)
            return (compileNotExpression ((NotExpression) e, expectedType));

        if (e instanceof NamedExpression)
            return (compileNamedExpression ((NamedExpression) e, expectedType));

        if (e instanceof CallExpression)
            return (compileCallExpression ((CallExpression) e, expectedType));

        if (e instanceof TypeCheckExpression)
            return (compileTypeCheckExpression ((TypeCheckExpression) e, expectedType));

        if (e instanceof InExpression)
            return (compileInExpression ((InExpression) e, expectedType));
        
        if (e instanceof NullCheckExpression)
            return (compileNullCheckExpression ((NullCheckExpression) e, expectedType));

//        if (e instanceof SelectorExpression)
//            return (compileSelectorExpression ((SelectorExpression) e, expectedType));
//
//        if (e instanceof CastExpression)
//            return (compileCastExpression ((CastExpression) e, expectedType));

        throw new UnsupportedOperationException (e.getClass ().getName ());
    }

    private CompiledExpression      compileConstant (Constant e, DataType expectedType) {
        if (e instanceof BooleanConstant) 
            return (new CompiledConstant (StandardTypes.CLEAN_BOOLEAN, ((BooleanConstant) e).value));
        
        if (e instanceof IntegerConstant) {
            long    value = ((IntegerConstant) e).value;
            
            if (expectedType instanceof FloatDataType)
                return (new CompiledConstant (StandardTypes.CLEAN_FLOAT, new Double (value)));
            
            return (new CompiledConstant (StandardTypes.CLEAN_INTEGER, value));
        }
        
        if (e instanceof FloatConstant)
            return (new CompiledConstant (StandardTypes.CLEAN_FLOAT, ((FloatConstant) e).toDouble ()));

        if (e instanceof StringConstant)
            return (new CompiledConstant (StandardTypes.CLEAN_VARCHAR, ((StringConstant) e).value));                        
        
        if (e instanceof DateConstant)
            return (
                new CompiledConstant (
                    StandardTypes.CLEAN_TIMESTAMP, 
                    ((DateConstant) e).nanoseconds / 1000000
                )
            );

        if (e instanceof TimeConstant)
            return (
                new CompiledConstant (
                    StandardTypes.CLEAN_TIMEOFDAY, 
                    (int) (((TimeConstant) e).nanoseconds / 1000000)
                )
            );

        if (e instanceof BinConstant)
            return (
                new CompiledConstant (
                    StandardTypes.CLEAN_BINARY, 
                    ((BinConstant) e).bytes
                )
            );

        if (e instanceof CharConstant)
            return (
                new CompiledConstant (
                    StandardTypes.CLEAN_CHAR, 
                    ((CharConstant) e).ch
                )
            );

        if (e instanceof Null) {
            if (expectedType != null && !expectedType.isNullable ())
                throw new UnacceptableNullException ((Null) e);

            return (new CompiledConstant (expectedType, null));
        }

        throw new UnsupportedOperationException (e.getClass ().getName ());
    }
   
    
    private CompiledExpression    convertIfNecessary (
        CompiledExpression            x,
        CompiledExpression            other
    )
    {
        DataType                            xt = x.type;
        DataType                            ot = other.type;

        if (xt instanceof IntegerDataType && ot instanceof FloatDataType) {
            if (x instanceof CompiledConstant) {
                CompiledConstant            c = (CompiledConstant) x;

                return (new CompiledConstant (StandardTypes.CLEAN_FLOAT, c.getDouble ()));
            }

            return (new SimpleFunction (SimpleFunctionCode.INTEGER_TO_FLOAT, x));
        }

        return (x);
    }

    private DataType                intersectTypes (Expression e, DataType a, DataType b) {
        if (a == null)
            return (b);

        if (b == null)
            return (a);

        checkType (b, a, e);
        
        return (a);
    }

    private CompiledExpression      compileNamedExpression (NamedExpression e, DataType expectedType) {
        String      name = e.name;
        
        switch (name) {
            case KEYWORD_TIMESTAMP:
                expectedType = intersectTypes (e, expectedType, StandardTypes.NULLABLE_TIMESTAMP);
                break;
                
            case KEYWORD_SYMBOL:
                expectedType = intersectTypes (e, expectedType, StandardTypes.CLEAN_VARCHAR);
                break;
                
            case KEYWORD_TYPE:
                throw new UnsupportedOperationException ("type initializer");
                //expectedType = intersectTypes (e, expectedType, ?);
        }

        CompiledExpression    ret = compile (e.getArgument (), expectedType);
        ret.name = name;
        return (ret);
    }
    
    private CompiledExpression      compileBetweenExpression (BetweenExpression e, DataType expectedType) {
        checkBooleanType (expectedType, e);
        
        CompiledExpression    arg = compile (e.args [0], null);
        CompiledExpression    min = compile (e.args [1], null);
        CompiledExpression    max = compile (e.args [2], null);

        return (
            processAnd (
                processRelation (e, OrderRelation.GE, arg, min), 
                processRelation (e, OrderRelation.LE, arg, max)
            )
        );
    }
    
    private CompiledExpression      processRelation (
        Expression                      e,
        OrderRelation                   relation, 
        CompiledExpression              left, 
        CompiledExpression              right
    )
    {
        left = convertIfNecessary (left, right);
        right = convertIfNecessary (right, left);

        if (left instanceof CompiledConstant && right instanceof CompiledConstant)
            return (computeRelationExpression (e, relation, (CompiledConstant) left, (CompiledConstant) right));

        DataType                    leftType = left.type;
        DataType                    rightType = right.type;
        SimpleFunctionCode          f = null;

        if (leftType instanceof IntegerDataType && rightType instanceof IntegerDataType)
            switch (relation) {
                case GT:    f = SimpleFunctionCode.INTEGER_GT;    break;
                case GE:    f = SimpleFunctionCode.INTEGER_GE;    break;
                case LE:    f = SimpleFunctionCode.INTEGER_LE;    break;
                case LT:    f = SimpleFunctionCode.INTEGER_LT;    break;
                default:    throw new RuntimeException ();
            }
        else if (leftType instanceof FloatDataType && rightType instanceof FloatDataType)
            switch (relation) {
                case GT:    f = SimpleFunctionCode.FLOAT_GT;    break;
                case GE:    f = SimpleFunctionCode.FLOAT_GE;    break;
                case LE:    f = SimpleFunctionCode.FLOAT_LE;    break;
                case LT:    f = SimpleFunctionCode.FLOAT_LT;    break;
                default:    throw new RuntimeException ();
            }
        else if (leftType instanceof VarcharDataType && rightType instanceof VarcharDataType)
            switch (relation) {
                case GT:    f = SimpleFunctionCode.VARCHAR_GT;    break;
                case GE:    f = SimpleFunctionCode.VARCHAR_GE;    break;
                case LE:    f = SimpleFunctionCode.VARCHAR_LE;    break;
                case LT:    f = SimpleFunctionCode.VARCHAR_LT;    break;
                default:    throw new RuntimeException ();
            }
        else if (leftType instanceof CharDataType && rightType instanceof CharDataType)
            switch (relation) {
                case GT:    f = SimpleFunctionCode.CHAR_GT;    break;
                case GE:    f = SimpleFunctionCode.CHAR_GE;    break;
                case LE:    f = SimpleFunctionCode.CHAR_LE;    break;
                case LT:    f = SimpleFunctionCode.CHAR_LT;    break;
                default:    throw new RuntimeException ();
            }
        else if (leftType instanceof DateTimeDataType && rightType instanceof DateTimeDataType)
            switch (relation) {
                case GT:    f = SimpleFunctionCode.TIMESTAMP_GT;    break;
                case GE:    f = SimpleFunctionCode.TIMESTAMP_GE;    break;
                case LE:    f = SimpleFunctionCode.TIMESTAMP_LE;    break;
                case LT:    f = SimpleFunctionCode.TIMESTAMP_LT;    break;
                default:    throw new RuntimeException ();
            }
        else if (leftType instanceof TimeOfDayDataType && rightType instanceof TimeOfDayDataType)
            switch (relation) {
                case GT:    f = SimpleFunctionCode.TIMEOFDAY_GT;    break;
                case GE:    f = SimpleFunctionCode.TIMEOFDAY_GE;    break;
                case LE:    f = SimpleFunctionCode.TIMEOFDAY_LE;    break;
                case LT:    f = SimpleFunctionCode.TIMEOFDAY_LT;    break;
                default:    throw new RuntimeException ();
            }

        if (f == null)
            throw new IllegalTypeCombinationException (e, leftType, rightType);

        return (new SimpleFunction (f, left, right));    
    }
    
    private CompiledExpression  compileRelationExpression (RelationExpression e, DataType expectedType) {
        checkBooleanType (expectedType, e);
        
        CompiledExpression    left = compile (e.getLeft (), null);
        CompiledExpression    right = compile (e.getRight (), null);

        return (processRelation (e, e.relation, left, right));
    }

    private CompiledConstant    computeRelationExpression (
        Expression                  e,
        OrderRelation               relation,
        CompiledConstant            left,
        CompiledConstant            right
    )
    {
        DataType                    leftType = left.type;
        DataType                    rightType = right.type;
        boolean                     ret;

        if (leftType instanceof IntegerDataType && rightType instanceof IntegerDataType ||
            leftType instanceof DateTimeDataType && rightType instanceof DateTimeDataType ||
            leftType instanceof TimeOfDayDataType && rightType instanceof TimeOfDayDataType)
        {
            long        a = left.getLong ();
            long        b = right.getLong ();

            switch (relation) {
                case GT:    ret = a > b;  break;
                case GE:    ret = a >= b;  break;
                case LE:    ret = a <= b;  break;
                case LT:    ret = a < b;  break;
                default:    throw new RuntimeException ();
            }
        }
        else if (leftType instanceof FloatDataType && rightType instanceof FloatDataType) {
            double        a = left.getDouble ();
            double        b = right.getDouble ();

            switch (relation) {
                case GT:    ret = a > b;  break;
                case GE:    ret = a >= b;  break;
                case LE:    ret = a <= b;  break;
                case LT:    ret = a < b;  break;
                default:    throw new RuntimeException ();
            }
        }
        else if (leftType instanceof VarcharDataType && rightType instanceof VarcharDataType){
            String        a = (String) left.value;
            String        b = (String) right.value;

            switch (relation) {
                case GT:    ret = a.compareTo (b) > 0;  break;
                case GE:    ret = a.compareTo (b) >= 0;  break;
                case LE:    ret = a.compareTo (b) <= 0;  break;
                case LT:    ret = a.compareTo (b) < 0;  break;
                default:    throw new RuntimeException ();
            }
        }
        else if (leftType instanceof CharDataType && rightType instanceof CharDataType){
            char        a = left.getChar ();
            char        b = right.getChar ();

            switch (relation) {
                case GT:    ret = a > b;  break;
                case GE:    ret = a >= b;  break;
                case LE:    ret = a <= b;  break;
                case LT:    ret = a < b;  break;
                default:    throw new RuntimeException ();
            }
        }
        else
            throw new IllegalTypeCombinationException (e, leftType, rightType);

        return (new CompiledConstant (StandardTypes.CLEAN_BOOLEAN, ret));
    }

    private CompiledExpression      compileEqualsExpression (EqualsExpression e, DataType expectedType) {
        checkBooleanType (expectedType, e);

        CompiledExpression    left = compile (e.getLeft (), null);
        CompiledExpression    right = compile (e.getRight (), null);

        left = convertIfNecessary (left, right);
        right = convertIfNecessary (right, left);

        return (compileEqualityTest (e, e.isEqual, left, right));
    }

    private CompiledExpression      compileEqualityTest (
        Expression                      e,
        boolean                         positive,
        CompiledExpression              left,
        CompiledExpression              right
    )
    {
        if (left instanceof CompiledConstant && right instanceof CompiledConstant)
            return (computeEqualityTest (e, positive, (CompiledConstant) left, (CompiledConstant) right));
        else if (QQLPostProcessingPatterns.isNull (left))
            return (processNullCheckExpression (right, positive));
        else if (QQLPostProcessingPatterns.isNull (right))
            return (processNullCheckExpression (left, positive));
        
        DataType                    leftType = left.type;
        DataType                    rightType = right.type;
        SimpleFunctionCode          f = null;

        if (leftType instanceof IntegerDataType && rightType instanceof IntegerDataType ||
            leftType instanceof EnumDataType && rightType instanceof EnumDataType)
            f = positive ? SimpleFunctionCode.INTEGER_EQ : SimpleFunctionCode.INTEGER_NEQ;
        else if (leftType instanceof FloatDataType && rightType instanceof FloatDataType)
            f = positive ? SimpleFunctionCode.FLOAT_EQ : SimpleFunctionCode.FLOAT_NEQ;
        else if (leftType instanceof VarcharDataType && rightType instanceof VarcharDataType)
            f = positive ? SimpleFunctionCode.VARCHAR_EQ : SimpleFunctionCode.VARCHAR_NEQ;
        else if (leftType instanceof CharDataType && rightType instanceof CharDataType)
            f = positive ? SimpleFunctionCode.CHAR_EQ : SimpleFunctionCode.CHAR_NEQ;
        else if (leftType instanceof DateTimeDataType && rightType instanceof DateTimeDataType)
            f = positive ? SimpleFunctionCode.TIMESTAMP_EQ : SimpleFunctionCode.TIMESTAMP_NEQ;
        else if (leftType instanceof TimeOfDayDataType && rightType instanceof TimeOfDayDataType)
            f = positive ? SimpleFunctionCode.TIMEOFDAY_EQ : SimpleFunctionCode.TIMEOFDAY_NEQ;
        else if (leftType instanceof BooleanDataType && rightType instanceof BooleanDataType)
            f = positive ? SimpleFunctionCode.BOOLEAN_EQ : SimpleFunctionCode.BOOLEAN_NEQ;

        if (f == null)
            throw new IllegalTypeCombinationException (e, leftType, rightType);

        return (new SimpleFunction (f, left, right));
    }


    private CompiledConstant      computeEqualityTest (
        Expression                  e,
        boolean                     positive,
        CompiledConstant            left,
        CompiledConstant            right
    )
    {
        DataType                    leftType = left.type;
        DataType                    rightType = right.type;
        boolean                     ret;

        if (left.isNull ())
            if (right.isNull ())
                ret = true;
            else
                ret = false;
        else if (right.isNull ())
            ret = false;
        else if (leftType instanceof IntegerDataType && rightType instanceof IntegerDataType ||
                 leftType instanceof DateTimeDataType && rightType instanceof DateTimeDataType ||
                 leftType instanceof TimeOfDayDataType && rightType instanceof TimeOfDayDataType)        
            ret = positive == (left.getLong () == right.getLong ());
        else if (leftType instanceof FloatDataType && rightType instanceof FloatDataType) 
            ret = positive == (left.getDouble () == right.getDouble ());
        else if (leftType instanceof VarcharDataType && rightType instanceof VarcharDataType)
            ret = positive == (left.getString ().equals (right.getString ()));
        else if (leftType instanceof CharDataType && rightType instanceof CharDataType)
            ret = positive == (left.getChar () == right.getChar ());
        else if (leftType instanceof BooleanDataType && rightType instanceof BooleanDataType)
            ret = positive == (left.getBoolean () == right.getBoolean ());
        else
            throw new IllegalTypeCombinationException (e, leftType, rightType);

        return (new CompiledConstant (StandardTypes.CLEAN_BOOLEAN, ret));
    }

    private CompiledExpression      compileLikeExpression(LikeExpression e, DataType expectedType) {
        checkBooleanType(expectedType, e);

        CompiledExpression left = compile(e.getLeft(), null);
        CompiledExpression right = compile(e.getRight(), null);

        left = convertIfNecessary(left, right);
        right = convertIfNecessary(right, left);

        DataType leftType = left.type;
        DataType rightType = right.type;

        if (left instanceof CompiledConstant && right instanceof CompiledConstant) {
            return (computeLikeExpression(e, (CompiledConstant) left, (CompiledConstant) right));
        } else if (QQLPostProcessingPatterns.isNull (left))
            return (new CompiledConstant (StandardTypes.CLEAN_BOOLEAN, false));
        else if (QQLPostProcessingPatterns.isNull (right))
            return (new CompiledConstant (StandardTypes.CLEAN_BOOLEAN, false));

        SimpleFunctionCode f = null;
        if (leftType instanceof VarcharDataType && rightType instanceof VarcharDataType)
            f = e.isNegative ? SimpleFunctionCode.VARCHAR_NLIKE : SimpleFunctionCode.VARCHAR_LIKE;

        if (f == null)
            throw new IllegalTypeCombinationException (e, leftType, rightType);

        return (new SimpleFunction (f, left, right));
    }

    private CompiledConstant      computeLikeExpression(LikeExpression e, CompiledConstant left, CompiledConstant right) {
        boolean result;

        DataType leftType = left.type;
        DataType rightType = right.type;
        if (leftType instanceof VarcharDataType && rightType instanceof VarcharDataType) {
            result = StringUtils.wildcardMatch(left.getString(), right.getString(), false);
        } else
            throw new IllegalTypeCombinationException (e, leftType, rightType);

        return new CompiledConstant (StandardTypes.CLEAN_BOOLEAN, result);
    }

    private CompiledExpression      compileArithmeticExpression (ArithmeticExpression e, DataType expectedType) {
        CompiledExpression    left = compile (e.getLeft (), null);
        CompiledExpression    right = compile (e.getRight (), null);

        left = convertIfNecessary (left, right);
        right = convertIfNecessary (right, left);

        if (left instanceof CompiledConstant && right instanceof CompiledConstant)
            return (computeArithmeticExpression (e, (CompiledConstant) left, (CompiledConstant) right));

        DataType                    leftType = left.type;
        DataType                    rightType = right.type;
        SimpleFunctionCode          f = null;

        if (leftType instanceof IntegerDataType && rightType instanceof IntegerDataType)
            switch (e.function) {
                case ADD:    f = SimpleFunctionCode.INTEGER_ADD;    break;
                case SUB:    f = SimpleFunctionCode.INTEGER_SUB;    break;
                case MUL:    f = SimpleFunctionCode.INTEGER_MUL;    break;
                case DIV:    f = SimpleFunctionCode.INTEGER_DIV;    break;
                default:    throw new RuntimeException ();
            }
        else if (leftType instanceof FloatDataType && rightType instanceof FloatDataType)
            switch (e.function) {
                case ADD:    f = SimpleFunctionCode.FLOAT_ADD;    break;
                case SUB:    f = SimpleFunctionCode.FLOAT_SUB;    break;
                case MUL:    f = SimpleFunctionCode.FLOAT_MUL;    break;
                case DIV:    f = SimpleFunctionCode.FLOAT_DIV;    break;
                default:    throw new RuntimeException ();
            }
            
        // imlpement date and time of day add/sub?

        if (f == null)
            throw new IllegalTypeCombinationException (e, leftType, rightType);

        return (new SimpleFunction (f, left, right));
    }

    private CompiledConstant      computeArithmeticExpression (
        ArithmeticExpression        e,
        CompiledConstant            left,
        CompiledConstant            right
    )
    {
        DataType                    leftType = left.type;
        DataType                    rightType = right.type;
        
        if (leftType instanceof IntegerDataType && rightType instanceof IntegerDataType) {
            long        a = left.getLong ();
            long        b = right.getLong ();
            long        ret;
                
            switch (e.function) {
                case ADD:    ret = a + b;    break;
                case SUB:    ret = a - b;    break;
                case MUL:    ret = a * b;    break;
                case DIV:    ret = a / b;    break;
                default:    throw new RuntimeException ();
            }

            return (new CompiledConstant (StandardTypes.CLEAN_INTEGER, ret));
        }
        else if (leftType instanceof FloatDataType && rightType instanceof FloatDataType) {
            double        a = left.getDouble ();
            double        b = right.getDouble ();
            double        ret;

            switch (e.function) {
                case ADD:    ret = a + b;    break;
                case SUB:    ret = a - b;    break;
                case MUL:    ret = a * b;    break;
                case DIV:    ret = a / b;    break;
                default:    throw new RuntimeException ();
            }

            return (new CompiledConstant (StandardTypes.CLEAN_FLOAT, ret));
        }
        else
            throw new IllegalTypeCombinationException (e, leftType, rightType);
    }

    private CompiledExpression      compileUnaryMinusExpression (UnaryMinusExpression e, DataType expectedType) {
        CompiledExpression    arg = compile (e.getArgument (), null);
        
        if (arg instanceof CompiledConstant)
            return (computeUnaryMinusExpression (e, (CompiledConstant) arg));

        DataType                    argType = arg.type;
        SimpleFunctionCode          f = null;

        if (argType instanceof IntegerDataType)
            f = SimpleFunctionCode.INTEGER_NEGATE;
        else if (argType instanceof FloatDataType)
            f = SimpleFunctionCode.FLOAT_NEGATE;
            
        if (f == null)
            throw new UnexpectedTypeException (e, StandardTypes.CLEAN_FLOAT, argType);

        return (new SimpleFunction (f, arg));
    }

    private CompiledConstant      computeUnaryMinusExpression (
        UnaryMinusExpression        e,
        CompiledConstant            arg
    )
    {
        DataType                    argType = arg.type;
        
        if (argType instanceof IntegerDataType) {
            long        a = arg.getLong ();            

            return (new CompiledConstant (StandardTypes.CLEAN_INTEGER, -a));
        }
        else if (argType instanceof FloatDataType) {
            double        a = arg.getDouble ();            

            return (new CompiledConstant (StandardTypes.CLEAN_FLOAT, -a));
        }
        else
            throw new UnexpectedTypeException (e, StandardTypes.CLEAN_FLOAT, argType);
    }

    private CompiledExpression      compileAndExpression (AndExpression e, DataType expectedType) {
        checkBooleanType (expectedType, e);

        CompiledExpression    left = compile (e.getLeft (), StandardTypes.NULLABLE_BOOLEAN);
        CompiledExpression    right = compile (e.getRight (), StandardTypes.NULLABLE_BOOLEAN);

        return (processAnd (left, right));        
    }

    private CompiledExpression      processAnd (CompiledExpression left, CompiledExpression right) {
        if (left instanceof CompiledConstant && right instanceof CompiledConstant) {
            CompiledConstant    cleft = (CompiledConstant) left;
            CompiledConstant    cright = (CompiledConstant) right;

            return (CompiledConstant.trueOrFalse (cleft.getBoolean () && cright.getBoolean ()));
        }

        return (new SimpleFunction (SimpleFunctionCode.AND, left, right));
    }

    private CompiledExpression      compileOrExpression (OrExpression e, DataType expectedType) {
        checkBooleanType (expectedType, e);

        CompiledExpression    left = compile (e.getLeft (), StandardTypes.NULLABLE_BOOLEAN);
        CompiledExpression    right = compile (e.getRight (), StandardTypes.NULLABLE_BOOLEAN);

        return (processOr (left, right));
    }

    private CompiledExpression      processOr (CompiledExpression left, CompiledExpression right) {
        if (left instanceof CompiledConstant && right instanceof CompiledConstant) {
            CompiledConstant    cleft = (CompiledConstant) left;
            CompiledConstant    cright = (CompiledConstant) right;

            return (CompiledConstant.trueOrFalse (cleft.getBoolean () || cright.getBoolean ()));
        }

        return (new SimpleFunction (SimpleFunctionCode.OR, left, right));
    }

    private CompiledExpression      compileNotExpression (NotExpression e, DataType expectedType) {
        checkBooleanType (expectedType, e);

        CompiledExpression    arg = compile (e.getArgument (), StandardTypes.NULLABLE_BOOLEAN);

        if (arg instanceof CompiledConstant) {
            CompiledConstant    carg = (CompiledConstant) arg;

            return (CompiledConstant.trueOrFalse (!carg.getBoolean ()));
        }

        return (new SimpleFunction (SimpleFunctionCode.NOT, arg));
    }

    private CompiledExpression      compileNullCheckExpression (
        NullCheckExpression             e, 
        DataType                        expectedType
    ) 
    {
        checkBooleanType (expectedType, e);
        
        CompiledExpression      arg = compile (e.getArgument (), null);
        
        return (processNullCheckExpression (arg, e.checkIsNull));
    }
    
    private CompiledExpression      processNullCheckExpression (
        CompiledExpression              arg,
        boolean                         positive
    )
    {
        DataType                argType = arg.type;
        
        if (!argType.isNullable ())
            return (CompiledConstant.trueOrFalse (!positive));
        
        if (arg instanceof CompiledConstant) {
            CompiledConstant    carg = (CompiledConstant) arg;

            if (carg.isNull ())
                return (carg);

            return (CompiledConstant.trueOrFalse (carg.isNull () == positive));
        }

        SimpleFunctionCode      code =
            positive ? 
                SimpleFunctionCode.IS_NULL : 
                SimpleFunctionCode.IS_NOT_NULL;
        
        return (new SimpleFunction (code, arg));
    }
    
    //  SIDE EFFECT env=...
    private void                    setUpQueryEnv (CompiledQuery q) {
        Set <ClassDescriptor>           cds = new HashSet <> ();
        
        q.getAllTypes (cds);
        
        for (ClassDescriptor cd : cds)
            classMap.register (cd);
        
        RecordClassDescriptor []        types = q.getConcreteOutputTypes ();
        
        EnvironmentFrame                selectorEnv = new EnvironmentFrame (env);

        selectorEnv.bind (NamedObjectType.VARIABLE, KEYWORD_THIS, new ThisRef (new ClassDataType (false, types)));

        for (ClassDescriptor cd : classMap.getAllDescriptors ())
            setUpEnv (selectorEnv, cd);        

        env = selectorEnv;
    }
    
    //  SIDE EFFECT env=...
    public void                     setUpClassSetEnv (RecordClassDescriptor ... types) {
        for (RecordClassDescriptor cd : types)
            classMap.register (cd);
        
        EnvironmentFrame                selectorEnv = new EnvironmentFrame (env);

        selectorEnv.bind (NamedObjectType.VARIABLE, KEYWORD_THIS, new ThisRef (new ClassDataType (false, types)));

        for (ClassDescriptor cd : classMap.getAllDescriptors ())
            setUpEnv (selectorEnv, cd);        

        env = selectorEnv;
    }
    
    @SuppressWarnings ("ConvertToStringSwitch")
    private TupleConstructor        createAnonymousTuple (
        Expression []                   origExpressions,
        CompiledExpression  []          args,
        boolean                         clearTimeAndIdentity
    )
    {
        int                             n = args.length;
        ArrayList <DataField>           fields = new ArrayList <> (n);
        ArrayList <CompiledExpression>  nsInits = new ArrayList <> (n);
        CompiledExpression              tsInit = null;
        CompiledExpression              symbolInit = null;
        CompiledExpression              typeInit = null;
        HashSet <String>                namesInUse = new HashSet <> ();

        for (int ii = 0; ii < n; ii++) {
            CompiledExpression          e = args [ii];
            Expression                  oe = origExpressions [ii];
            DataType                    type = e.type;
            String                      name = e.name;  
            
            // no switch! - could be null
            if (KEYWORD_TIMESTAMP.equals (name)) {
                    if (tsInit != null) 
                        throw new DuplicateNameException (oe, name);
                
                    tsInit = e;
            }
            else if (KEYWORD_SYMBOL.equals (name)) {
                    if (symbolInit != null) 
                        throw new DuplicateNameException (oe, name);
                
                    symbolInit = e;
            }
            else if (KEYWORD_TYPE.equals (name)) {
                    if (typeInit != null) 
                        throw new DuplicateNameException (oe, name);
                
                    typeInit = e;
            }
            else {
                    if (oe instanceof NamedExpression) {
                        if (!namesInUse.add (name))
                            throw new DuplicateNameException (oe, name);                    
                    }
                    else {  
                        // name was implied; make unambiguous
                        if (name == null || !namesInUse.add (name)) {
                            if (name == null)
                                name = "$";
                            
                            for (int jj = 1; ; jj++) {
                                String  test = name + jj;
                            
                                if (namesInUse.add (test)) {
                                    name = test;
                                    break;
                                }
                            }
                        }
                    }
                    
                    if (e instanceof CompiledConstant) {
                        CompiledConstant        c = (CompiledConstant) e;

                        if (type == null) {
                            assert c.isNull ();
                            type = StandardTypes.NULLABLE_VARCHAR;
                        }

                        fields.add (
                            new StaticDataField (
                                name, 
                                name, 
                                type, 
                                type.toString (c.value)
                            )
                        );
                    }
                    else {
                        fields.add (new NonStaticDataField (name, name, type));
                        nsInits.add (e);
                    }
            }
        }

        if (clearTimeAndIdentity) {
            if (tsInit == null)
                tsInit = new CompiledConstant (StandardTypes.NULLABLE_TIMESTAMP, null);
            
            if (symbolInit == null)
                symbolInit = new CompiledConstant (StandardTypes.CLEAN_VARCHAR, "");
            
//            if (typeInit == null)
//                typeInit = new CompiledConstant (StdEnvironment.INSTR_TYPE_ENUM, InstrumentType.SYSTEM.ordinal ());
        }
        
        RecordClassDescriptor   rcd =
            new RecordClassDescriptor (
                null, null, false, null,
                fields.toArray (new DataField [fields.size ()])
            );

        ClassDataType           type = new ClassDataType (false, rcd);

        return (
            new TupleConstructor (
                type, tsInit, symbolInit, typeInit,
                nsInits.toArray (new CompiledExpression [nsInits.size ()])
            )
        );
    }

    private CompiledQuery           compileSelect (SelectExpression e) {
        Expression                      src = e.getSource ();
        Expression                      fe = e.getFilter ();
        Expression []                   selectors = e.getSelectors ();
        boolean                         selectFirst = false;
        boolean                         selectLast = false;
        boolean                         selectCurrent = false;
        boolean                         someFormOfSelectStar = false;
        
        if (selectors.length == 1) {
            Expression                  se = selectors [0];
            
            if (QQLPreProcessingPatterns.isThis (se)) {
                selectCurrent = true;
                someFormOfSelectStar = true;
            }
            else if (QQLPreProcessingPatterns.isFirstThis (se)) {
                selectFirst = true;
                someFormOfSelectStar = true;
            }
            else if (QQLPreProcessingPatterns.isLastThis (se)) {
                selectLast = true;
                someFormOfSelectStar = true;
            }
        }
        
        CompiledQuery                   q;
        
        if (src == null)
            q = new SingleMessageSource ();
        else {
            CompiledExpression          csrc = compile (src, null);

            if (csrc != null && !(csrc instanceof CompiledQuery))
                throw new IllegalMessageSourceException (src);

            q = (CompiledQuery) csrc;
        }
        
        if (!selectCurrent || fe != null) {
            Environment                 saveEnv = env;
            CompiledExpression          cond = null;
            TupleConstructor            compiledSelector = null;
            TimestampLimits             tslimits = null;
            
            try {
                setUpQueryEnv (q);

                if (fe != null) {
                    cond = compile (fe, StandardTypes.NULLABLE_BOOLEAN);
                    
                    List <CompiledExpression>   flatCond =
                        QQLPostProcessingPatterns.flattenConjunction (cond);
                    
                    tslimits = 
                        QQLPostProcessingPatterns.extractTimestampLimits (flatCond);
                    
                    cond = QQLPostProcessingPatterns.reconstructConjunction (flatCond);
                }
                
                if (!someFormOfSelectStar) {
                    int                     ns = selectors.length;
                    CompiledExpression []   css = new CompiledExpression [ns];

                    for (int ii = 0; ii < ns; ii++) 
                        css [ii] = compile (selectors [ii], null);
                    
                    if (ns == 1 && (css [0] instanceof TupleConstructor))
                        compiledSelector = (TupleConstructor) css [0];
                    else 
                        compiledSelector = createAnonymousTuple (selectors, css, e.isDistinct ());
                }
            } finally {
                env = saveEnv;
            }

            boolean             aggregate =
                !e.isRunning () &&
                compiledSelector != null && 
                compiledSelector.impliesAggregation ();
            
            CompiledFilter.RunningFilter runningFilter =
                selectFirst ?
                    CompiledFilter.RunningFilter.FIRST_ONLY :
                e.isDistinct () ?
                    CompiledFilter.RunningFilter.DISTINCT :
                    CompiledFilter.RunningFilter.NONE;
            
            q = 
                new CompiledFilter (
                    q, 
                    cond, 
                    runningFilter, 
                    aggregate, 
                    QQLPreProcessingPatterns.processGroupBy (e.groupBy), 
                    compiledSelector,
                    tslimits
                );
        }
        
        return (q);
    }

    private CompiledExpression      compileIdentifier (Identifier id) {
        final String    text = id.id;
        
        if (text.equals (KEYWORD_SYMBOL))
            return (new SymbolSelector ());

        if (text.equals (KEYWORD_TIMESTAMP))
            return (new TimestampSelector ());

//        if (text.equals (KEYWORD_TYPE))
//            return (new InstrumentTypeSelector ());

        Object      obj = lookUpVariable (env, id);

        if (obj instanceof TickStream) 
            return (compileStreamSelector ((TickStream) obj));

        if (obj instanceof DataFieldRef)
            return (compileFieldSelector ((DataFieldRef) obj));

        if (obj instanceof EnumValueRef)
            return (compileEnumValueRef ((EnumValueRef) obj));

        if (obj instanceof ParamRef)
            return (new ParamAccess ((ParamRef) obj));
        
        if (obj instanceof ThisRef)
            return (new ThisSelector (((ThisRef) obj).type));

        throw new IllegalObjectException (id, obj);
    }

    private CompiledExpression      compileFieldSelector (DataFieldRef dfr) {
        DataField           df = dfr.field;
        
        if (df instanceof NonStaticDataField) 
            return (new FieldSelector (dfr));
        
        StaticDataField     sdf = (StaticDataField) df;
        DataType            dt = sdf.getType ();
        
        return (new CompiledConstant (dt, sdf.getBoxedStaticValue (), sdf.getName ()));
    }
    
    private CompiledExpression      compileEnumValueRef (EnumValueRef evr) {
        return (
            new CompiledConstant (
                new EnumDataType (false, evr.parent),
                evr.field.value,
                evr.field.symbol
            )
        );
    }
    
    private StreamSelector          compileStreamSelector (TickStream s) {
        return (new StreamSelector (s));
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
    
    private CompiledExpression      compileTypeCheckExpression (TypeCheckExpression e, DataType expectedType) {
        checkBooleanType (expectedType, e);

        CompiledExpression              carg = compile (e.getArgument (), null);
        ClassMap.ClassInfo              ci = classMap.lookUpClass (e.typeId);
        
        return (new TypeCheck (carg, ci.cd));        
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

    private CompiledExpression      compileFieldAccessExpression (
        FieldAccessExpression           e
    )
    {
        ClassMap.ClassInfo      ci = classMap.lookUpClass (e.typeId);
        
        if (ci instanceof ClassMap.RecordClassInfo) {
            ClassMap.RecordClassInfo    rci = (ClassMap.RecordClassInfo) ci;
            DataFieldRef                dfr = rci.lookUpField (e.fieldId);

            return (compileFieldSelector (dfr));
        }
        
        if (ci instanceof ClassMap.EnumClassInfo) {
            ClassMap.EnumClassInfo      eci = (ClassMap.EnumClassInfo) ci;
            EnumValueRef                evr = eci.lookUpValue (e.fieldId);
            
            return (compileEnumValueRef (evr));
        }
        
        throw new RuntimeException (ci.toString ());
    }

    private CompiledExpression      compileCallExpression (
        CallExpression                  e,
        DataType                        expectedType
    )
    {
        Object                  func = 
            env.lookUp (NamedObjectType.FUNCTION, e.name, e.location);
        
        if (func == KEYWORD_LAST) 
            return (compileLast (e, expectedType));
            
        if (func == KEYWORD_REVERSE) 
            return (compileModalStreamSelector (e, expectedType, SelectionMode.REVERSE));
            
        if (func == KEYWORD_LIVE) 
            return (compileModalStreamSelector (e, expectedType, SelectionMode.LIVE));
            
        if (func == KEYWORD_HYBRID) 
            return (compileModalStreamSelector (e, expectedType, SelectionMode.HYBRID));
            
        OverloadedFunctionSet   ofd = (OverloadedFunctionSet) func;
        int                     numArgs = e.args.length;
        CompiledExpression []   args = new CompiledExpression [numArgs];
        FunctionDescriptor      fd;
                
        DataType []         signature = ofd.getSignature (numArgs);

        if (signature == null)
            throw new WrongNumArgsException (e, numArgs);

        DataType []         actualArgTypes = new DataType [numArgs];

        for (int ii = 0; ii < numArgs; ii++) {
            CompiledExpression      arg = 
                compile (e.args [ii], signature [ii]);

            args [ii] = arg;
            actualArgTypes [ii] = arg.type;
        }

        fd = ofd.getDescriptor (actualArgTypes);

        if (fd == null)
            throw new WrongArgTypesException (e, actualArgTypes);
                
        return (new PluginFunction (fd, args));
    }

    private CompiledExpression      compileLast (
        CallExpression                  e,
        DataType                        expectedType
    )
    {
        int                     numArgs = e.args.length;
        
        if (numArgs != 1)
            throw new WrongNumArgsException (e, numArgs);
        
        CompiledExpression      arg = compile (e.args [0], expectedType);
        
        arg.impliesAggregation = true;
        
        return (arg);
    }
    
    private CompiledExpression      compileModalStreamSelector (
        CallExpression                  e,
        DataType                        expectedType,
        SelectionMode             mode
    )
    {
        int                     numArgs = e.args.length;
        
        if (numArgs != 1)
            throw new WrongNumArgsException (e, numArgs);
        
        Expression              earg = e.getArgument ();
        CompiledExpression      arg = compile (earg, expectedType);
        
        if (!(arg instanceof StreamSelector))
            throw new IllegalStreamSelectorException (earg);
        
        StreamSelector          ss = (StreamSelector) arg;
        
        if (ss.mode != SelectionMode.NORMAL)
            throw new IllegalStreamSelectorException (earg);
        
        return (new StreamSelector (ss, mode));
    }
    
    private CompiledExpression      compileInExpression (InExpression e, DataType expectedType) {
        checkBooleanType(expectedType, e);

        CompiledExpression              carg = compile (e.getArgument (), null);
        int                             numTests = e.getNumTests ();

        if (numTests == 0)  // allowed in QQL
            return (CompiledConstant.B_False);

        ArrayList<CompiledExpression>           constants = new ArrayList<CompiledExpression>();
        ArrayList<CompiledExpression>           other = new ArrayList<CompiledExpression>();
        
        for (int ii = 0; ii < numTests; ii++) {
            CompiledExpression compiled = compile(e.getTest(ii), null);

            if (compiled instanceof CompiledConstant)
                constants.add(compiled);
            else
                other.add(compiled);
        }

        for (int ii = 0; ii < constants.size(); ii++)
            carg = convertIfNecessary (carg, constants.get(ii));
        for (int ii = 0; ii < other.size(); ii++)
            carg = convertIfNecessary (carg, other.get(ii));

        for (int ii = 0; ii < constants.size(); ii++)
            constants.set(ii, convertIfNecessary (constants.get(ii), carg));
        for (int ii = 0; ii < other.size(); ii++)
            other.set(ii, convertIfNecessary (other.get(ii), carg));
        
//        if (left instanceof CompiledConstant && right instanceof CompiledConstant)
//            return (computeEqualsExpression (e, (CompiledConstant) left, (CompiledConstant) right));

        CompiledExpression      ret = null;

        for (int ii = 0; ii < other.size(); ii++) {
            CompiledExpression x = compileEqualityTest(e, e.positive, carg, other.get(ii));

            ret = ret == null ? x : e.positive ? processOr (ret, x) : processAnd (ret, x);
        }

        // adding argument into constants
        constants.add(0, carg);

        ConnectiveExpression expr = new ConnectiveExpression(!e.positive,
                StandardTypes.CLEAN_BOOLEAN, constants.toArray(new CompiledExpression[constants.size()]));

        if (ret == null) {
            ret = expr;
        }
        else if (constants.size() > 0) {
            ret = e.positive ? processOr (ret, expr) : processAnd (ret, expr);
        }

        return ret;
    }
    //
    //  STATIC COMPUTE
    //
    public long            computeStaticInt (Expression value) {
        return (computeStatic (value, StandardTypes.CLEAN_INTEGER).getLong ());
    }
    
    public double          computeStaticFloat (Expression value) {
        return (computeStatic (value, StandardTypes.CLEAN_FLOAT).getDouble ());
    }
    
    public Long            computeStaticIntOrStar (Expression value) {
        if (value == null || QQLPreProcessingPatterns.isThis (value))
            return (null);
        
        return (computeStaticInt (value));
    }
    
    public Double          computeStaticFloatOrStar (Expression value) {
        if (value == null || QQLPreProcessingPatterns.isThis (value))
            return (null);
        
        return (computeStaticFloat (value));
    }
    
    public CompiledConstant computeStatic (Expression value, DataType type) {
        CompiledExpression      e = compile (value, type);
        
        if (e instanceof CompiledConstant) 
            return ((CompiledConstant) e);
        
        throw new NonStaticExpressionException (value);
    }        
    
    @SuppressWarnings ("unchecked")
    public void             processOptions (
        OptionProcessor []      processors,
        OptionElement []        opts, 
        Object                  target
    )
    {
        if (opts == null)
            return;
        
        for (OptionElement opt : opts) {
            OptionProcessor     found = null;
            Identifier          id = opt.id;
            
            for (OptionProcessor p : processors) {                
                if (id.id.equalsIgnoreCase (p.key)) {
                    found = p;
                    break;
                }
            }
            
            if (found == null)
                throw new UnknownIdentifierException (id);
            
            CompiledConstant    value;
            
            if (opt.value == null)
                value = null;
            else
                value = computeStatic (opt.value, found.valueType);
            
            found.process (opt, value, target);
        }
    }
}
