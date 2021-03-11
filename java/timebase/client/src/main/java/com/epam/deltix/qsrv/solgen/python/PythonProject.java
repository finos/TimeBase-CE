package com.epam.deltix.qsrv.solgen.python;

import com.epam.deltix.qsrv.solgen.SolgenUtils;
import com.epam.deltix.qsrv.solgen.base.Project;
import com.epam.deltix.qsrv.solgen.base.Property;
import com.epam.deltix.qsrv.solgen.base.PropertyFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class PythonProject implements Project {

    public static final String PROJECT_TYPE = "pip";

    public static final Property PYTHON_PROJECT_ROOT = PropertyFactory.create(
        "python.project.root",
        "Directory, where project will be stored",
        true,
        SolgenUtils::isValidPath,
        SolgenUtils.getDefaultSamplesDirectory().resolve("python").toString()
    );
    public static final Property PYTHON_PROJECT_NAME = PropertyFactory.create(
        "python.project.name",
        "Project name, like google-collections",
        false,
        SolgenUtils::isValidName,
        "sample"
    );

    public static final List<Property> PYTHON_PROJECT_PROPERTIES =
        Arrays.asList(PYTHON_PROJECT_ROOT, PYTHON_PROJECT_NAME);


    // settings.gradle template params
    private static final String PROJECT_NAME = "PROJECT_NAME";

    private final Path root;
    private final Path srcPath;
    private final String name;
    private final Map<String, String> templateParams = new HashMap<>();

    private final Properties properties = new Properties();

    private final List<Path> scripts = new ArrayList<>();

    public PythonProject(Path root, String name) {
        this.root = root;
        this.name = name;
        this.srcPath = root.resolve("src");
        templateParams.put(PROJECT_NAME, this.name);
    }

    public PythonProject(Properties properties) {
        this(Paths.get(properties.getProperty(PYTHON_PROJECT_ROOT.getName())),
            properties.getProperty(PYTHON_PROJECT_NAME.getName()));
    }

    @Override
    public Path getSourcesRoot() {
        return srcPath;
    }

    @Override
    public Path getResourcesRoot() {
        return null;
    }

    @Override
    public Path getProjectRoot() {
        return root;
    }

    @Override
    public Path getLibsRoot() {
        return null;
    }

    @Override
    public List<Path> getScripts() {
        return scripts;
    }

    @Override
    public void markAsScript(Path path) {
        scripts.add(path);
    }

    @Override
    public void setProjectProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    @Override
    public void createSkeleton() throws IOException {
    }

    @Override
    public void flush() throws IOException {
    }

}
