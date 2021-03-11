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
