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
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Token;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.Parameter;
import com.epam.deltix.util.parsers.CompilationException;

import java.util.List;

public interface QueryExecutor {

    /**
     * Returns schema for the given query.
     *
     * @param qql     Query text.
     * @param options Selection options.
     * @param params  Specified message types to be subscribed. If null, then all types will be subscribed.*
     * @return Schema contains classes definitions.
     * @throws CompilationException when query has errors
     */
    ClassSet<?> describeQuery(String qql, Parameter... params)
        throws CompilationException;

    /**
     * Compiles QQL/DDL Query.
     * <p>Adds parsed tokens into provided {@code outTokens} list.
     * <p>If query contains errors, throws {@link CompilationException}.
     *
     * @param query query to compile
     * @param outTokens list to store parsed tokens into
     */
    void compileQuery(String query, List<Token> outTokens);

    /**
     * <p>Execute Query and creates a message source for reading data from it,
     * according to the specified options. The messages
     * are returned from the cursor strictly ordered by time. Within the same
     * exact time stamp, the order of messages is undefined and may vary from
     * call to call, i.e. it is non-deterministic.</p>
     *
     * <code>select * from bars</code>
     *
     * @param qql    Query text.
     * @param params The parameter values of the query.
     * @return An iterable message source to read messages.
     */
    InstrumentMessageSource executeQuery(
        String qql,
        Parameter... params
    )
        throws CompilationException;

    /**
     * <p>Execute Query and creates a message source for reading data from it,
     * according to the specified options. The messages
     * are returned from the cursor strictly ordered by time. Within the same
     * exact time stamp, the order of messages is undefined and may vary from
     * call to call, i.e. it is non-deterministic.
     * By default start timestamp equals Long.MIN_VALUE.
     * </p>
     *
     * <code>select * from bars</code>
     *
     * @param qql     Query text.
     * @param options Selection options.
     * @param params  The parameter values of the query.
     * @return An iterable message source to read messages.
     * @throws CompilationException when query has errors
     */
    InstrumentMessageSource executeQuery(
        String qql,
        SelectionOptions options,
        Parameter... params
    )
        throws CompilationException;

    /**
     * <p>Execute Query and creates a message source for reading data from it,
     * according to the specified options. The messages
     * are returned from the cursor strictly ordered by time. Within the same
     * exact time stamp, the order of messages is undefined and may vary from
     * call to call, i.e. it is non-deterministic.</p>
     *
     * <code>select * from bars</code>
     *
     * @param qql     Query text.
     * @param options Selection options.
     * @param time    The start timestamp.
     * @param params  The parameter values of the query.
     * @return An iterable message source to read messages.
     * @throws CompilationException when query has errors.
     */
    InstrumentMessageSource executeQuery(
        String qql,
        SelectionOptions options,
        long time,
        Parameter... params
    )
        throws CompilationException;

    /**
     * <p>Execute Query and creates a message source for reading data from it,
     * according to the specified options. The messages
     * are returned from the cursor strictly ordered by time. Within the same
     * exact time stamp, the order of messages is undefined and may vary from
     * call to call, i.e. it is non-deterministic.</p>
     *
     * <code>select * from bars</code>
     *
     * @param qql            Query text.
     * @param options        Selection options.
     * @param startTimestamp The start timestamp.
     * @param endTimestamp   The end timestamp
     * @param params         The parameter values of the query.
     * @return An iterable message source to read messages.
     * @throws CompilationException when query has errors.
     */
    InstrumentMessageSource executeQuery(
        String qql,
        SelectionOptions options,
        long startTimestamp,
        long endTimestamp,
        Parameter... params
    )
        throws CompilationException;

}
