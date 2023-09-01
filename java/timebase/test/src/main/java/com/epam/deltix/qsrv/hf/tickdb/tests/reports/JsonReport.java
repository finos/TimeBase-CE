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
package com.epam.deltix.qsrv.hf.tickdb.tests.reports;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JsonReport implements Report{

    private final Map<String, Metric<?>> metrics = new HashMap<>();
    private final Map<String, TimestampedMetric<?>> timestampedMetrics = new HashMap<>();
    private final StringBuilder sb = new StringBuilder();

    @Override
    public void addMetric(String id, Metric<?> metric) {
        metrics.put(id, metric);
    }

    @Override
    public void addMetric(String id, TimestampedMetric<?> metric) {
        timestampedMetrics.put(id, metric);
    }

    @Override
    public <T> CharSequence metricToCharSequence(Metric<T> metric) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"values\":[")
                .append(metric.values().stream().map(metric::valueToString)
                        .collect(Collectors.joining(",")))
                .append("]}");
        return sb;
    }

    @Override
    public <T> CharSequence metricToCharSequence(TimestampedMetric<T> metric) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"timestamps\":[")
                .append(metric.timestamps().stream().map(x -> Long.toString(x))
                        .collect(Collectors.joining(",")))
                .append("],")
                .append("\"values\":[")
                .append(metric.values().stream().map(metric::valueToString)
                        .collect(Collectors.joining(",")))
                .append("]}");
        return sb;
    }

    @Override
    public CharSequence stringReport() {
        sb.setLength(0);
        sb.append("{");
        metrics.forEach((key, metric) -> {
            sb.append("\"").append(key).append("\":")
                    .append(metricToCharSequence(metric))
                    .append(",");
        });
        timestampedMetrics.forEach((key, metric) -> {
            sb.append("\"").append(key).append("\":")
                    .append(metricToCharSequence(metric))
                    .append(",");
        });
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.setLength(sb.length() - 1);
        }
        sb.append("}");
        return sb;
    }

    public static void mergeReports(Path reportsDir, String name) {
        try {
            Map<String, Object> mergedReportMap = Files.walk(reportsDir, 1)
                    .filter(path -> !path.toFile().isDirectory() && path.getFileName().toString().endsWith(".json"))
                    .flatMap(report -> {
                        Map<String, Object> map = convertJsonToMap(report);
                        Map<String, Object> result = new HashMap<>(map.size());
                        map.forEach((key, value) -> {
                            result.put(report.getFileName() + "-" + key, value);
                        });
                        return result.entrySet().stream();
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            writeAsJson(mergedReportMap, reportsDir.resolve(name + ".json"));
        } catch (IOException exc) {
            exc.printStackTrace();
        }
    }

    private static Map<String, Object> convertJsonToMap(Path json) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map;
        try {
            map = mapper.readValue(json.toFile(), new TypeReference<HashMap<String, Object>>(){});
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return map;
    }

    private static void writeAsJson(Map<String, Object> map, Path path) {
        ObjectMapper mapper = new ObjectMapper();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile()))) {
            mapper.writeValue(writer, map);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        Path path = Paths.get("stress-test-reports");
        mergeReports(path, "merged.json");
    }
}