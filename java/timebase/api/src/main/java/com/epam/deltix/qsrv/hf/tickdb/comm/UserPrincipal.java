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
package com.epam.deltix.qsrv.hf.tickdb.comm;

import java.security.Principal;

/**
 *
 */
public class UserPrincipal implements Principal {
    public static final UserPrincipal UNDEFINED = new UserPrincipal();

    private final String name;
    private final String pass;

    private UserPrincipal() {
        this.name = this.pass = null;
    }

    public UserPrincipal(UserPrincipal copy) {
        this.name = copy.name;
        this.pass = copy.pass;
    }

    public UserPrincipal(String name, String pass) {
        this.name = name;
        this.pass = pass;
    }

    public String   getName() {
        return name;
    }

    public String getPass() {
        return pass;
    }
    
    public String   getToken() {
        if (this == UNDEFINED)
            return "";

        return name + ":" + pass;
    }
}
