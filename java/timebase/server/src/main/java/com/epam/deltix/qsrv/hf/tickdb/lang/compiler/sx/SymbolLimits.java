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

package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class SymbolLimits {
    private List<String> symbols = new ArrayList<>();
    private boolean subscribeAll;

    public SymbolLimits() {

    }

    public SymbolLimits(boolean subscribeAll) {
        this.subscribeAll = subscribeAll;
    }

    public SymbolLimits(boolean subscribeAll, String symbol) {
        this.subscribeAll = subscribeAll;
        this.symbols.add(symbol);
    }

    public SymbolLimits(boolean subscribeAll, List<String> symbols) {
        this.subscribeAll = subscribeAll;
        this.symbols.addAll(symbols);
    }

    public void addSymbol(String symbol) {
        symbols.add(symbol);
    }

    public List<String> symbols() {
        return symbols;
    }

    public boolean isSubscribeAll() {
        return subscribeAll;
    }

    public void setSubscribeAll(boolean subscribeAll) {
        this.subscribeAll = subscribeAll;
    }
}
