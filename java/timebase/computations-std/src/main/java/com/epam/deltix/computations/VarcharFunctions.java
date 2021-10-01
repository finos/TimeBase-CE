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

import com.epam.deltix.computations.api.annotations.*;
import com.epam.deltix.computations.api.util.FunctionsUtils;
import com.epam.deltix.qsrv.hf.codec.cg.StringBuilderPool;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.util.collections.generated.IntegerArrayList;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.lang.Util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@FunctionsRepo
public class VarcharFunctions {

    @Function("LENGTH")
    public static int length(@Nullable CharSequence cs) {
        return cs == null ? IntegerDataType.INT32_NULL : cs.length();
    }

    @Function("LENGTH")
    public static boolean length(@Type("ARRAY(VARCHAR?)?") @Nullable ObjectArrayList<CharSequence> list,
                                 @Nonnull @Result IntegerArrayList result) {
        result.clear();

        if (FunctionsUtils.isNullOrEmpty(list))
            return false;

        for (CharSequence charSequence : list) {
            result.add(length(charSequence));
        }
        return true;
    }

    @Function("INDEXOF")
    public static int indexOf(@Nullable CharSequence source, @Nullable CharSequence target) {
        if (source == null || target == null)
            return IntegerDataType.INT32_NULL;
        return StringUtils.indexOf(source, target);
    }

    @Function("INDEXOF")
    public static boolean indexOf(@Type("ARRAY(VARCHAR?)?") @Nullable ObjectArrayList<CharSequence> source,
                                  @Nullable CharSequence target, @Nonnull @Result IntegerArrayList result) {
        result.clear();
        if (FunctionsUtils.isNullOrEmpty(source) || target == null)
            return false;

        result.setSize(source.size());
        for (int i = 0; i < source.size(); i++) {
            result.set(i, indexOf(source.get(i), target));
        }
        return true;
    }

    @Function("REVERSED")
    public static boolean reverse(@Nullable CharSequence source, @Nonnull @Result StringBuilder sb) {
        sb.setLength(0);
        if (source == null)
            return false;

        for (int i = source.length() - 1; i > -1; i--) {
            sb.append(source.charAt(i));
        }
        return true;
    }

    @Function("REVERSED")
    public static boolean reverse(@Type("ARRAY(VARCHAR?)?") @Nullable ObjectArrayList<CharSequence> source,
                                  @Type("ARRAY(VARCHAR?)?") @Nonnull @Result ObjectArrayList<CharSequence> result,
                                  @Nonnull @Pool StringBuilderPool pool) {
        result.clear();
        if (FunctionsUtils.isNullOrEmpty(source))
            return false;

        result.setSize(source.size());
        for (int i = 0; i < source.size(); i++) {
            StringBuilder sb = pool.borrow();
            result.set(i, reverse(source.get(i), sb) ? sb : null);
        }
        return true;
    }

    @Function("UPPERCASE")
    public static boolean uppercase(@Nullable CharSequence source, @Nonnull @Result StringBuilder sb) {
        sb.setLength(0);
        if (source == null)
            return false;

        for (int i = 0; i < source.length(); i++) {
            sb.append(Character.toUpperCase(source.charAt(i)));
        }
        return true;
    }

    @Function("UPPERCASE")
    public static boolean uppercase(@Type("ARRAY(VARCHAR?)?") @Nullable ObjectArrayList<CharSequence> source,
                                    @Type("ARRAY(VARCHAR?)?") @Nonnull @Result ObjectArrayList<CharSequence> result,
                                    @Nonnull @Pool StringBuilderPool pool) {
        result.clear();
        if (FunctionsUtils.isNullOrEmpty(source))
            return false;

        result.setSize(source.size());
        for (int i = 0; i < source.size(); i++) {
            StringBuilder sb = pool.borrow();
            result.set(i, uppercase(source.get(i), sb) ? sb : null);
        }
        return true;
    }

    @Function("LOWERCASE")
    public static boolean lowercase(@Nullable CharSequence source, @Nonnull @Result StringBuilder sb) {
        sb.setLength(0);
        if (source == null)
            return false;

        for (int i = 0; i < source.length(); i++) {
            sb.append(Character.toLowerCase(source.charAt(i)));
        }
        return true;
    }

    @Function("LOWERCASE")
    public static boolean lowercase(@Type("ARRAY(VARCHAR?)?") @Nullable ObjectArrayList<CharSequence> source,
                                    @Type("ARRAY(VARCHAR?)?") @Nonnull @Result ObjectArrayList<CharSequence> result,
                                    @Nonnull @Pool StringBuilderPool pool) {
        result.clear();
        if (FunctionsUtils.isNullOrEmpty(source))
            return false;

        result.setSize(source.size());
        for (int i = 0; i < source.size(); i++) {
            StringBuilder sb = pool.borrow();
            result.set(i, lowercase(source.get(i), sb) ? sb : null);
        }
        return true;
    }

    @Function("SUBSTR")
    public static boolean substr(@Nullable CharSequence source, int start, int end, @Nonnull @Result StringBuilder sb) {
        sb.setLength(0);
        if (source == null || start >= end || start >= source.length())
            return false;

        for (int i = start; i < Math.min(end, source.length()); i++) {
            sb.append(source.charAt(i));
        }
        return true;
    }

    @Function("SUBSTR")
    public static boolean substr(@Type("ARRAY(VARCHAR?)?") @Nullable ObjectArrayList<CharSequence> source, int start, int end,
                                 @Type("ARRAY(VARCHAR?)?") @Nonnull @Result ObjectArrayList<CharSequence> result,
                                 @Nonnull @Pool StringBuilderPool pool) {
        result.clear();
        if (FunctionsUtils.isNullOrEmpty(source) || start > end)
            return false;

        result.setSize(source.size());
        for (int i = 0; i < source.size(); i++) {
            StringBuilder sb = pool.borrow();
            result.set(i, substr(source.get(i), start, end, sb) ? sb : null);
        }
        return true;
    }

    @Function("SORT")
    public static boolean sort(@Type("ARRAY(VARCHAR?)?") @Nullable ObjectArrayList<CharSequence> list,
                               @Type("ARRAY(VARCHAR?)?") @Nonnull @Result ObjectArrayList<CharSequence> result,
                               @Nonnull @Pool StringBuilderPool pool) {
        result.clear();
        if (FunctionsUtils.isNullOrEmpty(list))
            return false;

        result.setSize(list.size());
        for (int i = 0; i < list.size(); i++) {
            StringBuilder sb = pool.borrow();
            result.set(i, list.get(i) == null ? null : sb.append(list.get(i)));
        }
        result.sort((x, y) -> Util.compare(x, y, true));
        return true;
    }

}
