package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import java.util.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.*;

/**
 *
 */
abstract class QQLPostProcessingPatterns {
    public static boolean       isConjunction (CompiledExpression e) {
        return (isSimpleFunction (e, SimpleFunctionCode.AND));
    }
    
    public static boolean       isNull (CompiledExpression e) {
        return (e instanceof CompiledConstant && ((CompiledConstant) e).isNull ());
    }
    
    public static boolean       isSimpleFunction (
        CompiledExpression          e,
        SimpleFunctionCode          code
    )
    {
        return (
            e instanceof SimpleFunction &&
            ((SimpleFunction) e).code == code
        );
    }
    
    private static void                     flattenConjunction (
        CompiledExpression                      e,
        List <CompiledExpression>               out
    )
    {
        if (isConjunction (e)) {
            SimpleFunction       ae = (SimpleFunction) e;
            
            flattenConjunction (ae.args [0], out);
            flattenConjunction (ae.args [1], out);
        }
        else
            out.add (e);
    }
    
    public static List <CompiledExpression> flattenConjunction (
        CompiledExpression                      e
    )
    {
        List <CompiledExpression>   ret = new ArrayList <CompiledExpression> ();
        
        flattenConjunction (e, ret);
        
        return (ret);
    }
    
    public static CompiledExpression    reconstructConjunction (
        List <CompiledExpression>           list
    )
    {
        int                     n = list.size ();
        
        if (n == 0)
            return (null);
        
        CompiledExpression      e = list.get (0);
        
        for (int ii = 1; ii < n; ii++)
            e = new SimpleFunction (SimpleFunctionCode.AND, e, list.get (ii));
        
        return (e);
    }
    
    public static boolean       isStatic (CompiledExpression e) {        
        return (e instanceof CompiledConstant || e instanceof ParamAccess);
    }
    
    public static TimestampLimits extractTimestampLimits (
        List <CompiledExpression>       condition
    )
    {
        TimestampLimits     range = null;
        
        for (int ii = 0; ii < condition.size (); ) {
            CompiledExpression      e = condition.get (ii);
            boolean                 matched = false;
            
            if (e instanceof SimpleFunction) {
                SimpleFunction      sf = (SimpleFunction) e;
                SimpleFunctionCode  code = sf.code;
                
                switch (code) {
                    case TIMESTAMP_LT:
                    case TIMESTAMP_LE:
                    case TIMESTAMP_GE:
                    case TIMESTAMP_GT:
                    case TIMESTAMP_EQ: {
                        CompiledExpression  left = sf.args [0];
                        CompiledExpression  right = sf.args [1];

                        if (left instanceof TimestampSelector && isStatic (right)) {
                            if (range == null)
                                range = new TimestampLimits ();
                            
                            range.update (right, code, false);
                            matched = true;
                        }
                        else if (right instanceof TimestampSelector && isStatic (left)) {
                            if (range == null)
                                range = new TimestampLimits ();
                            
                            range.update (left, code, true);
                            matched = true;
                        }
                        break;
                    }                       
                }                
            }
            
            if (matched)
                condition.remove (ii);
            else
                ii++;
        }
        
        return (range);
    }
}
