package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.IllegalGroupBy;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler.*;

/**
 *
 */
abstract class QQLPreProcessingPatterns {
    public static boolean       isThis (Expression e) {
        return (e instanceof This);
    }
    
    public static boolean       isLastThis (Expression e) {
        return (isCallThis (e, KEYWORD_LAST));
    }
    
    public static boolean       isFirstThis (Expression e) {
        return (isCallThis (e, KEYWORD_FIRST));
    }
    
    private static boolean      isCallThis (Expression e, String func) {
        if (!(e instanceof CallExpression))
            return (false);
        
        CallExpression      ce = (CallExpression) e;
        
        return (
            ce.args.length == 1 &&
            ce.name.equals (func) &&
            isThis (ce.getArgument ())
        );            
    }
    
    public static GroupBySpec   processGroupBy (FieldIdentifier [] groupByIds) {
        GroupBySpec         groupBy = null;
            
        if (groupByIds != null) {
            if (groupByIds.length == 1 && 
                    groupByIds [0].fieldName.equals (KEYWORD_ENTITY) ||
                groupByIds.length == 2 && 
                    (groupByIds [0].fieldName.equals (KEYWORD_SYMBOL) &&
                        groupByIds [1].fieldName.equals (KEYWORD_TYPE) ||
                     groupByIds [1].fieldName.equals (KEYWORD_SYMBOL) &&
                        groupByIds [0].fieldName.equals (KEYWORD_TYPE))
                )
                groupBy = new GroupByEntity ();
            else
                throw new IllegalGroupBy (groupByIds);
        }
        
        return (groupBy);
    }    
}
