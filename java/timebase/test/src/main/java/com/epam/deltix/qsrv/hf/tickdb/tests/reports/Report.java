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
