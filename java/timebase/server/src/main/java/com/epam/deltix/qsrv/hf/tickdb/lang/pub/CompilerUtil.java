package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.util.parsers.Element;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCodeGenerator;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.CompiledQuery;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.parser.QQLParser;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;
import java.io.*;

/**
 *
 */
public abstract class CompilerUtil {
    private static final StdEnvironment     STDENV = new StdEnvironment (null);
    
    /**
     *  Creates a {@link TextMap} object, which can be optionally passed into 
     *  the parsing methods for the purpose of mapping out program text for
     *  syntax coloring.
     */
    public static TextMap       createTextMap () {
        return (QQLParser.createTextMap ());
    }
    
    /**
     *  Parse a QQL SELECT expression.
     * 
     *  @param text     Program text.
     */
    public static SelectExpression  parseSelect (String text)
        throws CompilationException
    {
        return (parseSelect (text, null));
    }
    
    /**
     *  Parse a QQL expression.
     * 
     *  @param text     Program text.
     */
    public static Expression    parseExpression (String text)
        throws CompilationException
    {
        return (parseExpression (text, null));
    }
    
    /**
     *  Parse a QQL program.
     * 
     *  @param text     Program text.
     *  @param map      If specified, this text map object will be filled with
     *                  information about the location of various tokens in the 
     *                  program.
     */
    public static Element          parse (String text, TextMap map)
        throws CompilationException
    {
        return (QQLParser.parse (text, map));
    }
    
    /**
     *  Parse a QQL program.
     * 
     *  @param text     Program text.
     */
    public static Element          parse (String text)
        throws CompilationException
    {
        return (parse (text, null));
    }
    
    /**
     *  Parse a QQL program.
     * 
     *  @param text     Program text.
     *  @param map      If specified, this text map object will be filled with
     *                  information about the location of various tokens in the 
     *                  program.
     */
    public static Element           parse (Reader text, TextMap map)
        throws CompilationException
    {
        return (QQLParser.parse (text, map));
    }
    
    /**
     *  Parse a QQL program.
     * 
     *  @param text     Program text.
     */
    public static Element           parse (Reader text)
        throws CompilationException
    {
        return (parse (text, null));
    }
    
    public static SelectExpression  parseSelect (String text, TextMap map)
        throws CompilationException
    {
        Element ret = QQLParser.parse (text, map);

        try {
            return ((SelectExpression) ret);
        } catch (ClassCastException x) {
            throw new SelectRequiredException (ret.location);
        }
    }

    public static Expression        parseExpression (String text, TextMap map)
        throws CompilationException
    {
        Element ret = QQLParser.parse (text, map);

        try {
            return ((Expression) ret);
        } catch (ClassCastException x) {
            throw new ExpressionRequiredException (ret.location);
        }
    }

    public static PreparedQuery         prepareQuery (
        DXTickDB                            db,
        String                              text,
        ParamSignature ...                  params
    )
        throws CompilationException
    {
        return (prepareQuery (db, parse (text), params));
    }

    /**
     *  Create an instance of QQL compiler, used to compile previously parsed
     *  queries.
     * 
     *  @param db           TimeBase connection.
     *  @param params       Bound parameters.
     *  @return             A compiler instance.
     */
    public static QuantQueryCompiler    createCompiler (
        DXTickDB                            db,
        ParamSignature ...                  params
    )
    {
        Environment         env = new TimeBaseEnvironment (db, STDENV);

        if (params != null && params.length != 0) {
            InputParameterEnvironment penv = new InputParameterEnvironment (env);

            for (int ii = 0; ii < params.length; ii++)
                penv.addParameter (params [ii], ii);

            env = penv;
        }

        return (new QQLCompiler (db, env));
    }
    
    public static PreparedQuery         prepareQuery (
        DXTickDB                            db,
        Element                             qql, 
        ParamSignature ...                  params
    )
        throws CompilationException
    {
        QuantQueryCompiler      compiler = createCompiler (db, params);
        
        if (qql instanceof SelectExpression) {
            CompiledQuery           cq = 
                (CompiledQuery) compiler.compile ((SelectExpression) qql, StandardTypes.CLEAN_QUERY);

            return (QCodeGenerator.createQuery (cq));
        }
        if (qql instanceof Statement) {
            return (compiler.compileStatement ((Statement) qql));
        }
        else
            throw new UnsupportedOperationException (qql.toString ());
    }    
    
    public static DataType              parseDataType (String text) {
        DataTypeSpec    spec = (DataTypeSpec) parse ('\01' + text);
        
        DDLCompiler     ddlc = new DDLCompiler (null, STDENV);
        
        return (ddlc.compileTopDataTypeSpec (spec));
    }
}
