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
package com.epam.deltix.qsrv.util.text;

import com.epam.deltix.util.io.IOUtil;

public class Mangle {
    private static final String ENCRYPTION_KEY             = "superKey";
    private static final String ENCRYPTED_VALUE_PREFIX     = "EV";

    public static String concat(String value) {
        String encryptedValue = IOUtil.concat(value, ENCRYPTION_KEY);
        return ENCRYPTED_VALUE_PREFIX + encryptedValue;
    }

    public static String split(String encryptedValue) {
        if(encryptedValue == null)
            return null;

        if(!isEncrypted(encryptedValue))
            return encryptedValue;

        encryptedValue = encryptedValue.substring(ENCRYPTED_VALUE_PREFIX.length());
        return IOUtil.split(encryptedValue, ENCRYPTION_KEY);
    }

    public static boolean isEncrypted(String value){
        return value.startsWith(ENCRYPTED_VALUE_PREFIX);
    }
}
