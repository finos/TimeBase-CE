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
package com.epam.deltix.qsrv.hf.tickdb.web.controller;

import com.epam.deltix.gflog.api.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBMonitor;
import com.epam.deltix.qsrv.hf.tickdb.web.model.pub.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 *
 */
@Controller
@RequestMapping(value = "/")
public class TimeBaseMonitorController {

    public static final Log LOGGER = LogFactory.getLog(TimeBaseMonitorController.class);

    private static final String MODEL_ARG = "model";
    private static final String MENU_MODEL_ARG = "menuModel";
    private static final String ALERT_TYPE_ARG = "alert_type";
    private static final String ALERT_MSG_ARG = "alert_msg";
    private static final String ALERT_TYPE_DANGER = "danger";
    private static final String ALERT_TYPE_SUCCESS = "success";

    @Autowired
    private ModelFactory modelFactory;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String startPage() {
        return "redirect:/cursors";
    }

    @RequestMapping(value = "/index", method = RequestMethod.GET)
    public String index() {
        return "redirect:/cursors";
    }

//    @RequestMapping(value = "/license", method = RequestMethod.GET)
//    public String license(ModelMap modelMap) {
//        setModel(modelMap, modelFactory.getLicenseModel(checkLicense(modelMap)));
//
//        return "license";
//    }

    @RequestMapping(value = "/loaders", method = RequestMethod.GET)
    public String loaders(ModelMap modelMap) {
        setModel(modelMap, modelFactory.getLoadersModel());

        return "loaders";
    }

    @RequestMapping(value = "/loaders/{id}", method = RequestMethod.GET)
    public String loader(@PathVariable("id") Long id, ModelMap modelMap, final RedirectAttributes redirectAttributes) {
        LoaderModel loaderModel = modelFactory.getLoaderModel(id);
        if (loaderModel.getLoader() == null) {
            addRedirectAttribute(redirectAttributes, ALERT_TYPE_DANGER, "Unknown loader with id " + id + ".");
            return "redirect:/loaders";
        }

        setModel(modelMap, loaderModel);

        return "loader";
    }

    @RequestMapping(value = "/cursors/{id}", method = RequestMethod.GET)
    public String cursor(@PathVariable("id") Long id, ModelMap modelMap, final RedirectAttributes redirectAttributes) {
        CursorModel cursorModel = modelFactory.getCursorModel(id);
        if (cursorModel.getCursor() == null) {
            addRedirectAttribute(redirectAttributes, ALERT_TYPE_DANGER, "Unknown cursor with id " + id + ".");
            return "redirect:/cursors";
        }

        setModel(modelMap, cursorModel);

        return "cursor";
    }

    @RequestMapping(value = "/cursors", method = RequestMethod.GET)
    public String cursors(ModelMap modelMap) {
        setModel(modelMap, modelFactory.getCursorsModel());

        return "cursors";
    }

    @RequestMapping(value = "/connections", method = RequestMethod.GET)
    public String connections(ModelMap modelMap) {
        setModel(modelMap, modelFactory.getConnectionsModel());

        return "connections";
    }

    @RequestMapping(value = "/connection", method = RequestMethod.GET)
    public String connection(@ModelAttribute("clientId") String id, ModelMap modelMap, final RedirectAttributes redirectAttributes) {
        ConnectionModel cursorModel = modelFactory.getConnectionModel(id);
        if (cursorModel.getDispatcher() == null) {
            addRedirectAttribute(redirectAttributes, ALERT_TYPE_DANGER, "Unknown connection with id " + id + ".");
            return "redirect:/connections";
        }

        setModel(modelMap, cursorModel);

        return "connection";
    }

    @RequestMapping(value = "/locks", method = RequestMethod.GET)
    public String locks(ModelMap modelMap) {
        setModel(modelMap, modelFactory.getLocksModel());

        return "locks";
    }

    @RequestMapping(value = "/track/{on}", method = RequestMethod.GET)
    public String track(HttpServletRequest request, @PathVariable("on") boolean on) {
        ((TBMonitor) com.epam.deltix.qsrv.hf.tickdb.http.AbstractHandler.TDB).setTrackMessages(on);
        String referer = request.getHeader("Referer");
        return "redirect:" + referer;
    }

    private void addAlertAttribute(final ModelMap modelMap, final String type, final String message) {
        modelMap.addAttribute(ALERT_TYPE_ARG, type);
        modelMap.addAttribute(ALERT_MSG_ARG, message);
    }

    private void addRedirectAttribute(final RedirectAttributes redirectAttributes, final String type, final String message) {
        redirectAttributes.addFlashAttribute(ALERT_TYPE_ARG, type);
        redirectAttributes.addFlashAttribute(ALERT_MSG_ARG, message);
    }

    private void setModel(final ModelMap modelMap, final TimeBaseModel model) {
        modelMap.addAttribute(MODEL_ARG, model);
    }

    private void setMenuModel(final ModelMap modelMap, final MenuModel model) {
        modelMap.addAttribute(MENU_MODEL_ARG, model);
    }
}