/*
 * Copyright 2021 EPAM Systems, Inc
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
package com.epam.deltix.util.jdbc;

import com.epam.deltix.util.collections.generated.ObjectToIntegerHashMap;
import java.io.*;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;

/**
 *
 */
public class MemResultSetImpl extends ResultSetImpl {
    private final ResultSetMetaData md;
    private final ObjectToIntegerHashMap <String>  labelTo1_BasedIndex =
        new ObjectToIntegerHashMap <> ();
    private final Object [][]       rows;
    private int                     idx = -1;
    private boolean                 wasNull = false;
    
    public MemResultSetImpl (String [] labels, Object [] ... rows) {
        this.rows = rows;

        for (int ii = 0; ii < labels.length; ii++)
            labelTo1_BasedIndex.put (labels [ii], ii + 1);

        md = null;
    }

    public MemResultSetImpl (ResultSetMetaData md, Object [] ... rows)
        throws SQLException
    {
        this.rows = rows;
        this.md = md;
        
        int         n = md.getColumnCount ();

        for (int ii = 1; ii <= n; ii++)
            labelTo1_BasedIndex.put (md.getColumnLabel (ii), ii);
    }

    //########################################################################
    //                  NAVIGATION
    //########################################################################
    private boolean     inRange () {
        return (idx >= 0 && idx < rows.length);
    }

    @Override
    public boolean      absolute (int row) throws SQLException {
        idx = row == -1 ? rows.length - 1 : row - 1;
        return (inRange ());
    }

    @Override
    public boolean      first () throws SQLException {
        idx = 0;
        return (inRange ());
    }

    @Override
    public int          getRow () throws SQLException {
        return idx + 1;
    }

    @Override
    public boolean      last () throws SQLException {
        idx = rows.length - 1;
        return (inRange ());
    }

    @Override
    public boolean      next () throws SQLException {
        idx++;
        return (inRange ());
    }

    @Override
    public boolean      previous () throws SQLException {
        idx--;
        return (inRange ());
    }

    @Override
    public boolean      relative (int rows) throws SQLException {
        idx += rows;
        return (inRange ());
    }

    @Override
    public void         afterLast () throws SQLException {
        idx = rows.length;
    }

    @Override
    public void         beforeFirst () throws SQLException {
        idx = -1;
    }

    @Override
    public boolean      isAfterLast () throws SQLException {
        return (idx >= rows.length);
    }

    @Override
    public boolean      isBeforeFirst () throws SQLException {
        return (idx < 0);
    }

    @Override
    public boolean      isFirst () throws SQLException {
        return (idx == 0);
    }

    @Override
    public boolean      isLast () throws SQLException {
        return (idx == rows.length - 1);
    }

    //########################################################################
    //                  GET DATA
    //########################################################################
    @Override
    public Object       getObject (int columnIndex) throws SQLException {
        Object      obj = rows [idx][columnIndex - 1];

        if (obj == null)
            wasNull = true;

        return (obj);
    }

    @Override
    public Object       getObject (String columnLabel) throws SQLException {
        return (getObject (findColumn (columnLabel)));
    }

    @Override
    public boolean      wasNull () throws SQLException {
        return (wasNull);
    }
    
    @Override
    public boolean      getBoolean (int columnIndex) throws SQLException {
        Object      obj = getObject (columnIndex);

        return (obj == null ? false : ((Boolean) obj).booleanValue ());
    }

    @Override
    public boolean      getBoolean (String columnLabel) throws SQLException {
        return (getBoolean (findColumn (columnLabel)));
    }

    @Override
    public byte         getByte (int columnIndex) throws SQLException {
        Object      obj = getObject (columnIndex);

        return (obj == null ? 0 : ((Number) obj).byteValue ());
    }

    @Override
    public byte         getByte (String columnLabel) throws SQLException {
        return (getByte (findColumn (columnLabel)));
    }

    @Override
    public Reader       getCharacterStream (int columnIndex) throws SQLException {
        return (new StringReader (getString (columnIndex)));
    }

    @Override
    public Reader       getCharacterStream (String columnLabel) throws SQLException {
        return (getCharacterStream (findColumn (columnLabel)));
    }

    @Override
    public Date         getDate (int columnIndex) throws SQLException {
        Object      obj = getObject (columnIndex);

        return (obj == null ? null : (Date) obj);
    }

    @Override
    public Date         getDate (String columnLabel) throws SQLException {
        return (getDate (findColumn (columnLabel)));
    }

    @Override
    public Date         getDate (int columnIndex, Calendar cal) throws SQLException {
        return getDate (columnIndex);
    }

    @Override
    public Date         getDate (String columnLabel, Calendar cal) throws SQLException {
        return (getDate (findColumn (columnLabel), cal));
    }

    @Override
    public double       getDouble (int columnIndex) throws SQLException {
        Object      obj = getObject (columnIndex);

        return (obj == null ? Double.NaN : ((Number) obj).doubleValue ());
    }

    @Override
    public double       getDouble (String columnLabel) throws SQLException {
        return (getDouble (findColumn (columnLabel)));
    }

    @Override
    public float        getFloat (int columnIndex) throws SQLException {
        Object      obj = getObject (columnIndex);

        return (obj == null ? Float.NaN : ((Number) obj).floatValue ());
    }

    @Override
    public float        getFloat (String columnLabel) throws SQLException {
        return (getFloat (findColumn (columnLabel)));
    }

    @Override
    public int          getInt (int columnIndex) throws SQLException {
        Object      obj = getObject (columnIndex);

        return (obj == null ? 0 : ((Number) obj).intValue ());
    }

    @Override
    public int          getInt (String columnLabel) throws SQLException {
        return (getInt (findColumn (columnLabel)));
    }

    @Override
    public long         getLong (int columnIndex) throws SQLException {
        Object      obj = getObject (columnIndex);

        return (obj == null ? 0 : ((Number) obj).longValue ());
    }

    @Override
    public long         getLong (String columnLabel) throws SQLException {
        return (getLong (findColumn (columnLabel)));
    }

    @Override
    public Reader       getNCharacterStream (int columnIndex) throws SQLException {
        return (getCharacterStream (columnIndex));
    }

    @Override
    public Reader       getNCharacterStream (String columnLabel) throws SQLException {
        return (getCharacterStream (columnLabel));
    }

    @Override
    public String       getNString (int columnIndex) throws SQLException {
        return (getString (columnIndex));
    }

    @Override
    public String       getNString (String columnLabel) throws SQLException {
        return (getString (columnLabel));
    }

    @Override
    public short        getShort (int columnIndex) throws SQLException {
        Object      obj = getObject (columnIndex);

        return (obj == null ? 0 : ((Number) obj).shortValue ());
    }

    @Override
    public short        getShort (String columnLabel) throws SQLException {
        return (getShort (findColumn (columnLabel)));
    }

    @Override
    public String       getString (int columnIndex) throws SQLException {
        Object      obj = getObject (columnIndex);

        return (obj == null ? null : obj.toString ());
    }

    @Override
    public String       getString (String columnLabel) throws SQLException {
        return (getString (findColumn (columnLabel)));
    }

    @Override
    public URL          getURL (int columnIndex) throws SQLException {
        Object      obj = getObject (columnIndex);

        return (obj == null ? null : (URL) obj);
    }

    @Override
    public URL          getURL (String columnLabel) throws SQLException {
        return (getURL (findColumn (columnLabel)));
    }

    @Override
    public InputStream getUnicodeStream (int columnIndex) throws SQLException {
        return super.getUnicodeStream (columnIndex);
    }

    @Override
    public InputStream getUnicodeStream (String columnLabel) throws SQLException {
        return super.getUnicodeStream (columnLabel);
    }

    //########################################################################
    //                  META DATA
    //########################################################################
    @Override
    public ResultSetMetaData getMetaData () {
        return (md);
    }

    @Override
    public int              findColumn (String columnLabel) throws SQLException {
        int col = labelTo1_BasedIndex.get (columnLabel, 1);
        
        if (col < 1)
            throw new SQLException ("Column not found: " + columnLabel);
        
        return (col);
    }        
}