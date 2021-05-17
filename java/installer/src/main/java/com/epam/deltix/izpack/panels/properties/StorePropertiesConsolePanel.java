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
import com.izforge.izpack.installer.console.AbstractConsolePanel;
import com.izforge.izpack.installer.console.ConsolePanel;
import com.izforge.izpack.installer.panel.PanelView;
import com.izforge.izpack.util.Console;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class StorePropertiesConsolePanel extends AbstractConsolePanel {

    public final static Logger LOGGER = Logger.getLogger(StorePropertiesConsolePanel.class.getName());

    private PropertiesHelper propertiesHelper;

    public StorePropertiesConsolePanel(PanelView<ConsolePanel> panel) {
        super(panel);
    }

    @Override
    public boolean run(InstallData installData, Properties properties) {
        return true;
    }

    @Override
    public boolean run(InstallData installData, Console console) {
        try {
            getPropertiesHelper(installData).storeProperties();
        } catch (Throwable t) {
            console.println("Error storing properties: " + t.getMessage());
            LOGGER.log(Level.WARNING, "Error storing properties.", t);
        }

        return true;
    }

    private PropertiesHelper getPropertiesHelper(final InstallData installData) {
        if (propertiesHelper == null)
            propertiesHelper = new PropertiesHelper(installData);

        return propertiesHelper;
    }
}