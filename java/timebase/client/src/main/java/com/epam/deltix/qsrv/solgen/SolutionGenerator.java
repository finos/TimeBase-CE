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

import com.google.common.collect.Sets;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.solgen.base.*;
import com.epam.deltix.util.cmdline.AbstractShell;

import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SolutionGenerator extends AbstractShell {

    private static final Log LOG = LogFactory.getLog(SolutionGenerator.class);

    private final ProjectFactoryBase projectFactory = new ProjectFactory();
    private final SampleFactoryProviderBase sampleFactoryProvider = new SampleFactoryProvider();
    private final Properties properties = new Properties();
    private final Map<String, Property> availableProperties;

    private Language language = Language.JAVA;
    private Set<String> samples = new HashSet<>();

    protected SolutionGenerator(String[] args) {
        super(args);
        availableProperties = SolgenUtils.collectProperties();
    }

    protected boolean buildProject() throws ValidationException, IOException {
        if (language == null) {
            System.err.println("'language' property is not set.");
            return true;
        }
        List<String> projectTypes = projectFactory.listProjectTypes(language);
        if (projectTypes == null || projectTypes.isEmpty()) {
            System.err.printf("Project for %s has not been implemented yet.\n", language.getTitle());
            return true;
        }
        String projectType;
        if (projectTypes.size() == 1) {
            projectType = projectTypes.get(0);
        } else {
            projectType = properties.getProperty(language.getSetOption() + ".project-type", projectTypes.get(0));
        }
        List<Property> propNames = projectFactory.getProperties(language, projectType);
        if (propNames == null) {
            System.err.printf("Project for %s has not been implemented yet.\n", language.getTitle());
            return true;
        }
        Properties projectProps = new Properties();
        addPropertiesFromSet(propNames, projectProps);
        Project project = projectFactory.create(language, projectType, projectProps);
        project.createSkeleton();

        SampleFactory sampleFactory = sampleFactoryProvider.create(language);
        List<String> sampleTypes;
        if (samples.isEmpty()) {
            sampleTypes = sampleFactory.listSampleTypes();
        } else {
            sampleTypes = sampleFactory.listSampleTypes().stream()
                    .filter(samples::contains)
                    .collect(Collectors.toList());
        }
        if (sampleTypes.isEmpty()) {
            System.err.printf("No sample types for language %s are set. List of available options: %s.",
                    language.getTitle(), sampleFactory.listSampleTypes().stream().map(s -> language.getCmdOption() + "." + s).collect(Collectors.toList()));
            return true;
        }

        List<Property> commonSamplePropDefs = sampleFactory.getCommonProps();
        List<Property> samplePropDefs = sampleFactory.getSampleProps(sampleTypes);

        Properties sampleProperties = new Properties();
        addPropertiesFromSet(commonSamplePropDefs, sampleProperties);
        addPropertiesFromSet(samplePropDefs, sampleProperties);

        Sample sample = sampleFactory.create(sampleTypes, sampleProperties);
        sample.addToProject(project);

        project.flush();
        makeScriptsExecutable(project);
        System.err.printf(
                "%s project %s has been successfully created in directory %s with samples %s.\n",
                language.getTitle(), project.getProjectRoot().getFileName(), project.getProjectRoot(), sampleTypes
        );
        return true;
    }

    @Override
    protected void run() throws Throwable {
        boolean runShell = true;
        for (Language language : Language.values()) {
            if (isArgSpecified(language.getCmdOption())) {
                runShell = false;


                List<String> projectTypes = projectFactory.listProjectTypes(language);
                if (projectTypes == null || projectTypes.isEmpty()) {
                    System.err.printf("Project for %s has not been implemented yet.\n", language.getTitle());
                    continue;
                }

                String projectType;
                if (projectTypes.size() == 1) {
                    projectType = projectTypes.get(0);
                } else {
                    projectType = getArgValue(language.getCmdOption() + ".project-type", projectTypes.get(0));
                }

                List<Property> propNames = projectFactory.getProperties(language, projectType);
                if (propNames == null) {
                    System.err.printf("Project for %s has not been implemented yet.\n", language.getTitle());
                    continue;
                }
                Properties projectProps = new Properties();
                addPropertiesFromArgs(propNames, projectProps);
                Project project = projectFactory.create(language, projectType, projectProps);
                project.createSkeleton();


                SampleFactory sampleFactory = sampleFactoryProvider.create(language);
                List<String> sampleTypes = sampleFactory.listSampleTypes().stream()
                        .filter(s -> isArgSpecified(language.getCmdOption() + "." + s))
                        .collect(Collectors.toList());
                if (sampleTypes.isEmpty()) {
                    System.err.printf("No sample types for language %s are set. List of available options: %s. Ignoring language.",
                            language.getTitle(), sampleFactory.listSampleTypes().stream().map(s -> language.getCmdOption() + "." + s).collect(Collectors.toList()));
                }

                List<Property> commonSamplePropDefs = sampleFactory.getCommonProps();
                List<Property> samplePropDefs = sampleFactory.getSampleProps(sampleTypes);

                Properties sampleProperties = new Properties();
                addPropertiesFromArgs(commonSamplePropDefs, sampleProperties);
                addPropertiesFromArgs(samplePropDefs, sampleProperties);

                Sample sample = sampleFactory.create(sampleTypes, sampleProperties);
                sample.addToProject(project);

                project.flush();
                makeScriptsExecutable(project);
                System.err.printf(
                        "%s project %s has been successfully created in directory %s with samples %s.\n",
                        language.getTitle(), project.getProjectRoot().getFileName(), project.getProjectRoot(), sampleTypes
                );
            }
        }

        if (runShell) {
            System.err.println("Solution Generator CLI. ");
            System.err.println("Please, enter " + Arrays.toString(Language.values()) + " to start interactive mode.");
            super.run();
        } else {
            doQuit();
        }
    }

    @Override
    protected boolean doSet(String option, String value) throws Exception {
        if (option.equalsIgnoreCase("language")) {
            language = Language.valueOf(value.toUpperCase());
            System.err.println("Language " + language.getTitle() + " is set.");
            return true;
        } else if (option.equalsIgnoreCase("samples")) {
            samples = new HashSet<>(Arrays.asList(value.split(",")));
            System.err.println("Samples: [" + String.join(",", samples) + "]");
            return true;
        } else if (availableProperties.containsKey(option)) {
            if (availableProperties.get(option).getValueValidator().test(value, properties)) {
                properties.setProperty(option, value);
                System.err.println(option + "=" + value);
            } else {
                System.err.printf("Value %s is not valid for property %s.\n", value, option);
            }
            return true;
        }
        return super.doSet(option, value);
    }

    @Override
    protected void doSet() {
        availableProperties.values().forEach(property -> System.out.println(beautifyString(property.getName(), 30) + property.getDefaultValue()));
        super.doSet();
    }

    private static String beautifyString(String string, int length) {
        if (string.length() > length) {
            return string;
        }
        return string + IntStream.range(0, length - string.length()).mapToObj(i -> " ").collect(Collectors.joining());
    }

    @Override
    protected boolean doCommand(String key, String args, String fileId, LineNumberReader rd) throws Exception {
        for (Language language : Language.values()) {
            if (key.equalsIgnoreCase(language.name())) {
                List<String> projectTypes = projectFactory.listProjectTypes(language);
                if (projectTypes == null || projectTypes.isEmpty()) {
                    System.err.println("This type of project has not been implemented yet.");
                    return true;
                }
                String projectType;
                if (projectTypes.size() == 1) {
                    projectType = projectTypes.get(0);
                } else {
                    projectType = projectTypes.get(select("Select project type.", projectTypes, rd));
                }

                List<Property> propNames = projectFactory.getProperties(language, projectType);
                if (propNames == null) {
                    System.err.println("This type of project has not been implemented yet.");
                    return true;
                }
                Properties projectProps = new Properties();
                addProperties(propNames, rd, projectProps);
                Project project = projectFactory.create(language, projectType, projectProps);
                project.createSkeleton();
                if (getYesOrNo("Do you want to set some additional properties?", false, rd)) {
                    project.setProjectProperties(getCustomPropertiesFromUser(rd));
                }

                SampleFactory sampleFactory = sampleFactoryProvider.create(language);
                List<String> availableSamples = sampleFactory.listSampleTypes();

                List<String> sampleTypes = multiSelect("Select sample types.", availableSamples, rd, false);
                List<Property> commonSamplePropDefs = sampleFactory.getCommonProps();
                List<Property> samplePropDefs = sampleFactory.getSampleProps(sampleTypes);
                Properties sampleProperties = new Properties();
                addProperties(commonSamplePropDefs, rd, sampleProperties);
                addProperties(samplePropDefs, rd, sampleProperties);
                Sample sample = sampleFactory.create(sampleTypes, sampleProperties);
                sample.addToProject(project);

                project.flush();
                makeScriptsExecutable(project);
                System.err.printf(
                        "%s project %s has been successfully created in directory %s with samples %s.\n",
                        language.getTitle(), project.getProjectRoot().getFileName(), project.getProjectRoot(), sampleTypes
                );
                return true;
            }
        }
        if (key.equalsIgnoreCase("build")) {
            return buildProject();
        } else if (key.equalsIgnoreCase("clear")) {
            properties.clear();
            System.err.println("Cleared set properties.");
            return true;
        }
        return super.doCommand(key, args, fileId, rd);
    }

    protected void addPropertiesFromArgs(List<Property> properties, Properties toAdd) throws ValidationException {
        for (Property property : properties) {
            String value = getArgValue(property.getName());
            if (property.isValid(value, toAdd) || notRequired(property, value)) {
                toAdd.setProperty(property.getName(), value == null ? property.getDefaultValue() : value.trim());
            } else {
                throw new ValidationException(property, value);
            }
        }
    }

    protected void addPropertiesFromSet(List<Property> list, Properties toAdd) throws ValidationException {
        for (Property property : list) {
            String value = properties.getProperty(property.getName());
            if (property.isValid(value, toAdd) || notRequired(property, value)) {
                toAdd.setProperty(property.getName(), value == null ? property.getDefaultValue() : value.trim());
            } else {
                throw new ValidationException(property, value);
            }
        }
    }

    protected void addProperties(List<Property> properties, LineNumberReader reader, Properties toAdd) throws IOException {
        String line;
        for (Property property : properties) {
            do {
                System.err.println(property.getDoc());
                System.err.println(
                        "This property " + (property.isRequired() ? "is required. " : "is not required. ") +
                                (property.getDefaultValue() != null ? "Default value: " + property.getDefaultValue() : "")
                );
                System.err.printf("%s%s=", getPrompt(), property.getName());
                System.err.flush();
                line = reader.readLine();
            } while ((!property.isValid(line, toAdd)) && !notRequired(property, line));
            toAdd.setProperty(property.getName(), line == null || line.trim().isEmpty() ? property.getDefaultValue() : line.trim());
        }
    }

    protected List<String> multiSelect(String question, List<String> options, LineNumberReader reader, boolean couldBeEmpty) throws IOException {
        System.err.println(question);
        assert !options.isEmpty();
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (String option : options) {
            if (sb.length() > 40) {
                sb.append('\n');
            }
            sb.append('(').append(i++).append(") - ").append(option).append(", ");
        }
        sb.append("(A) - all");
        System.err.println(sb);
        List<String> result = new ArrayList<>();
        for (; ; ) {
            System.err.print("Input space-separated option numbers, like '3 5 1': ");
            String line = reader.readLine();
            if (line == null || (line.isEmpty() && !couldBeEmpty)) {
                System.err.println("Invalid input.");
            } else if (line.isEmpty()) {
                return result;
            } else {
                if (line.trim().equalsIgnoreCase("a")) {
                    result.addAll(options);
                    System.err.println("Selected: " + result);
                    break;
                } else {
                    boolean wrongNumber = false;
                    try {
                        Arrays.stream(line.trim().split(" "))
                                .mapToInt(Integer::parseInt)
                                .mapToObj(n -> options.get(n - 1))
                                .forEach(result::add);
                    } catch (NumberFormatException | IndexOutOfBoundsException exc) {
                        wrongNumber = true;
                    }
                    if (!wrongNumber) {
                        System.err.println("Selected: " + result);
                        break;
                    }
                    System.err.println("Invalid input");
                }
            }
        }
        return result;
    }

    protected int select(String question, List<String> options, LineNumberReader reader) throws IOException {
        System.err.println(question);
        assert !options.isEmpty();
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (String option : options) {
            if (sb.length() > 40) {
                sb.append('\n');
            }
            sb.append('(').append(i++).append(") - ").append(option).append(", ");
        }
        sb.setLength(sb.length() - 2);
        System.err.println(sb);
        int number = 0;
        for (; ; ) {
            System.err.print("Input option number: ");
            String line = reader.readLine();
            if (line == null || line.isEmpty()) {
                System.err.println("Invalid input.");
            } else {
                boolean wrongNumber = false;
                try {
                    number = Integer.parseInt(line);
                    if (number < 1 || number > options.size()) {
                        wrongNumber = true;
                    }
                } catch (NumberFormatException exc) {
                    wrongNumber = true;
                }
                if (!wrongNumber) {
                    System.err.printf("Selected %d - %s.\n", number, options.get(number - 1));
                    break;
                }
                System.err.println("Invalid input");
            }
        }
        return number - 1;
    }

    protected boolean getYesOrNo(String question, boolean defaultAnswer, LineNumberReader reader) throws IOException {
        System.err.println(question);
        System.err.print("Y (yes), N (no), default - " + (defaultAnswer ? "yes" : "no") + ". ");
        String line = reader.readLine();
        if (line == null || line.trim().isEmpty()) {
            return defaultAnswer;
        } else if (line.trim().equalsIgnoreCase("y") || line.trim().equalsIgnoreCase("yes")) {
            return true;
        } else if (line.trim().equalsIgnoreCase("n") || line.trim().equalsIgnoreCase("no")) {
            return false;
        }
        return defaultAnswer;
    }

    protected Properties getCustomPropertiesFromUser(LineNumberReader reader) throws IOException {
        System.err.println("Please, enter properties in format 'property=value'.");
        System.err.println("When you finish, type 'q' or 'quit'");
        Properties result = new Properties();
        for (; ; ) {
            System.err.print(getPrompt());
            String line = reader.readLine();
            if (line == null) {
                System.err.println("Invalid input.");
            } else if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("q")) {
                System.err.println("Properties saved.");
                break;
            } else if (!parseProperty(line.trim(), result)) {
                System.err.println("Invalid input.");
            }
        }
        return result;
    }

    private static boolean parseProperty(String line, Properties properties) {
        int i = line.indexOf('=');
        if (i == -1) {
            return false;
        }
        String key = line.substring(0, i);
        String value = line.substring(i + 1);
        properties.setProperty(key, value);
        return true;
    }

    private static boolean notRequired(Property property, String value) {
        return !property.isRequired() && (value == null || value.trim().isEmpty());
    }

    private static void makeScriptsExecutable(Project project) throws IOException {
        Set<PosixFilePermission> permissions = Sets.newHashSet(PosixFilePermission.OTHERS_EXECUTE);
        for (Path script : project.getScripts()) {
            if (Files.exists(script)) {
                if (Files.isDirectory(script)) {
                    try (Stream<Path> stream = Files.walk(script)) {
                        stream.forEach(path -> {
                            if (!Files.isDirectory(path)) {
                                try {
                                    Files.setPosixFilePermissions(path, permissions);
                                } catch (IOException e) {
                                    System.err.print(e.getMessage());
                                } catch (UnsupportedOperationException ignored) {
                                }
                            }
                        });
                    }
                } else {
                    try {
                        Files.setPosixFilePermissions(script, permissions);
                    } catch (UnsupportedOperationException ignored) {
                    }
                }
            }
        }
    }

    private static class ValidationException extends Exception {
        private final Property property;

        private final String value;

        private ValidationException(Property property, String value) {
            this.property = property;
            this.value = value;
        }

        @Override
        public String getMessage() {
            return String.format("Value '%s' set for property %s is invalid. Doc: %s", value, property.getName(), property.getDoc());
        }

    }

    public static void main(String[] args) {
        new SolutionGenerator(args).start();
    }
}