package com.epam.deltix.util.jdbc;

import java.sql.SQLException;
import java.sql.Wrapper;

/**
 *
 */
public class WrapperImpl implements Wrapper {
    public boolean      isWrapperFor (Class <?> iface) throws SQLException {
        return (iface.isAssignableFrom (getClass ()));
    }

    public <T> T        unwrap (Class <T> iface) throws SQLException {
        try {
            return (iface.cast (this));
        } catch (ClassCastException x) {
            throw new SQLException (x);
        }
    }
}
