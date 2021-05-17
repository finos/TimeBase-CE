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
package com.epam.deltix.izpack.panels.properties;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.installer.console.ConsolePanel;
import com.izforge.izpack.installer.panel.PanelView;
import com.izforge.izpack.panels.htmlhello.HTMLHelloConsolePanel;
import com.izforge.izpack.util.Console;
import com.epam.deltix.izpack.Utils;

import java.util.Properties;
import java.util.logging.Logger;

/**
 *
 */
public class InitConsolePanel extends HTMLHelloConsolePanel {

    public final static Logger LOGGER = Logger.getLogger(InitConsolePanel.class.getName());

    private PropertiesHelper propertiesHelper;

    public InitConsolePanel(Resources resources, PanelView<ConsolePanel> panel) {
        super(resources, panel);
    }

    @Override
    public boolean run(InstallData installData, Properties properties) {
        return true;
    }

    @Override
    public boolean run(InstallData installData, Console console) {
        PropertiesHelper propertiesHelper = getPropertiesHelper(installData);
        propertiesHelper.loadProperties();
        String errorMessage = propertiesHelper.checkPlatform();
        if (errorMessage != null)
            throw new RuntimeException(errorMessage);

        console.println(installData.getMessages().get(Utils.WELCOME_MESSAGE_STR));
        return true;
    }

    private PropertiesHelper getPropertiesHelper(final InstallData installData) {
        if (propertiesHelper == null)
            propertiesHelper = new PropertiesHelper(installData);

        return propertiesHelper;
    }
}
