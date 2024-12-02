package com.epam.deltix.util.oauth.impl;

import java.io.IOException;

public interface RestClient {

    String postForm(TokenQuery query) throws IOException;

}
