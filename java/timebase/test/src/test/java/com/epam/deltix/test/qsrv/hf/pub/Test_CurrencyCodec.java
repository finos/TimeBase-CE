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
package com.epam.deltix.test.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.CurrencyCodec;
import org.junit.*;
import static org.junit.Assert.*;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.UHFFramework;

/**
 *
 */

@Category(UHFFramework.class)
public class Test_CurrencyCodec {
    private void        roundTrip (String ccy) {
        short   x = CurrencyCodec.codeToShort (ccy);
        String  s = CurrencyCodec.intToCode (x);

        assertEquals (ccy.toUpperCase (), s);
    }

    private void        checkFail (String badCcy) {
        try {
            CurrencyCodec.codeToInt (badCcy);
            assertTrue ("No exception", false);
        } catch (NumberFormatException x) {
            assertEquals (badCcy, x.getMessage ());
        }
    }

    @Test
    public void         smoke () {        
        assertEquals ("XXX", CurrencyCodec.intToCode (CurrencyCodec.XXX));
        assertEquals ("USD", CurrencyCodec.intToCode (CurrencyCodec.USD));
        assertEquals ("197", CurrencyCodec.intToCode (197));

        roundTrip ("XXX");
        roundTrip ("usD");
        roundTrip ("AbC");
        roundTrip ("109");

        checkFail ("");
        checkFail ("A");
        checkFail ("1t6");
    }
}