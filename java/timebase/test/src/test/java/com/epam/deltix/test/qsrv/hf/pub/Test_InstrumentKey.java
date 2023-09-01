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

import com.epam.deltix.timebase.messages.InstrumentKey;
import com.epam.deltix.util.JUnitCategories;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Andy
 *         Date: Apr 2, 2010 12:45:55 PM
 */
@Category(JUnitCategories.TickDB.class)
public class Test_InstrumentKey {

    private static class NonSerializableCharSequence implements CharSequence {

        private final String data;

        public NonSerializableCharSequence(String data) {
            this.data = data;
        }

        @Override
        public int length() {
            return data.length();
        }

        @Override
        public char charAt(int index) {
            return data.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return data.subSequence(start, end);
        }

        @Override
        public String toString() {
            return data;
        }
    }

    /** Testing serialization of InstrumentKey that uses non-serializable symbol */
    @Test
    public void testSerialization () throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        InstrumentKey key = new InstrumentKey(new NonSerializableCharSequence("ABCDE"));
        oos.writeObject(key);
        oos.close();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));


        InstrumentKey deserialized = (InstrumentKey) ois.readObject();
        ois.close();
        assertEquals (key.symbol.toString(), deserialized.symbol.toString());
        assertEquals (key, deserialized);
    }
}