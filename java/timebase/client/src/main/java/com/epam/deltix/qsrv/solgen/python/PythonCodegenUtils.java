package com.epam.deltix.qsrv.solgen.python;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.solgen.StreamMetaData;

public class PythonCodegenUtils {

    static final String INDENT0 = "";
    static final String INDENT1 = INDENT0 + "    ";
    static final String INDENT2 = INDENT1 + "    ";
    static final String INDENT3 = INDENT1 + "    ";
    static final String NL = "\n";

    static final String[] INDENT = new String[] {
        INDENT0, INDENT1, INDENT2, INDENT3
    };

    static String messagesDefinition(StreamMetaData md, int indentLevel) {
        StringBuilder messages = new StringBuilder();
        int iCount = md.getConcreteTypes().size();
        if (iCount == 1) {
            messages.append(INDENT[indentLevel])
                .append("message = dxapi.InstrumentMessage()")
                .append(PythonCodegenUtils.NL);
            String name = md.getConcreteTypes().get(0).getName();
            messages.append(INDENT[indentLevel])
                .append(String.format("message.typeName = '%s'", name))
                .append(PythonCodegenUtils.NL);
        } else {
            messages.append(INDENT[indentLevel]).append("messages = []").append(PythonCodegenUtils.NL);
            for (int i = 0; i < iCount; i++) {
                String name = md.getConcreteTypes().get(i).getName();
                messages.append(INDENT[indentLevel])
                    .append("messages.append(dxapi.InstrumentMessage())")
                    .append(PythonCodegenUtils.NL);
                messages.append(INDENT[indentLevel])
                    .append(String.format("messages[%d].typeName = '%s'", i, name))
                    .append(PythonCodegenUtils.NL);
            }
        }

        return messages.toString();
    }

    static String messagesSend(StreamMetaData md, int indentLevel) {
        StringBuilder sends = new StringBuilder();
        for (int i = 0, iCount = md.getConcreteTypes().size(); i < iCount; i++) {
            RecordClassDescriptor rcd = md.getConcreteTypes().get(i);
            String varname = iCount > 1 ? "messages[" + i + "]" : "message";

            sends.append(INDENT[indentLevel]);
            sends.append("# Initialize the fields of ");
            sends.append(rcd.getName ());
            sends.append(":").append(PythonCodegenUtils.NL);
            sends.append(INDENT[indentLevel])
                .append(varname).append(".symbol = 'DLTX'")
                .append(PythonCodegenUtils.NL);
            sends.append(INDENT[indentLevel])
                .append(varname)
                .append(".instrumentType = 'EQUITY'")
                .append(PythonCodegenUtils.NL);

            for (; rcd != null; rcd = rcd.getParent ()) {
                for (DataField df : rcd.getFields()) {
                    if (!(df instanceof NonStaticDataField)) {
                        continue;
                    }

                    PythonCodegenUtils.generateSafeSampleAssign(varname, (NonStaticDataField) df, sends, indentLevel);
                }
            }

            sends.append(INDENT[indentLevel])
                .append("loader.send(").append(varname).append (")")
                .append(PythonCodegenUtils.NL);
            sends.append(INDENT[indentLevel])
                .append("print('[SENT]: ', str(vars(")
                .append(varname).append (")))")
                .append(PythonCodegenUtils.NL)
                .append(PythonCodegenUtils.NL);
        }

        return sends.toString();
    }

    private static void generateSafeSampleAssign(String varname, NonStaticDataField ndf, StringBuilder sends, int indentLevel) {
        DataType ftype = ndf.getType();

        if (ftype instanceof ClassDataType) {
// not supported TODO
        } else if (ftype instanceof ArrayDataType) {
// complicated, TODO
        } else if (ftype instanceof EnumDataType) {
            String[] symbols = ((EnumDataType) ftype).getDescriptor().getSymbols();
            if (symbols.length > 0) {
                writeSimpleAssign(sends, varname, ndf, "'" + symbols[0] + "'", indentLevel);
            }
        }
        else if (ftype instanceof BooleanDataType) {
            writeSimpleAssign(sends, varname, ndf, ftype.isNullable() ? "None" : "True", indentLevel);
        }
        else if (ftype instanceof IntegerDataType) {
            IntegerDataType idt = (IntegerDataType) ftype;
            long min = idt.getMinNotNull().longValue();
            long max = idt.getMaxNotNull().longValue();
            String value = 1 >= min && 1 <= max ? "1" : String.valueOf ((min + max) / 2);

            writeSimpleAssign(sends, varname, ndf, value, indentLevel);
        } else if (ftype instanceof FloatDataType) {
            FloatDataType fdt = (FloatDataType) ftype;
            double min = fdt.getMinNotNull().doubleValue();
            double max = fdt.getMaxNotNull().doubleValue();

            String value = 1.5 >= min && 1.5 <= max ? "1.5" : String.valueOf (min);
            writeSimpleAssign(sends, varname, ndf, value, indentLevel);
        }
        else if (ftype instanceof TimeOfDayDataType) {
            writeSimpleAssign(sends, varname, ndf, "46888345  # = 13:01:28.345", indentLevel);
        } else if (ftype instanceof CharDataType) {
            writeSimpleAssign(sends, varname, ndf, "'X'", indentLevel);
        } else if (ftype instanceof DateTimeDataType) {
            writeSimpleAssign(sends, varname, ndf, "1373321607447  # = 2013-07-08 22:13:27.447 GMT", indentLevel);
        } else if (ftype instanceof VarcharDataType) {
            VarcharDataType vdt = (VarcharDataType) ftype;
            String value = "\"This is a string\"";
            if (vdt.getEncodingType() == VarcharDataType.ALPHANUMERIC) {
                switch (vdt.getLength()) {
                    case 1:  value = "\"X\"";   break;
                    case 2:  value = "\"A1\"";  break;
                    default: value = "\"NY4\""; break;
                }
            }
            writeSimpleAssign(sends, varname, ndf, value, indentLevel);
        } else if (ftype instanceof BinaryDataType) {
            if (ftype.isNullable()) {
                writeHead(sends, varname, ndf, indentLevel);
                sends.append(" = None").append(NL);
            } else {
                writeHead(sends, varname, ndf, indentLevel);
                sends.append(" = bytearray([236])").append(NL);
            }
        }
    }

    private static void writeSimpleAssign(StringBuilder sends, String varname, NonStaticDataField ndf, String value, int indentLevel) {
        writeHead(sends, varname, ndf, indentLevel);
        sends.append(" = ");
        sends.append(value).append(NL);
    }

    private static void writeHead(StringBuilder sends, String varname, NonStaticDataField ndf, int indentLevel) {
        writeHead(sends, varname, JavaBeanGenerator.escapeIdentifierForJava(ndf.getName()), indentLevel);
    }

    private static void writeHead(StringBuilder sends, String varname, String ndfName, int indentLevel) {
        sends.append(INDENT[indentLevel]);
        sends.append(varname);
        sends.append(".");
        sends.append(ndfName);
    }

}
