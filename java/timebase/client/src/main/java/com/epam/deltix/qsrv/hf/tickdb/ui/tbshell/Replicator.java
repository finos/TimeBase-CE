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
package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell;

import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.replication.*;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.util.lang.Util;

import java.io.LineNumberReader;

/**
 *
 */
public class Replicator {
    private final TickDBShell               shell;
    
    public String       bkpath;					    // path to backup folder
    public String       sourcedb;					// to replicate from
    public String       sourceStream;				// source stream to replicate from
    
    public long         threshold = 100000;  		// number of messages of the single transaction when writing into data file
    public long         rollSize = 100;  			// size in megabytes of the one backup file, -1 means single file
    public boolean      live = false;
    public int          retries = 0;                // number of retries
    public long         retryTimeout = 5000;        // 5 sec

    public ReloadMode   mode = ReloadMode.prohibit;

    //public String filter; 					// qql "where clause" to filtering messages

    public Replicator(TickDBShell shell) {
        this.shell = shell;
    }

    protected void              doSet () {
        System.out.println ("srcdb:         " + sourcedb);
        System.out.println ("srcstream:     " + sourceStream);
        System.out.println ("reload:        " + mode);
        System.out.println ("retries:       " + retries);
        System.out.println ("retry timeout: " + retryTimeout + "ms");
        System.out.println ("cpmode:        " + (live ? "live" : "offline"));

        if (bkpath != null) {
            System.out.println ("bkpath:        " + bkpath);
            System.out.println ("bkcommitsize:  " + threshold);
            System.out.println ("bkfilesize:    " + rollSize);
        }
    }

    boolean                     doSet (String option, String value) throws Exception {
        if (option.equalsIgnoreCase("bkpath")) {
            bkpath = value;
            shell.confirm ("Backup path: " + bkpath);
            return (true);
        } else if (option.equalsIgnoreCase("srcdb")) {
            sourcedb = value;
            shell.confirm ("Replication source timebase: " + sourcedb);
            return (true);
        } else if (option.equalsIgnoreCase("srcstream")) {
            sourceStream = value;
            shell.confirm ("Replication source stream: " + sourceStream);
            return (true);
        } else if (option.equalsIgnoreCase("bkcommitsize")) {
            threshold = Long.valueOf(value);
            shell.confirm ("Backup threshold: " + threshold);
            return (true);
        } else if (option.equalsIgnoreCase("bkfilesize")) {
            rollSize = Long.valueOf(value);
            shell.confirm ("Backup file size limit: " + rollSize + "MB");
            return (true);
        } else if (option.equalsIgnoreCase("cpmode")) {
            live = value.equalsIgnoreCase ("live");
            shell.confirm ("Replication live mode: " + live);
            return (true);
        } else if (option.equalsIgnoreCase("reload")) {
            try {
                mode = ReloadMode.valueOf(value);
                shell.confirm ("Replication reload mode: " + mode);
            } catch (IllegalArgumentException e) {
                shell.confirm ("Unknown reload mode: " + value);
            }

            return (true);
        } else if (option.equalsIgnoreCase("retries")) {
            retries = Integer.valueOf(value);
            shell.confirm("Retry attempts: " + retries);
            return (true);
        } else if (option.equalsIgnoreCase("retrytimeout")) {
            retryTimeout = Integer.valueOf(value);
            shell.confirm("Retry timeout: " + retryTimeout);
            return (true);
        }

        return (false);
    }

    public boolean              doCommand (String key, String args, String fileId, LineNumberReader reader)
            throws Exception
    {
        String[] values = args != null ? args.split(" ") : new String[0];

        String[] types = shell.selector.getSelectedTypes();
        IdentityKey[] entities = shell.selector.getSelectedEntities();

        if (types != null && types.length == 0) {
            System.out.println ("Selected message types is empty.");
            return true;
        }

        if (entities != null && entities.length == 0) {
            System.out.println ("Selected entities is empty.");
            return true;
        }

        if ("backup".equalsIgnoreCase(key)) {
            if (!shell.dbmgr.checkDb())
                return true;

            if (!shell.dbmgr.checkSingleStream())
                return true;

            DXTickStream stream = shell.dbmgr.getSingleStream();
            long[] range = stream.getTimeRange();
            if (range == null)
                range = new long[] { TimeStamp.TIMESTAMP_UNKNOWN, TimeStamp.TIMESTAMP_UNKNOWN };

            final StreamStorage source = new StreamStorage(shell.dbmgr.getDB(), stream.getKey());
            final FileStorage target = new FileStorage(bkpath);

            final ReplicationOptions options = new ReplicationOptions();
            options.types = types;
            options.entities = entities;
            options.range = new long[] { shell.selector.getTime(range[1]), shell.selector.getEndtime(range[0]) };
            options.live = live;
            options.rollSize = rollSize;
            options.threshold = threshold;
            options.format = values.length > 0 && "format".equalsIgnoreCase(values[0]);
            options.mode = mode;
            options.retries = retries;
            options.retryTimeout = retryTimeout;

            new StreamReplicator().replicate(source, target, options);
            return true;

        } else if ("replicate".equalsIgnoreCase(key)) {
            if (!shell.dbmgr.checkDb())
                return true;
            
            if (sourcedb == null) {
                System.out.println ("Source database is not chosen. Use 'set srcdb <uri>'");
                return true;
            }

            if (sourceStream == null) {
                System.out.println ("Source stream is not chosen. Use 'set srcstream <key>'");
                return true;
            }
            
            if (values.length == 0) {
                System.out.println ("Target stream is not defined. Use 'replicate <stream>'");
                return true;
            }

            DXTickDB sourceDB = null;

            try {
                sourceDB = TickDBFactory.openFromUrl(sourcedb, false);
                final StreamStorage source = new StreamStorage(sourceDB, sourceStream);

                DXTickStream stream = source.getSource();
                if (stream == null) {
                    System.out.println ("Source stream " + source.name + " is not found in " + sourcedb);
                    return true;
                }

                long[] range = stream.getTimeRange();
                if (range == null)
                    range = new long[] { TimeStamp.TIMESTAMP_UNKNOWN, TimeStamp.TIMESTAMP_UNKNOWN };

                final StreamStorage target = new StreamStorage(shell.dbmgr.getDB(), values[0]);

                final ReplicationOptions options = new ReplicationOptions();
                options.types = types;
                options.entities = entities;
                options.range = new long[] { shell.selector.getTime(range[1]), shell.selector.getEndtime(range[0]) };
                options.live = live;
                options.mode = mode;
                options.retries = retries;
                options.retryTimeout = retryTimeout;

                new StreamReplicator().replicate(source, target, options);
            } finally {
                Util.close(sourceDB);
            }

            return true;

        } else if ("restore".equalsIgnoreCase(key)) {
            if (!shell.dbmgr.checkDb())
                return true;

            if (bkpath != null && sourcedb != null) {
                System.out.println ("Both 'backup path' and 'source database' is defined. Restore can use one of them only.");
                return true;
            }

            if (values.length == 0) {
                System.out.println ("Source stream is not specified. Use 'restore <stream>'");
                return true;
            }

            DXTickDB sourceDB = null;

            try {
                final RestoreOptions options = new RestoreOptions();
                options.name = sourceStream != null ? sourceStream : values[0];
                options.types = types;
                options.entities = entities;
                options.mode = mode;

                final Storage source;
                if (sourcedb != null) {
                    sourceDB = TickDBFactory.openFromUrl(sourcedb, false);
                    StreamStorage ss = new StreamStorage(sourceDB, sourceStream);
                    
                    DXTickStream stream = ss.getSource();
                    if (stream == null) {
                        System.out.println ("Source stream " + ss.name + " is not found in source database.");
                        return true;
                    }

                    long[] range = stream.getTimeRange();
                    if (range == null) {
                        System.out.println ("Source stream " + ss.name + " has no data available.");
                        return true;
                    }

                    source = ss;
                    
                    options.range = new long[] { shell.selector.getTime(range[1]), shell.selector.getEndtime(range[0]) };
                } else if (bkpath != null) {
                    source = new FileStorage(bkpath);
                    options.range = new long[] { shell.selector.getTime(), shell.selector.getEndtime()};
                } else {
                    source = null;
                }

                final StreamStorage target = new StreamStorage(shell.dbmgr.getDB(), values[0]);

                new StreamReplicator().restore(source, target, options);
            } finally {
                Util.close(sourceDB);
            }

            return true;
        }

        return false;
    }

}
