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
package com.epam.deltix.qsrv.hf.tickdb.web.model.impl;

import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBMonitor;
import com.epam.deltix.qsrv.hf.tickdb.web.model.pub.MenuModel;
import com.epam.deltix.qsrv.hf.tickdb.web.model.pub.TimeBaseModel;
import com.epam.deltix.util.Version;
import com.epam.deltix.util.time.GMT;
import com.epam.deltix.util.time.TimeKeeper;

import java.util.Date;

/**
 *
 */
public class TimeBaseModelImpl implements TimeBaseModel {

    private MenuModel menuModel;

    public String getVersion() {
        return Version.VERSION_STRING;
    }

    public String getTitle() {
        return "TimeBase Monitor";
    }

    @Override
    public Date getCurrentDate() {
        return new Date(TimeKeeper.currentTime);
    }

    @Override
    public String getFormat(Date forDate) {
        if (forDate != null && forDate.getTime() != Long.MIN_VALUE)
            return (GMT.clearTime(new Date()).getTime() != GMT.clearTime(forDate).getTime() ? "yyyy-MM-dd " : "") + "HH:mm:ss.SSS";
        return "";
    }

    @Override
    public void setMenuModel(MenuModel menuModel) {
        this.menuModel = menuModel;
    }

    @Override
    public MenuModel getMenuModel() {
        return menuModel;
    }

    @Override
    public TBMonitor getMonitor() {
        return (TBMonitor) com.epam.deltix.qsrv.hf.tickdb.http.AbstractHandler.TDB;
    }

}