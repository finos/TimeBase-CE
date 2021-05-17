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
package com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.io.UncheckedIOException;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * @author Alexei Osipov
 */
class AeronWorkDirManager {
    private static final Log LOGGER = LogFactory.getLog(AeronWorkDirManager.class);

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter
            .ofPattern("uuuuMMdd_HHmmss")
            .withZone(ZoneId.systemDefault());

    // If set then TB uses external Aeron driver instead of running it's own embedded driver.
    private static final String EXTERNAL_AERON_DIR = System.getProperty("TimeBase.transport.aeron.external.driver.dir", null);
    private static final String EXPLICIT_STREAM_ID_COUNTER_FILE = QSHome.getPath("timebase" + File.separator + "id.sequence");

    // Base dir for auto-generated folder
    private static final String AERON_BASE_DIR = QSHome.getPath("temp/dxaeron");
    private static final String DRIVER_FOLDER = "driver";

    // We will delete older folders if they at least this old (seconds).
    // This is needed to avoid some corner cases when there 3 or more instances of time start at same time with same port and same DeltixHome.
    private static final int SAFETY_INTERVAL = 10;

    /**
     * Returns path like "C:\dev\main\temp\dxaeron\8057\20170314_152847\driver".
     * <ul>
     * <li>"temp\dxaeron" - is used as common path for Aeron
     * <li>"8057" - is provided port. This helps to run multiple instances of Aeron on different ports. So each of such instances will not interfere with other.
     * <li>"20170314_152847" - is current local date. This part is needed to prevent issue when we restarted server on same port with same directory and Aeron thinks that driver is already started.
     * <li>"driver" - extra folder that is not created by this method. Aeron prefers to create work folder itself. Without this folder aeron will log a warning about existing folder.
     * </ul>
     */
    public static String setupWorkingDirectory(int port, long startTimestamp) {
        if (EXTERNAL_AERON_DIR != null) {
            // Use externally provided directory
            return EXTERNAL_AERON_DIR;
        }

        File portDir = createPortSpecificDir(port);
        assert portDir.exists();

        Instant currentDate = Instant.ofEpochMilli(startTimestamp);
        Path workFolder = createWorkFolder(portDir, currentDate);
        cleanupNestedDirectories(portDir, currentDate.minus(SAFETY_INTERVAL, ChronoUnit.SECONDS));
        workFolder.toFile().deleteOnExit(); // Mark for deletion on shutdown

        return workFolder.resolve(DRIVER_FOLDER).toString();
    }

    public static boolean useEmbeddedDriver() {
        return EXTERNAL_AERON_DIR == null;
    }

    public static String getExplicitStreamIdCounterFile() {
        return EXPLICIT_STREAM_ID_COUNTER_FILE;
    }

    /**
     * Creates directory in the parent directory using current local time for directory name.
     * In case of existing directory will increment time by one second.
     *
     * This method lets OS to deal with possible race conditions and expects
     * that it's not possible for two applications who attempt to create same directory to succeed both.
     */
    private static Path createWorkFolder(File parentDir, Instant currentDate) {
        while (true) {
            String tempDirName = dateTimeFormatter.format(currentDate);
            try {
                return Files.createDirectory(parentDir.toPath().resolve(tempDirName));
            } catch (FileAlreadyExistsException e) {
                // Try next timestamp
                currentDate = currentDate.plus(1, ChronoUnit.SECONDS);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Creates parent directory using provided port number.
     */
    @Nonnull
    private static File createPortSpecificDir(int port) {
        File portDir = Paths.get(AERON_BASE_DIR, Integer.toString(port)).toAbsolutePath().toFile();
        if (portDir.exists()) {
            // Cleanup
            if (!portDir.isDirectory()) {
                throw new RuntimeException("Is not a directory: " + portDir.getPath());
            }
        } else {
            if (!portDir.mkdirs()) {
                throw new RuntimeException("Unable to create directory: " + portDir.getPath());
            }
        }
        return portDir;
    }

    /**
     * Cleanups nested directories if they match date prior to provided date (so we don't delete folders that younger than ours).
     * If delete operation fails then we just write a log event.
     */
    private static void cleanupNestedDirectories(File portDir, Instant currentDate) {
        String currentTimeDirName = dateTimeFormatter.format(currentDate);
        File[] files = portDir.listFiles();
        if (files == null) {
            throw new RuntimeException("Unable to get folders from: " + portDir.getPath());
        }
        for (File subfolder: files) {
            if (subfolder.isDirectory() && isBefore(subfolder, currentTimeDirName)) {
                boolean deleted = false;
                if (deleteRecursively(subfolder)) {
                    deleted = true;
                } else {
                    if (deleteRecursively(subfolder)) {
                        deleted = true;
                    }
                }
                if (!deleted) {
                    LOGGER.warn().append("Failed to delete temporary folder: ").append(subfolder.getPath()).commit();
                } else {
                    LOGGER.debug().append("Deleted temporary folder: ").append(subfolder.getPath()).commit();
                }
            }
        }
    }

    private static boolean deleteRecursively(File dir) {
        try {
            IOUtil.delete(dir);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean isBefore(File subfolder, String currentTimeDirName) {
        return subfolder.getName().compareTo(currentTimeDirName) < 0;
    }
}
