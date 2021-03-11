package com.epam.deltix.util.security;

/**
 *
 */
public interface SecurityReloadNotifier {

    void addReloadListener(SecurityReloadListener listener);

    void removeReloadListener(SecurityReloadListener listener);
}
