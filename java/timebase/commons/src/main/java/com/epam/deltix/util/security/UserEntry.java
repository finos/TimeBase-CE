package com.epam.deltix.util.security;

import java.security.Principal;

public class UserEntry extends PrincipalEntry implements Principal {

    public UserEntry(String id) {
        super(id);
    }
}