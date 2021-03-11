package com.epam.deltix.qsrv.hf.tickdb.pub.query;

import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;

/**
 * Adapter for <b>client</b> implementations of {@link deltix.qsrv.hf.tickdb.pub.TickCursor}.
 * @author Daniil Yarmalkevich
 * Date: 11/18/2019
 */
public interface SymbolAndTypeSubscriptionControllerClient extends SymbolAndTypeSubscriptionController {
    @Override
    default void             subscribeToAllSymbols() {
        subscribeToAllEntities();
    }

    @Override
    default void             clearAllSymbols() {
        clearAllEntities();
    }

    @Override
    default void             addSymbol(CharSequence symbol) {
        addEntity(new ConstantIdentityKey(symbol));
    }

    @Override
    default void             addSymbols(
            CharSequence[] symbols,
            int offset,
            int length
    ) {
        IdentityKey[] ids = new IdentityKey[length];
        for (int i = 0; i < length; i++)
            ids[i] = new ConstantIdentityKey(symbols[i]);

        addEntities(ids);
    }

    @Override
    default void             removeSymbol(CharSequence symbol) {
        removeEntity(new ConstantIdentityKey(symbol));
    }

    @Override
    default void             removeSymbols(
            CharSequence[] symbols,
            int offset,
            int length
    ) {
        IdentityKey[] ids = new IdentityKey[length];
        for (int i = 0; i < length; i++)
            ids[i] = new ConstantIdentityKey(symbols[i]);

        removeEntities(ids);
    }

    @Override
    default void add (CharSequence[] symbols, String[] types) {
        IdentityKey[] ids = new IdentityKey[symbols.length];

        for (int i = 0; i < symbols.length; i++)
            ids[i] = new ConstantIdentityKey(symbols[i]);

        add(ids, types);
    }

    @Override
    default void remove(CharSequence[] symbols, String[] types) {
        IdentityKey[] ids = new IdentityKey[symbols.length];

        for (int i = 0; i < symbols.length; i++)
            ids[i] = new ConstantIdentityKey(symbols[i]);

        remove(ids, types);
    }
}
