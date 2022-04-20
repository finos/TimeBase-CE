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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.StandardTypes;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCodeGenerator;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.DDLCompiler;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.Environment;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.InputParameterEnvironment;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.StdEnvironment;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.TimeBaseEnvironment;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.CompiledQuery;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.ExpressionRequiredException;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.SelectRequiredException;
import com.epam.deltix.qsrv.hf.tickdb.lang.parser.QQLParser;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.PreparedQuery;
import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.util.parsers.Element;

import java.io.Reader;

/**
 *
 */
public abstract class CompilerUtil {
    public static final StdEnvironment     STDENV = new StdEnvironment (null);
    
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
        
        if (qql instanceof SelectExpression || qql instanceof UnionExpression) {
            CompiledQuery cq = (CompiledQuery) compiler.compile((Expression) qql, StandardTypes.CLEAN_QUERY);
            return QCodeGenerator.createQuery(cq, db);
        }
        if (qql instanceof Statement) {
            return (compiler.compileStatement ((Statement) qql));
        }
        throw new UnsupportedOperationException (qql.toString ());
    }

    public static PreparedQuery prepareQuery(DXTickDB db, Element qql, long endTimestamp, ParamSignature... params)
            throws CompilationException {
        QuantQueryCompiler compiler = createCompiler(db, params);

        if (qql instanceof SelectExpression) {
            SelectExpression selectExpression = (SelectExpression) qql;
            selectExpression.setEndTime(endTimestamp);
            CompiledQuery cq = (CompiledQuery) compiler.compile(selectExpression, StandardTypes.CLEAN_QUERY);

            return (QCodeGenerator.createQuery(cq, db));
        }
        if (qql instanceof Statement) {
            return (compiler.compileStatement((Statement) qql));
        } else
            throw new UnsupportedOperationException(qql.toString());
    }

    public static DataType              parseDataType (String text) {
        DataTypeSpec    spec = (DataTypeSpec) parse ('\01' + text);
        
        DDLCompiler     ddlc = new DDLCompiler (null, STDENV);
        
        return (ddlc.compileTopDataTypeSpec (spec));
    }
}
