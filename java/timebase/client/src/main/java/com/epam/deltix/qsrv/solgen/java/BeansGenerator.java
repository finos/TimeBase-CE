package com.epam.deltix.qsrv.solgen.java;

import com.epam.deltix.qsrv.hf.pub.md.JavaBeanGenerator;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.solgen.base.Project;
import com.epam.deltix.qsrv.solgen.base.StringSource;

class BeansGenerator {

    private static final String TYPE_MAP_CLASS_NAME = "TypeMap";
    private static final String PACK = "data";

    void generateBeans(Project project, String tbUrl, String ... keys) {

        StringBuilder typeMap = new StringBuilder();

        typeMap.append(
                "package " + PACK + ";\n\n" +
                        "import com.epam.deltix.qsrv.hf.pub.*;\n\n" +
                        "public class " + TYPE_MAP_CLASS_NAME + " {\n" +
                        "    public static final MappingTypeLoader TYPE_LOADER = new MappingTypeLoader ();\n\n" +
                        "    static {\n"
        );

        JavaBeanGenerator jbg = new JavaBeanGenerator(JavaBeanGenerator.Language.JAVA);

        jbg.setDefaultPackage(PACK);

        try (DXTickDB db = TickDBFactory.openFromUrl(tbUrl, true)) {
            for (String key : keys) {
                DXTickStream s = db.getStream(key);

                if (s.isFixedType())
                    jbg.addClass(s.getFixedType());
                else for (RecordClassDescriptor rcd : s.getPolymorphicDescriptors())
                    jbg.addClass(rcd);
            }
        }

        jbg.process();

        for (JavaBeanGenerator.Bean bean : jbg.beans()) {
            String fullClassName = bean.getNativeClassName();
            project.addSource(new StringSource(fullClassName.replace('.', '/') + ".java",
                    bean.getSourceCode()));
            typeMap.append("        TYPE_LOADER.bind (").append(fullClassName).append(".class);\n");
        }

        typeMap.append("    }\n}\n\n");

        project.addSource(new StringSource(PACK + "/" + TYPE_MAP_CLASS_NAME + ".java", typeMap.toString()));
    }
}
