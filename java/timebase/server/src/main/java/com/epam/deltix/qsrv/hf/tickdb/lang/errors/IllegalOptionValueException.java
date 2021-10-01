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
package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *
 */
public class IllegalOptionValueException extends CompilationException {
    private static String   formatMessage (
        OptionElement   option,
        Object          obj,
        Object          min,
        Object          max
    )
    {
        StringBuilder   s = new StringBuilder ();
        
        option.id.print (s);
        s.append (" value ");
        s.append (obj);
        s.append (" is illegal");
        
        if (obj != null) {
            if (min != null && max != null) {
                s.append ("; allowed range: [");
                s.append (min);
                s.append (" .. ");
                s.append (max);
                s.append ("]");
            }
            else if (min != null) {
                s.append ("; minimum: ");
                s.append (min);
            }
            else if (max != null) {
                s.append ("; maximum: ");
                s.append (max);
            }
        }
        
        return (s.toString ());
    }
    
    public IllegalOptionValueException (
        OptionElement   option,
        Object          obj
    )
    {
        this (option, obj, null, null);
    }
    
    public IllegalOptionValueException (
        OptionElement   option,
        Object          obj,
        Object          min,
        Object          max
    )
    {
        super (formatMessage (option, obj, min, max), option);
    }
}