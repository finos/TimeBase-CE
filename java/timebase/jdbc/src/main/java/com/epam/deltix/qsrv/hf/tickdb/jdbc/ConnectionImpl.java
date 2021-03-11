package com.epam.deltix.qsrv.hf.tickdb.jdbc;

import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.jdbc.WrapperImpl;

import java.sql.*;
import java.util.*;
import java.util.concurrent.Executor;

/**
 *
 */
class ConnectionImpl 
    extends WrapperImpl
    implements Connection
{
    DXTickDB                    db;
    final String                url;
    boolean                     autoCommit = false;
    
    ConnectionImpl (String url, Properties info) {
        this.url = url;
        
        db = TickDBFactory.createFromUrl (url);
        db.open (true); // ro for now...
    }

    public void                 close () {
        db.close ();
    }

    public DatabaseMetaData     getMetaData () {
        return (new DatabaseMetaDataImpl (this));
    }

    public String               getCatalog () {
        return (null);
    }

    public void                 setCatalog (String catalog) {
        // Ignore, per JDBC specification
    }

    public boolean              getAutoCommit () {
        return (autoCommit);
    }

    public void                 setAutoCommit (boolean autoCommit) {
        this.autoCommit = autoCommit;
    }
    
    public void                 commit () throws SQLException {
    }

    //UNIMP BELOW
    
    public void clearWarnings () throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public Array createArrayOf (String typeName, Object[] elements) throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public Blob createBlob () throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public Clob createClob () throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public NClob createNClob () throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public SQLXML createSQLXML () throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public Statement createStatement () throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public Statement createStatement (int resultSetType, int resultSetConcurrency) throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public Statement createStatement (int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public Struct createStruct (String typeName, Object[] attributes) throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public String getClientInfo (String name) throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public Properties getClientInfo () throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public int getHoldability () throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public int getTransactionIsolation () throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public Map<String, Class<?>> getTypeMap () throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public SQLWarning getWarnings () throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public boolean isClosed () throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public boolean isReadOnly () throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public boolean isValid (int timeout) throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public String nativeSQL (String sql) throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public CallableStatement prepareCall (String sql) throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public CallableStatement prepareCall (String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public CallableStatement prepareCall (String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public PreparedStatement prepareStatement (String sql) throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public PreparedStatement prepareStatement (String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public PreparedStatement prepareStatement (String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public PreparedStatement prepareStatement (String sql, int autoGeneratedKeys) throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public PreparedStatement prepareStatement (String sql, int[] columnIndexes) throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public PreparedStatement prepareStatement (String sql, String[] columnNames) throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public void releaseSavepoint (Savepoint savepoint) throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public void rollback () throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public void rollback (Savepoint savepoint) throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public void setClientInfo (String name, String value) throws SQLClientInfoException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public void setClientInfo (Properties properties) throws SQLClientInfoException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public void setHoldability (int holdability) throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public void setReadOnly (boolean readOnly) throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public Savepoint setSavepoint () throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public Savepoint setSavepoint (String name) throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public void setTransactionIsolation (int level) throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public void setTypeMap (Map<String, Class<?>> map) throws SQLException {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    // Since Java 7

    public int getNetworkTimeout() {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public void setNetworkTimeout(Executor executor, int milliseconds) {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public void abort(Executor executor) {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public void setSchema(String schema) {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public String getSchema() {
        throw new UnsupportedOperationException ("Not supported yet.");
    }
}
