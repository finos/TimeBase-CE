package com.epam.deltix.qsrv.hf.blocks;

import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.util.collections.CharSequenceToObjectMap;

import java.util.Iterator;

public class TimeIdentitySet<T extends TimeIdentity> implements TimeIdentity, Iterable<T> {

    private final CharSequenceToObjectMap<T> map = new CharSequenceToObjectMap<T>();
    private T                              entry;    

    public TimeIdentitySet(T entry) {
        this.entry = entry;
    }

    @SuppressWarnings ("unchecked")
    public TimeIdentity get(IdentityKey id) {

        T instance = map.get(id);
        
        if (instance == null) {
            entry = instance = (T) entry.create(id);
            map.put(id.getSymbol(), instance);
        }

        return instance;
    }

    @Override
    public TimeIdentity create(IdentityKey id) {
        return entry.create(id);
    }

    @Override
    public long getTime() {
        return entry.getTime();
    }

    @Override
    public void setTime(long timestamp) {
        entry.setTime(timestamp);
    }

    @Override
    public CharSequence getSymbol() {
        return entry.getSymbol();
    }

    @Override
    public Iterator<T> iterator() {
        return map.values().iterator();
    }
}
