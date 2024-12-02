package com.epam.deltix.util.oauth;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.util.oauth.impl.*;
import com.epam.deltix.util.time.TimeKeeper;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class Oauth2Client implements AutoCloseable {

    public static final Log LOGGER = LogFactory.getLog(Oauth2Client.class.getName());

    public static final String GRANT_TYPE_PARAM = "grant_type";
    public static final String CLIENT_ID_PARAM = "client_id";
    public static final String CLIENT_SECRET_PARAM = "client_secret";

    public static final String CLIENT_CREDENTIALS_GRANT_TYPE = "client_credentials";

    private static final long DEFAULT_RETRY_DELAY_MS = 5 * 1000;
    private static final long MAX_RETRY_DELAY_MS = 5 * 60 * 1000; // 5 min

    private final Oauth2ClientConfig config;
    private final RestClient restClient;
    private final TokenQuery tokenQuery;

    private final TokenResponseParser parser;
    private final String clientId;
    private final Map<String, String> parameters = new HashMap<>();

    private final TokenListener listener;
    private final RefreshTokenScheduler refreshScheduler;

    private long retryDelay = DEFAULT_RETRY_DELAY_MS;

    private volatile TokenInfo tokenInfo;
    private volatile long expirationTimestampMs;

    private volatile boolean closed;

    private final ReentrantLock lock = new ReentrantLock();

    public static Oauth2Client create(Oauth2ClientConfig config) {
        RestClient restClient = HttpConnectionRestClient.create(
            config.getUrl(), config.getConnectTimeoutMs(), config.getReadTimeoutMs()
        );
        return new Oauth2Client(config, restClient);
    }

    private Oauth2Client(Oauth2ClientConfig config, RestClient restClient) {
        this.config = config;
        this.restClient = restClient;
        this.tokenQuery = createTokenQuery(config);
        this.parser = new TokenResponseParser();
        this.parameters.putAll(config.getParameters());
        this.clientId = parameters.get(CLIENT_ID_PARAM);
        this.listener = config.getListener();
        this.refreshScheduler = config.getTimer() != null
            ? new TimerTokenScheduler(config.getTimer()) : null;

        // initial token request
        try {
            requestToken();
        } catch (Throwable t) {
            LOGGER.error().append("Failed to request token").append(t).commit();
        }
    }

    private static TokenQuery createTokenQuery(Oauth2ClientConfig config) {
        if (CLIENT_CREDENTIALS_GRANT_TYPE.equals(config.getParameters().get(GRANT_TYPE_PARAM))) {
            String clientId = config.getParameters().get(CLIENT_ID_PARAM);
            if (clientId == null) {
                throw new RuntimeException(CLIENT_ID_PARAM + " is not specified");
            }

            if (config.getParameters().get(CLIENT_SECRET_PARAM) == null) {
                if (config.getKeystoreConfig() != null) {
                    return new CertificateTokenQuery(config.getUrl(), clientId,
                        config.getKeystoreConfig(), config.getParameters()
                    );
                }

                throw new RuntimeException("Invalid credentials: specify `" + CLIENT_SECRET_PARAM + "` parameter or keystore config.");
            }
        }

        return new ParametersTokenQuery(config.getParameters());
    }

    public String clientId() {
        return clientId;
    }

    public String token() {
        return getOrRequestToken().accessToken();
    }

    public long expirationTimestampMs() {
        return expirationTimestampMs;
    }

    private TokenInfo getOrRequestToken() {
        if (tokenInfo == null) {
            requestTokenIfNeed();
        } else if (refreshScheduler == null) {
            // configured without refresh task
            // perform refresh token in current thread if needed
            if (refreshRequired()) {
                refreshTokenIfNeed();
            }
        }

        return tokenInfo;
    }

    private void requestTokenIfNeed() {
        boolean requestTimeout = !tryUnderLock(() -> {
            // double check under lock
            if (tokenInfo == null) {
                requestToken();
            }
        });
        if (requestTimeout) {
            throw new RuntimeException("Request token timeout");
        }
    }

    private void refreshTokenIfNeed() {
        tryUnderLock(() -> {
            // double check under lock
            if (refreshRequired()) {
                requestToken();
            }
        });
    }

    //returns false in case of timeout
    private boolean tryUnderLock(Runnable logic) {
        boolean locked;
        try {
            locked = lock.tryLock(config.getTimeoutMs(), TimeUnit.MILLISECONDS);
            if (locked) {
                try {
                    logic.run();
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        return locked;
    }

    private boolean refreshRequired() {
        return currentTime() >= expirationTimestampMs;
    }

    private long currentTime() {
        return TimeKeeper.currentTime;
    }

    private void requestToken() {
        if (closed) {
            return;
        }

        long requestTime = currentTime();
        tokenInfo = parser.parse(sendTokenRequest());
        if (tokenInfo.expiresInSec() == 0) {
            LOGGER.warn().append("Token updated (grant type: ").append(parameters.get(GRANT_TYPE_PARAM))
                .append("); expiration timestamp unknown, refresh task is not scheduled.").commit();
        } else {
            // update expiration time
            long expirationDelayMs = tokenInfo.expiresInSec() * (long) (config.getExpirationMultiplier() * 1000.0d);
            expirationTimestampMs = requestTime + expirationDelayMs;

            LOGGER.info().append("Token updated (grant type: ").append(parameters.get(GRANT_TYPE_PARAM))
                .append("); expiration timestamp: ")
                .append(Instant.ofEpochMilli(expirationTimestampMs)).commit();

            scheduleRefresh(expirationDelayMs);
        }

        notifyRefreshed();
    }

    private String sendTokenRequest() {
        try {
            return restClient.postForm(tokenQuery);
        } catch (IOException e) {
            throw new RuntimeException("Failed to perform REST query", e);
        } catch (Throwable t) {
            LOGGER.warn().append("Failed to request token").append(t).commit();
            throw t;
        }
    }

    private void notifyRefreshed() {
        if (listener != null) {
            listener.refreshed(tokenInfo.accessToken(), tokenInfo.expiresInSec());
        }
    }

    private void scheduleRefresh(long delayMs) {
        if (refreshScheduler != null && !closed) {
            refreshScheduler.schedule(delayMs, this::refreshTokenTask);
            LOGGER.info().append("Refresh token task scheduled in ").append(delayMs / 1000).append(" seconds.").commit();
        }
    }

    private void refreshTokenTask() {
        try {
            lock.lock();
            try {
                requestToken();
                refreshRetryDelay();
            } finally {
                lock.unlock();
            }
        } catch (Throwable t) {
            LOGGER.warn().append("Failed to execute task").append(t).commit();
            scheduleRefresh(getRetryDelay());
        }
    }

    private void refreshRetryDelay() {
        retryDelay = DEFAULT_RETRY_DELAY_MS;
    }

    private long getRetryDelay() {
        long currentRetryDelay = retryDelay;
        retryDelay *= 2;
        if (retryDelay > MAX_RETRY_DELAY_MS) {
            retryDelay = MAX_RETRY_DELAY_MS;
        }

        return currentRetryDelay;
    }

    @Override
    public void close() {
        closed = true;
        if (refreshScheduler != null) {
            refreshScheduler.close();
        }
    }

}
