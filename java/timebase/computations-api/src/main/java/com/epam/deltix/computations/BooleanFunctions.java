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
package com.epam.deltix.computations;

import com.epam.deltix.computations.api.annotations.Function;
import com.epam.deltix.computations.api.annotations.FunctionsRepo;
import com.epam.deltix.computations.api.annotations.Result;
import com.epam.deltix.qsrv.hf.pub.md.BooleanDataType;

import javax.annotation.Nonnull;

import static com.epam.deltix.computations.api.util.FunctionsUtils.band;
import static com.epam.deltix.computations.api.util.FunctionsUtils.bnot;
import static com.epam.deltix.computations.api.util.FunctionsUtils.bor;
import static com.epam.deltix.computations.api.util.FunctionsUtils.isNullOrEmpty;

import com.epam.deltix.util.annotations.Bool;
import com.epam.deltix.util.collections.generated.ByteArrayList;

@FunctionsRepo
public class BooleanFunctions {

    @Function("ANY")
    @Bool
    public static byte any(@Bool ByteArrayList list) {
        if (isNullOrEmpty(list))
            return BooleanDataType.FALSE;
        for (Byte v : list) {
            if (v == BooleanDataType.TRUE) {
                return BooleanDataType.TRUE;
            }
        }
        return BooleanDataType.FALSE;
    }

    @Function("ALL")
    @Bool
    public static byte all(@Bool ByteArrayList list) {
        if (isNullOrEmpty(list))
            return BooleanDataType.FALSE;
        for (Byte v : list) {
            if (v == BooleanDataType.FALSE) {
                return BooleanDataType.FALSE;
            }
        }
        return BooleanDataType.TRUE;
    }

    public static void and(@Nonnull @Bool ByteArrayList l1, @Nonnull @Bool ByteArrayList l2,
                           @Nonnull @Result @Bool ByteArrayList result) {
        int size = Math.max(l1.size(), l2.size());
        result.setSize(size);
        for (int i = 0; i < size; i++) {
            result.set(i, band(l1.getByte(i), l2.getByte(i)));
        }
    }

    public static void and(@Nonnull @Bool ByteArrayList l, byte v, @Nonnull @Result @Bool ByteArrayList result) {
        result.setSize(l.size());
        for (int i = 0; i < l.size(); i++) {
            result.set(i, band(l.getByte(i), v));
        }
    }

    public static void and(@Bool byte v, @Nonnull @Bool ByteArrayList l, @Nonnull @Result @Bool ByteArrayList result) {
        result.setSize(l.size());
        for (int i = 0; i < l.size(); i++) {
            result.set(i, band(l.getByte(i), v));
        }
    }

    public static void or(@Nonnull @Bool ByteArrayList l1, @Nonnull @Bool ByteArrayList l2,
                          @Nonnull @Result @Bool ByteArrayList result) {
        int size = Math.max(l1.size(), l2.size());
        result.setSize(size);
        for (int i = 0; i < size; i++) {
            result.set(i, bor(l1.getByte(i), l2.getByte(i)));
        }
    }

    public static void or(@Nonnull @Bool ByteArrayList l, @Bool byte v, @Nonnull @Result @Bool ByteArrayList result) {
        result.setSize(l.size());
        for (int i = 0; i < l.size(); i++) {
            result.set(i, bor(l.getByte(i), v));
        }
    }

    public static void or(@Bool byte v, @Nonnull @Bool ByteArrayList l, @Nonnull @Result @Bool ByteArrayList result) {
        result.setSize(l.size());
        for (int i = 0; i < l.size(); i++) {
            result.set(i, bor(l.getByte(i), v));
        }
    }

    public static void not(@Nonnull @Bool ByteArrayList l, @Nonnull @Result @Bool ByteArrayList result) {
        result.setSize(l.size());
        for (int i = 0; i < l.size(); i++) {
            result.set(i, bnot(l.getByte(i)));
        }
    }

}