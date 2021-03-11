package com.epam.deltix.qsrv.hf.tickdb.pub.query;

/**
 * Controls various aspects of message feed subscription
 *
 * @see InstrumentMessageSource 
 */
public interface SubscriptionController extends
        StreamSubscriptionController,
        TimeController,
        EntityAndTypeSubscriptionController,
        SymbolAndTypeSubscriptionController
{

}
