package com.epam.deltix.qsrv.solgen.cpp;

import com.epam.deltix.qsrv.solgen.SolgenUtils;
import com.epam.deltix.qsrv.solgen.base.Project;
import com.epam.deltix.qsrv.solgen.base.Sample;
import com.epam.deltix.qsrv.solgen.base.Source;
import com.epam.deltix.qsrv.solgen.base.StringSource;

import java.util.HashMap;
import java.util.Map;

public abstract class CppSample implements Sample {

    private static final String RUN_SAMPLE_TEMPLATE = "run_sample.sh-template";

    private Source runSampleScript;

    private final String functionName;
    private final String stream;

    public CppSample(String functionName, String stream) {
        this.functionName = functionName;
        this.stream = stream;

        Map<String, String> params = new HashMap<>();
        params.put("cpp.sample.name", functionName);
        runSampleScript = new StringSource(
            "../run_" + functionName + ".sh",
            SolgenUtils.convertLineSeparators(
                SolgenUtils.readTemplateFromClassPath(this.getClass().getPackage(), RUN_SAMPLE_TEMPLATE, params),
                "\n"
            )
        );
    }

    public String functionName() {
        return functionName;
    }

    public String getStream() {
        return stream;
    }

    @Override
    public void addToProject(Project project) {
        if (project instanceof MakeProject) {
            project.addSource(runSampleScript);
        }
    }
}
