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

import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;
import com.epam.deltix.qsrv.util.text.Mangle;

import java.util.function.Consumer;

public class Oauth2ConfigProvider {

    protected String applicationName;

    protected boolean https;

    protected String host;

    protected int port;

    public Oauth2ConfigProvider() {
    }

    public boolean https() {
        return https;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public Oauth2ConfigProvider withApplication(String applicationName) {
        this.applicationName = applicationName;
        return this;
    }

    public Oauth2ConfigProvider withClient(TickDBClient client) {
        this.https = client.getSslTermination();
        this.host = client.getHost();
        this.port = client.getPort();
        return this;
    }

    public Oauth2ConfigProvider withHost(String host) {
        this.host = host;
        return this;
    }

    public Oauth2ConfigProvider withPort(int port) {
        this.port = port;
        return this;
    }

    public Oauth2ConfigProvider withHttps(boolean https) {
        this.https = https;
        return this;
    }

    public Oauth2ClientConfig discover(String secret) {
        return discover(secret, null);
    }

    public Oauth2ClientConfig discover(String secret, Consumer<Oauth2ClientConfig.Builder> customizer) {
        if (applicationName == null) {
            applicationName = AuthInfoProvider.TB_CLIENT_SERVICE;
        }

        AuthInfo authInfo = new AuthInfoProvider(applicationName).getAuthInfo(https, host, port);
        Oauth2ClientConfig.Builder builder = Oauth2ClientConfig.builder()
            .withUrl(authInfo.getOpenIdConfiguration().getTokenEndpoint())
            .withClientCredentials(
                authInfo.getApplicationClientId(),
                Mangle.split(secret)
            );
        String scope = authInfo.getApplicationScope();
        if (scope != null) {
            builder = builder.withParameter("scope", scope);
        }

        if (customizer != null) {
            customizer.accept(builder);
        }

        return builder.build();
    }

    public Oauth2ClientConfig discover(KeystoreConfig keystoreConfig) {
        return discover(keystoreConfig, null);
    }

    public Oauth2ClientConfig discover(KeystoreConfig keystoreConfig, Consumer<Oauth2ClientConfig.Builder> customizer) {
        if (applicationName == null) {
            applicationName = AuthInfoProvider.TB_CLIENT_SERVICE;
        }

        AuthInfo authInfo = new AuthInfoProvider(applicationName).getAuthInfo(https, host, port);
        Oauth2ClientConfig.Builder builder = Oauth2ClientConfig.builder()
            .withUrl(authInfo.getOpenIdConfiguration().getTokenEndpoint())
            .withClientCredentials(
                authInfo.getApplicationClientId(),
                keystoreConfig
            );
        String scope = authInfo.getApplicationScope();
        if (scope != null) {
            builder = builder.withParameter("scope", scope);
        }

        if (customizer != null) {
            customizer.accept(builder);
        }

        return builder.build();
    }

}