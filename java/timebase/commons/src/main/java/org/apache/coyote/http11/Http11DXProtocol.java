/*
 * Copyright 2021 EPAM Systems, Inc
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

import com.epam.deltix.util.tomcat.ConnectionHandler;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.Processor;
import org.apache.coyote.UpgradeToken;
import org.apache.coyote.http11.upgrade.BioProcessor;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.net.*;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 *
 */
public class Http11DXProtocol extends AbstractHttp11JsseProtocol<Socket> {

    private static final org.apache.juli.logging.Log log
        = org.apache.juli.logging.LogFactory.getLog(Http11DXProtocol.class);

    @Override
    protected Log                       getLog() { return log; }


    @Override
    protected AbstractEndpoint.Handler  getHandler() {
        return cHandler;
    }


    // ------------------------------------------------------------ Constructor


    public Http11DXProtocol() {
        endpoint = new JIoEndpoint();
        cHandler = new Http11DXConnectionHandler(this);
        ((JIoEndpoint) endpoint).setHandler(cHandler);
        setSoLinger(Constants.DEFAULT_CONNECTION_LINGER);
        setSoTimeout(Constants.DEFAULT_CONNECTION_TIMEOUT);
        setTcpNoDelay(Constants.DEFAULT_TCP_NO_DELAY);
    }


    // ----------------------------------------------------------------- Fields

    private final Http11DXConnectionHandler cHandler;

    public void                             setConnectionHandler(ConnectionHandler connectionHandler) {
        cHandler.setCustomHandler(connectionHandler);
    }


    // ------------------------------------------------ HTTP specific properties
    // ------------------------------------------ managed in the ProtocolHandler

    private int                             disableKeepAlivePercentage = 75;

    public int                              getDisableKeepAlivePercentage() {
        return disableKeepAlivePercentage;
    }

    public void                             setDisableKeepAlivePercentage(int disableKeepAlivePercentage) {
        if (disableKeepAlivePercentage < 0) {
            this.disableKeepAlivePercentage = 0;
        } else if (disableKeepAlivePercentage > 100) {
            this.disableKeepAlivePercentage = 100;
        } else {
            this.disableKeepAlivePercentage = disableKeepAlivePercentage;
        }
    }

    // ----------------------------------------------------- JMX related methods

    @Override
    protected String                        getNamePrefix() {
        return ("deltix-protocol");
    }


    // -----------------------------------  Http11DXConnectionHandler Inner Class

    public static class Http11DXConnectionHandler
        extends AbstractConnectionHandler<Socket, Http11DXProcessor> implements JIoEndpoint.Handler {

        protected Http11DXProtocol proto;
        protected ConnectionHandler customHandler;

        Http11DXConnectionHandler(Http11DXProtocol proto) {
            this.proto = proto;
        }

        @Override
        protected                           AbstractProtocol<Socket> getProtocol() {
            return proto;
        }

        @Override
        protected Log                       getLog() {
            return log;
        }

        @Override
        public SSLImplementation            getSslImplementation() {
            return proto.sslImplementation;
        }

        public void                         setCustomHandler(ConnectionHandler customHandler) {
            this.customHandler = customHandler;
        }

        /**
         * Expected to be used by the handler once the processor is no longer
         * required.
         *
         * @param socket            Not used in BIO
         * @param processor
         * @param isSocketClosing   Not used in HTTP
         * @param addToPoller       Not used in BIO
         */
        @Override
        public void                         release(SocketWrapper<Socket> socket,
                                                    Processor<Socket> processor, boolean isSocketClosing,
                                                    boolean addToPoller) {
            processor.recycle(isSocketClosing);
            recycledProcessors.push(processor);
        }

        @Override
        protected void                      initSsl(SocketWrapper<Socket> socket,
                                                    Processor<Socket> processor) {
            if (proto.isSSLEnabled() && (proto.sslImplementation != null)) {
                processor.setSslSupport(
                    proto.sslImplementation.getSSLSupport(
                        socket.getSocket()));
            } else {
                processor.setSslSupport(null);
            }

        }

        @Override
        protected void                      longPoll(SocketWrapper<Socket> socket,
                                                    Processor<Socket> processor) {
            // NO-OP
        }

        @Override
        protected Http11DXProcessor         createProcessor() {
            Http11DXProcessor processor = new Http11DXProcessor(
                customHandler,
                proto.getMaxHttpHeaderSize(), proto.getRejectIllegalHeaderName(),
                (JIoEndpoint)proto.endpoint, proto.getMaxTrailerSize(),
                proto.getAllowedTrailerHeadersAsSet(), proto.getMaxExtensionSize(),
                proto.getMaxSwallowSize(), proto.getRelaxedPathChars(),
                proto.getRelaxedQueryChars());

            proto.configureProcessor(processor);
            // BIO specific configuration
            processor.setDisableKeepAlivePercentage(proto.getDisableKeepAlivePercentage());
            register(processor);
            return processor;
        }

        @Override
        protected Processor<Socket> createUpgradeProcessor(
            SocketWrapper<Socket> socket, ByteBuffer leftoverInput,
            UpgradeToken upgradeToken)
            throws IOException {
            return new BioProcessor(socket, leftoverInput, upgradeToken,
                proto.getUpgradeAsyncWriteBufferSize());
        }

        @Override
        public void                         beforeHandshake(SocketWrapper<Socket> socket) {
        }
    }
}