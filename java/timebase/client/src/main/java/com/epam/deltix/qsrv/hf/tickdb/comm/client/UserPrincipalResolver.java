/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.tickdb.comm.UserPrincipal;
import com.epam.deltix.qsrv.util.secrets.SecretsStorage;
import com.epam.deltix.util.oauth.Oauth2Client;

public class UserPrincipalResolver {

    public static final Log LOGGER = LogFactory.getLog("tickdb.client");

    private volatile Oauth2Client oauth2Client;

    void setOauth2Client(Oauth2Client oauth2Client) {
        this.oauth2Client = oauth2Client;
    }

    UserPrincipal resolve(UserPrincipal user) {
        if (SecretsStorage.isSecretsStorageValue(user.getPass())) {
            try {
                return new UserPrincipal(
                    user.getName(),
                    SecretsStorage.INSTANCE.getSecret(user.getPass())
                );
            } catch (Throwable t) {
                LOGGER.warn().append("Failed to resolve SecretsStorage value for user ").append(user.getName()).append(t).commit();
            }
        }

        if (oauth2Client != null) {
            String clientId = oauth2Client.clientId();
            String token = oauth2Client.token();
            return new UserPrincipal(clientId, token);
        }

        return user;
    }

    void close() {
        if (oauth2Client != null) {
            oauth2Client.close();
        }
    }
}