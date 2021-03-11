package com.epam.deltix.qsrv.hf.pub.md;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 */
public final class QueryDataType extends DataType {
    private final ClassDataType         output;

    public QueryDataType (boolean nullable, ClassDataType output) {
        super (null, nullable);
        this.output = output;
    }

    public ClassDataType                getOutputType () {
        return output;
    }

    @Override
    public String                       getBaseName () {
        return ("SOURCE");
    }

    @Override
    public int                          getCode() {
        throw new UnsupportedOperationException ();
    }

    @Override
    public ConversionType               isConvertible (DataType to) {
        return (ConversionType.NotConvertible);
    }

    @Override
    protected void                      assertValidImpl (Object obj) {
        throw unsupportedType (obj);
    }

    @Override
    protected Object                    toBoxedImpl (CharSequence text) {
        throw new UnsupportedOperationException ();
    }
    
    @Override
    protected String                    toStringImpl (Object obj) {
        throw new UnsupportedOperationException ();
    }
    
    @Override
    public void                         writeTo (DataOutputStream out)
        throws IOException
    {
        throw new UnsupportedOperationException ();
    }
}
