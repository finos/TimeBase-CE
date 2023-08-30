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
package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.FwdStringCodec;

/**
 *
 */
class FwdStringFieldDecoder extends StringFieldDecoder {
    FwdStringFieldDecoder (NonStaticFieldLayout f) {
        super (f);
    }

    @Override
    public boolean isNull(DecodingContext ctxt) {
        return ctxt.in.readInt() == FwdStringCodec.NULL;
    }

    @Override
    CharSequence    getCharSequence (DecodingContext ctxt) {
        return FwdStringCodec.read(ctxt.in);        
    }

    @Override
    public void     skip (DecodingContext ctxt) {
        ctxt.in.skipBytes (4);
    }
}