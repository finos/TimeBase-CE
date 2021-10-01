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
package com.epam.deltix.qsrv.hf.ui.repair;

import com.epam.deltix.util.cmdline.DefaultApplication;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.epam.deltix.qsrv.hf.tickdb.impl.TickDBImpl.LOCK_FILE_NAME;

/**
 * Created by Alex Karpovich on 16/03/2020.
 */
public class TBRecover extends DefaultApplication {

    public TBRecover(String[] args) {
        super(args);
    }

    public static void main(String[] args) throws Throwable {
        new TBRecover(args).run();
    }

    @Override
    protected void run() throws Exception {
        final String home = getMandatoryArgValue("-home");
        run(home);
    }

    public static void run(String home) throws Exception {
        Path tickdb = Paths.get(home, "tickdb");

        System.out.println("Deleting index files ...");

        // drop all index files to make timebase recover on start
        if (Files.exists(tickdb, LinkOption.NOFOLLOW_LINKS))
            dropFiles(tickdb);

        // drop all index files to make timebase recover on start
        Path timebase = Paths.get(home, "timebase");
        if (Files.exists(timebase, LinkOption.NOFOLLOW_LINKS))
            dropFiles(timebase);

        // special start on any folder
        if (!Files.exists(tickdb, LinkOption.NOFOLLOW_LINKS) && !Files.exists(timebase, LinkOption.NOFOLLOW_LINKS)) {
            dropFiles(Paths.get(home));
        }

        Files.walk(Paths.get(home))
                .filter(x -> x.toFile().getName().endsWith("uhfq.xml"))
                .forEach( x -> {
                        File lockFile = new File(x.getParent().toFile(), LOCK_FILE_NAME);
                            try {
                                // create empty lock to indicates that stream required validation
                                if (!lockFile.exists()) {
                                    if (!lockFile.createNewFile())
                                        throw new RuntimeException("Failed create lock file in " + x.getParent());
                                }

                            } catch (IOException e) {
                                System.out.println("Failed to create: " + lockFile + ". Error: " + e);
                            }
                        }
                );
    }

    private static void dropFiles(Path path) throws IOException {
        Files.walk(path)
                .filter(x -> "index.dat".equals(x.toFile().getName()))
                .forEach(x -> {
                    try {
                        Files.delete(x);
                        System.out.println(x + " deleted.");
                    } catch (IOException e) {
                        System.out.println("Failed to delete: " + x + ". Error: " + e);
                    }
                });
    }
}