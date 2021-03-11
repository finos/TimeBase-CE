package com.epam.deltix.qsrv.hf.pub;

import java.util.HashMap;

import com.epam.deltix.qsrv.hf.pub.codec.AlphanumericCodec;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.util.text.UpperCaseCharSequence;
import org.apache.commons.lang3.mutable.MutableLong;

/**
 * Utility class provides methods to convert exchange code from string to long and vice versa.
 * <p>
 * Methods are thread-safe and {@link #longToCode} caches strings internally to optimise string allocations.
 * From the other side, synchronized methods may be too slow for scenarios, when they are used frequently.
 * Consider using of {@link AlphanumericCodec} instance in this case.
 * </p>
 * <p>
 * If you not sure a name is less or equal to 10 characters and all of the characters are uppercase,
 * use {@link #codeToLongSafely}. It truncates the name if its length is more than 10 characters and
 * converts the name to uppercase. For example, the name "GoldmanSachs" will be converted to "GOLDMANSAC"
 * before encoded.
 * </p>
 */
public abstract class ExchangeCodec {
    public static final long                NULL = IntegerDataType.INT64_NULL;
    public static final int                 MAX_LEN = 10;

    private static final MutableLong        v = new MutableLong();
    private static final HashMap<MutableLong, String> 
                                            map = new HashMap<>();
    private static final AlphanumericCodec  codec = new AlphanumericCodec(MAX_LEN);

    private static final UpperCaseCharSequence 
                                            upcs = new UpperCaseCharSequence();
    
    public static String longToCode(long n) {
        if (n == NULL)
            return null;

        synchronized (ExchangeCodec.class) {
            v.setValue(n);
            String code = map.get(v);
            if (code == null) {
                code = codec.decodeFromLong(n).toString();
                map.put(new MutableLong(n), code);
            }
            return code;
        }
    }

    public static long codeToLong(String code) {
        return codeToLong((CharSequence) code);
    }

    public static long codeToLong(CharSequence code) {
        if (code == null)
            return NULL;

        synchronized (ExchangeCodec.class) {
            return codec.encodeToLong(code);
        }
    }
    
    public static long codeToLongSafely(String code) {
        return codeToLongSafely((CharSequence) code);
    }

    public static long codeToLongSafely(CharSequence code) {
        if (code == null) {
            return NULL;
        }

        final int len = code.length();
        
        synchronized (ExchangeCodec.class) {

            if (len > ExchangeCodec.MAX_LEN) {
                code = code.subSequence(0, ExchangeCodec.MAX_LEN);
            }                        

            upcs.setCharSequence(code);
            
            return ExchangeCodec.codeToLong(upcs);
        }
    }
}
