package com.epam.deltix.qsrv.hf.blocks;

import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.InstrumentKey;
import com.epam.deltix.util.collections.ElementsEnumeration;
import com.epam.deltix.util.collections.VLinkHashMapBase;
import com.epam.deltix.util.lang.Util;
import org.agrona.BitUtil;

import java.util.Arrays;
import java.util.Iterator;

/**
 *  Base class for Object to anything hash maps.
 */
@SuppressWarnings ("unchecked")
abstract class InstrumentKeyHashMapBase extends VLinkHashMapBase {

    protected ConstantIdentityKey[] keys;
    protected int indexMask;

    public InstrumentKeyHashMapBase() {
        super ();
    }

    public InstrumentKeyHashMapBase(int cap) {
        super (cap, null);
    }

    protected void          putKey (int idx, ConstantIdentityKey key) {
        keys [idx] = key;
    }

    @Override
    public long             getSizeInMemory () {
        return (
            super.getSizeInMemory () + (SIZE_OF_POINTER + ARRAY_OVERHEAD) +
            keys.length * SIZE_OF_POINTER
        );
    }

    protected final boolean       keyEquals (ConstantIdentityKey a, CharSequence bSymbol) {
        // Inlined code from InstrumentKey.equals (a, b);
        return (
                        Util.equals (a.getSymbol (), bSymbol)
        );
    }

    protected final boolean       keyEquals (ConstantIdentityKey a, ConstantIdentityKey b) {
        return InstrumentKey.equals (a, b);
    }

    @Override
    public void             clear () {
        super.clear ();
        Arrays.fill (keys, null);
    }

    @Override
    protected void          free (int idx) {
        super.free (idx);
        keys [idx] = null;
    }

    protected final class KeyIterator implements Iterator <ConstantIdentityKey> {
    	private int             pos = -1;

    	public KeyIterator () {
    		move ();
    	}

    	private void            move () {
    		do {
    			pos++;
    		} while (pos < keys.length && isEmpty (pos));
    	}

        @Override
    	public boolean          hasNext () {
    		return (pos < keys.length);
    	}

        @Override
    	public ConstantIdentityKey                next () {
            ConstantIdentityKey   ret = keys [pos];
    		move ();
    		return (ret);
    	}

        @Override
        public void             remove () {
            throw new UnsupportedOperationException ();
        }
    }

    public Iterator <ConstantIdentityKey>         keyIterator () {
    	return (new KeyIterator ());
    }

    @Override
    protected void          allocTable (int cap) {
        assert BitUtil.isPowerOfTwo(cap);
        super.allocTable (cap);

        keys = new ConstantIdentityKey [cap];
        indexMask = cap - 1;
    }

    protected final int     hashIndex (CharSequence symbol) {
        return InstrumentKey.hashCode(symbol) & indexMask;
    }

    protected final int     hashIndex (ConstantIdentityKey key) {
        return key.hashCode() & indexMask;
    }

    public boolean          remove (ConstantIdentityKey key) {
        int         idx = find (key);

        if (idx == NULL)
            return (false);

        free (idx);
        return (true);
    }

    protected int           find (CharSequence symbol) {
        return (find (hashIndex (symbol), symbol));
    }

    protected int           find (ConstantIdentityKey key) {
        return (find (hashIndex (key), key));
    }

    protected int           find (int hidx, ConstantIdentityKey key) {
        for (int chain = hashIndex [hidx]; chain != NULL; chain = next [chain]) {
            assert hashIndex (keys [chain]) == hidx;

            if (keyEquals (key, keys [chain]))
                return (chain);
        }

        return (NULL);
    }

    protected int           find (int hidx, CharSequence keySymbol) {
        for (int chain = hashIndex [hidx]; chain != NULL; chain = next [chain]) {
            assert hashIndex (keys [chain]) == hidx;

            if (keyEquals (keys [chain], keySymbol))
                return (chain);
        }

        return (NULL);
    }

    /**
     *	Quick test for the presence of the specified key.
     *
     *  @param key  The key to search.
     */
    public boolean          containsKey (ConstantIdentityKey key) {
        return (find (key) != NULL);
    }

    public final boolean    isEmpty () {
        return (count == 0);
    }

    protected final class KeyEnumeration implements ElementsEnumeration<ConstantIdentityKey> {
    	private int             pos = -1;

    	public KeyEnumeration () {
    		move ();
    	}

    	private void            move () {
    		do {
    			pos++;
    		} while (pos < keys.length && isEmpty (pos));
    	}

        @Override
    	public boolean          hasMoreElements () {
    		return (pos < keys.length);
    	}

        @Override
        public void             reset() {
            pos = -1;
            move();
        }

        @Override
    	public ConstantIdentityKey            nextElement () {
            ConstantIdentityKey   ret = keys [pos];
    		move ();
    		return (ret);
    	}
    }

    public ElementsEnumeration<ConstantIdentityKey> keys () {
    	return (new KeyEnumeration ());
    }

    public final ConstantIdentityKey []       keysToArray (ConstantIdentityKey [] ret) {
        if (ret.length < count)
            ret =  (ConstantIdentityKey [])
                java.lang.reflect.Array.newInstance (
                    ret.getClass().getComponentType(),
                    count
                );

        int     retIdx = 0;

        for (int ii = 0; ii < keys.length; ii++)
            if (isFilled (ii))
                ret [retIdx++] = keys [ii];

        assert retIdx == count;

        if (ret.length > count)
            ret [count] = null;

        return (ret);
    }


}