package com.epam.deltix.qsrv.hf.tickdb.jdbc;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 *
 */
public class QuantServerSqlDriver implements Driver {
    public static final String              PROTOCOL_PREFIX = "dxsql:";
    
    public boolean      acceptsURL (String url) throws SQLException {
        return (url.startsWith (PROTOCOL_PREFIX));
    }

    public Connection   connect (String url, Properties info) throws SQLException {
        if (!acceptsURL (url))
            throw new IllegalArgumentException (url);

        url = url.substring (PROTOCOL_PREFIX.length ());
        
        return (new ConnectionImpl (url, info));
    }

    public int          getMajorVersion () {
        return (5);
    }

    public int          getMinorVersion () {
        return (0);
    }

    public DriverPropertyInfo [] getPropertyInfo (String url, Properties info) 
        throws SQLException 
    {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public boolean      jdbcCompliant () {
        return (false);
    }
    
    public static void main (String [] args) {
        QuantServerSqlDriver    d = new QuantServerSqlDriver ();
        
        System.out.println (d.getMajorVersion () + "." + d.getMinorVersion ());
    }


    // Java 1.7

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }
}
