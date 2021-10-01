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

public class OverCountExpression extends OverExpression {

    private final int count;

    public OverCountExpression(long location, boolean reset, int count) {
        super(location, reset);
        this.count = count;
    }

    @Override
    protected void print(int outerPriority, StringBuilder s) {
        if (reset){
            s.append("RESET ");
        }
        s.append("OVER COUNT(").append(count).append(")");
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OverCountExpression))
            return false;
        OverCountExpression other = (OverCountExpression) obj;
        return count == other.count && reset == other.reset;
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 31 * 31 + Integer.hashCode(count) * 31 + Boolean.hashCode(reset);
    }
}