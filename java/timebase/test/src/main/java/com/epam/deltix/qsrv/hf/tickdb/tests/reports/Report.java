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
package com.epam.deltix.qsrv.hf.tickdb.tests.reports;

import java.io.*;
import java.nio.file.Path;

public interface Report {

    void addMetric(String id, Metric<?> metric);

    void addMetric(String id, TimestampedMetric<?> metric);

    <T> CharSequence metricToCharSequence(Metric<T> metric);

    <T> CharSequence metricToCharSequence(TimestampedMetric<T> metric);

    CharSequence stringReport();

    default void writeToFile(File file) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.print(stringReport());
        } catch (IOException exc) {
            throw new UncheckedIOException(exc);
        }
    }

    default void writeToFile(Path path) {
        writeToFile(path.toFile());
    }

}
