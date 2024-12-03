package com.epam.deltix.util.oauth.impl;

public class TokenInfo {

    private final String token;
    private final long expiresInSec;

    public TokenInfo(String token, long expiresInSec) {
        this.token = token;
        this.expiresInSec = expiresInSec;
    }

    public String accessToken() {
        return token;
    }

    public long expiresInSec() {
        return expiresInSec;
    }

}
