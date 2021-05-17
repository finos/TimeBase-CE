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
package com.epam.deltix.izpack.panels.shortcuts;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.handler.Prompt;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.installer.console.ConsolePanel;
import com.izforge.izpack.installer.data.UninstallData;
import com.izforge.izpack.installer.event.InstallerListeners;
import com.izforge.izpack.installer.panel.PanelView;
import com.izforge.izpack.panels.shortcut.ShortcutPanelLogic;
import com.izforge.izpack.util.Console;
import com.izforge.izpack.util.Housekeeper;
import com.izforge.izpack.util.PlatformModelMatcher;
import com.izforge.izpack.util.TargetFactory;
import com.izforge.izpack.util.os.Shortcut;
import com.epam.deltix.izpack.Utils;

import java.util.logging.Logger;

/**
 * Used reflection to access private field. Refactor it asap!
 */
public class ShortcutConsolePanel extends com.izforge.izpack.panels.shortcut.ShortcutConsolePanel {

    static final Logger LOGGER = Logger.getLogger(ShortcutPanel.class.getName());

    private ShortcutHelper shortcutHelper = new ShortcutHelper();

    public ShortcutConsolePanel(InstallData installData,
                                Resources resources,
                                UninstallData uninstallData,
                                Housekeeper housekeeper,
                                TargetFactory factory,
                                InstallerListeners listeners,
                                PlatformModelMatcher matcher,
                                Prompt prompt,
                                PanelView<ConsolePanel> panel) {
        super(installData, resources, uninstallData, housekeeper, factory, listeners, matcher, prompt, panel);
    }

    @Override
    public boolean run(InstallData installData, Console console) {
        try {
            if (Utils.isExpress(installData)) {
                return true;
            }

            ShortcutPanelLogic shortcutPanelLogic = ShortcutHelper.getShortcutPanelLogic(
                    com.izforge.izpack.panels.shortcut.ShortcutConsolePanel.class,
                    this
            );
            if (shortcutPanelLogic == null)
                throw new Exception("Can't find shortcut panel logic.");

            shortcutPanelLogic.refreshShortcutData();
            shortcutPanelLogic.setUserType(Shortcut.ALL_USERS);
            shortcutPanelLogic.setCreateMenuShortcuts(true);
            shortcutPanelLogic.setCreateDesktopShortcuts(true);
            shortcutPanelLogic.setGroupName(shortcutPanelLogic.getSuggestedProgramGroup());

            shortcutHelper.createDesktopDirectory(
                    shortcutPanelLogic,
                    Utils.getPrivateValue(ShortcutPanelLogic.class, shortcutPanelLogic, "groupName")
            );

            shortcutPanelLogic.createAndRegisterShortcuts();
        } catch (Exception e) {
            console.println(e.toString());
        }

        return true;
    }
}
