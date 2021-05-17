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
package com.epam.deltix.qsrv.hf.pub.md;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 */
public final class QueryDataType extends DataType {
    private final ClassDataType         output;

    public QueryDataType (boolean nullable, ClassDataType output) {
        super (null, nullable);
        this.output = output;
    }

    public ClassDataType                getOutputType () {
        return output;
    }

    @Override
    public String                       getBaseName () {
        return ("SOURCE");
    }

    @Override
    public int                          getCode() {
        throw new UnsupportedOperationException ();
    }

    @Override
    public ConversionType               isConvertible (DataType to) {
        return (ConversionType.NotConvertible);
    }

    @Override
    protected void                      assertValidImpl (Object obj) {
        throw unsupportedType (obj);
    }

    @Override
    protected Object                    toBoxedImpl (CharSequence text) {
        throw new UnsupportedOperationException ();
    }
    
    @Override
    protected String                    toStringImpl (Object obj) {
        throw new UnsupportedOperationException ();
    }
    
    @Override
    public void                         writeTo (DataOutputStream out)
        throws IOException
    {
        throw new UnsupportedOperationException ();
    }
}
