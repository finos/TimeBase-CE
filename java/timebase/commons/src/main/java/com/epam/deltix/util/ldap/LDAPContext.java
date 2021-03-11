package com.epam.deltix.util.ldap;

import com.epam.deltix.util.ldap.security.Configuration;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

public class LDAPContext {
    public DirContext       context;
    public Configuration    config;

    public LDAPContext(DirContext context, Configuration config) {
        this.context = context;
        this.config = config;
    }

    public void             reconnect() throws NamingException {
        if (context != null)
            context.close();

        if (config.credentials != null)
            context = LDAPConnection.connect(config.connection, config.credentials.name, config.credentials.get());
        else
            context = LDAPConnection.connect(config.connection);
    }

}
