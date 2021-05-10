package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.values.ValueBean;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.FunctionDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.QRT;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.*;

import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.Varchar;
import com.epam.deltix.util.jcg.*;

import java.lang.reflect.Array;
import java.util.*;

/**
 *  Generates the accept() method.
 */
class EvalGenerator {
    final JExpr                             params;
    final JExpr                             inMsg;
    final QClassRegistry                    classRegistry;
    final QVariableContainer                localVarContainer;
    final QVariableContainer                stateVarContainer;
    final JCompoundStatement                addTo;
    
    private final Map <CompiledExpression, QValue>  x2v =
        new HashMap <CompiledExpression, QValue> ();

    public EvalGenerator (
        JExpr                   params,
        JExpr                   inMsg,
        QClassRegistry          classRegistry,
        QVariableContainer      localVarContainer,
        QVariableContainer      stateVarContainer,
        JCompoundStatement      addTo
    )
    {
        this.addTo = addTo;
        this.params = params;
        this.inMsg = inMsg;
        this.classRegistry = classRegistry;
        this.localVarContainer = localVarContainer;
        this.stateVarContainer = stateVarContainer;
    }

    public void                 bind (CompiledExpression e, QValue v) {
        x2v.put (e, v);
    }

    private void                add (JStatement s) {
        addTo.add (s);
    }

    private void                move (
        QValue                      from,
        QValue                      to
    )
    {
        QCodeGenerator.move (from, to, addTo);
    }

    public void  genEval (JExpr jExpr)
    {
        addTo.add(jExpr);
    }

    public QValue               genEval (CompiledExpression e) {
        QValue      value = getFromCache (e);

        if (value == null) {
            QType   type = QType.forExpr (e);
            
            value = 
                type.declareValue (
                    "Result of " + e, 
                    type.instanceAllocatesMemory () ?
                        stateVarContainer : 
                        localVarContainer, 
                    classRegistry, 
                    false
                );
            
            genEvalNoCache (e, value);

            x2v.put (e, value);
        }

        return (value);
    }

    public void                 genEval (
        CompiledExpression          e,
        QValue                      outValue
    )
    {
        QValue      value = getFromCache (e);

        if (value != null) {
            if (outValue != value)
                move (value, outValue);
        }
        else {
            genEvalNoCache (e, outValue);
            x2v.put (e, outValue);
        }
    }

    private QValue              getFromCache (CompiledExpression e) {
        QValue      value = x2v.get (e);

        if (value == null &&
            e instanceof CompiledConstant &&
            canInline ((CompiledConstant) e))
        {
            value = genInlineCompiledConstant ((CompiledConstant) e);
            x2v.put (e, value);
        }

        return (value);
    }  

    private void                genEvalNoCache (
        CompiledExpression          e,
        QValue                      outValue
    )
    {
        if (e instanceof CompiledConstant)
            genCompiledConstantEval((CompiledConstant) e, outValue);
        else if (e instanceof SimpleFunction)
            genSimpleFunctionEval ((SimpleFunction) e, outValue);
        else if (e instanceof PluginFunction)
            genPluginFunctionEval ((PluginFunction) e, outValue);
        else if (e instanceof TupleConstructor)
            genTupleConstructorEval ((TupleConstructor) e, outValue);
        else if (e instanceof ParamAccess)
            genParamAccessEval ((ParamAccess) e, outValue);
        else if (e instanceof SymbolSelector)
            genSymbolSelectorEval ((SymbolSelector) e, outValue);
        else  if (e instanceof TimestampSelector)
            genTimestampSelectorEval ((TimestampSelector) e, outValue);
        else if (e instanceof CompiledFilter)
            genCompiledFilterEval ((CompiledFilter) e, outValue);
        else if (e instanceof ConnectiveExpression)
            genContainsExpression((ConnectiveExpression) e, outValue);
        else
            throw new UnsupportedOperationException (e.getClass ().getName ());
    }

    private void genContainsExpression (ConnectiveExpression e, QValue outValue) {

        CompiledExpression []       args = e.args;
        int                         n = args.length;

        // java class of constants
        Class<?> clazz = QType.forDataType(args[0].type).getJavaClass();
        if (clazz == Varchar.class)
            clazz = Object.class;

        Class<?> arrayClass = Array.newInstance(clazz, 0).getClass();  // Class.forName(clazz.getName() + "[]")

        // array definition of constant values
        JArrayInitializer init = CTXT.arrayInitializer(clazz);
        for (int i = 1; i < n; i++) { // args starts from 1
            QValue value = genEval(args[i]);
            init.add(value.read());
        }

        Class<?> collection = null;
        try {
            collection = Class.forName("com.epam.deltix.util.collections.generated." + primitiveWrapper(clazz).getSimpleName() + (n > 10 ? "HashSet" : "ArrayList"));
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }

        JVariable                   collectionVar =
                stateVarContainer.addVar(
                        "Constants of " + e,
                        true,
                        collection,
                        CTXT.newExpr(collection, CTXT.newExpr(arrayClass, null, init))
                );

        JExpr cInstance = stateVarContainer.access (collectionVar);

        JExpr expr = cInstance.call("contains", genEval(e.getArgument()).read());
        expr = CTXT.staticCall (QRT.class, e.isConjunction() ? "bneg" : "bpos", expr);

        addTo.add(outValue.write(expr));
    }

    private void                genCompiledConstantEval (
        CompiledConstant            e,        
        QValue                      outValue
    )
    {
        throw new UnsupportedOperationException ("Non-inlineable constant");
    }

    private void                genParamAccessEval (
        ParamAccess                 e,        
        QValue                      outValue
    )
    {
        Class <? extends ValueBean> beanClass = QCGHelpers.getValueBeanClass (e.type);
        
        addTo.add (
            outValue.write (
                params.index (e.ref.index).cast (beanClass).call ("getRaw")
            )
        );
    }

    private boolean             canInline (CompiledConstant e) {
        return (true);  // Not correct for all cases... TODO: fix
    }

    private QValue              genInlineCompiledConstant (
        CompiledConstant            e
    )
    {
        return (QType.forExpr (e).makeConstant (e.value));
    }

    private void                genTupleConstructorEval (
        TupleConstructor            e,        
        QValue                      outValue
    )
    {
        throw new UnsupportedOperationException ();
    }
    
    private void                genPluginFunctionEval (
        PluginFunction              e,
        QValue                      outValue
    )
    {
        FunctionDescriptor          fd = e.fd;
        DataType []                 sig = fd.signature;
        Class <?>                   rtc = fd.cls;
        JVariable                   instanceVar = 
            stateVarContainer.addVar (
                "State of " + e,
                true, 
                rtc, 
                CTXT.newExpr (rtc)
            );
        
        JExpr                       instance =
            stateVarContainer.access (instanceVar);

        CompiledExpression []       args = e.args;
        int                         n = args.length;
        
        for (int ii = 0; ii < n; ii++) {
            CompiledExpression      arg = args [ii];
            QType                   argqtype = QType.forDataType (sig [ii]);
            QValue                  argqval = 
                new QPluginArgValue (argqtype, instance, ii);            

            genEval (arg, argqval);
        }
                
        try {
            rtc.getMethod ("update");            
            addTo.add (instance.call ("update"));
        } catch (NoSuchMethodException x) {
            // not required.
        }
        
        QType                   resultqtype = QType.forExpr (e);
        QValue                  resultqval = 
            new QPluginResultValue (resultqtype, instance);
        
        move (resultqval, outValue);
    }

    private void                genSimpleFunctionEval (
        SimpleFunction              e,
        QValue                      outValue
    )
    {
        CompiledExpression []       args = e.args;
        int                         n = args.length;
        QValue []                   argValues = new QValue [n];

        for (int ii = 0; ii < n; ii++)
            argValues [ii] = genEval (args [ii]);

        switch (e.code) {
            case INTEGER_TO_FLOAT:
                QCodeGenerator.move (argValues [0], outValue, addTo);
                break;            

            case FLOAT_LT:
            case INTEGER_LT:
            case TIMESTAMP_LT:
            case CHAR_LT:
            case TIMEOFDAY_LT:
                QType.genBinOp (argValues [0], "<", argValues [1], outValue, addTo);
                break;
            
            case FLOAT_LE:
            case INTEGER_LE:
            case TIMESTAMP_LE:
            case CHAR_LE:
            case TIMEOFDAY_LE:
                QType.genBinOp (argValues [0], "<=", argValues [1], outValue, addTo);
                break;

            case FLOAT_GT:
            case INTEGER_GT:
            case TIMESTAMP_GT:
            case CHAR_GT:
            case TIMEOFDAY_GT:
                QType.genBinOp (argValues [0], ">", argValues [1], outValue, addTo);
                break;

            case FLOAT_GE:
            case INTEGER_GE:
            case TIMESTAMP_GE:
            case CHAR_GE:
            case TIMEOFDAY_GE:
                QType.genBinOp (argValues [0], ">=", argValues [1], outValue, addTo);
                break;

            case FLOAT_EQ:
            case INTEGER_EQ:
            case TIMESTAMP_EQ:
            case CHAR_EQ:
            case TIMEOFDAY_EQ:
                QType.genEqOp (argValues [0], "==", true, argValues [1], outValue, addTo);
                break;

            case BOOLEAN_EQ:
                QType.genEqOp (argValues [0], "QRT.beq", true, argValues [1], outValue, addTo);
                break;

            case FLOAT_NEQ:
            case INTEGER_NEQ:
            case TIMESTAMP_NEQ:
            case CHAR_NEQ:
            case TIMEOFDAY_NEQ:
                QType.genEqOp (argValues [0], "!=", false, argValues [1], outValue, addTo);
                break;

            case BOOLEAN_NEQ:
                QType.genEqOp (argValues [0], "QRT.bneq", false, argValues [1], outValue, addTo);
                break;

            case VARCHAR_LT:
                QType.genBinOp (argValues [0], "QRT.slt", argValues [1], outValue, addTo);
                break;

            case VARCHAR_LE:
                QType.genBinOp (argValues [0], "QRT.sle", argValues [1], outValue, addTo);
                break;

            case VARCHAR_GT:
                QType.genBinOp (argValues [0], "QRT.sgt", argValues [1], outValue, addTo);
                break;

            case VARCHAR_GE:
                QType.genBinOp (argValues [0], "QRT.sge", argValues [1], outValue, addTo);
                break;

            case VARCHAR_EQ:
                QType.genEqOp (argValues [0], "QRT.seq", true, argValues [1], outValue, addTo);
                break;

            case VARCHAR_LIKE:
                QType.genEqOp (argValues [0], "QRT.slike", true, argValues [1], outValue, addTo);
                break;

            case VARCHAR_NLIKE:
                QType.genEqOp (argValues [0], "QRT.snlike", true, argValues [1], outValue, addTo);
                break;

            case VARCHAR_NEQ:
                QType.genEqOp (argValues [0], "QRT.sneq", false, argValues [1], outValue, addTo);
                break;

            case FLOAT_ADD:
            case INTEGER_ADD:
                QType.genBinOp (argValues [0], "+", argValues [1], outValue, addTo);
                break;

            case FLOAT_SUB:
            case INTEGER_SUB:
                QType.genBinOp (argValues [0], "-", argValues [1], outValue, addTo);
                break;

            case FLOAT_MUL:
            case INTEGER_MUL:
                QType.genBinOp (argValues [0], "*", argValues [1], outValue, addTo);
                break;

            case FLOAT_DIV:
            case INTEGER_DIV:
                QType.genBinOp (argValues [0], "/", argValues [1], outValue, addTo);
                break;

            case AND:
                QBooleanType.genBooleanOp (argValues [0], "band", argValues [1], outValue, addTo);
                break;
                
            case OR:
                QBooleanType.genBooleanOp (argValues [0], "bor", argValues [1], outValue, addTo);
                break;

            case NOT:
                QBooleanType.genNotOp (argValues [0], outValue, addTo);
                break;

            case IS_NULL:  
            case IS_NOT_NULL:
                addTo.add (
                    outValue.write (
                        CTXT.staticCall (
                            QRT.class, "bpos",
                            argValues [0].readIsNull (e.code == SimpleFunctionCode.IS_NULL)
                        )
                    )
                );
                break;                
                
            default:
                throw new UnsupportedOperationException (e.code.name ());
        }
    }

    private void                genSymbolSelectorEval (
        SymbolSelector              e,
        QValue                      outValue
    )
    {
        addTo.add (outValue.write (inMsg.call ("getSymbol")));
    }
    
//    private void                genInstrumentTypeSelectorEval (
//        InstrumentTypeSelector      e,
//        QValue                      outValue
//    )
//    {
//        addTo.add (outValue.write (inMsg.call("getInstrumentType").call ("ordinal").cast (byte.class)));
//    }
    
    private void                genTimestampSelectorEval (
        TimestampSelector           e,
        QValue                      outValue
    )
    {
        addTo.add (outValue.write (inMsg.call ("getTimeStampMs")));
    }
    
    private void                genCompiledFilterEval (
        CompiledFilter              e,        
        QValue                      outValue
    )
    {
        //TMP
    }    
}
