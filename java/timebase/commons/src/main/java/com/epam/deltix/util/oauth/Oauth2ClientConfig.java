package com.epam.deltix.util.oauth;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

public class Oauth2ClientConfig {

    private String url;

    private final Map<String, String> parameters = new HashMap<>();

    private KeystoreConfig keystoreConfig;

    private Timer timer;

    private TokenListener listener;

    private long timeoutMs = 5000;
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 5000;

    private double expirationMultiplier = 0.7;

    private Oauth2ClientConfig() {
    }

    public String getUrl() {
        return url;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public KeystoreConfig getKeystoreConfig() {
        return keystoreConfig;
    }

    public Timer getTimer() {
        return timer;
    }

    public TokenListener getListener() {
        return listener;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public double getExpirationMultiplier() {
        return expirationMultiplier;
    }

    public static Builder builder() {
        return new Oauth2ClientConfig().new Builder();
    }

    public class Builder {

        private Builder() {
        }

        public Builder withUrl(String url) {
            Oauth2ClientConfig.this.url = url;
            return this;
        }

        public Builder withClientCredentials(String clientId, String clientSecret) {
            parameters.put(Oauth2Client.GRANT_TYPE_PARAM, Oauth2Client.CLIENT_CREDENTIALS_GRANT_TYPE);
            parameters.put(Oauth2Client.CLIENT_ID_PARAM, clientId);
            parameters.put(Oauth2Client.CLIENT_SECRET_PARAM, clientSecret);
            return this;
        }

        public Builder withClientCredentials(String clientId, KeystoreConfig keystoreConfig) {
            parameters.put(Oauth2Client.GRANT_TYPE_PARAM, Oauth2Client.CLIENT_CREDENTIALS_GRANT_TYPE);
            parameters.put(Oauth2Client.CLIENT_ID_PARAM, clientId);
            Oauth2ClientConfig.this.keystoreConfig = keystoreConfig;
            return this;
        }

        public Builder withKeystoreConfig(KeystoreConfig keystoreConfig) {
            Oauth2ClientConfig.this.keystoreConfig = keystoreConfig;
            return this;
        }

        public Builder withParameter(String name, String value) {
            Oauth2ClientConfig.this.parameters.put(name, value);
            return this;
        }

        public Builder withTimer(Timer timer) {
            Oauth2ClientConfig.this.timer = timer;
            return this;
        }

        public Builder withListener(TokenListener listener) {
            Oauth2ClientConfig.this.listener = listener;
            return this;
        }

        public Builder withTimeout(long timeoutMs) {
            Oauth2ClientConfig.this.timeoutMs = timeoutMs;
            return this;
        }

        public Builder withConnectTimeout(int connectTimeoutMs) {
            Oauth2ClientConfig.this.connectTimeoutMs = connectTimeoutMs;
            return this;
        }

        public Builder withReadTimeout(int readTimeoutMs) {
            Oauth2ClientConfig.this.readTimeoutMs = readTimeoutMs;
            return this;
        }

        public Builder withExpirationMultiplier(double multiplier) {
            Oauth2ClientConfig.this.expirationMultiplier = multiplier;
            return this;
        }

        public Oauth2ClientConfig build() {
            return Oauth2ClientConfig.this;
        }

    }
}
