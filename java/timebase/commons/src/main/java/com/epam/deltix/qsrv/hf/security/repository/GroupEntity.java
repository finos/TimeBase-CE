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
package com.epam.deltix.qsrv.hf.security.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Description: GroupEntity
 * Date: Feb 28, 2011
 *
 * @author Nickolay Dul
 */
public class GroupEntity extends SecurityEntity {

    private HashSet<String> members = new HashSet<String>();


    public GroupEntity(String entityId) {
        super(entityId, null);
    }

    public GroupEntity(String entityId, String description,
                       Set<String> members) {
        super(entityId, description);
        if (members != null && !members.isEmpty())
            this.members.addAll(members);
    }

    @SuppressWarnings("unchecked")
    public Set<String> getMembers() {
        return members.isEmpty() ? Collections.<String>emptySet() : (Set<String>) members.clone();
    }

    public void addMembers(String... members) {
        Collections.addAll(this.members, members);
    }

    public void addMembers(Collection<String> members) {
        this.members.addAll(members);
    }

    public void clearMembers() {
        members.clear();
    }



}