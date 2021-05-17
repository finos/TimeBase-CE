/*
 * Copyright 2021 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.solgen;

import com.epam.deltix.qsrv.hf.pub.md.JavaBeanGenerator;
import com.epam.deltix.qsrv.solgen.base.Language;
import com.epam.deltix.qsrv.solgen.base.ProjectFactoryBase;
import com.epam.deltix.qsrv.solgen.base.Property;
import com.epam.deltix.qsrv.solgen.base.SampleFactory;
import com.epam.deltix.util.io.IOUtil;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.text.StringSubstitutor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public final class SolgenUtils {

    private SolgenUtils() {
    }

    public static void unzip(String path, Path targetDir) throws IOException {
        Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        IOUtil.mkDirIfNeeded(targetDir.toFile());
        try (ArchiveInputStream i = new ZipArchiveInputStream(Thread.currentThread().getContextClassLoader().getResourceAsStream(path))) {
            ArchiveEntry entry;
            while ((entry = i.getNextEntry()) != null) {
                File f = targetDir.resolve(entry.getName()).toFile();
                if (entry.isDirectory()) {
                    IOUtil.mkDirIfNeeded(f);
                } else {
                    File parent = f.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("failed to create directory " + parent);
                    }
                    try (OutputStream o = Files.newOutputStream(f.toPath())) {
                        IOUtils.copy(i, o);
                    }
                }
            }
        }
    }

    public static void unzip(Package pkg, String name, Path targetDir) throws IOException {
        unzip(getPath(pkg, name), targetDir);
    }

    public static String readFromClassPath(Package pkg, String name) {
        return IOUtil.readTextFromClassPathNoX(getPath(pkg, name));
    }

    public static String readTemplateFromClassPath(Package pkg, String name, Map<String, String> params) {
        String template = readFromClassPath(pkg, name);
        return StringSubstitutor.replace(template, params);
    }

    public static void copyFromClassPath(Package pkg, String name, Path target) throws FileNotFoundException {
        String text = readFromClassPath(pkg, name);
        try (PrintWriter writer = new PrintWriter(target.toFile())) {
            writer.print(text);
        }
    }

    private static String getPath(Package pkg, String name) {
        return pkg.getName().replace('.', '/') + "/" + name;
    }

    public static Path getDefaultSamplesDirectory() {
        return Paths.get(System.getProperty("user.home"), "Deltix", "samples");
    }

    public static String getEscapedName(String name) {
        name = name.replaceAll("[\\s\\.]", "_");
        name = JavaBeanGenerator.escapeIdentifierForJava(name);
        name = name.replaceAll("[-$]", "_");
        return name;
    }

    public static boolean isValidPath(String path) {
        if (path == null) {
            return false;
        }
        try {
            Paths.get(path);
        } catch (InvalidPathException ex) {
            return false;
        }
        return true;
    }

    public static boolean isValidUrl(String url) {
        return true; //todo
    }

    public static boolean isValidName(String s) {
        return s.matches("^[a-z][a-z0-9]*(-[a-z][a-z0-9]*)*$");
    }

    public static String convertLineSeparators(String str, String sep) {
        return str == null ? null : str.replaceAll("\r\n|\r|\n", sep);
    }

    public static String collectDocs() {
        return STARS + "\n" + Arrays.stream(Language.values())
                .map(SolgenUtils::collectDocForLanguage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n" + STARS + "\n")) + "\n" + STARS;
    }

    public static String collectDocForLanguage(Language language) {
        Map<String, List<Property>> projectProps = collectProjectProperties(language);
        Map<String, List<Property>> sampleProps = collectSampleProperties(language);

        if (projectProps.isEmpty() || sampleProps.isEmpty())
            return null;

        StringBuilder sb = new StringBuilder();
        sb.append(language.getTitle()).append(", cmd arg: ").append(language.getCmdOption())
                .append(", project type cmd arg: ").append(language.getCmdOption()).append(".projectType <projectType>")
                .append("\n").append(STARS).append("\n")
                .append("Projects:\n").append(SHORT_STARS).append("\n");
        int i = 1;
        for (Map.Entry<String, List<Property>> entry : projectProps.entrySet()) {
            sb.append(i++).append(". ").append(entry.getKey()).append("\n\n");

            if (entry.getValue().isEmpty()) {
                sb.append("No specific properties for this project type.\n\n");
                continue;
            }

            for (Property property : entry.getValue()) {
                sb.append(property.getName()).append(" - ").append(property.getDoc())
                        .append("\nDefault value: ").append(property.getDefaultValue())
                        .append("\n\n");
            }
        }
        sb.append(SHORT_STARS).append("\nSamples:\n").append(SHORT_STARS).append('\n');

        sb.append("Common properties:\n\n");
        for (Property property : getCommonProperties(language)) {
            sb.append(property.getName()).append(" - ").append(property.getDoc())
                    .append("\nDefault value: ").append(property.getDefaultValue())
                    .append("\n\n");
        }

        i = 1;
        for (Map.Entry<String, List<Property>> entry : sampleProps.entrySet()) {
            sb.append(i++).append(". ").append(entry.getKey()).append(", cmd arg: ").append(language.getCmdOption())
                    .append(".").append(entry.getKey()).append("\n\n");

            if (entry.getValue().isEmpty()) {
                sb.append("No specific properties for this sample type.\n\n");
                continue;
            }

            for (Property property : entry.getValue()) {
                sb.append(property.getName()).append(" - ").append(property.getDoc())
                        .append("\nDefault value: ").append(property.getDefaultValue())
                        .append("\n\n");
            }
        }

        sb.setLength(sb.length() - 1);

        return sb.toString();
    }

    public static Map<String, List<Property>> collectProjectProperties(Language language) {
        ProjectFactoryBase projectFactory = ProjectFactory.getInstance();
        Map<String, List<Property>> result = new HashMap<>();
        List<String> projectTypes = projectFactory.listProjectTypes(language);
        if (projectTypes == null) {
            return result;
        }
        for (String projectType : projectTypes) {
            result.put(projectType, projectFactory.getProperties(language, projectType));
        }
        return result;
    }

    public static Map<String, List<Property>> collectSampleProperties(Language language) {
        SampleFactoryProvider provider = SampleFactoryProvider.getInstance();
        SampleFactory factory = provider.create(language);
        Map<String, List<Property>> result = new HashMap<>();
        if (factory == null) {
            return result;
        }
        for (String sampleType : factory.listSampleTypes()) {
            result.put(sampleType, factory.getSampleProps(sampleType));
        }
        return result;
    }

    public static List<Property> getCommonProperties(Language language) {
        SampleFactoryProvider provider = SampleFactoryProvider.getInstance();
        SampleFactory factory = provider.create(language);
        if (factory == null) {
            return Collections.emptyList();
        }
        return factory.getCommonProps();
    }

    public static Map<String, Property> collectProperties() {
        Map<String, Property> result = new TreeMap<>();
        for (Language value : Language.values()) {
            getCommonProperties(value).forEach(property -> result.put(property.getName(), property));
            collectProjectProperties(value).values().stream().flatMap(Collection::stream)
                    .forEach(property -> result.put(property.getName(), property));
            collectSampleProperties(value).values().stream().flatMap(Collection::stream)
                    .forEach(property -> result.put(property.getName(), property));
        }
        return result;
    }

    public static void main(String[] args) {
        System.out.println(collectDocs());
    }

    private static final String STARS = "****************************************************************************************";
    private static final String SHORT_STARS = "*************************************";

}
