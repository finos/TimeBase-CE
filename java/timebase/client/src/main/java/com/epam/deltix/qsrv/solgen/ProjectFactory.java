package com.epam.deltix.qsrv.solgen;

import com.epam.deltix.qsrv.solgen.base.Project;
import com.epam.deltix.qsrv.solgen.base.ProjectFactoryBase;
import com.epam.deltix.qsrv.solgen.base.Property;
import com.epam.deltix.qsrv.solgen.cpp.MakeProject;
import com.epam.deltix.qsrv.solgen.cpp.VsProject;
import com.epam.deltix.qsrv.solgen.java.GradleProject;
import com.epam.deltix.qsrv.solgen.python.PythonProject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class ProjectFactory implements ProjectFactoryBase {

    private static final ProjectFactory INSTANCE = new ProjectFactory();

    public static ProjectFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public List<String> listJavaProjectTypes() {
        return Collections.singletonList(GradleProject.PROJECT_TYPE);
    }

    @Override
    public Project createJavaProject(String projectType, Properties properties) {
        return new GradleProject(properties);
    }

    @Override
    public List<Property> getJavaProjectProps(String projectType) {
        return GradleProject.PROPERTIES;
    }

    @Override
    public Project createNETProject(String projectType, Properties properties) {
        return new com.epam.deltix.qsrv.solgen.net.VsProject(properties);
    }

    @Override
    public List<String> listNETProjectTypes() {
        return Collections.singletonList(com.epam.deltix.qsrv.solgen.net.VsProject.PROJECT_TYPE);
    }

    @Override
    public List<Property> getNETProjectProps(String projectType) {
        return com.epam.deltix.qsrv.solgen.net.VsProject.PROPERTIES;
    }

    @Override
    public Project createPythonProject(String projectType, Properties properties) {
        return new PythonProject(properties);
    }

    @Override
    public List<String> listPythonProjectTypes() {
        return Collections.singletonList(PythonProject.PROJECT_TYPE);
    }

    @Override
    public List<Property> getPythonProjectProps(String projectType) {
        return PythonProject.PYTHON_PROJECT_PROPERTIES;
    }

    @Override
    public Project createCppProject(String projectType, Properties properties) {
        switch (projectType) {
            case MakeProject.PROJECT_TYPE:
                return new MakeProject(properties);
            case VsProject.PROJECT_TYPE:
                return new VsProject(properties);
            default:
                throw new RuntimeException("Unknown project type");
        }
    }

    @Override
    public List<String> listCppProjectTypes() {
        return Arrays.asList(VsProject.PROJECT_TYPE, MakeProject.PROJECT_TYPE);
    }

    @Override
    public List<Property> getCppProjectProps(String projectType) {
        return VsProject.PROPERTIES;
    }

    @Override
    public Project createGoProject(String projectType, Properties properties) {
        return null;
    }

    @Override
    public List<String> listGoProjectTypes() {
        return null;
    }

    @Override
    public List<Property> getGoProjectProps(String projectType) {
        return null;
    }
}
