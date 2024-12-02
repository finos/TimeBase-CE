/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub.executor;

import com.epam.deltix.qsrv.hf.pub.md.ClassSet;
import com.epam.deltix.qsrv.hf.pub.md.StandardTypes;
import com.epam.deltix.qsrv.hf.tickdb.lang.parser.QQLParser;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.Parameter;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.PreparedQuery;
import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.util.parsers.Element;

import java.util.Arrays;
import java.util.List;

class LocalQueryExecutor implements QueryExecutor {

    private final DXTickDB db;

    LocalQueryExecutor(DXTickDB db) {
        this.db = db;
    }

    @Override
    public ClassSet<?> describeQuery(String qql, Parameter... params) {
        return prepareQuery(qql, params).getSchema();
    }

    @Override
    public InstrumentMessageSource executeQuery(String qql, Parameter... params) throws CompilationException {
        return (executeQuery(qql, null, params));
    }

    @Override
    public InstrumentMessageSource executeQuery(String qql, SelectionOptions options, Parameter... params)
        throws CompilationException {

        return (executeQuery(qql, options, TimeConstants.TIMESTAMP_UNKNOWN, params));
    }

    @Override
    public InstrumentMessageSource executeQuery(String qql, SelectionOptions options, long time,
                                                Parameter... params) throws CompilationException {

        return (executeQuery(CompilerUtil.parse(qql), options, time, params));
    }

    @Override
    public InstrumentMessageSource executeQuery(String qql, SelectionOptions options,
                                                long startTimestamp, long endTimestamp,
                                                Parameter... params) throws CompilationException {

        return (executeQuery(CompilerUtil.parse(qql), options, startTimestamp, endTimestamp, params));
    }

    public InstrumentMessageSource executeQuery(Element qql, SelectionOptions options, long time,
                                                Parameter... params) throws CompilationException {

        return (executePreparedQuery(prepareQuery(qql, params),
            options, time == TimeConstants.TIMESTAMP_UNKNOWN, time, params));
    }

    private InstrumentMessageSource executeQuery(Element qql, SelectionOptions options,
                                                 long startTimestamp, long endTimestamp,
                                                 Parameter... params)
        throws CompilationException {

        return (executePreparedQuery(prepareQuery(qql, endTimestamp, params),
            options, startTimestamp == TimeConstants.TIMESTAMP_UNKNOWN, startTimestamp, params));
    }

    private PreparedQuery prepareQuery(String qql, Parameter... params) {
        return prepareQuery(CompilerUtil.parse(qql), params);
    }

    private PreparedQuery prepareQuery(Element query, Parameter... params) {
        return CompilerUtil.prepareQuery(db, query, ParamSignature.signatureOf(params));
    }

    private PreparedQuery prepareQuery(Element query, long endTimestamp, Parameter... params) {
        return CompilerUtil.prepareQuery(db, query, endTimestamp, ParamSignature.signatureOf(params));
    }

    private InstrumentMessageSource executePreparedQuery(PreparedQuery pq, SelectionOptions options,
                                                         boolean fullScan, long time, Parameter[] params)
        throws CompilationException {

        InstrumentMessageSource ims =
            pq.executeQuery(options, Parameter.valuesOf(params));

        // pq controls all possible entities it should subscribe
        ims.subscribeToAllEntities();

        if (fullScan)
            ims.reset(pq.isReverse() ? Long.MAX_VALUE : Long.MIN_VALUE);
        else
            ims.reset(time);

        return (ims);
    }

    @Override
    public synchronized void compileQuery(String query, List<Token> outTokens) {
        TextMap map = QQLParser.createTextMap();
        try {
            Object sx = CompilerUtil.parse(query, map);

            if (sx instanceof Expression) {
                // TODO: Optimize - creating compiler takes a lot of time
                QuantQueryCompiler compiler = CompilerUtil.createCompiler(db);
                compiler.compile((Expression) sx, StandardTypes.CLEAN_QUERY);
            }
        } finally {
            // Add parsed tokens to output even in case of exception
            outTokens.addAll(Arrays.asList(map.getTokens()));
        }
    }

}
