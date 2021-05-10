package com.epam.deltix.util.os;

//import com.epam.deltix.anvil.util.Reusable;
import com.epam.deltix.util.io.Home;
import org.apache.commons.text.StringSubstitutor;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class SystemdServiceBuilder {

    private static final String systemdTemplate;

    static {
        InputStream systemdStream = SystemdServiceBuilder.class.getClassLoader()
                .getResourceAsStream("com/epam/deltix/installer/admin/model/systemd.service");
        assert systemdStream != null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(systemdStream));
        systemdTemplate = reader.lines().reduce((x, y) -> String.join("\n", x, y)).get();
    }

    private String user;
    private String description = "";
    private String exec;
    private Path workingDir = Paths.get("").toAbsolutePath();
    private Path deltixHome = Paths.get(Home.get()).toAbsolutePath();
    private final List<String> javaOpts = new ArrayList<>();
    private final Map<String, String> templateParams = new HashMap<>();

    public SystemdServiceBuilder withUser(@Nonnull String user) {
        this.user = user;
        return this;
    }

    public SystemdServiceBuilder withExec(@Nonnull String exec) {
        this.exec = exec;
        return this;
    }

    public SystemdServiceBuilder withDescription(@Nonnull String description) {
        this.description = description;
        return this;
    }

    public SystemdServiceBuilder withWorkingDir(@Nonnull Path workingDir) {
        this.workingDir = workingDir;
        return this;
    }

    public SystemdServiceBuilder withDeltixHome(@Nonnull Path deltixHome) {
        this.deltixHome = deltixHome;
        return this;
    }

    public SystemdServiceBuilder withJavaOptions(@Nonnull String ... javaOptions) {
        javaOpts.addAll(Arrays.asList(javaOptions));
        return this;
    }

    public SystemdServiceBuilder withJavaOptions(@Nonnull List<String> javaOptions) {
        javaOpts.addAll(javaOptions);
        return this;
    }

    //@Override
    public void reuse() {
        user = null;
        description = "";
        exec = null;
        workingDir = Paths.get("").toAbsolutePath();
        deltixHome = Paths.get(Home.get()).toAbsolutePath();
        javaOpts.clear();
        templateParams.clear();
    }

    private void validate() throws IllegalArgumentException {
        if (user == null) {
            throw new IllegalArgumentException("User must be set in systemd service.");
        } else if (exec == null) {
            throw new IllegalArgumentException("Executable must be set in systemd service.");
        }
    }

    public String build() {
        validate();
        templateParams.put("DELTIX_HOME", deltixHome.toString());
        templateParams.put("JAVA_OPTS", String.join(" ", javaOpts));
        templateParams.put("description", description);
        templateParams.put("user", user);
        templateParams.put("workingDir", workingDir.toString());
        templateParams.put("executable", exec);
        return StringSubstitutor.replace(systemdTemplate, templateParams);
    }

}
