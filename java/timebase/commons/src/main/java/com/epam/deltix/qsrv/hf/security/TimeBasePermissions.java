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
package com.epam.deltix.qsrv.hf.security;

public class TimeBasePermissions {
    // resource permissions
    public static final String READ_PERMISSION              = "READ";
    public static final String WRITE_PERMISSION             = "WRITE";

    // system permissions
    public static final String CREATE_STREAM_PERMISSION     = "CREATE";
    public static final String CHANGE_SCHEMA_PERMISSION     = "CHANGE_SCHEMA";
    public static final String IMPERSONATE_PERMISSION       = "IMPERSONATE";
    public static final String SHUTDOWN_PERMISSION          = "SHUTDOWN";

}