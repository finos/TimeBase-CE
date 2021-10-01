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

import com.epam.deltix.qsrv.hf.tickdb.lang.errors.*;

/**
 *  Levels of conversion allowed when modifying schema.
 */
public enum ConversionConfirmation {
    NO_CONVERSION,
    CONVERT_DATA,
    DROP_ATTRIBUTES,
    DROP_TYPES,
    DROP_DATA;
    
    public static ConversionConfirmation    fromId (Identifier id) {
        if (id == null)
            return (NO_CONVERSION);
        
        String  key = id.id.replaceAll ("[_|-]", "");
        
        for (ConversionConfirmation v : ConversionConfirmation.values ())
            if (key.equalsIgnoreCase (v.name ().replaceAll ("[_|-]", "")))
                return (v);
        
        throw new UnknownIdentifierException (id);
    }        
}