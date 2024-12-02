/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.computations.math;

import com.epam.deltix.computations.api.util.FunctionsUtils;
import com.epam.deltix.computations.api.util.IntObjObjConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class Util {

    static <T extends List<?>> boolean process(@Nullable T array, @Nonnull T result, @Nonnull IntObjObjConsumer<T> consumer) {
        result.clear();

        if (FunctionsUtils.isNullOrEmpty(array)) {
            return false;
        }

        for (int i = 0; i < array.size(); i++) {
            consumer.consume(i, array, result);
        }
        return true;
    }

}