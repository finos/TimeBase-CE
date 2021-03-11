package com.epam.deltix.qsrv.solgen.python;

import com.epam.deltix.qsrv.solgen.SolgenUtils;
import com.epam.deltix.qsrv.solgen.base.Project;
import com.epam.deltix.qsrv.solgen.base.Sample;
import com.epam.deltix.qsrv.solgen.base.Source;
import com.epam.deltix.qsrv.solgen.base.StringSource;

import java.util.*;

public class MainSample implements Sample {

    private static final String DEFAULT_REPO = "nexus.deltixhub.com/repository/epm-rtc-python";

    private static final String REPOSITORY_LINUX = "https://DELTIX_REPOSITORY_USER:DELTIX_REPOSITORY_PASS@" + DEFAULT_REPO + "/simple";
    private static final String REPOSITORY_WINDOWS = "https://%DELTIX_REPOSITORY_USER%:%DELTIX_REPOSITORY_PASS%@" + DEFAULT_REPO + "/simple";

    protected static final String REPOSITORY_PROP = "python.pip.repository";

    private static final String REQUIREMENTS_TEMPLATE = "requirements.txt-template";
    private static final String CMD_INIT = "init.cmd-template";
    private static final String SH_INIT = "init.sh-template";

    private Source requirements;
    private Source windowsInitScript;
    private Source linuxInitScript;

    private final List<Sample> samples = new ArrayList<>();

    public MainSample(Properties properties, PythonSample... samples) {
        this(properties, Arrays.asList(samples));
    }

    public MainSample(Properties properties, List<PythonSample> samples) {
        this.samples.addAll(samples);

        Map<String, String> params = new HashMap<>();

        requirements = new StringSource(
            "../requirements.txt",
            SolgenUtils.readTemplateFromClassPath(this.getClass().getPackage(), REQUIREMENTS_TEMPLATE, params)
        );

        params.put(REPOSITORY_PROP, REPOSITORY_WINDOWS);
        windowsInitScript = new StringSource(
            "init.cmd",
            SolgenUtils.readTemplateFromClassPath(this.getClass().getPackage(), CMD_INIT, params)
        );

        params.put(REPOSITORY_PROP, REPOSITORY_LINUX);
        linuxInitScript = new StringSource(
            "init.sh",
            SolgenUtils.convertLineSeparators(
                SolgenUtils.readTemplateFromClassPath(this.getClass().getPackage(), SH_INIT, params),
                "\n"
            )
        );
    }

    @Override
    public void addToProject(Project project) {
        if (requirements != null) {
            project.addSource(requirements);
        }

        if (windowsInitScript != null) {
            project.addScript(windowsInitScript);
        }

        if (linuxInitScript != null) {
            project.addScript(linuxInitScript);
        }

        samples.forEach(s -> s.addToProject(project));
    }
}
