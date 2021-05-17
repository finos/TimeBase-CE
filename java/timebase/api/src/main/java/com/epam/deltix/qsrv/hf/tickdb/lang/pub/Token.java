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

import com.epam.deltix.util.parsers.Location;

/**
 *  Token information used for syntax highlighting.
 */
public class Token {
    /**
     *  Token type.
     */
    public final TokenType          type;
    
    /**
     *  Token location, basically consisting of start and end line and position 
     *  numbers, packed into a 64-bit long integer. Use methods in the 
     * {@link Location} class to decode start and end line and position numbers.
     */
    public final long               location;

    public Token (TokenType type, long location) {
        this.type = type;
        this.location = location;
    }       
    
    @Override
    public String       toString () {
        return (type + ":" + Location.toString (location));
    }
}
