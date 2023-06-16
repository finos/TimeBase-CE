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
package com.epam.deltix.qsrv.hf.tickdb.http;

import com.epam.deltix.qsrv.hf.tickdb.comm.UnknownStreamException;
import com.epam.deltix.qsrv.hf.tickdb.http.download.*;
import com.epam.deltix.qsrv.hf.tickdb.http.stream.*;
import com.epam.deltix.qsrv.hf.tickdb.http.upload.UploadHandler;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickDBWrapper;
import com.epam.deltix.qsrv.hf.tickdb.lang.parser.QQLParser;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.TextMap;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.util.io.BasicIOUtil;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.parsers.CompilationException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.StringReader;
import java.security.AccessControlException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import static com.epam.deltix.qsrv.hf.tickdb.http.AbstractHandler.*;
import static com.epam.deltix.qsrv.hf.tickdb.http.HTTPProtocol.marshall;
import static com.epam.deltix.qsrv.hf.tickdb.http.HTTPProtocol.LOGGER;

/**
 *
 */
public class TimebaseServlet extends HttpServlet {

    final static boolean DEBUG = Boolean.getBoolean("TimeBase.http.debug");

    private final Map<String, DXTickDB> userNameToDb = new HashMap<>();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //LOGGER.info(Thread.currentThread().getName());

        String method = req.getMethod();
        if (!method.equals("POST")) {
            resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
            return;
        }

        try {
            final Principal user = req.getUserPrincipal();
            DXTickDB db = TDB;

            if (SC != null) {
                if (user != null) {
                    synchronized (userNameToDb) {
                        db = userNameToDb.get(user.getName());
                        if (db == null)
                            userNameToDb.put(user.getName(), (db = new TickDBWrapper(TDB, SC, user)));
                    }
                } else
                    throw new AccessControlException("User is not specified.");
            }

            // get xml request
            if ("/tb/xml".equals(req.getRequestURI())) {

                final Unmarshaller um = TBJAXBContext.createUnmarshaller();
                final Object body;

                if (DEBUG) {
                    try {
                        final String xml = BasicIOUtil.readFromStream(req.getInputStream());

                        if (StringUtils.isEmpty(xml)) {
                            throw new UnmarshalException("request xml is empty");
                        } else {
                            LOGGER.fine("request: " + xml);
                        }

                        body = um.unmarshal(new StringReader(xml));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                else
                    body = um.unmarshal(req.getInputStream());

                if (body instanceof XmlRequest)
                    HTTPProtocol.validateVersion(((XmlRequest) body).version);

                if (body instanceof ValidateQQLRequest) {
                    validateQQL((ValidateQQLRequest)body, resp);
                } else if (body instanceof CreateStreamRequest) {
                    StreamHandler.createStream(db, (CreateStreamRequest) body, resp);
                } else if (body instanceof ListStreamsRequest) {
                    //UAC will be checked downstream
                    StreamHandler.processListStreams(db, resp);
                } else if (body instanceof LoadStreamsRequest) {
                    //UAC will be checked downstream
                    StreamHandler.processLoadStreams(db, resp);
                } else if (body instanceof FormatDbRequest) {
                    db.format();
                    resp.setStatus(HttpServletResponse.SC_OK);
                } else if (body instanceof GetServerTimeRequest) {
                    GetServerTimeResponse r = new GetServerTimeResponse(db.getServerTime());

                    marshall(r, resp.getOutputStream());
                    resp.setStatus(HttpServletResponse.SC_OK);
                }
                else if (body instanceof DownloadRequest) {

                    if (body instanceof QQLRequest) {
                        AbstractHandler.validatePermissionUnimplemented();
                        final QQLRequest r = (QQLRequest) body;

                        DownloadHandler.getInstance(db, r, resp).run();
                    } else  if (body instanceof SelectRequest) {
                        final SelectRequest r = (SelectRequest) body;

                        DownloadHandler.getInstance(db, r, resp).run();
                    } else if (body instanceof SelectAsStructRequest) {
                        final SelectAsStructRequest r = (SelectAsStructRequest) body;

                        new DownloadStructHandler(db, r, resp).run();
                    }

                } else if (body instanceof StreamRequest) {

                    if (body instanceof RenameStreamRequest) {
                        final RenameStreamRequest r = (RenameStreamRequest) body;

                        StreamHandler.processRename(db, r, resp);
                    } else  if (body instanceof ChangeSchemaRequest) {
                        final ChangeSchemaRequest r = (ChangeSchemaRequest) body;

                        StreamHandler.processChangeSchema(db, r, resp);
                    }
                    else  if (body instanceof SetSchemaRequest) {
                        final SetSchemaRequest r = (SetSchemaRequest) body;

                        StreamHandler.processSetSchema(db, r, resp);
                    } else  if (body instanceof GetSchemaRequest) {
                        final GetSchemaRequest r = (GetSchemaRequest) body;

                        StreamHandler.processGetSchema(db, r, resp);
                    } else if (body instanceof GetPeriodicityRequest) {
                        final GetPeriodicityRequest r = (GetPeriodicityRequest) body;

                        StreamHandler.processGetPeriodicity(db, r, resp);
                    } else if (body instanceof GetRangeRequest) {
                        final GetRangeRequest r = (GetRangeRequest) body;

                        StreamHandler.processGetRange(db, r, resp);
                    } else if (body instanceof ListEntitiesRequest) {
                        final ListEntitiesRequest r = (ListEntitiesRequest) body;

                        StreamHandler.processListEntities(db, r, resp);
                    } else if (body instanceof LockStreamRequest) {
                        StreamHandler.processLock(db, (LockStreamRequest) body, resp);
                    } else if (body instanceof UnlockStreamRequest) {
                        StreamHandler.processUnlock(db, (UnlockStreamRequest) body, resp);
                    } else if (body instanceof ClearRequest) {
                        StreamHandler.processClear(db, (ClearRequest) body, resp);
                    } else if (body instanceof TruncateRequest) {
                        StreamHandler.processTruncate(db, (TruncateRequest) body, resp);
                    } else if (body instanceof PurgeRequest) {
                        StreamHandler.processPurge(db, (PurgeRequest) body, resp);
                    } else if (body instanceof DeleteRequest) {
                        StreamHandler.processDelete(db, (DeleteRequest) body, resp);
                    } else if (body instanceof AbortBGProcessRequest) {
                        StreamHandler.processAbortBG(db, (AbortBGProcessRequest) body, resp);
                    } else if (body instanceof GetBGProcessRequest) {

                        StreamHandler.processGetBG(db, (GetBGProcessRequest) body, resp);
                    } else if (body instanceof ListSpacesRequest) {
                        StreamHandler.processListSpaces(db, (ListSpacesRequest) body, resp);
                    } else if (body instanceof RenameSpaceRequest) {
                        StreamHandler.processRenameSpace(db, (RenameSpaceRequest) body, resp);
                    } else if (body instanceof DeleteSpacesRequest) {
                        StreamHandler.processDeleteSpaces(db, (DeleteSpacesRequest) body, resp);
                    } else if (body instanceof GetSpaceTimeRangeRequest) {
                        StreamHandler.processGetSpaceRange(db, (GetSpaceTimeRangeRequest) body, resp);
                    } else if (body instanceof DescribeStreamRequest) {
                        StreamHandler.processDescribeStreamRequest(db, (DescribeStreamRequest) body, resp);
                    }
                }
                else if (body instanceof CursorRequest) {
                    // sub protocol for cursors

                    DownloadHandler instance = DownloadHandler.getInstance(((CursorRequest)body).id);

                    if (instance == null) {
                        LOGGER.log(Level.WARNING, "Cannot find handler with id:", ((CursorRequest) body).id);
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                                String.format("Cursor with id '%d' doesn't exist", ((CursorRequest) body).id));
                        return;
                    }

                    CursorResponse response = new CursorResponse();

                    if (body instanceof CloseRequest) {
                        instance.close();
                        DownloadHandler.removeInstance(instance.getId());

                    } else if (body instanceof ResetRequest) {
                        response.serial = instance.reset(((ResetRequest) body).time);
                    } else if (body instanceof EntitiesRequest) {
                        EntitiesRequest r = (EntitiesRequest) body;
                        response.serial = instance.changeEntities(r.time, r.mode, StreamHandler.identityKeys(r.identities));
                    } else if (body instanceof TypesRequest) {
                        TypesRequest r = (TypesRequest) body;
                        response.serial = instance.changeTypes(r.mode, r.types);
                    } if (body instanceof StreamsRequest) {
                        StreamsRequest r = (StreamsRequest) body;

                        DXTickStream[] streams = null;
                        if (r.streams != null) {
                            streams = new DXTickStream[r.streams.length];
                            for (int i = 0; i < streams.length; i++) {
                                String key = r.streams[i];
                                streams[i] = db.getStream(key);
                                if (streams[i] == null)
                                    throw new UnknownStreamException(String.format("Stream '%s' doesn't exist", key));
                            }
                        }

                        response.serial = instance.changeStreams(r.mode, streams);
                    }

                    marshall(response, resp.getOutputStream());
                }
                else
                    throw new IllegalArgumentException("unknown request type " + body.getClass().getName());
            } else if ("/tb/bin".equals(req.getRequestURI())) {
                final boolean useCompression = HTTPProtocol.GZIP.equals(req.getHeader(HTTPProtocol.CONTENT_ENCODING));
                new UploadHandler(db, useCompression, req.getInputStream(), resp, user).run();
            } else
                throw new IllegalArgumentException("unexpected URI " + req.getRequestURI());
        } catch (JAXBException e) {
            if (e instanceof UnmarshalException) {
                LOGGER.log(Level.WARNING, "Unmarshalling failed", e);
                sendError(resp, e);
            } else
                throw new ServletException(e);
        } catch (AccessControlException e) {
            if (!resp.isCommitted())
                sendForbiddenError(resp, e);
        } catch (Throwable t) {
            // unwrap
            if (t instanceof UncheckedException)
                t = t.getCause();

            LOGGER.log(Level.WARNING, "HTTP-TB query rejected: ", t);

            if (!resp.isCommitted()) {
                if (t instanceof AccessControlException) {
                    sendForbiddenError(resp, (AccessControlException) t);
                } else {
                    sendError(resp, t);
                }
            }
        }
    }

    static void     validateQQL(ValidateQQLRequest request, HttpServletResponse response) throws IOException {
        TextMap map = QQLParser.createTextMap();
        QQLState state = new QQLState();

        try {
            QQLParser.parse(request.qql, map);
            state.tokens = new ArrayList<>();

            for (com.epam.deltix.qsrv.hf.tickdb.lang.pub.Token token : map.getTokens())
                state.tokens.add(new Token(token.type, token.location));

        } catch (CompilationException e) {
            state.errorLocation = e.location;
        }

        final ValidateQQLResponse r = new ValidateQQLResponse(state);

        marshall(r, response.getOutputStream());
    }
}