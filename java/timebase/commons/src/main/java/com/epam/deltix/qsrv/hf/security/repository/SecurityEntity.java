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

import com.epam.deltix.util.id.Identifiable;
import com.epam.deltix.util.lang.StringUtils;

/**
 * Description: SecurityEntity
 * Date: Mar 2, 2011
 *
 * @author Nickolay Dul
 */
public abstract class SecurityEntity implements Identifiable<String> {
    protected final String entityId;
    protected String description;

    protected SecurityEntity(String entityId, String description) {
        this.entityId = StringUtils.trim(entityId);
        if (this.entityId == null)
            throw new IllegalStateException("Security entity id could not be empty!");

        this.description = description;
    }

    @Override
    public String getId() {
        return entityId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}