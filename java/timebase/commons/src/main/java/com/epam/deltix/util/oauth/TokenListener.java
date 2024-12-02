package com.epam.deltix.util.oauth;

public interface TokenListener {

    void refreshed(String token, long expiresInMs);

}
