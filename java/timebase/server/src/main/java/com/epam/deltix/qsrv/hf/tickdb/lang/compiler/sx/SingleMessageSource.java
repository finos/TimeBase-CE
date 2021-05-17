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
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.msgsrcs.SingleMessageEmitter;
import java.util.Set;

/**
 *
 */
public class SingleMessageSource extends CompiledQuery {
    public static final QueryDataType       TYPE = 
        new QueryDataType (
            false,
            new ClassDataType (false, SingleMessageEmitter.VOID_TYPE)
        );
    
    public SingleMessageSource () {
        super (TYPE);
    }

    @Override
    public boolean              isForward () {
        return (true);
    }
    
    @Override
    protected void              print (StringBuilder out) {
        out.append ("<void source>");
    }        
    
    @Override
    public void                getAllTypes (Set <ClassDescriptor> out) {
        out.add (SingleMessageEmitter.VOID_TYPE);
    }
}
