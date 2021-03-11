package org.apache.coyote.http11;

import org.apache.coyote.Request;
import org.apache.tomcat.util.http.parser.HttpParser;

/**
 *
 */
public class Http11DXInternalBuffer extends InternalInputBuffer {

    public Http11DXInternalBuffer(Request request, int headerBufferSize,
                                  boolean rejectIllegalHeaderName, HttpParser httpParser)
    {
        super(request, headerBufferSize, rejectIllegalHeaderName, httpParser);
    }

    public byte[]                   getBuffer() {
        return buf;
    }

    public int                      getBufferSize() {
        return lastValid;
    }

}
