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
package com.epam.deltix.qsrv.hf.tickdb.tests;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.service.BinaryMessage;
import com.epam.deltix.containers.BinaryArray;

import java.util.ArrayList;
import java.util.Random;

public class RandomBinaryMessageSource implements MessageSource<BinaryMessage> {

    private final byte[] bytes;
    private final Random random = new Random(System.currentTimeMillis());
    private final BinaryMessage message;
    private final BinaryArray array;
    private final ArrayList<String> symbols;

    public RandomBinaryMessageSource(int size, int symbols) {
        this.bytes = new byte[size];
        this.symbols = TestUtils.getRandomStringsList(symbols - 1);
        this.symbols.add("DELETE_SYMBOL");
        this.message = new BinaryMessage();
        this.array = new BinaryArray(size);
        this.message.setData(this.array);
    }

    private String getSymbol() {
        return symbols.get(random.nextInt(symbols.size()));
    }

    private void setInstrumentMessage(InstrumentMessage instrumentMessage) {
        instrumentMessage.setSymbol(getSymbol());
        //instrumentMessage.setInstrumentType(InstrumentType.FX);
    }

    private void setBinaryMessage(BinaryMessage binaryMessage) {
        setInstrumentMessage(binaryMessage);
        array.clear();
        random.nextBytes(bytes);
        array.append(bytes);
    }

    public String removeSymbol() {
        synchronized (symbols) {
            return symbols.remove(symbols.size() - 1);
        }
    }

    @Override
    public BinaryMessage getMessage() {
        setBinaryMessage(message);
        return message;
    }

    @Override
    public boolean next() {
        return true;
    }

    @Override
    public boolean isAtEnd() {
        return false;
    }

    @Override
    public void close() {
        array.clear();
    }
}
