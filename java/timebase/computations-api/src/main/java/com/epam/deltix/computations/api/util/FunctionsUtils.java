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
package com.epam.deltix.computations.api.util;

import java.util.List;

public class FunctionsUtils {

    public static boolean isNullOrEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    public static byte bpos(boolean b) {
        return (b ? (byte) 1 : 0);
    }

    public static byte bneg(boolean b) {
        return (b ? 0 : (byte) 1);
    }

    public static byte bnot(byte a) {
        return ((byte) (1 - unnull(a)));
    }

    public static byte unnull(byte b) {
        return (bpos(b == 1));
    }

    public static byte band(byte a, byte b) {
        return ((byte) (unnull(a) & unnull(b)));
    }

    public static byte bor(byte a, byte b) {
        return ((byte) (unnull(a) | unnull(b)));
    }

}