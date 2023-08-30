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
package com.epam.deltix.qsrv.hf.tickdb.schema;

/**
 * Describes all possible error in conversion engine
 */
public class ConversionError extends RuntimeException {

    public static int OUT_OF_RANGE_ERROR;
    public static int NULL_VALUE_ERROR;
    public static int INCOMPATIBLE_TYPES_ERROR;

    private int code;

    public ConversionError(int code) {
        this(code, null);
    }

    public ConversionError(int code, Throwable cause) {
        super(getErrorMessage(code), cause);
        
        this.code = code;
    }

    public static String getErrorMessage(int code) {
        return "";
    }

    public int getErrorCode() {
        return code;
    }
}