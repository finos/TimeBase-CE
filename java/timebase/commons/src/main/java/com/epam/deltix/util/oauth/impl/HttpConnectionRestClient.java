package com.epam.deltix.util.oauth.impl;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class HttpConnectionRestClient implements RestClient {

    private final URL url;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public static RestClient create(String url, int connectTimeoutMs, int readTimeoutMs) {
        try {
            return new HttpConnectionRestClient(new URL(url), connectTimeoutMs, readTimeoutMs);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpConnectionRestClient(URL url, int connectTimeoutMs, int readTimeoutMs) {
        this.url = url;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    @Override
    public String postForm(TokenQuery query) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(connectTimeoutMs);
        connection.setReadTimeout(readTimeoutMs);

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("charset", "UTF-8");
        connection.setUseCaches(false);
        connection.setDoOutput(true);

        try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
            boolean first = true;
            Map<String, String> parameters = query.getParameters();
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    outputStream.writeBytes("&");
                }

                outputStream.writeBytes(entry.getKey());
                outputStream.writeBytes("=");
                outputStream.writeBytes(entry.getValue());
            }
        }

        if (connection.getResponseCode() != 200) {
            throw new RuntimeException("Http request failed with code: " + connection.getResponseCode() +
                "; message: " + connection.getResponseMessage());
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        return response.toString();
    }

}
