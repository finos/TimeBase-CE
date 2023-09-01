/*
 * Copyright 2023 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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