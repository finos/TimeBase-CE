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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.qsrv.hf.pub.md.DataType;

/**
 *  Note: neither type nor name are included in the equality relationship.
 *  Name is irrelevant and type is derivative of other content.
 */
public abstract class CompiledExpression <T extends DataType> {
    public final T                              type;
    public String                               name;
    public boolean                              impliesAggregation = false;

    protected CompiledExpression (T type) {
        this.type = type;
    }

    public boolean              impliesAggregation () {
        return (impliesAggregation);
    }
    
    protected abstract void     print (StringBuilder out);

    @Override
    public String               toString () {
        StringBuilder               sb = new StringBuilder ();
        
        print (sb);
        
        if (name != null) {
            sb.append (" as ");
            sb.append (name);
        }
        
        return (sb.toString ());
    }

    @Override
    public boolean  equals (Object obj) {
        return this == obj || obj != null && getClass () == obj.getClass ();
    }

    @Override
    public int      hashCode () {
        return getClass ().hashCode ();
    }
}
