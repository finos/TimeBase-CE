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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import java.util.List;
import java.util.Objects;

/**
 *
 */
public class CastObjectTypeExpression extends CastTypeExpression {
    public final List<CastTypeIdExpression> typeIdList;
    public final boolean nullable;

    public CastObjectTypeExpression(long location, List<CastTypeIdExpression> typeIdList, boolean nullable) {
        super(location);
        this.typeIdList = typeIdList;
        this.nullable = nullable;
    }

    @Override
    public void print(StringBuilder s) {
        s.append("OBJECT(");
        for (int i = 0; i < typeIdList.size(); ++i) {
            if (i > 0) {
                s.append(", ");
            }

            typeIdList.get(i).print(s);
        }
        s.append(")");
        if (!nullable) {
            s.append(" NOT NULL");
        }
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CastObjectTypeExpression that = (CastObjectTypeExpression) o;
        return nullable == that.nullable &&
            Objects.equals(typeIdList, that.typeIdList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), typeIdList, nullable);
    }

}