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
package com.epam.deltix.qsrv.hf.blocks;

import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.util.collections.KeyEntry;
import com.epam.deltix.util.collections.generated.IntegerEnumeration;
import org.agrona.BitUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class InstrumentKeyToIntegerHashMap
    extends InstrumentKeyHashMapBase
{
    private int []         values;

    public InstrumentKeyToIntegerHashMap() {
        super ();
    }

    public InstrumentKeyToIntegerHashMap(int cap) {
        super (validateSize(cap));
    }

    private static int validateSize(int cap) {
        if (!BitUtil.isPowerOfTwo(cap)) {
            throw new IllegalArgumentException("Size must be a power of 2");
        }
        return cap;
    }

    @Override
    protected void              allocTable (int cap) {
        super.allocTable (cap);

        values = new int [cap];
    }

    @Override
    public long                 getSizeInMemory () {
        return (
            super.getSizeInMemory () + (SIZE_OF_POINTER + ARRAY_OVERHEAD) +
            values.length * SIZE_OF_INT
        );
    }


    protected void              resizeTable (int newSize) {
        assert BitUtil.isPowerOfTwo(newSize);

        final int curLength = values.length;
        final ConstantIdentityKey[] saveKeys = keys;
        final int[] saveValues = values;
        final int[] savePrev = prev;

        allocTable (newSize);

        for (int ii = 0; ii < curLength; ii++)
            if (savePrev [ii] != NULL)
                putNewNoSpaceCheck (saveKeys [ii], saveValues [ii]);
    }

    public int               get (CharSequence symbol, int notFoundValue) {
        int         pos = find (symbol);

        return (pos == NULL ? notFoundValue : values [pos]);
    }

    /**
     * @return key object instance if key present in the map
     */
    @Nullable
    public ConstantIdentityKey getKeyObject(CharSequence symbol) {
        int pos = find(symbol);

        return (pos == NULL ? null : keys[pos]);
    }

    public int               get (ConstantIdentityKey key, int notFoundValue) {
        int         pos = find (key);

        return (pos == NULL ? notFoundValue : values [pos]);
    }

    /**
     *  Remove a value, if existed.
     *
     *  @param key          The key
     *  @param notFoundValue This will be returned if the key was not associated with a value.
     *  @return The old value associated with the key, or <code>notFoundValue</code>.
     */
    public int               remove (ConstantIdentityKey key, int notFoundValue) {
        int         idx = find (key);

        if (idx == NULL)
            return (notFoundValue);

        int        value = values [idx];

        free (idx);

        return (value);
    }

    private void                putNewNoSpaceCheck (ConstantIdentityKey key, int value) {
        int         hidx = hashIndex (key);
        int         idx = find (hidx, key);

        if (idx != NULL)
            throw new IllegalArgumentException (
                "Value for key " + key + " already exists = " + value
            );

        idx = allocEntry (hidx);

        values [idx] = value;
        putKey(idx, key);
    }

    /**
     *  Put new element into the map
     *
     *  @param key       The key
     *  @param value     The value
     *  @return  true if the element is new, false if the key was found.
     */
    public boolean              put (ConstantIdentityKey key, int value) {
        int         hidx = hashIndex (key);
        int         idx = find (hidx, key);

        if (idx != NULL) {
            values [idx] = value;
            return (false);
        }

        if (freeHead == NULL) {
            resizeTable (values.length * 2);
            hidx = hashIndex (key); // recompute!
        }

        idx = allocEntry (hidx);

        values [idx] = value;
        putKey (idx, key);

        return (true);
    }

    /**
     *  Replace a value and return the old one.
     *  @param key          The key
     *  @param value        The new value to put in table
     *  @param notFoundValue This will be returned if the key was not associated with a value.
     *  @return The old value associated with the key, or <code>notFoundValue</code>.
     */
    public int               putAndGet (
        ConstantIdentityKey   key,
        int                     value,
        int                     notFoundValue
    )
    {
        int         hidx = hashIndex (key);
        int         idx = find (hidx, key);

        if (idx != NULL) {
            int    old = values [idx];

            values [idx] = value;

            return (old);
        }

        if (freeHead == NULL) {
            resizeTable (values.length * 2);
            hidx = hashIndex (key); // recompute!
        }

        idx = allocEntry (hidx);

        values [idx] = value;
        putKey(idx, key);

        return (notFoundValue);
    }

    /**
     *  Put new value into the map only if there is no previously stored value under the same key.
     *
     *  @param key          The key
     *  @param value        The new value to put in table (but only if the key is not occupied)
     *  @return             Value that remains in the map.
     */
    public int               putAndGetIfEmpty (
        ConstantIdentityKey key,
        int                 value
    )
    {
        int         hidx = hashIndex (key);
        int         idx = find (hidx, key);

        if (idx != NULL) {
            return  values [idx];
        }

        if (freeHead == NULL) {
            resizeTable (values.length * 2);
            hidx = hashIndex (key); // recompute!
        }

        idx = allocEntry (hidx);

        values [idx] = value;
        putKey(idx, key);

        return value;
    }

    /**
     *  Put new value into the map only if there is no previously stored value under the same key.
     *
     *  @param key          The key
     *  @param value        The new value to put in table (but only if the key is not occupied)
     *  @return             True if this map has changed as the result of this call.
     */
    public boolean               putIfEmpty (
        ConstantIdentityKey key,
        int                 value
    )
    {
        int         hidx = hashIndex (key);
        int         idx = find (hidx, key);

        if (idx != NULL) {
            return false;
        }

        if (freeHead == NULL) {
            resizeTable (values.length * 2);
            hidx = hashIndex (key); // recompute!
        }

        idx = allocEntry (hidx);

        values [idx] = value;
        putKey(idx, key);

        return true;
    }


    /**
     *	Linearly searches for the specified value.
     *
     *  @param value    The value to search.
     *  @return         Whether the specified value is found.
     */
    public final boolean        containsValue (int value) {
    	int         tabSize = values.length;

    	for (int ii = 0; ii < tabSize; ii++)
    		if (isFilled (ii) && values [ii] == value)
    			return (true);

    	return (false);
    }

    public final int []   valuesToArray (int [] ret) {
        if (ret == null || ret.length < count)
            ret = new int [count];

        final int       tabSize = values.length;
        int             outIdx = 0;

        for (int ii = 0; ii < tabSize; ii++)
            if (isFilled (ii))
                ret [outIdx++] = values [ii];

        assert outIdx == count;

        return ret;
    }

    /**
     * Returns array that contains keys of this map as values with array indexes matching this map keys.
     * So map [(key1 - 1), (key2 - 2), (key3 - 0)] will be returned as [key3, key1, key2]
     *
     * Values for entries in current map must be distinct and in range 0..size-1. Otherwise Illegal state exception will be thrown.
     */
    @Nonnull
    public ConstantIdentityKey[] getInverseMapSnapshot() {
        ConstantIdentityKey[] result = new ConstantIdentityKey[count];

        for (int i = 0; i < values.length; i++) {
            ConstantIdentityKey key = keys[i];
            if (key != null) {
                int value = values[i];
                if (value < 0 || value >= count || result[value] != null) {
                    throw new IllegalStateException("Map is not invertible");
                }
                result[value] = key;
            }
        }
        return result;
    }


    protected final class ElementEnumeration
        implements IntegerEnumeration, KeyEntry<ConstantIdentityKey>
    {
    	private int             pos = -1;

    	public ElementEnumeration () {
    		move ();
    	}

    	private void            move () {
    		do {
    			pos++;
    		} while (pos < values.length && isEmpty (pos));
    	}

        @Override
    	public boolean          hasMoreElements () {
    		return (pos < values.length);
    	}

        @Override
        public void             reset() {
            pos = -1;
            move();
        }

        @Override
    	public Integer           nextElement () {
    		int   ret = values [pos];
    		move ();
    		return (ret);
    	}

        @Override
	    public int           nextIntElement () {
    		int   ret = values [pos];
    		move ();
    		return (ret);
    	}

        @Override
	    public ConstantIdentityKey                key () {
           return keys [pos];
        }
    }

    public IntegerEnumeration    elements () {
    	return (new ElementEnumeration ());
    }

    @Override
    public final boolean        equals (Object o) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public int                  hashCode () {
        throw new UnsupportedOperationException ();
    }

}