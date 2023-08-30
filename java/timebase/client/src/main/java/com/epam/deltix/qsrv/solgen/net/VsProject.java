/*
 * Copyright 2023 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.qsrv.solgen.net;

import com.epam.deltix.qsrv.solgen.SolgenUtils;
import com.epam.deltix.qsrv.solgen.base.Project;
import com.epam.deltix.qsrv.solgen.base.Property;
import com.epam.deltix.qsrv.solgen.base.PropertyFactory;
import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class VsProject implements Project {

    public static final String PROJECT_TYPE = "vs2010+";

    public static final Property NET_PROJECT_ROOT = PropertyFactory.create(
            "net.project.root",
            "Directory, where project will be stored",
            false,
            SolgenUtils::isValidPath,
            SolgenUtils.getDefaultSamplesDirectory().resolve("NET").toAbsolutePath().toString()
    );
    public static final Property NET_PROJECT_NAME = PropertyFactory.create(
            "net.project.name",
            "Project name",
            false,
            StringUtils::isNotEmpty,
            "TimebaseSample"
    );

    public static final List<Property> PROPERTIES = Collections.unmodifiableList(Arrays.asList(
            NET_PROJECT_ROOT, NET_PROJECT_NAME
    ));

    private static final String DELTIX_REPOSITORY_PROP = "deltix.repository";
    private static final String RTMATH_REPOSITORY_PROP = "rtmath.repository";
    private static final String TB_VERSION_PROP = "deltix.timebase.version";
    private static final String MESSAGES_VERSION_PROP = "deltix.messages.version";

    private static final String DEFAULT_DELTIX_REPO = "https://artifactory.epam.com/artifactory/api/nuget/EPM-RTC-net";
    private static final String DEFAULT_RTMATH_REPO = "https://artifactory.epam.com/artifactory/api/nuget/EPM-RTC-net";
    private static final String DEFAULT_TB_VERSION = "6.0.1";
    private static final String DEFAULT_MESSAGES_VERSION = "6.0.34";

    private static final String SLN_TEMPLATE = "TimebaseSample.sln-template";
    private static final String CSPROJ_TEMPLATE = "TimebaseSample.csproj-template";
    private static final String NUGET_TEMPLATE = "NuGet.config-template";

    private final Path root;
    private final String name;

    private final List<Path> scripts = new ArrayList<>();
    private final Map<String, String> templateParams = new HashMap<>();

    public VsProject(Path root, String name) {
        this.root = root;
        this.name = name;

        templateParams.put(NET_PROJECT_NAME.getName(), name);
        templateParams.put(TB_VERSION_PROP, DEFAULT_TB_VERSION);
        templateParams.put(MESSAGES_VERSION_PROP, DEFAULT_MESSAGES_VERSION);
    }

    public VsProject(Properties properties) {
        this(Paths.get(properties.getProperty(NET_PROJECT_ROOT.getName())),
                properties.getProperty(NET_PROJECT_NAME.getName()));
    }

    @Override
    public Path getSourcesRoot() {
        return root;
    }

    @Override
    public Path getResourcesRoot() {
        return root;
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
        templateParams.put(key, value);
    }

    @Override
    public void createSkeleton() throws IOException {
        Files.createDirectories(root);
        Files.createDirectories(root.resolve("Properties"));
    }

    @Override
    public void flush() throws IOException {
        createSln();
        createCsproj();
        createNuget();
    }

    public void createSln() throws FileNotFoundException {
        String sln = SolgenUtils.readTemplateFromClassPath(this.getClass().getPackage(), SLN_TEMPLATE, templateParams);
        try (PrintWriter writer = new PrintWriter(root.resolve(name + ".sln").toFile())) {
            writer.print(sln);
        }
    }

    public void createCsproj() throws FileNotFoundException {
        String sln = SolgenUtils.readTemplateFromClassPath(this.getClass().getPackage(), CSPROJ_TEMPLATE, templateParams);
        try (PrintWriter writer = new PrintWriter(root.resolve(name + ".csproj").toFile())) {
            writer.print(sln);
        }
    }

    public void createNuget() throws FileNotFoundException {
        String sln = SolgenUtils.readTemplateFromClassPath(this.getClass().getPackage(), NUGET_TEMPLATE, templateParams);
        try (PrintWriter writer = new PrintWriter(root.resolve("NuGet.config").toFile())) {
            writer.print(sln);
        }
    }
}