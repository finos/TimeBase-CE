package com.epam.deltix.qsrv.hf.pub.md;

import com.epam.deltix.util.time.TimeFormatter;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.xml.bind.annotation.*;

/**
 * Time Of Day (in HH:MM:SS or  HH:MM:SS.sss format). Mapped to Java <code>int</code> data type that specifies number of milliseconds since midnight (in undertermined timezone).
 */
@XmlType (name = "timeOfDay")
public final class TimeOfDayDataType extends DataType {

    public static final String NAME = "TIMEOFDAY";
	private static final long serialVersionUID = 1L;
	
    public static final int NULL = -1;

    public static TimeOfDayDataType getDefaultInstance() {
        return new TimeOfDayDataType(true);
    }

    TimeOfDayDataType() {   // For JAXB
        super();
    }

    public TimeOfDayDataType(boolean nullable) {
        super(null, nullable);
    }

    public String           getBaseName () {
        return NAME;
    }

    @Override
    public int              getCode() {
        return T_TIME_OF_DAY_TYPE;
    }

    public static int       staticParse (CharSequence text) {
        return (TimeFormatter.parseTimeOfDayMillis (text));
    }
    
    public static String    staticFormat (int value) {
        return (TimeFormatter.formatTimeofDayMillis (value));
    }
    
    @Override
    protected void          assertValidImpl (Object obj) {
        if (!(obj instanceof Integer))
            throw unsupportedType (obj);
        
        int     value = (Integer) obj;
        
        if (value < 0 || value > 86399999)
            throw outOfRange (value, 0, 86399999);
    }

    @Override
    protected Object        toBoxedImpl (CharSequence text) {
        return (staticParse (text));
    }

    @Override
    protected String        toStringImpl (Object obj) {
        return (staticFormat ((Integer) obj));
    }
    
    public ConversionType isConvertible(DataType to) {
        if (to instanceof VarcharDataType || to instanceof TimeOfDayDataType) {
            return ConversionType.Lossless;
        } else if (to instanceof FloatDataType) {
            return ((FloatDataType) to).check(0, Integer.MAX_VALUE) ? ConversionType.Lossless : ConversionType.Lossy;
        } else if (to instanceof IntegerDataType) {
            return ((IntegerDataType) to).check(0, Integer.MAX_VALUE) ? ConversionType.Lossless : ConversionType.Lossy;
        } else if (to instanceof DateTimeDataType) {
            return ConversionType.Lossy;
        }

        return ConversionType.NotConvertible;
    }

    @Override
    public void             writeTo (DataOutputStream out) throws IOException {
        out.writeByte (T_TIME_OF_DAY_TYPE);

        super.writeTo (out);
    }
}
