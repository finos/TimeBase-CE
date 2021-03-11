package com.epam.deltix.qsrv.hf.pub;

import java.io.OutputStream;
import java.io.InputStream;

import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;

/**
 *  Read access to a value.
 */
public interface ReadableValue {
    /**
     * Always returns <code>false</code> for non-nullable field.
     */
    public boolean              isNull ();

    public boolean              getBoolean () throws NullValueException;

    public char                 getChar () throws NullValueException;

    public int                  getInt () throws NullValueException;

    public long                 getLong () throws NullValueException;

    public float                getFloat () throws NullValueException;

    public double               getDouble () throws NullValueException;

    public String               getString () throws NullValueException;

    public int                  getArrayLength () throws NullValueException;

    public ReadableValue        nextReadableElement() throws NullValueException;

    /** Used to read nested objects */
    public UnboundDecoder       getFieldDecoder() throws NullValueException;

    public int                  getBinaryLength () throws NullValueException;

    public void                 getBinary (int offset, int length, OutputStream out)
        throws NullValueException;

    public void                 getBinary (int srcOffset, int length, byte [] dest, int destOffset)
        throws NullValueException;

    public InputStream          openBinary ()
        throws NullValueException;
}
