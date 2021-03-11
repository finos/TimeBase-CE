package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.qsrv.hf.pub.md.DataType;
import static com.epam.deltix.qsrv.hf.pub.md.StandardTypes.*;

/**
 *
 */
public enum SimpleFunctionCode {
    INTEGER_TO_FLOAT,

    BOOLEAN_EQ,
    BOOLEAN_NEQ,
    
    INTEGER_LT,
    INTEGER_LE,
    INTEGER_GE,
    INTEGER_GT,
    INTEGER_EQ,
    INTEGER_NEQ,

    FLOAT_LT,
    FLOAT_LE,
    FLOAT_GE,
    FLOAT_GT,
    FLOAT_EQ,
    FLOAT_NEQ,

    VARCHAR_LT,
    VARCHAR_LE,
    VARCHAR_GE,
    VARCHAR_GT,
    VARCHAR_EQ,
    VARCHAR_NEQ,

    VARCHAR_LIKE,
    VARCHAR_NLIKE,
    
    CHAR_LT,
    CHAR_LE,
    CHAR_GE,
    CHAR_GT,
    CHAR_EQ,
    CHAR_NEQ,
    
    TIMESTAMP_LT,
    TIMESTAMP_LE,
    TIMESTAMP_GE,
    TIMESTAMP_GT,
    TIMESTAMP_EQ,
    TIMESTAMP_NEQ,

    TIMEOFDAY_LT,
    TIMEOFDAY_LE,
    TIMEOFDAY_GE,
    TIMEOFDAY_GT,
    TIMEOFDAY_EQ,
    TIMEOFDAY_NEQ,

    INTEGER_MUL,
    INTEGER_DIV,
    INTEGER_ADD,
    INTEGER_SUB,
    INTEGER_NEGATE,

    FLOAT_MUL,
    FLOAT_DIV,
    FLOAT_ADD,
    FLOAT_SUB,
    FLOAT_NEGATE,

    NOT,
    OR,
    AND,
    
    IS_NULL,
    IS_NOT_NULL,
    
    ;

    public DataType     getOuputType (CompiledExpression ... args) {
        switch (this) {
            case NOT:
            case OR:
            case AND:
            case IS_NULL:
            case IS_NOT_NULL:
            case BOOLEAN_EQ:
            case BOOLEAN_NEQ:
            case INTEGER_EQ:
            case INTEGER_NEQ:
            case FLOAT_EQ:
            case FLOAT_NEQ:
            case VARCHAR_EQ:
            case VARCHAR_NEQ:
            case CHAR_EQ:
            case CHAR_NEQ:
            case TIMESTAMP_EQ:
            case TIMESTAMP_NEQ:  
            case TIMEOFDAY_EQ:
            case TIMEOFDAY_NEQ: 
                return (CLEAN_BOOLEAN);
        }
        
        boolean             foundNullable = false;

        for (CompiledExpression e : args)
            if (e.type.isNullable ()) {
                foundNullable = true;
                break;
            }

        return (getOuputType (foundNullable));
    }

    private DataType    getOuputType (boolean areArgsNullable) {
        switch (this) {
            case INTEGER_TO_FLOAT:
                return (areArgsNullable ? NULLABLE_FLOAT : CLEAN_FLOAT);

            case INTEGER_LT:
            case INTEGER_LE:
            case INTEGER_GE:
            case INTEGER_GT:
            case INTEGER_EQ:
            case INTEGER_NEQ:

            case VARCHAR_LIKE:
            case VARCHAR_NLIKE:

            case FLOAT_LT:
            case FLOAT_LE:
            case FLOAT_GE:
            case FLOAT_GT:
            
            case VARCHAR_LT:
            case VARCHAR_LE:
            case VARCHAR_GE:
            case VARCHAR_GT:
            
            case CHAR_LT:
            case CHAR_LE:
            case CHAR_GE:
            case CHAR_GT:
            
            case TIMESTAMP_LT:
            case TIMESTAMP_LE:
            case TIMESTAMP_GE:
            case TIMESTAMP_GT:
            
            case TIMEOFDAY_LT:
            case TIMEOFDAY_LE:
            case TIMEOFDAY_GE:
            case TIMEOFDAY_GT:
            
            case BOOLEAN_EQ:
            case BOOLEAN_NEQ:           
                return (areArgsNullable ? NULLABLE_BOOLEAN : CLEAN_BOOLEAN);

            case INTEGER_ADD:
            case INTEGER_SUB:
            case INTEGER_MUL:
            case INTEGER_DIV:
                return (areArgsNullable ? NULLABLE_INTEGER : CLEAN_INTEGER);

            case FLOAT_ADD:
            case FLOAT_SUB:
            case FLOAT_MUL:
            case FLOAT_DIV:
                return (areArgsNullable ? NULLABLE_FLOAT : CLEAN_FLOAT);
                            
            default:
                throw new UnsupportedOperationException (name ());
        }
    }
}
