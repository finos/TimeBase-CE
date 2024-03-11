package com.epam.deltix.util.vsocket;

public class DefaultConnectionAcceptor implements DBConnectionAcceptor {
    public static final DefaultConnectionAcceptor INSTANCE = new DefaultConnectionAcceptor();

    private DefaultConnectionAcceptor() {
    }

    @Override
    public boolean accept(String clientId) {
        return true;
    }
}