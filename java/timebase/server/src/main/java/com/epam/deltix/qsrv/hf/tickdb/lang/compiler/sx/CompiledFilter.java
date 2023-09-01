/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.QueryDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.OverCountExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.OverExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.OverTimeExpression;

import java.util.Set;

/**
 *  The result of compiling a select statement.
 */
public class CompiledFilter extends CompiledQuery {

    public enum RunningFilter {
        NONE,
        FIRST_ONLY,
        DISTINCT;
    }

    public final CompiledQuery                  source;

    public final CompiledExpression             condition;
    public final RunningFilter                  runningFilter;
    public final boolean                        aggregate;
    private final boolean                       running;
    public final GroupBySpec                    groupBy;
    public final TimestampLimits                tslimits;
    public final SymbolLimits                   symbolLimits;
    public final SelectLimit limit;
    private final OverExpression over;
    public TupleConstructor                     selector;  // selector can be changed during union merge
    public boolean                              someFormOfSelectStar;

    public CompiledFilter (
            CompiledQuery                   source,
            QueryDataType                   type,
            CompiledExpression              condition,
            RunningFilter                   runningFilter,
            boolean                         aggregate,
            boolean                         running,
            GroupBySpec                     groupBy,
            TupleConstructor                selector,
            TimestampLimits                 tslimits,
            SymbolLimits                    symbolLimits,
            SelectLimit limit,
            OverExpression over
    )
    {
        super(type);

        this.source = source;
        this.condition = condition;
        this.runningFilter = runningFilter;
        this.aggregate = aggregate;
        this.running = running;
        this.groupBy = groupBy;
        this.selector = selector;
        this.tslimits = tslimits;
        this.symbolLimits = symbolLimits;
        this.limit = limit;
        this.over = over;
    }

    @Override
    public boolean                      isForward () {
        return (source.isForward ());
    }

    @Override
    public void getAllTypes(Set<ClassDescriptor> out) {
        source.getAllTypes(out);

        if (selector != null) {
            RecordClassDescriptor[] descriptors = selector.getClassDescriptors();
            for (int i = 0; i < descriptors.length; ++i) {
                out.add(descriptors[i]);
            }
        }
    }

    @Override
    public boolean                      impliesAggregation () {
        return (false);
    }

    @Override
    public void print (StringBuilder out) {
        out.append ("select");

        if (runningFilter != RunningFilter.NONE) {
            out.append (" ");
            out.append (runningFilter.name ());
        }

        if (aggregate)
            out.append (" aggregate");

        if (selector != null) {
            out.append (" ");
            selector.print (out);
        }

        out.append (" from ");
        source.print (out);

        if (condition != null) {
            out.append (" ");
            condition.print (out);
        }

        if (groupBy != null) {
            out.append (" ");
            out.append (groupBy);
        }

        if (limit != null) {
            out.append("limit ");
            out.append(limit.getLimit());
            out.append(" offset ");
            out.append(limit.getOffset());
        }
    }

    public boolean isOverTime() {
        return over instanceof OverTimeExpression;
    }

    public boolean isOverCount() {
        return over instanceof OverCountExpression;
    }

    public OverExpression getOver() {
        return over;
    }

    public boolean isRunning() {
        return running;
    }
}