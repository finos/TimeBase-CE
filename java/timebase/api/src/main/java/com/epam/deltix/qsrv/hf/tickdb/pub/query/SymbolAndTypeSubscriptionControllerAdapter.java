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
package com.epam.deltix.qsrv.hf.tickdb.pub.query;

/**
 * Adapter for <b>server</b> cursor implementations of {@link deltix.qsrv.hf.tickdb.pub.TickCursor}.
 * @author Daniil Yarmalkevich
 * Date: 11/18/2019
 */
public interface SymbolAndTypeSubscriptionControllerAdapter extends SymbolAndTypeSubscriptionController {

    /**
     * Not supported for server cursors, cause makes no sense.
     */
    @Override
    default void subscribeToAllSymbols() {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported for server cursors, cause makes no sense.
     */
    @Override
    default void clearAllSymbols() {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported for server cursors, cause makes no sense.
     */
    @Override
    default void addSymbol(CharSequence symbol) {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported for server cursors, cause makes no sense.
     */
    @Override
    default void addSymbols(CharSequence[] symbols, int offset, int length) {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported for server cursors, cause makes no sense.
     */
    @Override
    default void addSymbols(CharSequence[] symbols) {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported for server cursors, cause makes no sense.
     */
    @Override
    default void removeSymbol(CharSequence symbol) {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported for server cursors, cause makes no sense.
     */
    @Override
    default void removeSymbols(CharSequence[] symbols, int offset, int length) {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported for server cursors, cause makes no sense.
     */
    @Override
    default void removeSymbols(CharSequence[] symbols) {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported for server cursors, cause makes no sense.
     */
    @Override
    default void add(CharSequence[] symbols, String[] types) {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported for server cursors, cause makes no sense.
     */
    @Override
    default void remove(CharSequence[] symbols, String[] types) {
        throw new UnsupportedOperationException();
    }
}