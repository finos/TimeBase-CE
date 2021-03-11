package com.epam.deltix.util.security;

/**
 * Something that knows how provide a list of users and groups, as well as read UAC permission rules.
 */
public interface SecurityProvider extends UserDirectory, AccessControlRulesFactory {
}
