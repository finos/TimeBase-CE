package com.epam.deltix.qsrv.hf.tickdb.comm;

import java.security.Principal;

/**
 *
 */
public class UserPrincipal implements Principal {
    public static final UserPrincipal UNDEFINED = new UserPrincipal();

    private final String name;
    private final String pass;

    private UserPrincipal() {
        this.name = this.pass = null;
    }

    public UserPrincipal(UserPrincipal copy) {
        this.name = copy.name;
        this.pass = copy.pass;
    }

    public UserPrincipal(String name, String pass) {
        this.name = name;
        this.pass = pass;
    }

    public String   getName() {
        return name;
    }

    public String getPass() {
        return pass;
    }
    
    public String   getToken() {
        if (this == UNDEFINED)
            return "";

        return name + ":" + pass;
    }
}
