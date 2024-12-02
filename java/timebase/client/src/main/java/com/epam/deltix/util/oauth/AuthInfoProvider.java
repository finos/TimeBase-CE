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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.epam.deltix.util.io.URLConnectionFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class AuthInfoProvider {

    public static final String TB_CLIENT_APPLICATION = "timebase.client.application";
    public static final String TB_CLIENT_SERVICE = "timebase.client.service";
    private static final String OPENID_CONFIG_SUFFIX_DEF = "/.well-known/openid-configuration";
    private static final String DEFAULT_URL_PATH = "/tb/oauthinfo";

    private static final int HTTP_CONNECT_TIMEOUT_SEC = Integer.getInteger("TimeBase.oauth2.authInfoDiscoverTimeoutSec", 10);

    private final String applicationName;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthInfoProvider() {
        this(TB_CLIENT_SERVICE);
    }

    public AuthInfoProvider(String applicationName) {
        this.applicationName = applicationName;
    }

    public AuthInfo getAuthInfo(boolean https, String authInfoHost, int authInfoPort) {
        return getAuthInfo(https, authInfoHost, authInfoPort, DEFAULT_URL_PATH);
    }

    public AuthInfo getAuthInfo(boolean https, String authInfoHost, int authInfoPort, String authInfoPath) {
        Objects.requireNonNull(authInfoHost, "authInfoHost required.");
        Objects.requireNonNull(authInfoPath, "authInfoPath required.");

        AuthInfo authInfo = requestAuthInfo(https, authInfoHost, authInfoPort, authInfoPath);
        authInfo.setApplication(applicationName);
        if (authInfo.getIssuer() == null || authInfo.getIssuer().trim().isEmpty()) {
            throw new RuntimeException("Unknown `issuer`. Please, configure `issuer parameter for UAC server.");
        }

        authInfo.setOpenIdConfiguration(requestOpenIdConfiguration(authInfo.getIssuer()));
        return authInfo;
    }

    public static String buildUrl(boolean https, String host, int port, String path) {
        return (https ? "https" : "http") + "://" + host + ":" + port + path;
    }

    private AuthInfo requestAuthInfo(boolean https, String authInfoHost, int authInfoPort, String authInfoPath) {
        try {
            return requestTimebaseEndpoint(https, authInfoHost, authInfoPort, authInfoPath, AuthInfo.class);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to request Authorization Info from UAC server: " + t.getMessage(), t);
        }
    }

    private OpenIdConfiguration requestOpenIdConfiguration(String issuer) {
        try {
            return get(
                appendURIPath(issuer, OPENID_CONFIG_SUFFIX_DEF),
                OpenIdConfiguration.class
            );
        } catch (Throwable t) {
            throw new RuntimeException("Failed to discover OpenId configuration for issuer `" + issuer + "`: " + t.getMessage(), t);
        }
    }

    private String appendURIPath(String uri, String path) {
        return uri.replaceAll("/$", "") + (path.startsWith("/") ? "" : "/") + path;
    }

    public <T> T get(String url, Class<T> outputType) {
        try {
            HttpClient.Builder builder = HttpClient.newBuilder();
            HttpClient client = builder
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.of(HTTP_CONNECT_TIMEOUT_SEC, ChronoUnit.SECONDS))
                .build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), outputType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse request (" + url + ") result: " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException("Request (" + url + ") failed: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException("Request (" + url + ") interrupted.");
        }
    }

    public <T> T requestTimebaseEndpoint(boolean https, String host, int port, String endpoint, Class<T> outputType) {
        try {
            URLConnection urlConnection = URLConnectionFactory.create(host, port, endpoint, https);
            if (urlConnection instanceof HttpURLConnection) {
                HttpURLConnection connection = (HttpURLConnection) urlConnection;
                HttpURLConnection redirected = (HttpURLConnection) URLConnectionFactory.verify(connection, null, null);
                if (redirected != connection) {
                    connection.disconnect();
                    connection = redirected;
                }

                int code = connection.getResponseCode();
                if (code == 200) {
                    try (InputStream in = connection.getInputStream()) {
                        String response = new String(in.readAllBytes());
                        return objectMapper.readValue(response, outputType);
                    } finally {
                        connection.disconnect();
                    }
                } else {
                    throw new RuntimeException("Request (" + buildUrl(https, host, port, endpoint) + ") failed with status code " + code);
                }
            } else {
                throw new RuntimeException("Invalid URL connection type. Required HTTP connection.");
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse request (" + buildUrl(https, host, port, endpoint) + ") result: " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException("Request (" + buildUrl(https, host, port, endpoint) + ") failed: " + e.getMessage());
        }
    }

}