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
