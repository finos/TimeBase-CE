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

import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.ParamRef;

/**
 *
 */
public class ParamAccess extends CompiledExpression <DataType> {
    public final ParamRef           ref;
    
    public ParamAccess (ParamRef param) {
        super (param.signature.type);
        this.name = param.signature.name;
        this.ref = param;
    }

    @Override
    public void print (StringBuilder out) {
        out.append ("[");
        out.append (ref);
        out.append ("]");
    }

    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            ref.signature.equals (((ParamAccess) obj).ref.signature) &&
            ref.index == ((ParamAccess) obj).ref.index
        );
    }

    @Override
    public int                      hashCode () {
        return super.hashCode () * 39 + ref.signature.hashCode () + ref.index;
    }
}