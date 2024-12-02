package com.epam.deltix.util.oauth.impl;

import java.util.HashMap;
import java.util.Map;

public class ParametersTokenQuery implements TokenQuery {

    private final Map<String, String> parameters = new HashMap<>();

    public ParametersTokenQuery(Map<String, String> parameters) {
        this.parameters.putAll(parameters);
    }

    @Override
    public Map<String, String> getParameters() {
        return parameters;
    }
}
