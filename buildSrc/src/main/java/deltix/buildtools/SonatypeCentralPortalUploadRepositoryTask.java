package deltix.buildtools;

/*
 * Copyright 2014-2025 Real Logic Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.http.HttpStatus;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * This task performs manual steps to publish artifacts to Central Portal via OSSRH Staging API.
 */
public class SonatypeCentralPortalUploadRepositoryTask extends DefaultTask {
    private static final String CENTRAL_PORTAL_OSSRH_API_URI = "https://ossrh-staging-api.central.sonatype.com";
    private static final int CONNECTION_TIMEOUT = 30000;

    private final Property<String> portalUsername;
    private final Property<String> portalPassword;
    private final Property<String> groupId;
    private final Property<Boolean> snapshotRelease;

    /**
     * Create new task instance.
     */
    public SonatypeCentralPortalUploadRepositoryTask() {
        portalUsername = getProject().getObjects().property(String.class);
        portalPassword = getProject().getObjects().property(String.class);
        groupId = getProject().getObjects().property(String.class);
        snapshotRelease = getProject().getObjects().property(Boolean.class);
    }

    /**
     * Return property to set Central Portal username.
     *
     * @return Central Portal username.
     */
    @Input
    public Property<String> getPortalUsername() {
        return portalUsername;
    }

    /**
     * Return property to set Central Portal password.
     *
     * @return Central Portal password.
     */
    @Input
    public Property<String> getPortalPassword() {
        return portalPassword;
    }

    /**
     * Return property to set {@code groupId} of the project.
     *
     * @return {@code groupId} of the project.
     */
    @Input
    public Property<String> getGroupId() {
        return groupId;
    }

    /**
     * Return property to set snapshot release.
     *
     * @return {@code true} if snapshot release.
     */
    @Input
    public Property<Boolean> getSnapshotRelease() {
        return snapshotRelease;
    }

    /**
     * Publish staging repository to the Central Portal.
     */
    @TaskAction
    public void run() throws IOException, InterruptedException {
        if (!portalUsername.isPresent()) {
            return;
        }

        if (snapshotRelease.get()) {
            return;
        }

        String userNameAndPassword = portalUsername.get() + ":" + portalPassword.get();
        String bearer = Base64.getEncoder().encodeToString(userNameAndPassword.getBytes(StandardCharsets.US_ASCII));
        URI apiUri = URI.create(CENTRAL_PORTAL_OSSRH_API_URI);

        String repositoryKey = findOpenRepository(apiUri, bearer);
        System.out.println("Found published repository: " + repositoryKey);

        int status = uploadRepository(apiUri, bearer, repositoryKey);
        if (status == HttpURLConnection.HTTP_CLIENT_TIMEOUT)
            uploadRepository(apiUri, bearer, repositoryKey);

        dropRepository(apiUri, bearer, repositoryKey);
    }

    private String findOpenRepository(URI apiUri, String bearer) throws IOException {
        String endpoint = apiUri.resolve("/manual/search/repositories?ip=client").toString();
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setConnectTimeout(CONNECTION_TIMEOUT);
        conn.setReadTimeout(CONNECTION_TIMEOUT);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + bearer);

        int status = conn.getResponseCode();
        String body = readBody(conn);
        if (status != 200) {
            throw new IllegalStateException("Failed to query repositories: " +
                "status=" + status + ", response=" + body);
        }

        JSONArray repositories = new JSONObject(body).getJSONArray("repositories");
        if (repositories.isEmpty()) {
            throw new IllegalStateException("Open repositories is empty!");
        }

        String repositoryKey = null;
        String group = groupId.get();
        for (int i = 0; i < repositories.length(); i++) {
            JSONObject repo = (JSONObject) repositories.get(i);
            if ("open".equals(repo.getString("state"))) {
                String key = repo.getString("key");
                if (key.contains(group)) {
                    repositoryKey = key;
                    break;
                }
            }
        }

        if (null == repositoryKey) {
            throw new IllegalStateException("No open repositories found with groupId=" + group);
        }
        return repositoryKey;
    }

    private static int uploadRepository(URI apiUri, String bearer, String repositoryKey) throws IOException {
        String response = null;
        int status = -1;
        HttpURLConnection conn = null;
        try {
            String endpoint = apiUri.resolve("/manual/upload/repository/" + repositoryKey + "?publishing_type=automatic").toString();
            conn = (HttpURLConnection) new URL(endpoint).openConnection();
            conn.setConnectTimeout(CONNECTION_TIMEOUT);
            conn.setReadTimeout(CONNECTION_TIMEOUT);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + bearer);
            conn.setDoOutput(true);

            status = conn.getResponseCode();
            response = readBody(conn);
        } catch (SocketTimeoutException ex) {
            return HttpURLConnection.HTTP_CLIENT_TIMEOUT;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to upload repository: repository_key=" + repositoryKey + ", status=" + status + ", response=" + response);
        } finally {
            conn.disconnect();
        }

        return status;
    }

    private static void dropRepository(URI apiUri, String bearer, String repositoryKey) throws IOException {
        String endpoint = apiUri.resolve("/manual/drop/repository/" + repositoryKey).toString();
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setConnectTimeout(CONNECTION_TIMEOUT);
        conn.setReadTimeout(CONNECTION_TIMEOUT);
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("Authorization", "Bearer " + bearer);

        int status = conn.getResponseCode();
        String body = readBody(conn);
        if (status != HttpURLConnection.HTTP_NO_CONTENT) {
            throw new IllegalStateException("Failed to drop repository: repository_key=" + repositoryKey + ", status=" + status + ", response=" + body);
        }
    }

    private static String readBody(HttpURLConnection conn) throws IOException {
        InputStream stream;
        try {
            stream = (conn.getResponseCode() < 400) ? conn.getInputStream() : conn.getErrorStream();
            if (stream == null) {
                return "";
            }
        } catch (IOException e) {
            return "";
        }

        StringBuilder body = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }
}
