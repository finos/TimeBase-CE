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
package com.epam.deltix.qsrv.dtb.store.pub;

/**
 *
 */
public class SingleEntityFilter implements AbstractSingleEntityFilter {
    public int              entity;

    public SingleEntityFilter() {
    }

    public SingleEntityFilter (int entity) {
        this.entity = entity;
    }

    @Override
    public boolean          accept (int entity) {
        return (this.entity == entity);
    }

    @Override
    public boolean          acceptAll () {
        return (false);
    }      
    
    @Override
    public int              getSingleEntity () {
        return (entity);
    }

    @Override
    public String           toString () {
        return "SingleEntityFilter [" + entity + "]";
    }

    @Override
    public boolean          restrictAll() {
        return false;
    }

    @Override
    public long             acceptFrom(int entity) {
        return Long.MIN_VALUE;
    }
}