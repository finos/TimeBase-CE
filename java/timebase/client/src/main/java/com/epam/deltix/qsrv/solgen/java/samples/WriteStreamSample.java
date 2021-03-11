package com.epam.deltix.qsrv.solgen.java.samples;

import com.epam.deltix.anvil.util.StringUtil;
import com.epam.deltix.qsrv.solgen.SolgenUtils;
import com.epam.deltix.qsrv.solgen.base.*;
import com.epam.deltix.qsrv.solgen.java.JavaSampleFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class WriteStreamSample implements JavaSample {

    public static final Property STREAM_KEY = PropertyFactory.create(
            "timebase.stream",
            "TimeBase stream key.",
            true,
            StringUtil::isNotEmpty
    );
    public static final Property CLASS_NAME = PropertyFactory.create(
            "java.samples.writestream.className",
            "Short class name like \"SampleClass\" (without package).",
            false,
            JavaSamplesUtil::isValidClassName,
            "WriteStream"
    );
    public static final Property PACKAGE_NAME = PropertyFactory.create(
            "java.samples.writestream.packageName",
            "Package name like deltix.timebase.sample",
            false,
            JavaSamplesUtil::isValidPackageName,
            "deltix.timebase.sample"
    );

    public static final List<Property> PROPERTIES = List.of(STREAM_KEY);

    private static final String WRITE_STREAM_TEMPLATE = "WriteStream.java-template";

    private final Source writeStreamSource;
    private final String key;
    private final String tbUrl;

    public WriteStreamSample(Properties properties) {
        this(properties.getProperty(JavaSampleFactory.TB_URL.getName()),
                properties.getProperty(STREAM_KEY.getName()),
                properties.getProperty(PACKAGE_NAME.getName(), PACKAGE_NAME.getDefaultValue()),
                properties.getProperty(CLASS_NAME.getName(), CLASS_NAME.getDefaultValue()));
    }

    public WriteStreamSample(String tbUrl, String key, String packageName, String className) {
        this.tbUrl = tbUrl;
        this.key = key;
        Map<String, String> params = new HashMap<>();
        params.put(JavaSampleFactory.TB_URL.getName(), tbUrl);
        params.put(STREAM_KEY.getName(), key);
        params.put(PACKAGE_NAME.getName(), packageName);
        params.put(CLASS_NAME.getName(), className);

        writeStreamSource = new StringSource(packageName.replace('.', '/') + "/" + className + ".java",
                SolgenUtils.readTemplateFromClassPath(this.getClass().getPackage(), WRITE_STREAM_TEMPLATE, params));
    }

    @Override
    public void addToProject(Project project) {
        project.addSource(writeStreamSource);
    }

    @Override
    public boolean generateBeans() {
        return true;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String tbUrl() {
        return tbUrl;
    }
}
