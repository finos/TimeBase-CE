package com.epam.deltix.qsrv.solgen.python;

import com.epam.deltix.qsrv.solgen.SolgenUtils;
import com.epam.deltix.qsrv.solgen.base.*;
import com.epam.deltix.qsrv.solgen.java.JavaSampleFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class SpeedTestSample extends PythonSample {

    public static final Property STREAM_KEY = PropertyFactory.create(
        "timebase.stream",
        "TimeBase stream key.",
        true,
        StringUtils::isNotEmpty
    );

    public static final Property NUM_PROCESS = PropertyFactory.create(
        "python.SpeedTest.numProcess",
        "Num processes (number)",
        true,
        (s) -> {
            try {
                int value = Integer.parseInt(s);
                return value > 0;
            } catch (Throwable t) {
                return false;
            }
        },
        "4"
    );

    static final List<Property> PROPERTIES = Collections.unmodifiableList(Arrays.asList(STREAM_KEY, NUM_PROCESS));

    private static final String SAMPLE_NAME = "SpeedTest";
    private static final String SCRIPT_NAME = SAMPLE_NAME + ".py";
    private static final String TEMPLATE_THREAD = SAMPLE_NAME + "Thread.python-template";
    private static final String TEMPLATE_PROCESS = SAMPLE_NAME + "Process.python-template";

    private final Source source;

    public SpeedTestSample(Properties properties) {
        this(properties.getProperty(PythonSampleFactory.TB_URL.getName()),
            properties.getProperty(STREAM_KEY.getName()),
            properties.getProperty(NUM_PROCESS.getName())
        );
    }

    public SpeedTestSample(String tbUrl, String stream, String numProcess) {
        Map<String, String> params = new HashMap<>();
        params.put(JavaSampleFactory.TB_URL.getName(), tbUrl);
        params.put(STREAM_KEY.getName(), stream);
        params.put(SAMPLE_NAME_PROP, SAMPLE_NAME);
        params.put(SCRIPT_NAME_PROP, SCRIPT_NAME);
        params.put(NUM_PROCESS.getName(), numProcess);

        int processes = Integer.parseInt(numProcess);

        source = new StringSource(
            SCRIPT_NAME,
            SolgenUtils.readTemplateFromClassPath(this.getClass().getPackage(), processes > 1 ? TEMPLATE_PROCESS : TEMPLATE_THREAD, params)
        );

        generateLaunchers(params);
    }

    @Override
    public void addToProject(Project project) {
        super.addToProject(project);

        project.addSource(source);
    }

}
