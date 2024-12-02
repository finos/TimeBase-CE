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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants.TimeIntervalConstant;
import com.epam.deltix.util.parsers.CompilationException;

import java.util.Objects;

public class OverTimeExpression extends OverExpression {

    private final boolean every;
    private final boolean trigger;
    private final TimeIntervalConstant timeInterval;
    private final TimeIntervalConstant offset;

    public OverTimeExpression(long location, boolean reset, boolean every, boolean trigger,
                              TimeIntervalConstant timeInterval, TimeIntervalConstant offset) {
        super(location, reset);
        this.every = every;
        this.trigger = trigger;
        this.timeInterval = timeInterval;
        this.offset = offset;
        if (offset != null && offset.getTimeStampMs() >= timeInterval.getTimeStampMs()) {
            throw new CompilationException("Offset is larger than interval", offset);
        }
    }

    @Override
    protected void print(int outerPriority, StringBuilder s) {
        if (reset) {
            s.append("RESET ");
        }
        s.append("OVER ");
        if (every) {
            s.append("EVERY ");
        }
        s.append("TIME(").append(timeInterval.getMnemonic());
        if (offset != null) {
            s.append(", ").append(offset.getMnemonic());
        }
        s.append(")");
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OverTimeExpression))
            return false;
        OverTimeExpression other = (OverTimeExpression) obj;
        return timeInterval.equals(other.timeInterval)
                && Objects.equals(offset, other.offset)
                && every == other.every
                && trigger == other.trigger
                && reset == other.reset;
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 31 * 31 * 31 * 31 * 31 +
                Boolean.hashCode(reset) * 31 * 31 * 31 * 31 +
                Boolean.hashCode(trigger) * 31 * 31 * 31 +
                Boolean.hashCode(every)  * 31* 31 +
                timeInterval.hashCode() * 31 +
                Objects.hashCode(timeInterval);
    }

    public boolean isEvery() {
        return every;
    }

    public boolean isTrigger() {
        return trigger;
    }

    public TimeIntervalConstant getTimeInterval() {
        return timeInterval;
    }

    public TimeIntervalConstant getOffset() {
        return offset;
    }
}
