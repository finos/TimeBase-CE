package com.epam.deltix.qsrv.hf.tickdb.pub.query;

/**
 * @author Daniil Yarmalkevich
 * Date: 11/14/2019
 */
public interface SymbolSubscriptionController extends EntitySubscriptionController {

    /**
     * Subscribe to all present symbols.
     */
    void                     subscribeToAllSymbols();

    /**
     * Unsubscribe all symbols.
     */
    void                     clearAllSymbols();

    /**
     * Add symbol to the subscription.
     * @param symbol symbol to be added
     */
    void                     addSymbol(CharSequence symbol);

    /**
     * Add array of symbols to the subscription from index <code>offset</code> to index <code>offset + length</code>.
     * @param symbols array of symbols to be added to subscription
     * @param offset start index
     * @param length length of symbols to be added
     */
    void                     addSymbols(
            CharSequence[] symbols,
            int offset,
            int length
    );

    /**
     * Add array of symbols to the subscription.
     * @param symbols symbols to be added
     */
    default void             addSymbols(CharSequence[] symbols) {
        addSymbols(symbols, 0, symbols.length);
    }

    /**
     * Remove symbol from the subscription.
     * @param symbol symbol to be removed
     */
    void                     removeSymbol(CharSequence symbol);

    /**
     * Remove array of symbols from the subscription from index <code>offset</code> to index <code>offset + length</code>.
     * @param symbols array of symbols to be removed
     * @param offset start index
     * @param length length of symbols to be removed
     */
    void                     removeSymbols(
            CharSequence[] symbols,
            int offset,
            int length
    );

    /**
     * Remove array of symbols from the subscription.
     * @param symbols symbols to be removed
     */
    default void             removeSymbols(CharSequence[] symbols) {
        removeSymbols(symbols, 0, symbols.length);
    }
}
