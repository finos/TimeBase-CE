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
package com.epam.deltix.qsrv.hf.security.simple;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.qsrv.hf.security.SecurityConfigurator;
import com.epam.deltix.qsrv.util.text.Mangle;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.lang.Depends;
import com.epam.deltix.util.lang.Factory;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.security.*;
import com.epam.deltix.util.text.IgnoreCaseComparator;

import java.io.File;
import java.security.AccessControlException;
import java.security.Principal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


@Depends("../jaxb.index")
public class SimpleUserDirectoryFactory implements Factory<AuthenticatingUserDirectory> {
    private final File userDirectoryFile;
    private long lastModified = Long.MIN_VALUE;
    private SimpleUserDirectory userDirectory;

    public SimpleUserDirectoryFactory (File userDirectoryFile) {
        this.userDirectoryFile = userDirectoryFile;
    }

    @Override
    public synchronized AuthenticatingUserDirectory create() {
        if (lastModified != userDirectoryFile.lastModified()) {
            userDirectory = new SimpleUserDirectory();
            userDirectory.readConfiguration(SimpleSecurityConfiguration.read(userDirectoryFile));
            lastModified = userDirectoryFile.lastModified();
        }
        return userDirectory;
    }

}

/**
 * File-based user directory
 */
class SimpleUserDirectory implements AuthenticatingUserDirectory {
    private static final Log LOGGER = SecurityConfigurator.LOGGER;
    private static final String LOGPREFIX = SecurityConfigurator.LOGPREFIX;

    private final TreeMap<String, GroupEntry> groups = new TreeMap<>(IgnoreCaseComparator.INSTANCE);
    private final TreeMap<String, FileUserEntry> users = new TreeMap<>(IgnoreCaseComparator.INSTANCE);
    private final HashMap<FileUserEntry, HashSet<GroupEntry>> userGroups = new HashMap<>();

    @Override
    public synchronized Principal[] users() {
        return users.values().toArray(new Principal[users.size()]);
    }

    @Override
    public synchronized Principal getUser(String name) {
        return users.get(name);
    }

    @Override
    public synchronized Principal[] groups() {
        return groups.values().toArray(new Principal[groups.size()]);
    }

    @Override
    public synchronized Principal getGroup(String name) {
        return groups.get(name);
    }

    @Override
    public synchronized Principal authenticate(String name, String pass) {
        FileUserEntry def = users.get(name);
        if (def != null && def.check(pass))
            return def;

        throw new AccessControlException("Not authorized.");
    }

    @Override
    public void changePassword(String user, String oldPassword, String newPassword) {
        throw new UnsupportedOperationException("Password changing not supported for Simple security.");
    }

    void readConfiguration(SimpleSecurityConfiguration config) {
        if (config.users != null && config.users.users != null)
            for (SimpleSecurityConfiguration.User user : config.users.users)
                processUser(user);

        if (config.groups != null && config.groups.groups != null)
            for (SimpleSecurityConfiguration.Group group : config.groups.groups)
                processGroup(group);

        for (GroupEntry groupEntry : groups.values()) {
            HashSet<GroupEntry> chain = new HashSet<>();
            buildChain(groupEntry, chain);
        }

        if (LOGGER.isDebugEnabled()) {
            StringBuilder builder = new StringBuilder(512);
            builder.append(LOGPREFIX).append("Loaded FILE UAC configuration:\n");
            builder.append("USERS:\n");
            for (FileUserEntry user : users.values())
                builder.append("\t").append(user.getName()).append("\n");

            builder.append("GROUPS:\n");
            for (GroupEntry group : groups.values()) {
                builder.append("\t").append(group.getName()).append("\n");
                builder.append("\t\tMembers: ").append(Arrays.toString(group.getPrincipals())).append('\n');
            }

            LOGGER.debug(builder.toString());
        }
    }

    private void buildChain(GroupEntry group, HashSet<GroupEntry> chain) {
        if (chain.contains(group))
            return;

        chain.add(group);

        for (Principal principal : group.getPrincipals())
            if (principal instanceof GroupEntry) {
                buildChain((GroupEntry) principal, chain);
            } else if (principal instanceof FileUserEntry) {
                HashSet<GroupEntry> set = userGroups.get(principal);
                if (set == null)
                    userGroups.put((FileUserEntry) principal, set = new HashSet<>());
                set.addAll(chain);
            }

        chain.remove(group);
    }


    private void processGroup(SimpleSecurityConfiguration.Group groupEntity) {
        String groupName = StringUtils.trim(groupEntity.name);

        if (groupName == null) {
            LOGGER.warn(LOGPREFIX + "Group name could not be empty: " + groupEntity);
            return;
        }

        if (exists(groupName)) {
            LOGGER.warn(LOGPREFIX + "Member already exists: {0}").with(groupName);
            return;
        }

        GroupEntry group = new GroupEntry(groupName);

        ArrayList<String> members = groupEntity.principal;
        if (members != null)
            for (String member : members) {
                PrincipalEntry def = find(member);
                if (def != null)
                    group.addPrincipal(def);
                else
                    LOGGER.warn(LOGPREFIX + "Unknown group member: {0}").with(member);
            }

        groups.put(groupName, group);
    }

    private void processUser(SimpleSecurityConfiguration.User userEntity) {
        String name = StringUtils.trim(userEntity.id);
        if (name == null) {
            LOGGER.warn(LOGPREFIX + "Skip user with empty name");
        } else {
            if (exists(name)) {
                LOGGER.warn(LOGPREFIX + "User already exists: {0}").with(name);
            } else {
                String password = userEntity.password;
                password = Mangle.split(password);
                if (password != null) {
                    users.put(name, new FileUserEntry(name, password));
                } else {
                    users.put(name, new FileUserEntry(name, "")); // TODO: do not allow empty passwords?
                }
            }
        }
    }

    private boolean exists(String principal) {
        return users.containsKey(principal) || groups.containsKey(principal);
    }

    private PrincipalEntry find(String name) {
        FileUserEntry def = users.get(name);
        if (def != null)
            return def;
        return groups.get(name);
    }

    ///////////////////////// HELPER CLASSES ////////////////////////

    private static final class FileUserEntry extends UserEntry {
        private final String pass;

        public FileUserEntry(String name, String pass) {
            super(name);
            this.pass = pass != null ? IOUtil.concat(pass, "UserDef") : null;
        }

        boolean check(String p) {
            return IOUtil.split(pass, "UserDef").equals(p);
        }
    }
}