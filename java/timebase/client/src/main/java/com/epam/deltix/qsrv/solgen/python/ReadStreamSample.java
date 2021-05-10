package com.epam.deltix.qsrv.solgen.python;

import com.epam.deltix.qsrv.solgen.SolgenUtils;
import com.epam.deltix.qsrv.solgen.base.*;
import com.epam.deltix.qsrv.solgen.java.JavaSampleFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class ReadStreamSample extends PythonSample {

    public static final Property STREAM_KEY = PropertyFactory.create(
        "timebase.stream",
        "TimeBase stream key.",
        true,
        StringUtils::isNotEmpty
    );

    static final List<Property> PROPERTIES = Collections.unmodifiableList(Arrays.asList(STREAM_KEY));

    private static final String SAMPLE_NAME = "ReadStream";
    private static final String SCRIPT_NAME = SAMPLE_NAME + ".py";
    private static final String TEMPLATE = SAMPLE_NAME + ".python-template";

    private final Source readStreamSource;

    public ReadStreamSample(Properties properties) {
        this(properties.getProperty(PythonSampleFactory.TB_URL.getName()),
            properties.getProperty(STREAM_KEY.getName())
        );
    }

    public ReadStreamSample(String tbUrl, String stream) {
        Map<String, String> params = new HashMap<>();
        params.put(JavaSampleFactory.TB_URL.getName(), tbUrl);
        params.put(STREAM_KEY.getName(), stream);
        params.put(SAMPLE_NAME_PROP, SAMPLE_NAME);
        params.put(SCRIPT_NAME_PROP, SCRIPT_NAME);

        readStreamSource = new StringSource(
            SCRIPT_NAME,
            SolgenUtils.readTemplateFromClassPath(this.getClass().getPackage(), TEMPLATE, params)
        );

        generateLaunchers(params);
    }

    @Override
    public void addToProject(Project project) {
        super.addToProject(project);

        project.addSource(readStreamSource);
    }

}
