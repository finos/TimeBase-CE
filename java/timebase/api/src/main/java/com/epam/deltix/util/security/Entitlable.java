package com.epam.deltix.util.security;

public interface Entitlable<T extends EntitlementID> {
    T getEntitlementID();    
}
