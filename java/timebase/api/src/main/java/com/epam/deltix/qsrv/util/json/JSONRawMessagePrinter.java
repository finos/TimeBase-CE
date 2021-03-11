package com.epam.deltix.qsrv.util.json;

import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.pub.codec.InterpretingCodecMetaFactory;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldInfo;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.collections.generated.ObjectToObjectHashMap;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.memory.MemoryDataInput;

import java.util.Arrays;

/**
 * Formats RawMessage to JSON. Not thread safe.
 * <p>
 * Usage example:
 * <pre>
 * RawMessage raw = ...
 * new JSONRawMessagePrinter().append(raw, sb);
 * System.out.println (sb.toString());
 * </pre>
 */
public class JSONRawMessagePrinter {

    private static final String[] REPLACEMENT_CHARS;
    private static final String[] HTML_SAFE_REPLACEMENT_CHARS;

    static {
        REPLACEMENT_CHARS = new String[128];
        for (int i = 0; i <= 0x1f; i++) {
            REPLACEMENT_CHARS[i] = String.format("\\u%04x", (int) i);
        }
        REPLACEMENT_CHARS['"'] = "\\\"";
        REPLACEMENT_CHARS['\\'] = "\\\\";
        REPLACEMENT_CHARS['\t'] = "\\t";
        REPLACEMENT_CHARS['\b'] = "\\b";
        REPLACEMENT_CHARS['\n'] = "\\n";
        REPLACEMENT_CHARS['\r'] = "\\r";
        REPLACEMENT_CHARS['\f'] = "\\f";
        HTML_SAFE_REPLACEMENT_CHARS = REPLACEMENT_CHARS.clone();
        HTML_SAFE_REPLACEMENT_CHARS['<'] = "\\u003c";
        HTML_SAFE_REPLACEMENT_CHARS['>'] = "\\u003e";
        HTML_SAFE_REPLACEMENT_CHARS['&'] = "\\u0026";
        HTML_SAFE_REPLACEMENT_CHARS['='] = "\\u003d";
        HTML_SAFE_REPLACEMENT_CHARS['\''] = "\\u0027";
    }

    protected final boolean prettyPrint;
    protected final boolean skipNull;
    protected int indent = 0;
    protected final DataEncoding dataEncoding;
    protected final String typeField;

    protected final boolean htmlSafe;
    protected final boolean printInstrumentType;
    protected final PrintType printType;

    private final ObjectToObjectHashMap<String, UnboundDecoder> _decoders = new ObjectToObjectHashMap<>();
    private final MemoryDataInput buffer = new MemoryDataInput();
    private final DateFormatter formatter = new DateFormatter();

    public JSONRawMessagePrinter() {
        this(false, true, DataEncoding.STANDARD, false, false, PrintType.FULL);
    }

    public JSONRawMessagePrinter(boolean printInstrumentType) {
        this(false, true, DataEncoding.STANDARD, false, printInstrumentType, PrintType.FULL);
    }

    public JSONRawMessagePrinter(boolean prettyPrint,
                                 boolean skipNull,
                                 DataEncoding dataEncoding,
                                 boolean htmlSafe,
                                 boolean printInstrumentType,
                                 PrintType printType) {
        this(prettyPrint, skipNull, dataEncoding, htmlSafe, printInstrumentType, printType, "type");
    }

    public JSONRawMessagePrinter(boolean prettyPrint,
                                 boolean skipNull,
                                 DataEncoding dataEncoding,
                                 boolean htmlSafe,
                                 boolean printInstrumentType,
                                 PrintType printType,
                                 String typeField) {
        this.prettyPrint = prettyPrint;
        this.skipNull = skipNull;
        this.dataEncoding = dataEncoding;
        this.htmlSafe = htmlSafe;
        this.printInstrumentType = printInstrumentType;
        this.printType = printType;
        this.typeField = typeField;
    }

    public void append(RawMessage raw, StringBuilder sb) {
        if (raw.data == null)
            return;

        final UnboundDecoder decoder = getDecoder(raw);

        appendBlock('{', sb);

        // standard message header
        appendHeader(raw, sb);

        buffer.setBytes(raw.data, raw.offset, raw.length);
        decoder.beginRead(buffer);

        boolean addSep = true;
        // custom message payload
        while (decoder.nextField()) {
            if (addSep)
                appendSeparator(sb);

            boolean changed = appendField(decoder, decoder.getField(), sb);
            addSep = changed || !skipNull;
        }

        if (sb.charAt(sb.length() - 1) == ',')
            sb.setLength(sb.length() - 1);

        appendBlock('}', sb);
    }

    protected void appendHeader(RawMessage raw, StringBuilder sb) {
        // standard message header

        if (printType != PrintType.NONE) {
            appendType(sb, raw.type.getName());
            appendSeparator(sb);
        }

        sb.append("\"symbol\":");
        appendString(raw.getSymbol(), sb);

        if (raw.getTimeStampMs() != 0) {
            appendSeparator(sb);
            sb.append("\"timestamp\":");
            appendTimestamp(raw.getTimeStampMs(), sb);
        }
    }

    protected UnboundDecoder getDecoder(final RawMessage raw) {
        final String guid = raw.type.getGuid();
        UnboundDecoder decoder = _decoders.get(guid, null);
        if (decoder == null) {
            decoder = InterpretingCodecMetaFactory.INSTANCE.createFixedUnboundDecoderFactory(raw.type).create();
            _decoders.put(guid, decoder);
        }
        return decoder;
    }

    protected boolean appendField(ReadableValue decoder, NonStaticFieldInfo field, StringBuilder sb) {
        int length = sb.length();
        sb.append('"');
        sb.append(field.getName());
        sb.append("\":");

        boolean hasValue = appendFieldValue(decoder, field, sb);
        if (skipNull && !hasValue)
            sb.setLength(length);

        return hasValue;
    }

    protected boolean appendFieldValue(ReadableValue decoder, NonStaticFieldInfo field, StringBuilder sb) {
        try {
            DataType type = field.getType();
            if (type instanceof IntegerDataType) {
                if (((IntegerDataType) type).getNativeTypeSize() >= 6) {
                    long v = decoder.getLong();
                    sb.append(v);
                    return ((IntegerDataType) type).getNullValue() != v;
                } else {
                    int v = decoder.getInt();
                    sb.append(v);
                    return ((IntegerDataType) type).getNullValue() != v;
                }
            } else if (type instanceof FloatDataType) {
                FloatDataType fType = (FloatDataType) type;
                if (fType.isFloat()) {
                    float v = decoder.getFloat();
                    appendFloat(sb, v);
                    return Float.isFinite(v);
                } else if (fType.isDecimal64()) {
                    long v = decoder.getLong();
                    if (dataEncoding == DataEncoding.NATIVE) {
                        sb.append(v);
                    } else {
                        sb.append("\"");
                        Decimal64Utils.appendTo(v, sb);
                        sb.append("\"");
                    }
                    return Decimal64Utils.isFinite(v);
                } else {
                    double v = decoder.getDouble();
                    appendDouble(sb, v);
                    return Double.isFinite(v);
                }
            } else if (type instanceof BooleanDataType) {
                if (dataEncoding == DataEncoding.NATIVE) {
                    sb.append(decoder.getBoolean() ? 1 : 0);
                } else {
                    sb.append(decoder.getBoolean());
                }
            } else if (type instanceof EnumDataType) {
                appendEnum(decoder, (EnumDataType) type, sb);
            } else if (type instanceof DateTimeDataType) {
                long timestamp = decoder.getLong();
                return appendTimestamp(timestamp, sb);
            } else if (type instanceof VarcharDataType) {
                appendString(decoder, sb, (VarcharDataType) type);
            } else if (type instanceof CharDataType) {
                appendChar(decoder.getChar(), sb);
            } else if (type instanceof ClassDataType) {
                int length = sb.length();
                boolean hasValue = appendClassField(decoder, sb);
                if (skipNull && !hasValue)
                    sb.setLength(length);

                return hasValue;
            } else if (type instanceof ArrayDataType) {
                int length = sb.length();
                boolean hasValue = appendArrayField((ArrayDataType) type, decoder, sb);
                if (skipNull && !hasValue)
                    sb.setLength(length);

                return hasValue;
            } else if (type instanceof BinaryDataType) {
                appendBinaryField((BinaryDataType) type, decoder, sb);
            } else if (type instanceof TimeOfDayDataType)
                sb.append(decoder.getInt());
            else {
                throw new IllegalArgumentException("Unsupported type " + type.getClass().getSimpleName());
            }
        } catch (NullValueException e) {
            return false;
        }

        return true;
    }

    protected void appendType(StringBuilder sb, String fullType) {
        sb.append('"').append(typeField).append('"').append(':');
        if (printType == PrintType.SHORT) {
            appendString(fullType.substring(fullType.lastIndexOf(".") + 1), sb);
        } else {
            appendString(fullType, sb);
        }
    }

    /// Type-specific appenders

    protected boolean appendClassField(ReadableValue udec, StringBuilder sb) throws NullValueException {
        try {
            appendBlock('{', sb);

            boolean needSepa = true;
            final UnboundDecoder decoder = udec.getFieldDecoder();
            String name = decoder.getClassInfo().getDescriptor().getName();

            appendType(sb, name);

            while (decoder.nextField()) {

                if (needSepa)
                    appendSeparator(sb);

                boolean changed = appendField(decoder, decoder.getField(), sb);
                needSepa = changed || !skipNull;
            }

            if (sb.charAt(sb.length() - 1) == ',')
                sb.setLength(sb.length() - 1);

        } catch (NullValueException e) {
            return false;
        } finally {
            appendBlock('}', sb);
        }

        return true;
    }

    protected boolean appendBinaryField(BinaryDataType type, ReadableValue udec, StringBuilder sb) throws NullValueException {
        final int len = udec.getBinaryLength();
        try {
            byte[] bytes = new byte[len];
            udec.getBinary(0, len, bytes, 0);
            sb.append(Arrays.toString(bytes));

        } catch (NullValueException e) {
            return false;
        }

        return true;
    }

    protected boolean appendArrayField(ArrayDataType type, ReadableValue udec, StringBuilder sb) throws NullValueException {

        final int len = udec.getArrayLength();
        final DataType underlineType = type.getElementDataType();

        appendBlock('[', sb);
        boolean needSepa = false;
        for (int i = 0; i < len; i++) {
            try {
                final ReadableValue rv = udec.nextReadableElement();
                if (needSepa)
                    appendSeparator(sb);
                else
                    needSepa = true;

                if (underlineType instanceof FloatDataType) {
                    FloatDataType fType = (FloatDataType) underlineType;
                    if (fType.isFloat()) {
                        float v = rv.getFloat();
                        appendFloat(sb, v);
                    } else if (fType.isDecimal64()) {
                        long v = rv.getLong();
                        if (dataEncoding == DataEncoding.NATIVE) {
                            sb.append(v);
                        } else {
                            sb.append("\"");
                            Decimal64Utils.appendTo(v, sb);
                            sb.append("\"");
                        }
                    } else {
                        double v = rv.getDouble();
                        appendDouble(sb, v);
                    }
                } else if (underlineType instanceof  IntegerDataType) {
                    if (((IntegerDataType) underlineType).getNativeTypeSize() >= 6) {
                        long v = rv.getLong();
                        sb.append(v);
                    } else {
                        int v = rv.getInt();
                        sb.append(v);
                    }
                } else if (underlineType instanceof BooleanDataType)
                    sb.append(rv.getBoolean());
                else if (underlineType instanceof DateTimeDataType) {
                    long timestamp = rv.getLong();
                    appendTimestamp(timestamp, sb);
                } else if (underlineType instanceof ClassDataType) {
                    int length = sb.length();
                    boolean hasValue = appendClassField(rv, sb);
                    if (skipNull && !hasValue)
                        sb.setLength(length);
                } else if (underlineType instanceof EnumDataType) {
                    int length = sb.length();
                    boolean hasValue = appendEnum(rv, (EnumDataType) underlineType, sb);
                    if (skipNull && !hasValue)
                        sb.setLength(length);
                } else
                    throw new IllegalArgumentException("Unexpected type: " + underlineType.getClass().getSimpleName());


            } catch (NullValueException e) {
                // do nothing
            }
        }
        appendBlock(']', sb);

        return true;
    }

    protected boolean appendTimestamp(long timestamp, StringBuilder sb) {
        if (dataEncoding == DataEncoding.NATIVE) {
            sb.append(timestamp);
        } else {
            sb.append('"');
            formatter.toDateString(timestamp, sb);
            sb.append('"');
        }
        return timestamp != Long.MIN_VALUE;
    }

    protected void appendString(CharSequence text, StringBuilder sb) {

        if (text == null) {
            sb.append("\"\"");
            return;
        }

        sb.append('"');

        if (htmlSafe) {

            String[] replacements = HTML_SAFE_REPLACEMENT_CHARS;

            int last = 0;
            int length = text.length();

            for (int i = 0; i < length; i++) {
                char c = text.charAt(i);
                String replacement;

                if (c < 128) {
                    replacement = replacements[c];
                    if (replacement == null)
                        continue;
                } else if (c == '\u2028') {
                    replacement = "\\u2028";
                } else if (c == '\u2029') {
                    replacement = "\\u2029";
                } else {
                    continue;
                }

                if (last < i)
                    sb.append(text, last, i);

                sb.append(replacement);
                last = i + 1;
            }

            if (last < length)
                sb.append(text, last, length);
        } else {
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                if (ch == '"')
                    sb.append('\\');
                sb.append(ch);
            }
        }
        sb.append('"');
    }

    protected void appendString(ReadableValue decoder, StringBuilder sb, VarcharDataType dataType) {
        // TODO: Alloc free alternative
        //TODO: CharSequence text = buffer.readCharSequence();
        if (dataType.getEncoding() != null && dataType.getEncoding().startsWith(VarcharDataType.ENCODING_ALPHANUMERIC) &&
            dataEncoding == DataEncoding.NATIVE) {
            // for ALPHANUMERIC encoding we print long if dataEncoding is NATIVE
            sb.append(decoder.getLong());
        } else {
            appendString(decoder.getString(), sb);
        }
    }

    protected static void appendChar(char ch, StringBuilder sb) {
        sb.append('"');
        if (ch == '"' || ch == '\\')
            sb.append('\\');
        sb.append(ch);
        sb.append('"');
    }

    protected boolean appendEnum(ReadableValue decoder, EnumDataType type, StringBuilder sb) {
        long ordinal = decoder.getLong();
        appendString(type.descriptor.longToString(ordinal), sb);
        return true;
    }

    /// Pretty formatting

    protected void appendSeparator(StringBuilder sb) {
        sb.append(',');
        if (prettyPrint) {
            sb.append('\n');
            appendIndent(sb);
        }
    }

    protected void appendBlock(char brace, StringBuilder sb) {
        if (prettyPrint) {
            if (brace == '{' || brace == '[') {
                indent++;
            } else {
                sb.append('\n');
                indent--;
                appendIndent(sb);
            }
            sb.append(brace);
            if (brace == '{' || brace == '[') {
                sb.append('\n');
                appendIndent(sb);
            }
        } else {
            sb.append(brace);
        }
    }

    protected void appendIndent(StringBuilder sb) {
        assert prettyPrint;
        for (int i = 0; i < indent; i++)
            sb.append(' ');
    }

    protected void appendFloat(StringBuilder sb, float f) {
        sb.append("\"")
                .append(StringUtils.toDecimalString(f))
                .append("\"");
    }

    protected void appendDouble(StringBuilder sb, double d) {
        sb.append("\"")
                .append(StringUtils.toDecimalString(d))
                .append("\"");
    }

}
