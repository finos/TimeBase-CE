package com.epam.deltix.qsrv.solgen.cpp;

import com.epam.deltix.qsrv.solgen.SolgenUtils;
import com.epam.deltix.qsrv.solgen.base.*;
import com.epam.deltix.qsrv.solgen.java.JavaSampleFactory;

import java.util.*;

public class ListStreamsSample extends CppSample {

    static final List<Property> PROPERTIES = Collections.emptyList();

    private static final String SAMPLE_NAME = "list-streams";
    private static final String SCRIPT_NAME = SAMPLE_NAME + ".cpp";
    private static final String TEMPLATE = SAMPLE_NAME + ".cpp-template";

    private final Source source;

    public ListStreamsSample(Properties properties) {
        this(properties.getProperty(CppSampleFactory.TB_URL.getName()));
    }

    public ListStreamsSample(String tbUrl) {
        super("liststreams", null);
        Map<String, String> params = new HashMap<>();
        params.put(JavaSampleFactory.TB_URL.getName(), tbUrl);

        source = new StringSource(
            SCRIPT_NAME,
            SolgenUtils.readTemplateFromClassPath(this.getClass().getPackage(), TEMPLATE, params)
        );
    }

    @Override
    public void addToProject(Project project) {
        super.addToProject(project);

        project.addSource(source);
    }

}
