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
package com.epam.deltix.qsrv.testsetup;

import com.epam.deltix.streaming.MessageSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

@SuppressFBWarnings(value = "PREDICTABLE_RANDOM", justification = "Random is used for internal tests, not cryptography")
public abstract class BaseGenerator<T> implements MessageSource<T> {
    protected final Random rnd = new Random(2011);
    protected GregorianCalendar calendar;
    private int interval;
    protected T current;
    protected CycleIterator<String> symbols;

    protected BaseGenerator(GregorianCalendar calendar, int interval, String ... tickers) {
        this.calendar = calendar;
        this.interval = interval;
        setSymbols(tickers);
    }

    protected double nextDouble() {
        BigDecimal v = new BigDecimal(rnd.nextDouble());
        return v.round(new MathContext(5)).doubleValue() + rnd.nextInt(1000);
    }

    public T getMessage() {
        return current;
    }

    protected long getNextTime() {
        calendar.add(Calendar.MILLISECOND, interval);
        return calendar.getTimeInMillis();
    }

    public long getActualTime() {
        return calendar.getTimeInMillis();
    }

    public void setSymbols(String ... tickers) {
        this.symbols = new CycleIterator<String>(Arrays.asList(tickers));
    }

    public void close() {
    }
}