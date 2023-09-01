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
package com.epam.deltix.qsrv.solgen.base;

import com.epam.deltix.util.io.UncheckedIOException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Base interface for all kinds of projects.
 */
public interface Project extends Flushable {

    /**
     * Sources root.
     *
     * @return sources root directory.
     */
    Path getSourcesRoot();

    /**
     * Resources root.
     *
     * @return resources root directory.
     */
    Path getResourcesRoot();

    /**
     * Project root.
     *
     * @return project root directory.
     */
    Path getProjectRoot();

    /**
     * Project libs directory.
     *
     * @return path to libs root.
     */
    Path getLibsRoot();

    /**
     * Project scripts.
     *
     * @return list of project scripts.
     */
    List<Path> getScripts();

    void markAsScript(Path path);

    /**
     * Sets project property.
     *
     * @param key   property path
     * @param value property value
     */
    void setProjectProperty(String key, String value);

    /**
     * Creates project skeleton.
     */
    void createSkeleton() throws IOException;

    /**
     * Set multiple project properties.
     *
     * @param properties properties to set.
     */
    default void setProjectProperties(Properties properties) {
        properties.stringPropertyNames().forEach(key -> setProjectProperty(key, properties.getProperty(key)));
    }

    /**
     * Adds source file to project.
     *
     * @param relativeDirPath relative path in sources directory.
     * @param fileName        file name.
     * @param content         source file content.
     */
    default void addSource(String relativeDirPath, String fileName, String content) {
        Path dirPath = getSourcesRoot().resolve(relativeDirPath);
        try {
            Files.createDirectories(dirPath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try (PrintWriter writer = new PrintWriter(dirPath.resolve(fileName).toFile())) {
            writer.print(content);
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    default void addSource(Source source) {
        Path path = getSourcesRoot().resolve(source.getRelativePath());
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try (PrintWriter writer = new PrintWriter(path.toFile())) {
            writer.print(source.getContent());
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    default void addScript(Source source) {
        addRoot("", source.getRelativePath(), source.getContent());
        markAsScript(getProjectRoot().resolve(source.getRelativePath()));
    }

    /**
     * Adds source file to project.
     *
     * @param relativeDirPath relative path in sources directory.
     * @param fileName        file name.
     * @param content         source file content.
     */
    default void addRoot(String relativeDirPath, String fileName, String content) {
        Path dirPath = getProjectRoot().resolve(relativeDirPath);
        try {
            Files.createDirectories(dirPath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try (PrintWriter writer = new PrintWriter(dirPath.resolve(fileName).toFile())) {
            writer.print(content);
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    default void addRoot(String relativeDirPath, String fileName, InputStream content) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(content))) {
            addRoot(relativeDirPath, fileName, reader.lines().collect(Collectors.joining("\n")));
        } catch (IOException ignored) {
        }
    }

}