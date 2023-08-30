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
package com.epam.deltix.izpack.panels.properties;

import com.izforge.izpack.api.data.Panel;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.gui.IzPanelLayout;
import com.izforge.izpack.gui.log.Log;
import com.izforge.izpack.installer.data.GUIInstallData;
import com.izforge.izpack.installer.gui.InstallerFrame;
import com.izforge.izpack.installer.gui.IzPanel;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class StorePropertiesPanel extends IzPanel {

    private final static Logger LOGGER = Logger.getLogger(StorePropertiesPanel.class.getName());

    private PropertiesHelper propertiesHelper;

    public StorePropertiesPanel(Panel panel, InstallerFrame parent, GUIInstallData installData, Resources resources, Log log) {
        super(panel, parent, installData, new IzPanelLayout(log), resources);

        propertiesHelper = new PropertiesHelper(installData);
    }

    @Override
    public void panelActivate() {
        try {
            propertiesHelper.storeProperties();
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Error storing properties.", t);
        } finally {
            parent.skipPanel();
        }
    }

    public boolean isValidated() {
        return true;
    }
}