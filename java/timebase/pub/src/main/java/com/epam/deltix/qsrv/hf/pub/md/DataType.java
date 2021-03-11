package com.epam.deltix.qsrv.hf.pub.md;

import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor.TypeResolver;
import java.io.*;
import java.text.ParseException;

import javax.xml.bind.annotation.XmlElement;

import com.epam.deltix.util.time.GMT;
import static com.epam.deltix.qsrv.hf.pub.util.SerializationUtils.*;

/**
 *  
 */
public abstract class DataType implements Serializable, Cloneable {
    public static final int  T_BINARY_TYPE =         1;
    public static final int  T_CHAR_TYPE =           2;
    public static final int  T_STRING_TYPE =         3;
    public static final int  T_DATE_TIME_TYPE =      4;
    public static final int  T_BOOLEAN_TYPE =        5;
    public static final int  T_TIME_OF_DAY_TYPE =    6;
    public static final int  T_INTEGER_TYPE =        7;
    public static final int  T_FLOAT_TYPE =          8;
    public static final int  T_ENUM_TYPE =           9;
    public static final int  T_OBJECT_TYPE =         10;
    public static final int  T_ARRAY_TYPE =          11;
    public static final int  T_DOUBLE_TYPE =         12; // not actually used in protocol

	private static final long serialVersionUID = 1L;

    @XmlElement (name = "encoding")
    protected String          encoding;

    @XmlElement (name = "nullable")
    private boolean           nullable;

    protected DataType() {
        encoding = null;
        nullable = false;
    }

    protected DataType(String encoding, boolean nullable) {
        parseEncoding(encoding);
        this.encoding = encoding;
        this.nullable = nullable;
    }

    @Override
    public DataType                 clone () {
        try {
            return ((DataType) super.clone ());
        } catch (CloneNotSupportedException x) {
            throw new RuntimeException (x);
        }
    }
    
    public final DataType           nullableInstance (boolean nullable) {
        if (this.nullable == nullable)
            return (this);
        
        
        DataType    copy = clone ();
        copy.nullable = nullable;
        return (copy);        
    }
    
    public final String             getEncoding() {
        return encoding;
    }

    public boolean                  isNullable() {
        return nullable;
    }

    public void                     parseEncoding(String encoding) {
        if (encoding != null && encoding.length() > 0)
            throw new IllegalArgumentException(encoding);
    }

    public static boolean           parseBoolean (String s) {
        return (Boolean.parseBoolean (s));
    }
    
    public static byte              parseByte (String s) {
        return (Byte.parseByte (s));
    }

    public static short             parseShort (String s) {
        return (Short.parseShort (s));
    }

    public static int               parseInt (String s) {
        return (Integer.parseInt (s));
    }
    
    public static long              parseLong (String s) {
        return (Long.parseLong (s));
    }
    
    public static double            parseDouble (String s) {
        return (Double.parseDouble (s));
    }
    
    public static float             parseFloat (String s) {
        return (Float.parseFloat (s));
    }
    
    public static long              parseDate (String s) {
        try {            
            return (GMT.parseDateTimeMillis (s).getTime ());
        } catch (ParseException x) {
            throw new NumberFormatException (s);
        }
    }

    public abstract ConversionType  isConvertible(DataType to);

    /**
     *  Parse non-null text and return an object without checking constraints.
     */
    protected abstract Object       toBoxedImpl (CharSequence text);
    
    /**
     *  Converts a non-null, constraint-compliant Boxed value to String.
     * 
     *  @param obj  A non-null, valid value.
     * 
     *  @return     A String representation.
     */
    protected abstract String       toStringImpl (Object obj);
    
    /**
     *  Helper method to construct an exception thrown when object type  
     *  passed to {@link #toString} or {@link #assertValid} is completely illegal.
     * 
     *  @param obj  The culprit.
     *  @return     An exception to throw.
     */
    protected final IllegalArgumentException unsupportedType (Object obj) {
        return (new IllegalArgumentException(obj + " is of the wrong type (" + obj.getClass().getName() + ") for " + this.getClass().getName()));
    }
    
    /**
     *  Helper method to construct an exception thrown when a value  
     *  passed to {@link #toString} or {@link #assertValid} is out of range.
     * 
     *  @param obj  The culprit.
     *  @param min  Inclusive minimum.
     *  @param max  Inclusive maximum.
     *  @return     An exception to throw.
     */
    protected final IllegalArgumentException outOfRange (Object obj, Object min, Object max) {
        return (
            new IllegalArgumentException (
                this + " value " + obj + " is out of allowed range: [" + 
                min + " .. " + max + "]"
            )
        );
    }        
    
    /**
     *  Checks a non-null Boxed value against constraints, if any. If the object
     *  is of unsupported type, this method should throw {@link #unsupportedType}.
     * 
     *  @param obj  A Boxed representation of a value.
     * 
     *  @exception IllegalArgumentException     If constraints are violated.
     */
    protected abstract void         assertValidImpl (Object obj);
    
    /**
     *  Convert the String representation to Boxed, also validating it against
     *  constraints, if any.
     *  <p>
     *  Boxed return type should be the same independently from encoding. For example: Long for all INTEGER encodings INT8, INT16 ... INT64
     *  </p>
     *  @param value    String representation.
     *  @return         Boxed representation or null if the argument is null.
     */
    public final Object             parse (CharSequence value) {
        if (value == null) {
            if (!nullable)
                throw new IllegalArgumentException (this + " is not nullable");
            
            return (null);
        }
        
        Object  ret = toBoxedImpl (value);
        
        assertValidImpl (ret);
        
        return (ret);
    }

    public abstract int                      getCode();
   
    /**
     *  Convert the Boxed representation to String, also validating it against
     *  constraints, if any.
     * 
     *  @param obj
     */
    public final String             toString (Object obj) {
        assertValid (obj);
        
        if (obj == null)
            return (null);
        
        return (toStringImpl (obj));
    }
    
    /**
     *  Checks a Boxed value against constraints, if any. 
     *  Overriding methods <b>must</b> call the superclass implementation first.
     * 
     *  @param obj  A Boxed representation of a value.
     * 
     *  @exception IllegalArgumentException     If constraints are violated.
     */
    public final void               assertValid (Object obj) {
        if (obj == null) 
            if (!nullable)
                throw new IllegalArgumentException (this + " is not nullable");
            else
                return;
        else
            assertValidImpl (obj);
    }
    
    public abstract String          getBaseName ();

    public enum ConversionType {
        Lossless, Lossy, NotConvertible
    }

    public void                     writeTo (DataOutputStream out)
        throws IOException
    {
        out.writeBoolean (nullable);
        writeNullableString (encoding, out);
    }

    protected void                  readFields (
        DataInputStream                 in,
        TypeResolver                    resolver
    )
        throws IOException
    {
        nullable = in.readBoolean ();
        encoding = readNullableString (in);
    }

    public static DataType          readFrom (
        DataInputStream                 in,
        TypeResolver                    resolver
    )
        throws IOException
    {
        int                 tag = in.readUnsignedByte ();
        DataType            dt;

        switch (tag) {
            case T_BINARY_TYPE:         dt = new BinaryDataType (); break;
            case T_BOOLEAN_TYPE:        dt = new BooleanDataType (); break;
            case T_CHAR_TYPE:           dt = new CharDataType (); break;
            case T_DATE_TIME_TYPE:      dt = new DateTimeDataType (); break;
            case T_ENUM_TYPE:           dt = new EnumDataType (); break;
            case T_FLOAT_TYPE:          dt = new FloatDataType (); break;
            case T_INTEGER_TYPE:        dt = new IntegerDataType (); break;
            case T_OBJECT_TYPE:         dt = new ClassDataType (); break;
            case T_STRING_TYPE:         dt = new VarcharDataType(); break;
            case T_TIME_OF_DAY_TYPE:    dt = new TimeOfDayDataType (); break;
            case T_ARRAY_TYPE:          dt = new ArrayDataType (); break;
            default:                    throw new IOException ("Illegal tag: " + tag);
        }

        dt.readFields (in, resolver);
        dt.parseEncoding (dt.encoding);
        return (dt);
    }

    public boolean              isPrimitive() {
        return !(this instanceof ArrayDataType) && ! (this instanceof ClassDataType);
    }
}
