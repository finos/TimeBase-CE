package com.epam.deltix.qsrv.hf.tickdb.jdbc;

import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.client.Version;
import com.epam.deltix.util.jdbc.MemResultSetImpl;
import com.epam.deltix.util.jdbc.ResultSetImpl;
import com.epam.deltix.util.jdbc.WrapperImpl;

import java.sql.*;

/**
 *
 */
class DatabaseMetaDataImpl extends WrapperImpl implements DatabaseMetaData
{
    private final ConnectionImpl        conn;

    final int       MAJOR_VERSION = 5;
    final int       MINOR_VERSION = 0;

    public DatabaseMetaDataImpl (ConnectionImpl conn) {
        this.conn = conn;
    }
    
    public boolean      allProceduresAreCallable () throws SQLException {
        return (true);
    }

    public boolean      allTablesAreSelectable () throws SQLException {
        return (true);
    }

    public boolean      autoCommitFailureClosesAllResultSets () throws SQLException {
        return (false);
    }

    public boolean      dataDefinitionCausesTransactionCommit () throws SQLException {
        return (true);
    }

    public boolean      dataDefinitionIgnoredInTransactions () throws SQLException {
        return (false);
    }

    public boolean      deletesAreDetected (int type) throws SQLException {
        return (false);
    }

    public boolean      doesMaxRowSizeIncludeBlobs () throws SQLException {
        return (false);
    }

    public ResultSet    getAttributes (
        String              catalog,
        String              schemaPattern,
        String              typeNamePattern,
        String              attributeNamePattern
    )
        throws SQLException
    {
        System.out.println (
            "getAttributes (" + catalog + ", " + schemaPattern + ", " +
            typeNamePattern + ", " + attributeNamePattern + ")"
        );

        return (new ResultSetImpl ());
    }

    public ResultSet    getBestRowIdentifier (
        String              catalog,
        String              schema,
        String              table,
        int                 scope,
        boolean             nullable
    )
        throws SQLException
    {
        System.out.println (
            "getBestRowIdentifier (" + catalog + ", " + schema + ", " +
            table + ", " + scope + ", " + nullable + ")"
        );

        return (new ResultSetImpl());
    }

    public String       getCatalogSeparator () throws SQLException {
        return (".");
    }

    public String       getCatalogTerm () throws SQLException {
        return ("CATALOG");
    }

    public ResultSet    getCatalogs () throws SQLException {
        System.out.println ("getCatalogs ()");

        return (new ResultSetImpl ());
    }

    public ResultSet    getClientInfoProperties () throws SQLException {
        System.out.println ("getClientInfoProperties ()");

        return (new ResultSetImpl ());
    }

    public ResultSet    getColumnPrivileges (
        String catalog,
        String schema,
        String table,
        String columnNamePattern
    )
        throws SQLException
    {
        System.out.println (
            "getColumnPrivileges (" + catalog + ", " + schema + ", " +
            table + ", " + columnNamePattern + ")"
        );

        return (new ResultSetImpl ());
    }

    public ResultSet    getColumns (
        String catalog,
        String schemaPattern,
        String tableNamePattern,
        String columnNamePattern
    )
        throws SQLException
    {
        System.out.println (
            "getColumns (" + catalog + ", " + schemaPattern + ", " +
            tableNamePattern + ", " + columnNamePattern + ")"
        );

        return (new ResultSetImpl ());
    }

    public Connection   getConnection () throws SQLException {
        return (conn);
    }

    public ResultSet    getCrossReference (
        String              parentCatalog,
        String              parentSchema,
        String              parentTable,
        String              foreignCatalog,
        String              foreignSchema,
        String              foreignTable
    )
        throws SQLException
    {
        System.out.println (
            "getCrossReference (" + parentCatalog + ", " + parentSchema + ", " +
            parentTable + ", " + foreignCatalog + ", " + foreignSchema + ", " +
            foreignTable + ")"
        );

        return (new ResultSetImpl ());
    }

    public int          getDatabaseMajorVersion () throws SQLException {
        return (MAJOR_VERSION); // FIX
    }

    public int          getDatabaseMinorVersion () throws SQLException {
        return (MINOR_VERSION); // FIX
    }

    public String       getDatabaseProductName () throws SQLException {
        return ("Deltix QuantServer");
    }

    public String       getDatabaseProductVersion () throws SQLException {
        return (Version.getVersion());
    }

    public int          getDefaultTransactionIsolation () throws SQLException {
        return (Connection.TRANSACTION_READ_COMMITTED);
    }

    public int          getDriverMajorVersion () {
        return (MAJOR_VERSION);
    }

    public int          getDriverMinorVersion () {
        return (MINOR_VERSION);
    }

    public String       getDriverName () throws SQLException {
        return ("Deltix QuantServer JDBC Driver");
    }

    public String       getDriverVersion () throws SQLException {
        return (Version.getVersion());
    }

    public ResultSet    getExportedKeys (
        String              catalog,
        String              schema,
        String              table
    )
        throws SQLException
    {
        System.out.println (
            "getExportedKeys (" + catalog + ", " + schema + ", " +
            table + ")"
        );

        return (new ResultSetImpl ());
    }

    public String       getExtraNameCharacters () throws SQLException {
        return ("");
    }

    public ResultSet    getFunctionColumns (
        String              catalog,
        String              schemaPattern,
        String              functionNamePattern,
        String              columnNamePattern
    )
        throws SQLException
    {
        System.out.println (
            "getFunctionColumns (" + catalog + ", " + schemaPattern + ", " +
            functionNamePattern + ", " + columnNamePattern + ")"
        );

        return (new ResultSetImpl ());
    }

    public ResultSet    getFunctions (
        String              catalog,
        String              schemaPattern,
        String              functionNamePattern
    )
        throws SQLException
    {
        System.out.println (
            "getFunctions (" + catalog + ", " + schemaPattern + ", " +
            functionNamePattern + ")"
        );

        return (new ResultSetImpl ());
    }

    public String       getIdentifierQuoteString () throws SQLException {
        return ("\"");
    }

    public ResultSet    getImportedKeys (
        String              catalog,
        String              schema,
        String              table
    )
        throws SQLException
    {
        System.out.println (
            "getImportedKeys (" + catalog + ", " + schema + ", " +
            table + ")"
        );

        return (new ResultSetImpl ());
    }

    public ResultSet    getIndexInfo (
        String              catalog,
        String              schema,
        String              table,
        boolean             unique,
        boolean             approximate
    )
        throws SQLException
    {
        System.out.println (
            "getImportedKeys (" + catalog + ", " + schema + ", " +
            table + ", " + unique + ", " + approximate + ")"
        );

        return (new ResultSetImpl ());
    }

    public int          getJDBCMajorVersion () throws SQLException {
        return (4);
    }

    public int          getJDBCMinorVersion () throws SQLException {
        return (0);
    }

    public int          getMaxBinaryLiteralLength () throws SQLException {
        return (0);
    }

    public int          getMaxCatalogNameLength () throws SQLException {
        return (0);
    }

    public int          getMaxCharLiteralLength () throws SQLException {
        return (10000);
    }

    public int          getMaxColumnNameLength () throws SQLException {
        return (1000);
    }

    public int          getMaxColumnsInGroupBy () throws SQLException {
        return (0);
    }

    public int          getMaxColumnsInIndex () throws SQLException {
        return (0);
    }

    public int          getMaxColumnsInOrderBy () throws SQLException {
        return (0);
    }

    public int          getMaxColumnsInSelect () throws SQLException {
        return (0);
    }

    public int          getMaxColumnsInTable () throws SQLException {
        return (0);
    }

    public int          getMaxConnections () throws SQLException {
        return (0);
    }

    public int          getMaxCursorNameLength () throws SQLException {
        return (0);
    }

    public int          getMaxIndexLength () throws SQLException {
        return (0);
    }

    public int          getMaxProcedureNameLength () throws SQLException {
        return (0);
    }

    public int          getMaxRowSize () throws SQLException {
        return (255);
    }

    public int          getMaxSchemaNameLength () throws SQLException {
        return (0);
    }

    public int          getMaxStatementLength () throws SQLException {
        return (0);
    }

    public int          getMaxStatements () throws SQLException {
        return (0);
    }

    public int          getMaxTableNameLength () throws SQLException {
        return (1000);
    }

    public int          getMaxTablesInSelect () throws SQLException {
        return (1000);
    }

    public int          getMaxUserNameLength () throws SQLException {
        return (1000);
    }

    public String       getNumericFunctions () throws SQLException {
        return ("");
    }

    public ResultSet    getPrimaryKeys (
        String              catalog,
        String              schema,
        String              table
    )
        throws SQLException
    {
        System.out.println (
            "getPrimaryKeys (" + catalog + ", " + schema + ", " +
            table + ")"
        );

        return (new ResultSetImpl ());
    }

    public ResultSet    getProcedureColumns (
        String              catalog,
        String              schemaPattern,
        String              procedureNamePattern,
        String              columnNamePattern
    )
        throws SQLException
    {
        System.out.println (
            "getProcedureColumns (" + catalog + ", " + schemaPattern + ", " +
            procedureNamePattern + ", " + columnNamePattern + ")"
        );

        return (new ResultSetImpl ());
    }

    public String       getProcedureTerm () throws SQLException {
        return ("FUNCTION");
    }

    public ResultSet    getProcedures (
        String              catalog,
        String              schemaPattern,
        String              procedureNamePattern
    )
        throws SQLException
    {
        System.out.println (
            "getProcedures (" + catalog + ", " + schemaPattern + ", " +
            procedureNamePattern + ")"
        );

        return (new ResultSetImpl ());
    }

    public int          getResultSetHoldability () throws SQLException {
        return (ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }

    public RowIdLifetime getRowIdLifetime () throws SQLException {
        return (RowIdLifetime.ROWID_UNSUPPORTED);
    }

    public String       getSQLKeywords () throws SQLException {
        return ("");
    }

    public int          getSQLStateType () throws SQLException {
        return (sqlStateSQL);
    }

    public String       getSchemaTerm () throws SQLException {
        return ("SCHEMA");
    }

    public ResultSet    getSchemas () throws SQLException {
        System.out.println ("getSchemas ()");

        return (
            new MemResultSetImpl (
                new String [] { "TABLE_SCHEM", "TABLE_CATALOG" }
            )
        );
    }

/*
The schema columns are:

   1. TABLE_SCHEM String => schema name
   2. TABLE_CATALOG String => catalog name (may be null)
*/
    public ResultSet    getSchemas (String catalog, String schemaPattern)
        throws SQLException
    {
        System.out.println (
            "getSchemas (" + catalog + ", " + schemaPattern + ")"
        );

        return (
            new MemResultSetImpl (
                new String [] { "TABLE_SCHEM", "TABLE_CATALOG" }
            )
        );
    }

    public String       getSearchStringEscape () throws SQLException {
        return ("\\");
    }

    public String       getStringFunctions () throws SQLException {
        return ("");
    }

    public ResultSet    getSuperTables (
        String              catalog,
        String              schemaPattern,
        String              tableNamePattern
    )
        throws SQLException
    {
        System.out.println (
            "getSuperTables (" + catalog + ", " + schemaPattern + ", " +
            tableNamePattern + ")"
        );

        return (new ResultSetImpl ());
    }

    public ResultSet    getSuperTypes (
        String              catalog,
        String              schemaPattern,
        String              typeNamePattern
    )
        throws SQLException
    {
        System.out.println (
            "getSuperTypes (" + catalog + ", " + schemaPattern + ", " +
            typeNamePattern + ")"
        );

        return (new ResultSetImpl ());
    }

    public String       getSystemFunctions () throws SQLException {
        return ("");
    }

    public ResultSet    getTablePrivileges (
        String              catalog,
        String              schemaPattern,
        String              tableNamePattern
    )
        throws SQLException
    {
        System.out.println (
            "getTablePrivileges (" + catalog + ", " + schemaPattern + ", " +
            tableNamePattern + ")"
        );

        return (new ResultSetImpl ());
    }

/*
Retrieves the table types available in this database. The results are ordered by
table type.

The table type is:

   1. TABLE_TYPE String => table type.
        Typical types are "TABLE", "VIEW", "SYSTEM TABLE",
        "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
*/
    public ResultSet    getTableTypes () throws SQLException {
        System.out.println ("getTableTypes ()");

        return (
            new MemResultSetImpl(
                new String [] { "TABLE_TYPE" },
                new Object [] { "TABLE" }
            )
        );
    }

    /*
 Each table description has the following columns:

   1. TABLE_CAT String => table catalog (may be null)
   2. TABLE_SCHEM String => table schema (may be null)
   3. TABLE_NAME String => table name
   4. TABLE_TYPE String => table type. Typical types are "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
   5. REMARKS String => explanatory comment on the table
   6. TYPE_CAT String => the types catalog (may be null)
   7. TYPE_SCHEM String => the types schema (may be null)
   8. TYPE_NAME String => type name (may be null)
   9. SELF_REFERENCING_COL_NAME String => name of the designated "identifier" column of a typed table (may be null)
  10. REF_GENERATION String => specifies how values in SELF_REFERENCING_COL_NAME are created. Values are "SYSTEM", "USER", "DERIVED". (may be null)
     */
    private static String []    TABLE_LABELS = {
        "TABLE_CAT",
        "TABLE_SCHEM",
        "TABLE_NAME",
        "TABLE_TYPE",
        "REMARKS",
        "TYPE_CAT",
        "TYPE_SCHEM",
        "TYPE_NAME",
        "SELF_REFERENCING_COL_NAME",
        "REF_GENERATION"
    };

    public ResultSet    getTables (
        String              catalog,
        String              schemaPattern,
        String              tableNamePattern,
        String []           types
    )
        throws SQLException
    {
        System.out.println (
            "getTables (" + catalog + ", " + schemaPattern + ", " +
            tableNamePattern + ", " + types + ")"
        );

        DXTickStream []     streams = conn.db.listStreams ();
        int                 num = streams.length;
        Object [][]         rows = new Object [num][];
        
        for (int ii = 0; ii < num; ii++) {
            DXTickStream    stream = streams [ii];

            rows [ii] =
                new Object [] {
                    null,
                    null,
                    stream.getKey (),
                    "TABLE",
                    stream.getDescription (),
                    null,
                    null,
                    null,   // type name...
                    null,
                    null
                };
        }

        return (new MemResultSetImpl (TABLE_LABELS, rows));
    }

    public String       getTimeDateFunctions () throws SQLException {
        return ("");
    }

/*
Retrieves a description of all the data types supported by this database.
They are ordered by DATA_TYPE and then by how closely the data type maps to the
corresponding JDBC SQL type.

If the database supports SQL distinct types, then getTypeInfo() will return a
single row with a TYPE_NAME of DISTINCT and a DATA_TYPE of Types.DISTINCT.
If the database supports SQL structured types, then getTypeInfo() will return a
single row with a TYPE_NAME of STRUCT and a DATA_TYPE of Types.STRUCT.

If SQL distinct or structured types are supported, then information on the
individual types may be obtained from the getUDTs() method.

Each type description has the following columns:

   1. TYPE_NAME String => Type name
   2. DATA_TYPE int => SQL data type from java.sql.Types
   3. PRECISION int => maximum precision
   4. LITERAL_PREFIX String => prefix used to quote a literal (may be null)
   5. LITERAL_SUFFIX String => suffix used to quote a literal (may be null)
   6. CREATE_PARAMS String => parameters used in creating the type (may be null)
   7. NULLABLE short => can you use NULL for this type.
          * typeNoNulls - does not allow NULL values
          * typeNullable - allows NULL values
          * typeNullableUnknown - nullability unknown
   8. CASE_SENSITIVE boolean=> is it case sensitive.
   9. SEARCHABLE short => can you use "WHERE" based on this type:
          * typePredNone - No support
          * typePredChar - Only supported with WHERE .. LIKE
          * typePredBasic - Supported except for WHERE .. LIKE
          * typeSearchable - Supported for all WHERE ..
  10. UNSIGNED_ATTRIBUTE boolean => is it unsigned.
  11. FIXED_PREC_SCALE boolean => can it be a money value.
  12. AUTO_INCREMENT boolean => can it be used for an auto-increment value.
  13. LOCAL_TYPE_NAME String => localized version of type name (may be null)
  14. MINIMUM_SCALE short => minimum scale supported
  15. MAXIMUM_SCALE short => maximum scale supported
  16. SQL_DATA_TYPE int => unused
  17. SQL_DATETIME_SUB int => unused
  18. NUM_PREC_RADIX int => usually 2 or 10

The PRECISION column represents the maximum column size that the server supports
for the given datatype. For numeric data, this is the maximum precision. For
character data, this is the length in characters. For datetime datatypes, this
is the length in characters of the String representation (assuming the maximum
allowed precision of the fractional seconds component). For binary data, this is
the length in bytes. For the ROWID datatype, this is the length in bytes. Null
is returned for data types where the column size is not applicable.
*/
    private static String []    TYPE_LABELS = {
        "TYPE_NAME",
        "DATA_TYPE",
        "PRECISION",
        "LITERAL_PREFIX",
        "LITERAL_SUFFIX",
        "CREATE_PARAMS",
        "NULLABLE",
        "CASE_SENSITIVE",
        "SEARCHABLE",
        "UNSIGNED_ATTRIBUTE",
        "FIXED_PREC_SCALE",
        "AUTO_INCREMENT",
        "LOCAL_TYPE_NAME",
        "MINIMUM_SCALE",
        "MAXIMUM_SCALE",
        "SQL_DATA_TYPE",
        "SQL_DATETIME_SUB",
        "NUM_PREC_RADIX"
    };

    public ResultSet    getTypeInfo () throws SQLException {
        System.out.println ("getTypeInfo ()");

        return (
            new MemResultSetImpl (
                TYPE_LABELS,
                new Object [][] {
                    { "BOOLEAN",    Types.BOOLEAN, null, null, null, null, typeNoNulls,
                          false, true, false, false, false, "Boolean", null, null, null, null, null },
                    { "INT8",       Types.INTEGER, 8, null, null, null, typeNoNulls,
                          false, true, false, false, false, "Signed Byte", null, null, null, null, 2 },
                    { "INT16",      Types.INTEGER, 16, null, null, null, typeNoNulls,
                          false, true, false, false, false, "Signed Byte", null, null, null, null, 2 },
                    { "VARCHAR",    Types.VARCHAR, null, "'", "'", null, typeNullable,
                          true, true, false, false, false, "String", null, null, null, null, null },
                }
            )
        );
    }

    public ResultSet    getUDTs (
        String              catalog,
        String              schemaPattern,
        String              typeNamePattern,
        int []              types
    )
        throws SQLException
    {
        System.out.println (
            "getUDTs (" + catalog + ", " + schemaPattern + ", " +
            typeNamePattern + ", " + types + ")"
        );

        return (new ResultSetImpl ());
    }

    public String       getURL () throws SQLException {
        return (QuantServerSqlDriver.PROTOCOL_PREFIX + conn.url);
    }

    public String       getUserName () throws SQLException {
        return ("");
    }

    public ResultSet    getVersionColumns (
        String              catalog,
        String              schema,
        String              table
    )
        throws SQLException
    {
        System.out.println (
            "getUDTs (" + catalog + ", " + schema + ", " +
            table + ")"
        );

        return (new ResultSetImpl ());
    }

    public boolean      insertsAreDetected (int type) throws SQLException {
        return (false);
    }

    public boolean      isCatalogAtStart () throws SQLException {
        return (true);
    }

    public boolean      isReadOnly () throws SQLException {
        return (conn.db.isReadOnly ());
    }

    public boolean      locatorsUpdateCopy () throws SQLException {
        return (false);
    }

    public boolean      nullPlusNonNullIsNull () throws SQLException {
        return (true);
    }

    public boolean      nullsAreSortedAtEnd () throws SQLException {
        return (false);
    }

    public boolean      nullsAreSortedAtStart () throws SQLException {
        return (false);
    }

    public boolean      nullsAreSortedHigh () throws SQLException {
        return (false);
    }

    public boolean      nullsAreSortedLow () throws SQLException {
        return (true);
    }

    public boolean      othersDeletesAreVisible (int type) throws SQLException {
        return (true);
    }

    public boolean      othersInsertsAreVisible (int type) throws SQLException {
        return (true);
    }

    public boolean      othersUpdatesAreVisible (int type) throws SQLException {
        return (true);
    }

    public boolean      ownDeletesAreVisible (int type) throws SQLException {
        return (true);
    }

    public boolean      ownInsertsAreVisible (int type) throws SQLException {
        return (true);
    }

    public boolean      ownUpdatesAreVisible (int type) throws SQLException {
        return (true);
    }

    public boolean      storesLowerCaseIdentifiers () throws SQLException {
        return (false);
    }

    public boolean      storesLowerCaseQuotedIdentifiers () throws SQLException {
        return (false);
    }

    public boolean      storesMixedCaseIdentifiers () throws SQLException {
        return (false);
    }

    public boolean      storesMixedCaseQuotedIdentifiers () throws SQLException {
        return (true);
    }

    public boolean      storesUpperCaseIdentifiers () throws SQLException {
        return (true);
    }

    public boolean      storesUpperCaseQuotedIdentifiers () throws SQLException {
        return (false);
    }

    public boolean      supportsANSI92EntryLevelSQL () throws SQLException {
        return (false);
    }

    public boolean      supportsANSI92FullSQL () throws SQLException {
        return (false);
    }

    public boolean      supportsANSI92IntermediateSQL () throws SQLException {
        return (false);
    }

    public boolean      supportsAlterTableWithAddColumn () throws SQLException {
        return (false);
    }

    public boolean      supportsAlterTableWithDropColumn () throws SQLException {
        return (false);
    }

    public boolean      supportsBatchUpdates () throws SQLException {
        return (true);
    }

    public boolean      supportsCatalogsInDataManipulation () throws SQLException {
        return (false);
    }

    public boolean      supportsCatalogsInIndexDefinitions () throws SQLException {
        return (false);
    }

    public boolean      supportsCatalogsInPrivilegeDefinitions () throws SQLException {
        return (false);
    }

    public boolean      supportsCatalogsInProcedureCalls () throws SQLException {
        return (false);
    }

    public boolean      supportsCatalogsInTableDefinitions () throws SQLException {
        return (false);
    }

    public boolean      supportsColumnAliasing () throws SQLException {
        return (true);
    }

    public boolean      supportsConvert () throws SQLException {
        return (false);
    }

    public boolean      supportsConvert (int fromType, int toType)
        throws SQLException
    {
        return (false);
    }

    public boolean      supportsCoreSQLGrammar () throws SQLException {
        return (false);
    }

    public boolean      supportsCorrelatedSubqueries () throws SQLException {
        return (false);
    }

    public boolean      supportsDataDefinitionAndDataManipulationTransactions ()
        throws SQLException
    {
        return (false);
    }

    public boolean      supportsDataManipulationTransactionsOnly () throws SQLException {
        return (false);
    }

    public boolean      supportsDifferentTableCorrelationNames () throws SQLException {
        return (false);
    }

    public boolean      supportsExpressionsInOrderBy () throws SQLException {
        return (false);
    }

    public boolean      supportsExtendedSQLGrammar () throws SQLException {
        return (false);
    }

    public boolean      supportsFullOuterJoins () throws SQLException {
        return (false);
    }

    public boolean      supportsGetGeneratedKeys () throws SQLException {
        return (false);
    }

    public boolean      supportsGroupBy () throws SQLException {
        return (false);
    }

    public boolean      supportsGroupByBeyondSelect () throws SQLException {
        return (false);
    }

    public boolean      supportsGroupByUnrelated () throws SQLException {
        return (false);
    }

    public boolean      supportsIntegrityEnhancementFacility () throws SQLException {
        return (false);
    }

    public boolean      supportsLikeEscapeClause () throws SQLException {
        return (false);
    }

    public boolean      supportsLimitedOuterJoins () throws SQLException {
        return (false);
    }

    public boolean      supportsMinimumSQLGrammar () throws SQLException {
        return (false);
    }

    public boolean      supportsMixedCaseIdentifiers () throws SQLException {
        return (true);
    }

    public boolean      supportsMixedCaseQuotedIdentifiers () throws SQLException {
        return (true);
    }

    public boolean      supportsMultipleOpenResults () throws SQLException {
        return (true);
    }

    public boolean      supportsMultipleResultSets () throws SQLException {
        return (true);
    }

    public boolean      supportsMultipleTransactions () throws SQLException {
        return (false);
    }

    public boolean      supportsNamedParameters () throws SQLException {
        return (false);
    }

    public boolean      supportsNonNullableColumns () throws SQLException {
        return (false);
    }

    public boolean      supportsOpenCursorsAcrossCommit () throws SQLException {
        return (true);
    }

    public boolean      supportsOpenCursorsAcrossRollback () throws SQLException {
        return (true);
    }

    public boolean      supportsOpenStatementsAcrossCommit () throws SQLException {
        return (true);
    }

    public boolean      supportsOpenStatementsAcrossRollback () throws SQLException {
        return (true);
    }

    public boolean      supportsOrderByUnrelated () throws SQLException {
        return (false);
    }

    public boolean      supportsOuterJoins () throws SQLException {
        return (false);
    }

    public boolean      supportsPositionedDelete () throws SQLException {
        return (false);
    }

    public boolean      supportsPositionedUpdate () throws SQLException {
        return (false);
    }

    public boolean      supportsResultSetConcurrency (int type, int concurrency) 
        throws SQLException
    {
        return (concurrency == ResultSet.CONCUR_READ_ONLY);
    }

    public boolean      supportsResultSetHoldability (int holdability) throws SQLException {
        return (holdability == ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }

    public boolean      supportsResultSetType (int type) throws SQLException {
        return (type == ResultSet.TYPE_FORWARD_ONLY);
    }

    public boolean      supportsSavepoints () throws SQLException {
        return (true);
    }

    public boolean      supportsSchemasInDataManipulation () throws SQLException {
        return (false);
    }

    public boolean      supportsSchemasInIndexDefinitions () throws SQLException {
        return (false);
    }

    public boolean      supportsSchemasInPrivilegeDefinitions () throws SQLException {
        return (false);
    }

    public boolean      supportsSchemasInProcedureCalls () throws SQLException {
        return (false);
    }

    public boolean      supportsSchemasInTableDefinitions () throws SQLException {
        return (false);
    }

    public boolean      supportsSelectForUpdate () throws SQLException {
        return (false);
    }

    public boolean      supportsStatementPooling () throws SQLException {
        return (false);
    }

    public boolean      supportsStoredFunctionsUsingCallSyntax () throws SQLException {
        return (false);
    }

    public boolean      supportsStoredProcedures () throws SQLException {
        return (false);
    }

    public boolean      supportsSubqueriesInComparisons () throws SQLException {
        return (false);
    }

    public boolean      supportsSubqueriesInExists () throws SQLException {
        return (false);
    }

    public boolean      supportsSubqueriesInIns () throws SQLException {
        return (false);
    }

    public boolean      supportsSubqueriesInQuantifieds () throws SQLException {
        return (false);
    }

    public boolean      supportsTableCorrelationNames () throws SQLException {
        return (false);
    }

    public boolean      supportsTransactionIsolationLevel (int level) throws SQLException {
        return (level == Connection.TRANSACTION_READ_COMMITTED);
    }

    public boolean      supportsTransactions () throws SQLException {
        return (true);
    }

    public boolean      supportsUnion () throws SQLException {
        return (false);
    }

    public boolean      supportsUnionAll () throws SQLException {
        return (false);
    }

    public boolean      updatesAreDetected (int type) throws SQLException {
        return (false);
    }

    public boolean      usesLocalFilePerTable () throws SQLException {
        return (true);
    }

    public boolean      usesLocalFiles () throws SQLException {
        return (true);
    }

    // Java 1.7

    public ResultSet getPseudoColumns(String catalog,
                         String schemaPattern,
                         String tableNamePattern,
                         String columnNamePattern) {
        throw new UnsupportedOperationException();
    }

    public boolean generatedKeyAlwaysReturned() {
        throw new UnsupportedOperationException();
    }

}
