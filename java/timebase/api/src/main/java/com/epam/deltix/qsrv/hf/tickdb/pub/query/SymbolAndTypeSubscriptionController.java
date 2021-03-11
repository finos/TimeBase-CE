package com.epam.deltix.qsrv.hf.tickdb.pub.query;

/**
 * @author Daniil Yarmalkevich
 * Date: 11/14/2019
 */
public interface SymbolAndTypeSubscriptionController extends
        SymbolSubscriptionController, EntityAndTypeSubscriptionController {

    /**
     * Add symbols of certain types to the subscription.
     * @param symbols symbols notations
     * @param types types (full names)
     */
    void        add(
            CharSequence[] symbols,
            String[] types
    );

    /**
     * Remove symbols of certain types from the subscription.
     * @param symbols symbols notations
     * @param types types (full names)
     */
    void        remove(
            CharSequence[] symbols,
            String[] types
    );

}
