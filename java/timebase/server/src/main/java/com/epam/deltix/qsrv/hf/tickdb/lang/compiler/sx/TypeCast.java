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

import com.epam.deltix.qsrv.hf.pub.md.*;

/**
 *
 */
public class TypeCast extends CompiledComplexExpression {
    public final ClassDataType           targetType;

    public TypeCast (CompiledExpression arg, ClassDataType targetType) {
        super (targetType, arg);
        this.targetType = targetType;
    }

    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return
            super.equals (obj) && targetType.equals (((TypeCast) obj).targetType);
    }

    @Override
    public int                      hashCode () {
        return super.hashCode () + targetType.hashCode ();
    }

    @Override
    protected void                      print (StringBuilder out) {
        out.append ("cast (");
        printArgs (out);
        out.append (" as ");
        out.append (targetType.getFixedDescriptor ().getName ());
        out.append (")");
    }
}
