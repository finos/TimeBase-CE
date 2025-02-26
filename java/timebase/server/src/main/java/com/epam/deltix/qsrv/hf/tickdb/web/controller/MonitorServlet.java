package com.epam.deltix.qsrv.hf.tickdb.web.controller;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.tickdb.http.AbstractHandler;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBMonitor;
import com.epam.deltix.qsrv.hf.tickdb.web.model.impl.ModelFactoryImpl;
import com.epam.deltix.qsrv.hf.tickdb.web.model.pub.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class MonitorServlet extends HttpServlet {

    private static final Log LOG = LogFactory.getLog(MonitorServlet.class);
    public static final String MODEL_ARG = "model";
    public static final String ALERT_TYPE_ARG = "alert_type";
    public static final String ALERT_TYPE_DANGER = "danger";
    public static final String ALERT_MSG_ARG = "alert_msg";
    public static final String REDIRECT_WITH_ALERT_PATTERN = "%s%s?alert_type=%s&alert_msg=%s";

    private final ModelFactory modelFactory;

    public MonitorServlet() {
        this.modelFactory = ModelFactoryImpl.getInstance();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resetAlertsToAttribute(req);
        String path = req.getServletPath();
        try {
            if (path.equals("/") || path.equals("/index")) {
                resp.sendRedirect(req.getContextPath() + "/cursors");
            } else if (path.startsWith("/loaders")) {
                if (req.getPathInfo() != null && req.getPathInfo().length() > 1) {
                    handleLoader(req, resp);
                } else {
                    handleLoaders(req, resp);
                }
            } else if (path.equals("/cursors")) {
                if (req.getPathInfo() != null && req.getPathInfo().length() > 1) {
                    handleCursor(req, resp);
                } else {
                    handleCursors(req, resp);
                }
            } else if (path.equals("/connections")) {
                handleConnections(req, resp);
            } else if (path.equals("/connection")) {
                handleConnection(req, resp);
            } else if (path.equals("/locks")) {
                handleLocks(req, resp);
            } else if (path.startsWith("/track")) {
                handleTrack(req, resp);
            } else {
                handleNotFound(resp);
            }
        } catch (Exception e) {
            LOG.error("Error processing request: %s").with(e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    private void handleLoaders(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        LoadersModel loadersModel = modelFactory.getLoadersModel();
        req.setAttribute(MODEL_ARG, loadersModel);
        req.getRequestDispatcher("/WEB-INF/jsp/loaders.jsp").forward(req, resp);
    }

    private void handleCursors(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        CursorsModel cursorsModel = modelFactory.getCursorsModel();
        req.setAttribute(MODEL_ARG, cursorsModel);
        req.getRequestDispatcher("/WEB-INF/jsp/cursors.jsp").forward(req, resp);
    }

    private void handleLoader(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String idParam = req.getPathInfo().substring(1);
        long id;
        try {
            id = Long.parseLong(idParam);
        } catch (NumberFormatException e) {
            redirectWithMessage(req, resp, "/loaders", ALERT_TYPE_DANGER, "Invalid loader id format.");
            return;
        }

        LoaderModel loaderModel = modelFactory.getLoaderModel(id);
        if (loaderModel.getLoader() == null) {
            redirectWithMessage(req, resp, "/loaders", ALERT_TYPE_DANGER, "Unknown loader with id " + id + ".");

        } else {
            req.setAttribute(MODEL_ARG, loaderModel);
            req.getRequestDispatcher("/WEB-INF/jsp/loader.jsp").forward(req, resp);
        }
    }

    private void handleCursor(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String idParam = req.getPathInfo().substring(1);
        long id;
        try {
            id = Long.parseLong(idParam);
        } catch (NumberFormatException e) {
            redirectWithMessage(req, resp, "/cursors", ALERT_TYPE_DANGER, "Invalid cursor ID format.");
            return;
        }
        CursorModel cursorModel = modelFactory.getCursorModel(id);
        if (cursorModel.getCursor() == null) {
            redirectWithMessage(req, resp, "/cursors", ALERT_TYPE_DANGER, "Unknown cursor with id " + id + ".");
        } else {
            req.setAttribute("model", cursorModel);
            req.getRequestDispatcher("/WEB-INF/views/cursor.jsp").forward(req, resp);
        }
    }

    private void handleConnections(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        ConnectionsModel connectionsModel = modelFactory.getConnectionsModel();
        req.setAttribute(MODEL_ARG, connectionsModel);
        req.getRequestDispatcher("/WEB-INF/jsp/connections.jsp").forward(req, resp);
    }

    private void handleConnection(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String id = req.getParameter("clientId");
        if (id == null || id.trim().isEmpty()) {
            redirectWithMessage(req, resp, "/connections", ALERT_TYPE_DANGER, "Client ID is required.");
            return;
        }

        ConnectionModel connectionModel = modelFactory.getConnectionModel(id);
        if (connectionModel.getDispatcher() == null) {
            redirectWithMessage(req, resp, "/connections", ALERT_TYPE_DANGER, "Unknown connection with id " + id + ".");
        } else {
            req.setAttribute("model", connectionModel);
            req.getRequestDispatcher("/WEB-INF/jsp/connection.jsp").forward(req, resp);
        }
    }

    private void handleLocks(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        LocksModel locksModel = modelFactory.getLocksModel();
        req.setAttribute(MODEL_ARG, locksModel);
        req.getRequestDispatcher("/WEB-INF/jsp/locks.jsp").forward(req, resp);
    }

    private void handleTrack(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String param = req.getPathInfo().substring(1);
        boolean trackMessages = Boolean.parseBoolean(param);
        ((TBMonitor) AbstractHandler.TDB).setTrackMessages(trackMessages);
        String referer = req.getHeader("Referer");
        resp.sendRedirect(referer != null ? referer : req.getContextPath() + "/");
    }

    private void redirectWithMessage(HttpServletRequest req, HttpServletResponse resp, String path, String alertType, String message) throws IOException {
        String redirectUrl = String.format(REDIRECT_WITH_ALERT_PATTERN,
                req.getContextPath(), path, alertType, URLEncoder.encode(message, StandardCharsets.UTF_8));
        resp.sendRedirect(redirectUrl);
    }

    private void resetAlertsToAttribute(HttpServletRequest req) {
        String alertType = req.getParameter(ALERT_TYPE_ARG);
        String alertMsg = req.getParameter(ALERT_MSG_ARG);
        if (alertType != null && alertMsg != null) {
            req.setAttribute(ALERT_TYPE_ARG, alertType);
            req.setAttribute(ALERT_MSG_ARG, alertMsg);
        }
    }

    private void handleNotFound(HttpServletResponse resp) throws IOException {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource not found");
    }
}