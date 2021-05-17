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
package com.epam.deltix.qsrv.solgen.cpp;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.solgen.CodegenUtils;
import com.epam.deltix.qsrv.solgen.StreamMetaData;
import com.epam.deltix.util.io.GUID;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CppCodegenUtils {

    static String projectFiles(Path root, List<Path> files) {
        StringBuilder sb = new StringBuilder();

        files.forEach(file -> {
            sb.append(CodegenUtils.INDENT1)
                .append("<CLCompile Include=\"")
                .append(root.relativize(file))
                .append("\"/>")
                .append(CodegenUtils.NL);
        });

        return sb.toString();
    }

    static String filterFiles(Path root, List<Path> files) {
        StringBuilder sb = new StringBuilder();

        files.forEach(file -> {
            Path relative = root.relativize(file);
            sb.append(CodegenUtils.INDENT1)
                .append("<CLCompile Include=\"")
                .append(relative)
                .append("\">")
                .append(CodegenUtils.NL);
            sb.append(CodegenUtils.INDENT2)
                .append("<Filter>")
                .append(relative.getParent())
                .append("</Filter>")
                .append(CodegenUtils.NL);
            sb.append(CodegenUtils.INDENT1)
                .append("</CLCompile>")
                .append(CodegenUtils.NL);
        });

        return sb.toString();
    }

    static String filters(Path root, List<Path> files) {
        StringBuilder sb = new StringBuilder();

        Set<String> filters = new HashSet<>();
        files.forEach(file -> {
            filters.add(root.relativize(file).getParent().toString());
        });

        filters.forEach(filter -> {
            sb.append(CodegenUtils.INDENT1)
                .append("<Filter Include=\"")
                .append(filter)
                .append("\">")
                .append(CodegenUtils.NL);
            sb.append(CodegenUtils.INDENT2)
                .append("<UniqueIdentifier>{")
                .append(new GUID().toString())
                .append("}</UniqueIdentifier>")
                .append(CodegenUtils.NL);
            sb.append(CodegenUtils.INDENT1)
                .append("</Filter>")
                .append(CodegenUtils.NL);
        });

        return sb.toString();
    }

    static String mainForwardDeclarations(List<CppSample> samples) {
        StringBuilder sb = new StringBuilder();

        samples.forEach(sample -> {
            sb.append("void ").append(sample.functionName()).append("();").append(CodegenUtils.NL);
        });

        return sb.toString();
    }

    static String mainDispatch(List<CppSample> samples) {
        StringBuilder sb = new StringBuilder();

        boolean first = true;
        for (CppSample sample : samples) {
            sb.append(CodegenUtils.INDENT1);
            if (!first) {
                sb.append("else ");
            }
            sb.append ("if (");

            if (first) {
                sb.append("argc < 2 || ");
            }

            sb.append("strcmp(argv[1], \"")
                .append(sample.functionName())
                .append("\") == 0)")
                .append(CodegenUtils.NL)
                .append(CodegenUtils.INDENT2)
                .append(sample.functionName())
                .append("();")
                .append(CodegenUtils.NL);

            first = false;
        }

        return sb.toString();
    }

    static String streamCodecHeader(StreamMetaData metaData) {
        StringBuilder sb = new StringBuilder();

        String ifdef = CppCodecGenerator.getIfdef(metaData.getNameSpace(), "streamCodec");
        sb.append("#ifndef ").append(ifdef).append(CodegenUtils.NL);
        sb.append("#define ").append(ifdef).append(CodegenUtils.NL2);
        sb.append("#include <string>")
            .append(CodegenUtils.NL).append("#include <stdint.h>")
            .append(CodegenUtils.NL).append("#include \"dxapi/schema.h\"")
            .append(CodegenUtils.NL2);

        sb.append("#include \"dxapi/dxapi.h\"").append(CodegenUtils.NL);
        sb.append("#include \"src/codecs/NativeMessage.h\"").append(CodegenUtils.NL2);

        for (RecordClassDescriptor rcd : metaData.getConcreteTypes()) {
            sb.append("#include \"");
            sb.append(CppCodecGenerator.getSimpleClassName(rcd.getName()));
            sb.append(".h\"");
            sb.append(CodegenUtils.NL);
        }

        sb.append(CodegenUtils.NL);

        return sb.toString();
    }

    static String streamCodecFooter(StreamMetaData metaData) {
        StringBuilder sb = new StringBuilder();

        String ifdef = CppCodecGenerator.getIfdef(metaData.getNameSpace(), "streamCodec");
        sb.append("#endif //").append(ifdef).append(CodegenUtils.NL);

        return sb.toString();
    }

    static String streamCodecDeclareMessages(StreamMetaData metaData) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < metaData.getConcreteTypes().size(); ++i) {
            RecordClassDescriptor rcd = metaData.getConcreteTypes().get(i);

            sb.append(CodegenUtils.INDENT1)
                .append(CppCodecGenerator.getCppFullClassName(metaData.getNameSpace(), rcd.getName()))
                .append(" ")
                .append(CppCodecGenerator.getSimpleVarName(rcd.getName())).append(";\n");
        }

        return sb.toString();
    }

    static String streamCodecMessageTypes(StreamMetaData metaData) {
        StringBuilder sb = new StringBuilder();
        for (RecordClassDescriptor rcd : metaData.getConcreteTypes()) {
            sb.append(CodegenUtils.INDENT1)
                .append(CppCodecGenerator.getSimpleEnumType(null, rcd.getName()))
                .append(",").append(CodegenUtils.NL);
        }

        return sb.toString();
    }

    static String streamCodecInitGuids(StreamMetaData metaData) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < metaData.getConcreteTypes().size(); ++i) {
            sb.append(CodegenUtils.INDENT3);
            if (i == 0)
                sb.append("if ");
            else
                sb.append("else if ");

            String typeName = metaData.getConcreteTypes().get(i).getName();
            String typeEnum = CppCodecGenerator.getSimpleEnumType(null, typeName);
            sb.append("(!descriptors[i].className.compare(\"").append(typeName).append("\"))").append(CodegenUtils.NL);
            sb.append(CodegenUtils.INDENT4).append("guids[").append(typeEnum).append("] = descriptors[i].guid;").append(CodegenUtils.NL);
        }

        return sb.toString();
    }

    static String streamCodecDetectType(StreamMetaData metaData) {
        StringBuilder sb = new StringBuilder();

        boolean first = true;
        for (RecordClassDescriptor rcd : metaData.getConcreteTypes()) {
            if (first) {
                first = false;
                sb.append(CodegenUtils.INDENT3).append("if ");
            } else {
                sb.append(" else if ");
            }

            String msgVar = CppCodecGenerator.getSimpleVarName(rcd.getName());
            String typeEnum = CppCodecGenerator.getSimpleEnumType(null, rcd.getName());
            sb.append("(!typeName->compare(\"").append(rcd.getName()).append("\")) {").append(CodegenUtils.NL);
            sb.append(CodegenUtils.INDENT4).append("knownMessages[header.typeId] = ").append(typeEnum).append(";").append(CodegenUtils.NL);
            sb.append(CodegenUtils.INDENT4).append("if (guids[").append(typeEnum).append("].compare(").append(msgVar).append(".getGuid()))").append(CodegenUtils.NL);
            sb.append(CodegenUtils.INDENT5).append("throw runtime_error(\"Type guid mismatch for type \" + ")
                .append(msgVar).append(".getTypeName());").append(CodegenUtils.NL);
            sb.append(CodegenUtils.INDENT3).append("}");
        }

        sb.append(CodegenUtils.NL);
        return sb.toString();
    }

    static String streamCodecDecodeMessage(StreamMetaData metaData) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < metaData.getConcreteTypes().size(); ++i) {
            if (i == 0) {
                sb.append(CodegenUtils.INDENT2).append("if ");
            } else {
                sb.append(CodegenUtils.INDENT2).append("} else if ");
            }

            RecordClassDescriptor rcd = metaData.getConcreteTypes().get(i);
            String msgVar = CppCodecGenerator.getSimpleVarName(rcd.getName());
            String typeEnum = CppCodecGenerator.getSimpleEnumType(null, rcd.getName());
            sb.append("(knownMessages[header.typeId] == ").append(typeEnum).append(") {").append(CodegenUtils.NL);
            sb.append(CodegenUtils.INDENT3).append(msgVar)
                .append(".decode(reader);").append(CodegenUtils.NL);
            sb.append(CodegenUtils.INDENT3).append("current = &").append(msgVar).append(";").append(CodegenUtils.NL);
        }

        return sb.toString();
    }

    static String streamCodecRegisterMessages(StreamMetaData metaData) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < metaData.getConcreteTypes().size(); ++i) {
            String typeName = CppCodecGenerator.getSimpleEnumType(null, metaData.getConcreteTypes().get(i).getName());
            sb.append(CodegenUtils.INDENT2).append("loader->registerMessageType(")
                .append(typeName).append(", \"").append(metaData.getConcreteTypes().get(i).getName()).append("\");")
                .append(CodegenUtils.NL);
        }

        return sb.toString();
    }

    static String messagesDefinitions(StreamMetaData metaData, int identLevel) {
        StringBuilder sb = new StringBuilder();

        for (RecordClassDescriptor rcd : metaData.getConcreteTypes()) {
            sb.append(CodegenUtils.INDENT[identLevel])
                .append(CppCodecGenerator.getCppFullClassName(metaData.getNameSpace(), rcd.getName()))
                .append(" ")
                .append(CppCodecGenerator.getSimpleVarName(rcd.getName())).append(";\n");
        }

        return sb.toString();
    }

    static String sendMessages(StreamMetaData metaData) {
        StringBuilder sb = new StringBuilder();

        for (RecordClassDescriptor rcd : metaData.getConcreteTypes()) {
            String varname = CppCodecGenerator.getSimpleVarName(rcd.getName());
            String enumType = CppCodecGenerator.getSimpleEnumType(metaData.getNameSpace(), rcd.getName());

            sb.append(CodegenUtils.INDENT[3]);
            sb.append("// Write fields of ");
            sb.append(rcd.getName ());
            sb.append(":\n");

            sb.append(CodegenUtils.INDENT[3]).append(varname).append(".clear();\n");

            sb.append(CodegenUtils.INDENT[3]).append(varname).append(".timestamp = now_ns();\n");
            sb.append(CodegenUtils.INDENT[3]).append(varname).append(".typeId = ").append(enumType).append(";\n");
            sb.append(CodegenUtils.INDENT[3]).append(varname).append(".entityId = instrument;\n");

            generateFields(sb, varname, rcd);

            sb.append("\n");
            sb.append(CodegenUtils.INDENT[3]).append("encoder.send(").append(varname).append(");\n");
            sb.append(CodegenUtils.INDENT[3]).append("cout << \"[SENT]: \" << ").append(varname).append (".toString() << endl;\n\n");
        }

        return sb.toString();
    }

    private static void generateFields(StringBuilder sends, String varname, RecordClassDescriptor rcd) {
        RecordClassDescriptor parentRcd = rcd.getParent();
        if (parentRcd != null)
            generateFields(sends, varname, parentRcd);

        for (DataField df : rcd.getFields()) {
            if (!(df instanceof NonStaticDataField))
                continue;

            NonStaticDataField dataField = (NonStaticDataField) df;
            generateSafeSampleAssign(varname, dataField.getName(), dataField.getType(), sends);
        }
    }

    private static void generateSafeSampleAssign(String varname, String fieldName, DataType ftype, StringBuilder sends) {
        String encoding = ftype.getEncoding();
        if (ftype instanceof ClassDataType) {
            //todo: complicated
        } else if (ftype instanceof ArrayDataType) {
            //todo: complicated
        } else if (ftype instanceof EnumDataType) {
            writeSimpleAssign (sends, varname, fieldName, "0");
        } else if (ftype instanceof BooleanDataType) {
            writeSimpleAssign (sends, varname, fieldName, ftype.isNullable() ? "DxApi::BOOL_NULL" : "true");
        } else if (ftype instanceof IntegerDataType) {
            IntegerDataType     idt = (IntegerDataType) ftype;
            long                min = idt.getMinNotNull ().longValue ();
            long                max = idt.getMaxNotNull ().longValue ();
            String              value =
                1 >= min && 1 <= max ?
                    "1" :
                    String.valueOf ((min + max) / 2);

            writeSimpleAssign (sends, varname, fieldName, value);
        } else if (ftype instanceof FloatDataType) {
            FloatDataType       fdt = (FloatDataType) ftype;
            double              min = fdt.getMinNotNull ().doubleValue ();
            double              max = fdt.getMaxNotNull ().doubleValue ();

            String              value =
                1.5 >= min && 1.5 <= max ?
                    (fdt.isFloat () ? "1.5F" : "1.5") :
                    (fdt.isFloat () ? String.valueOf (min) + "F" : String.valueOf (min));

            if (encoding.startsWith(FloatDataType.ENCODING_DECIMAL64)) {
                value = "(Decimal64) " + value;
            }

            writeSimpleAssign (sends, varname, fieldName, value);
        } else if (ftype instanceof TimeOfDayDataType) {
            writeSimpleAssign(sends, varname, fieldName, "46888345 /*= 13:01:28.345 */");
        } else if (ftype instanceof CharDataType) {
            writeSimpleAssign(sends, varname, fieldName, "'X'");
        } else if (ftype instanceof DateTimeDataType) {
            writeSimpleAssign(sends, varname, fieldName, "1373321607447L /* = 2013-07-08 22:13:27.447 GMT */");
        } else if (ftype instanceof VarcharDataType) {
            VarcharDataType     vdt = (VarcharDataType) ftype;
            String              value = "\"This is a string\"";

            if (vdt.getEncodingType () == VarcharDataType.ALPHANUMERIC) {
                switch (vdt.getLength ()) {
                    case 1:     value = "\"X\"";    break;
                    case 2:     value = "\"A1\"";    break;
                    default:     value = "\"NY4\"";    break;
                }
            }

            writeSimpleAssign (sends, varname, fieldName, value);
        } else if (ftype instanceof BinaryDataType) {
            writeSimpleAssign(sends, varname, fieldName, "vector<uint8_t>({'h', 'e', 'l', 'l', 'o'})");
        }
    }

    private static void writeSimpleAssign(StringBuilder sends, String varname, String fieldName, String value) {
        writeHead(sends, varname, fieldName);
        sends.append("(");
        sends.append(value);
        sends.append(");\n");
    }

    private static void writeHead(StringBuilder sends, String varname, String fieldName) {
        sends.append(CodegenUtils.INDENT[3]);
        sends.append(varname);
        sends.append(".");
        sends.append(CppCodecGenerator.getSetter(fieldName));
    }
}
