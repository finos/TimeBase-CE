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
package com.epam.deltix.computations.util;

import com.epam.deltix.dfp.Decimal;

import java.util.Objects;
import java.util.Spliterator;
import java.util.function.LongConsumer;

import static com.epam.deltix.dfp.Decimal64Utils.*;

public class RangeDecimalSpliterator implements Spliterator.OfLong {

    private final @Decimal long upTo;
    private final @Decimal long step;
    private @Decimal long from;
    private @Decimal long last;

    public RangeDecimalSpliterator(@Decimal long from, @Decimal long upTo, @Decimal long step, boolean closed) {
        this(from, upTo, step, closed ? step: ZERO);
    }

    private RangeDecimalSpliterator(@Decimal long from, @Decimal long upTo, @Decimal long step, @Decimal long last) {
        assert isGreater(add(subtract(upTo, from), step), ZERO);
        this.from = from;
        this.upTo = upTo;
        this.step = step;
        this.last = last;
    }

    @Override
    public OfLong trySplit() {
        long size = estimateSize();
        return size <= 1
                ? null
                // Left split always has a half-open range
                : new RangeDecimalSpliterator(from, from = add(from, multiply(splitPoint(size), step)), step, ZERO);
    }

    @Override
    public long estimateSize() {
        return toLong(divide(add(subtract(upTo, from), last), step));
    }

    @Override
    public int characteristics() {
        return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED |
                Spliterator.IMMUTABLE | Spliterator.NONNULL |
                Spliterator.DISTINCT | Spliterator.SORTED;
    }

    @Override
    public boolean tryAdvance(LongConsumer consumer) {
        Objects.requireNonNull(consumer);

        final @Decimal long i = from;
        if (isLess(i, upTo)) {
            from = add(from, step);
            consumer.accept(i);
            return true;
        } else if (isGreater(last, ZERO)) {
            last = ZERO;
            consumer.accept(i);
            return true;
        }
        return false;
    }

    @Override
    public void forEachRemaining(LongConsumer consumer) {
        Objects.requireNonNull(consumer);

        @Decimal long i = from;
        @Decimal long hLast = last;
        from = upTo;
        last = ZERO;
        while (isLess(i, upTo)) {
            consumer.accept(i);
            i = add(i, step);
        }
        if (isGreater(hLast, ZERO)) {
            // Last element of closed range
            consumer.accept(i);
        }
    }

    /**
     * The spliterator size below which the spliterator will be split
     * at the mid-point to produce balanced splits. Above this size the
     * spliterator will be split at a ratio of
     * 1:(RIGHT_BALANCED_SPLIT_RATIO - 1)
     * to produce right-balanced splits.
     *
     * <p>Such splitting ensures that for very large ranges that the left
     * side of the range will more likely be processed at a lower-depth
     * than a balanced tree at the expense of a higher-depth for the right
     * side of the range.
     *
     * <p>This is optimized for cases such as LongStream.range(0, Long.MAX_VALUE)
     * that is likely to be augmented with a limit operation that limits the
     * number of elements to a count lower than this threshold.
     */
    private static final long BALANCED_SPLIT_THRESHOLD = 1 << 24;

    /**
     * The split ratio of the left and right split when the spliterator
     * size is above BALANCED_SPLIT_THRESHOLD.
     */
    private static final long RIGHT_BALANCED_SPLIT_RATIO = 1 << 3;

    private static long splitPoint(long size) {
        long d = (size < BALANCED_SPLIT_THRESHOLD) ? 2 : RIGHT_BALANCED_SPLIT_RATIO;
        // 2 <= size <= Long.MAX_VALUE
        return size / d;
    }
}
