package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *
 */
public class DataTypeCompiler {
    public static final String      SIGNED = "SIGNED";
    public static final String      UNSIGNED = "UNSIGNED";
    public static final String      INTERVAL = "INTERVAL";
    public static final String      BINARY = "BINARY";
    public static final String      DECIMAL = "DECIMAL";
    public static final String      DECIMAL64 = "DECIMAL64";
    public static final String      UTF8 = "UTF8";
    public static final String      MULTILINE = "MULTILINE";
    public static final String      ALPHANUMERIC = "ALPHANUMERIC";
    
    public static final int         MAX_DECIMAL_SCALE = 
        deltix.util.memory.MemoryDataOutput.MAX_SCALE_EXP;
    
    static IntegerDataType   compileInteger (SimpleDataTypeSpec dts, Long min, Long max) {
        final String    enc = dts.encoding;
        String          resenc = null;
        
        if (enc == null) 
            resenc = IntegerDataType.ENCODING_INT64;
        else if (enc.equals (SIGNED)) {
            switch (dts.dimension) {
                case 8:     resenc = IntegerDataType.ENCODING_INT8;     break;
                case 16:    resenc = IntegerDataType.ENCODING_INT16;    break;
                case 32:    resenc = IntegerDataType.ENCODING_INT32;    break;
                case 48:    resenc = IntegerDataType.ENCODING_INT48;    break;
                case 64:    resenc = IntegerDataType.ENCODING_INT64;    break;                
            }
        }
        else if (enc.equals (UNSIGNED)) {
            switch (dts.dimension) {
                case 30:    resenc = IntegerDataType.ENCODING_PUINT30;  break;
                case 61:    resenc = IntegerDataType.ENCODING_PUINT61;  break;                              
            }
        }
        else if (enc.equals (INTERVAL) && dts.dimension == SimpleDataTypeSpec.NO_DIMENSION)
            resenc = IntegerDataType.ENCODING_PINTERVAL;
        
        if (resenc == null)
            throw new IllegalEncodingException (dts);
        
        return (new IntegerDataType (resenc, dts.nullable, min, max));
    }
    
    static FloatDataType    compileFloat (SimpleDataTypeSpec dts, Double min, Double max) {
        final String    enc = dts.encoding;
        String          resenc = null;
        
        if (enc == null) 
            resenc = FloatDataType.ENCODING_FIXED_DOUBLE;
        else if (enc.equals (BINARY)) {
            switch (dts.dimension) {
                case 32:    resenc = FloatDataType.ENCODING_FIXED_FLOAT;    break;
                case 64:    resenc = FloatDataType.ENCODING_FIXED_DOUBLE;    break;                
            }
        }
        else if (enc.equals (DECIMAL)) {
            if (dts.dimension >= 0 && dts.dimension <= MAX_DECIMAL_SCALE)
                resenc = "DECIMAL(" + dts.dimension + ")";
            else if (dts.dimension == SimpleDataTypeSpec.NO_DIMENSION)
                resenc = "DECIMAL";
        } else if (enc.equals (DECIMAL64)) {
            resenc = "DECIMAL64";
        }
        
        if (resenc == null)
            throw new IllegalEncodingException (dts);
        
        return (new FloatDataType (resenc, dts.nullable, min, max));
    }
    
    static VarcharDataType    compileVarchar (SimpleDataTypeSpec dts) {
        final String    enc = dts.encoding;
        String          resenc = null;
        boolean         multiline = false;
        
        if (enc == null || enc.equals (UTF8)) 
            resenc = VarcharDataType.ENCODING_INLINE_VARSIZE;
        else if (enc.equals (MULTILINE) && dts.dimension == SimpleDataTypeSpec.NO_DIMENSION) {
            resenc = VarcharDataType.ENCODING_INLINE_VARSIZE;
            multiline = true;
        }
        else if (enc.equals (ALPHANUMERIC) && dts.dimension > 0) 
            resenc = "ALPHANUMERIC(" + dts.dimension + ")";        
        
        if (resenc == null)
            throw new IllegalEncodingException (dts);
        
        return (new VarcharDataType (resenc, dts.nullable, multiline));
    }
}
