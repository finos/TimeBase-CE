package com.epam.deltix.qsrv.hf.tickdb.schema;

/**
 * Describes all possible error in conversion engine
 */
public class ConversionError extends RuntimeException {

    public static int OUT_OF_RANGE_ERROR;
    public static int NULL_VALUE_ERROR;
    public static int INCOMPATIBLE_TYPES_ERROR;

    private int code;

    public ConversionError(int code) {
        this(code, null);
    }

    public ConversionError(int code, Throwable cause) {
        super(getErrorMessage(code), cause);
        
        this.code = code;
    }

    public static String getErrorMessage(int code) {
        return "";
    }

    public int getErrorCode() {
        return code;
    }
}
