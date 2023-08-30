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
package com.epam.deltix.izpack.panels.shortcuts;

import com.izforge.izpack.api.data.Panel;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.installer.data.GUIInstallData;
import com.izforge.izpack.installer.data.UninstallData;
import com.izforge.izpack.installer.event.InstallerListeners;
import com.izforge.izpack.installer.gui.InstallerFrame;
import com.izforge.izpack.panels.shortcut.ShortcutPanelLogic;
import com.izforge.izpack.util.Housekeeper;
import com.izforge.izpack.util.PlatformModelMatcher;
import com.izforge.izpack.util.TargetFactory;
import com.epam.deltix.izpack.Utils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class ShortcutPanel extends com.izforge.izpack.panels.shortcut.ShortcutPanel {

    static final Logger LOGGER = Logger.getLogger(ShortcutPanel.class.getName());

    private ShortcutHelper shortcutHelper = new ShortcutHelper();

    public ShortcutPanel(Panel panel, InstallerFrame parent, GUIInstallData installData, Resources resources, UninstallData uninstallData, Housekeeper housekeeper, TargetFactory factory, InstallerListeners listeners, PlatformModelMatcher matcher) {
        super(panel, parent, installData, resources, uninstallData, housekeeper, factory, listeners, matcher);
    }

    @Override
    public void panelActivate() {
        try {
            super.panelActivate();

            if (Utils.isExpress(installData)) {
                return;
            }

            isValidated();
        } finally {
            // skipping the ui of panel
            // because we set program folder during installation name step
            parent.skipPanel();
        }
    }

    @Override
    public boolean isValidated() {
        try {
            ShortcutPanelLogic shortcutPanelLogic = ShortcutHelper.getShortcutPanelLogic(com.izforge.izpack.panels.shortcut.ShortcutPanel.class, this);
            shortcutHelper.createDesktopDirectory(
                    shortcutPanelLogic,
                    ShortcutHelper.getGroupName(
                            com.izforge.izpack.panels.shortcut.ShortcutPanel.class,
                            this,
                            shortcutPanelLogic.getSuggestedProgramGroup()
                    )
            );
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error creating desktop folder", e);
        }

        return super.isValidated();
    }

}