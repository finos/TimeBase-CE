/*
 * Copyright 2023 EPAM Systems, Inc
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
package org.apache.coyote.http11;

import com.epam.deltix.util.io.BufferedInputStreamEx;
import com.epam.deltix.util.tomcat.ConnectionHandler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.tomcat.util.net.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.Socket;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  dancing with trambone class
 */
@SuppressFBWarnings(value = "UNENCRYPTED_SOCKET", justification = "Internal socket variable to to substitute connected socket.")
public class Http11DXProcessor extends Http11Processor {

    public static final Logger LOGGER = Logger.getLogger(Http11DXProcessor.class.getName());

    public Http11DXProcessor(ConnectionHandler customHandler,
                             int headerBufferSize, boolean rejectIllegalHeaderName,
                             JIoEndpoint endpoint, int maxTrailerSize, Set<String> allowedTrailerHeaders,
                             int maxExtensionSize, int maxSwallowSize, String relaxedPathChars,
                             String relaxedQueryChars)
    {
        super(headerBufferSize, rejectIllegalHeaderName, endpoint, maxTrailerSize, allowedTrailerHeaders,
            maxExtensionSize, maxSwallowSize, relaxedPathChars, relaxedQueryChars);

        inputBuffer = new Http11DXInternalBuffer(request, headerBufferSize, rejectIllegalHeaderName, httpParser);
        request.setInputBuffer(inputBuffer);

        outputBuffer = new InternalOutputBuffer(response, headerBufferSize);
        response.setOutputBuffer(outputBuffer);

        initializeFilters(maxTrailerSize, allowedTrailerHeaders, maxExtensionSize, maxSwallowSize);

        this.customHandler = customHandler;
    }

    private ConnectionHandler                           customHandler;

    //already closed socket to substitute connected socket
    private Socket                                      closedSocket = new Socket();

    @Override
    public AbstractEndpoint.Handler.SocketState         process(SocketWrapper<Socket> socketWrapper) throws IOException {
        // Setting up the I/O
        setSocketWrapper(socketWrapper);
        Http11DXInternalBuffer input = (Http11DXInternalBuffer) getInputBuffer();
        InternalOutputBuffer output = (InternalOutputBuffer) getOutputBuffer();

        input.init(socketWrapper, endpoint);
        output.init(socketWrapper, endpoint);

        setRequestLineReadTimeout();

        BufferedInputStream bis = new BufferedInputStreamEx(
            socketWrapper.getSocket().getInputStream(), input.getBuffer(), input.getBufferSize());
        OutputStream os = socketWrapper.getSocket().getOutputStream();

        if (customHandler != null && customHandler.handleConnection(socketWrapper.getSocket(), bis, os)) {
            setClosedSocket(socketWrapper);
            socketWrapper.setAsync(false);
            socketWrapper.setBlockingStatus(true);
            socketWrapper.setComet(false);
            socketWrapper.clearDispatches();
            socketWrapper.setError(false);
            socketWrapper.setKeepAliveLeft(100);
            socketWrapper.setLocalAddr(null);
            socketWrapper.setLocalName(null);
            socketWrapper.setLocalPort(-1);
            socketWrapper.setRemoteAddr(null);
            socketWrapper.setRemoteHost(null);
            socketWrapper.setRemotePort(-1);
            socketWrapper.setTimeout(0);
            socketWrapper.setUpgraded(false);
            return AbstractEndpoint.Handler.SocketState.CLOSED;
        }

        return super.process(socketWrapper);
    }

    private void setClosedSocket(SocketWrapper<Socket> socketWrapper) {
        try {
            Field declaredField = SocketWrapper.class.getDeclaredField("socket");
            declaredField.setAccessible(true);
            declaredField.set(socketWrapper, closedSocket);
            declaredField.setAccessible(false);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to set socket", e);
        }
    }

}