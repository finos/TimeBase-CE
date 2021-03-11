package com.epam.deltix.qsrv.solgen.java.samples;

import com.epam.deltix.qsrv.solgen.SolgenUtils;
import com.epam.deltix.qsrv.solgen.base.*;
import com.epam.deltix.qsrv.solgen.java.JavaSampleFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ListStreamsSample implements JavaSample {

    public static final Property CLASS_NAME = PropertyFactory.create(
            "java.samples.liststreams.className",
            "Short class name like \"SampleClass\" (without package).",
            false,
            JavaSamplesUtil::isValidClassName,
            "ListStreams"
    );
    public static final Property PACKAGE_NAME = PropertyFactory.create(
            "java.samples.liststreams.packageName",
            "Package name like deltix.timebase.sample",
            false,
            JavaSamplesUtil::isValidPackageName,
            "deltix.timebase.sample"
    );

    public static final List<Property> PROPERTIES = List.of();

    private static final String LIST_STREAMS_TEMPLATE = "ListStreams.java-template";

    private final Source listStreamsSource;

    public ListStreamsSample(Properties properties) {
        this(properties.getProperty(JavaSampleFactory.TB_URL.getName()),
                properties.getProperty(PACKAGE_NAME.getName(), PACKAGE_NAME.getDefaultValue()),
                properties.getProperty(CLASS_NAME.getName(), CLASS_NAME.getDefaultValue()));
    }

    public ListStreamsSample(String tbUrl, String packageName, String className) {

        Map<String, String> params = new HashMap<>();
        params.put(JavaSampleFactory.TB_URL.getName(), tbUrl);
        params.put(PACKAGE_NAME.getName(), packageName);
        params.put(CLASS_NAME.getName(), className);

        listStreamsSource = new StringSource(packageName.replace('.', '/') + "/" + className + ".java",
                SolgenUtils.readTemplateFromClassPath(this.getClass().getPackage(), LIST_STREAMS_TEMPLATE, params));
    }

    @Override
    public void addToProject(Project project) {
        project.addSource(listStreamsSource);
    }

    @Override
    public boolean generateBeans() {
        return false;
    }

    @Override
    public String key() {
        return null;
    }

    @Override
    public String tbUrl() {
        return null;
    }
}
