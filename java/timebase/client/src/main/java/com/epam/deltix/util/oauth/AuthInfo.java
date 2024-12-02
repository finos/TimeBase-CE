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
package com.epam.deltix.util.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthInfo {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ClientId {

        public ClientId() {
        }

        private String app;

        private String name;

        public String getApp() {
            return app;
        }

        public void setApp(String app) {
            this.app = app;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ClientScope {

        public ClientScope() {
        }

        private String app;

        private String scope;

        public String getApp() {
            return app;
        }

        public void setApp(String app) {
            this.app = app;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }
    }

    private String provider = "unknown";

    private String issuer;

    @JsonProperty("clientid")
    private ArrayList<ClientId> clients;

    private String scope = "openid profile";

    private ArrayList<ClientScope> scopes;

    private String usernameClaim = "preferred_username";

    private OpenIdConfiguration openIdConfiguration;

    private String application;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public ArrayList<ClientId> getClients() {
        return clients;
    }

    public String getApplicationClientId() {
        return getClientId(application);
    }

    public String getClientId(String application) {
        if (application == null) {
            throw new RuntimeException("application is null");
        }
        if (clients == null) {
            return null;
        }

        if (clients.size() == 1 && clients.get(0).getApp() == null) {
            return clients.get(0).name;
        }

        for (ClientId client : clients) {
            if (application.equals(client.app)) {
                return client.name;
            }
        }

        return null;
    }

    public void setClients(ArrayList<ClientId> clients) {
        this.clients = clients;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getApplicationScope() {
        return getScope(application);
    }

    public String getScope(String application) {
        if (application == null) {
            throw new RuntimeException("application is null");
        }
        if (scopes == null) {
            return scope;
        }

        if (scopes.size() == 1 && scopes.get(0).getApp() == null) {
            return scopes.get(0).scope;
        }

        for (ClientScope scope : scopes) {
            if (application.equals(scope.app)) {
                return scope.scope;
            }
        }

        return null;
    }

    public void setScopes(ArrayList<ClientScope> scopes) {
        this.scopes = scopes;
    }

    public String getUsernameClaim() {
        return usernameClaim;
    }

    public void setUsernameClaim(String usernameClaim) {
        this.usernameClaim = usernameClaim;
    }

    public OpenIdConfiguration getOpenIdConfiguration() {
        return openIdConfiguration;
    }

    public void setOpenIdConfiguration(OpenIdConfiguration openIdConfiguration) {
        this.openIdConfiguration = openIdConfiguration;
    }
}