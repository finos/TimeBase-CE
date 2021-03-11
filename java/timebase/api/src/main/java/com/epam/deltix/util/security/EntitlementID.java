package com.epam.deltix.util.security;

public interface EntitlementID {
    
    boolean isNull();
        
    int numberOfEntries();
    
    byte[] content();

    int size();
    
    int entryOffset(int entryIndex);
    
    int entryLength(int entryIndex);
}
