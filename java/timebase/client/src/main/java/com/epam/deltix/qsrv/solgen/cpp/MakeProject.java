package com.epam.deltix.qsrv.solgen.cpp;

import com.epam.deltix.qsrv.solgen.SolgenUtils;
import com.epam.deltix.qsrv.solgen.base.*;
import com.epam.deltix.qsrv.solgen.java.GradleProject;
import com.epam.deltix.util.io.BasicIOUtil;
import com.epam.deltix.util.io.Home;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MakeProject implements Project {

    public static final String PROJECT_TYPE = "make";

    public static final Property CPP_PROJECT_ROOT = PropertyFactory.create(
        "cpp.project.root",
        "Directory, where project will be stored",
        true,
        SolgenUtils::isValidPath,
        SolgenUtils.getDefaultSamplesDirectory().resolve("cpp").toString()
    );
    public static final Property CPP_PROJECT_NAME = PropertyFactory.create(
        "cpp.project.name",
        "Project name, like google-collections",
        false,
        GradleProject::isValidName,
        "sample-project"
    );

    public static final List<Property> PROPERTIES = Collections.unmodifiableList(Arrays.asList(
        CPP_PROJECT_ROOT, CPP_PROJECT_NAME
    ));

    private static final String MAKE_RELEASE_TEMPLATE = "make_release.sh-template";
    private static final String MAKEFILE_TEMPLATE = "Makefile-template";

    private final Path root;
    private final Path srcPath;
    private final Path libPath;
    private final Path dfpLibPath;
    private final String name;
    private final Map<String, String> templateParams = new HashMap<>();

    private final List<Path> scripts = new ArrayList<>();

    private final Properties properties = new Properties();

    public MakeProject(Properties properties) {
        this(Paths.get(properties.getProperty(CPP_PROJECT_ROOT.getName())),
            properties.getProperty(CPP_PROJECT_NAME.getName()));
    }

    public MakeProject(Path root, String name) {
        this.root = root;
        this.srcPath = root.resolve("src");
        this.libPath = root.resolve("dxapi");
        this.dfpLibPath = root.resolve("dfp");
        this.name = name;
        templateParams.put(CPP_PROJECT_ROOT.getName(), this.name);
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
        return libPath;
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
        copyLibs();
    }

    @Override
    public void flush() throws IOException {
        String makeRelaseScript = SolgenUtils.convertLineSeparators(
            SolgenUtils.readTemplateFromClassPath(this.getClass().getPackage(), MAKE_RELEASE_TEMPLATE, new HashMap<>()),
            "\n"
        );
        try (PrintWriter writer = new PrintWriter(root.resolve("make_release.sh").toFile())) {
            writer.print(makeRelaseScript);
        }

        String makefile = SolgenUtils.convertLineSeparators(
            SolgenUtils.readTemplateFromClassPath(this.getClass().getPackage(), MAKEFILE_TEMPLATE, new HashMap<>()),
            "\n"
        );
        try (PrintWriter writer = new PrintWriter(root.resolve("Makefile").toFile())) {
            writer.print(makefile);
        }
    }

    public String getName() {
        return name;
    }

    private void copyLibs() {
        try {
            BasicIOUtil.copyDirectory(
                Home.getFile("cpp", "dxapi", "linux"),
                libPath.toFile(),
                true, true, null, null
            );

            BasicIOUtil.copyDirectory(
                Home.getFile("cpp", "dfp", "include"),
                dfpLibPath.resolve("include").resolve("dfp").toFile(),
                true, true, null, null
            );
            BasicIOUtil.copyDirectory(
                Home.getFile("cpp", "dfp", "lib", "linux", "64"),
                dfpLibPath.resolve("lib").toFile(),
                true, true, null, null
            );
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
