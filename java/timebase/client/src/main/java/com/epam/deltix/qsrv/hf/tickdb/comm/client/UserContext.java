package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import java.security.Principal;

public class UserContext {

    private static final ThreadLocal<Principal> USER = new ThreadLocal<>();

    /*
        Set user for the current thread
     */
    public static Principal         set(Principal user) {
        Principal previous = USER.get();
        USER.set(user);
        return previous;
    }

    /*
        Get current user
     */
    public static Principal         get() {
        return USER.get();
    }
}
