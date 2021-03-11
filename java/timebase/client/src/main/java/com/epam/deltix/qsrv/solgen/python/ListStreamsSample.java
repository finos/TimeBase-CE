package com.epam.deltix.qsrv.solgen.python;

import com.epam.deltix.qsrv.solgen.SolgenUtils;
import com.epam.deltix.qsrv.solgen.base.*;
import com.epam.deltix.qsrv.solgen.java.JavaSampleFactory;

import java.util.*;

public class ListStreamsSample extends PythonSample {

    static final List<Property> PROPERTIES = Collections.emptyList();

    private static final String SAMPLE_NAME = "ListStreams";
    private static final String SCRIPT_NAME = SAMPLE_NAME + ".py";
    private static final String TEMPLATE = SAMPLE_NAME + ".python-template";

    private final Source listStreamsSource;

    public ListStreamsSample(Properties properties) {
        this(properties.getProperty(JavaSampleFactory.TB_URL.getName()));
    }

    public ListStreamsSample(String tbUrl) {
        Map<String, String> params = new HashMap<>();
        params.put(JavaSampleFactory.TB_URL.getName(), tbUrl);
        params.put(SAMPLE_NAME_PROP, SAMPLE_NAME);
        params.put(SCRIPT_NAME_PROP, SCRIPT_NAME);

        listStreamsSource = new StringSource(
            SCRIPT_NAME,
            SolgenUtils.readTemplateFromClassPath(this.getClass().getPackage(), TEMPLATE, params)
        );

        generateLaunchers(params);
    }
//
//    private String generateLauncher(String scriptName, boolean windows) {
//        StringBuilder script = new StringBuilder ();
//
//        if (!windows) {
//            script.append ("#! /bin/sh\n\n");
//        } else {
//            script.append ("@echo off").append("\r\n");
//        }
//        script.append("python");
//        script.append(Util.IS_WINDOWS_OS ? " main.py " : " ./main.py ");
//        script.append(scriptName).append("\r\n");
//
//        if (!windows) {
//            script.append("if [ -z ${1+x} ] || [ \"$1\" != \"-force\" ]; then").append("\r\n");
//            script.append("read -p \"Press enter to exit\" nothing\n").append("\r\n");
//            script.append("fi");
//        } else {
//            script.append("if not \"%1\"==\"-force\" pause");
//        }
//
//        return script.toString();
//    }

    @Override
    public void addToProject(Project project) {
        super.addToProject(project);

        project.addSource(listStreamsSource);
    }

}
