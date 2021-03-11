package com.epam.deltix.util.security;

import java.security.Principal;

/**
 * Something that knows what users and groups we have and how to authenticate a user.
 */
public interface UserDirectory {

    Principal[]     users();

    Principal       getUser(String name);

    Principal[]     groups();

    Principal       getGroup(String name);
}
