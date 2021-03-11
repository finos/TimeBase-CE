package com.epam.deltix.qsrv.hf.tickdb.pub.query;

/**
 * Subscription controller, which provides subscription information - subscribed entities and types.
 */
public interface SubscriptionManager extends
        SubscriptionInfo, EntitySubscriptionController, TypeSubscriptionController {
}