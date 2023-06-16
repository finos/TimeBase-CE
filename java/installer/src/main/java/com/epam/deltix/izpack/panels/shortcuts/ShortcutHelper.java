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

import com.izforge.izpack.panels.shortcut.ShortcutData;
import com.izforge.izpack.panels.shortcut.ShortcutPanelLogic;
import com.izforge.izpack.util.os.Shortcut;
import com.izforge.izpack.util.os.Win_Shortcut;
import com.epam.deltix.izpack.Utils;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.logging.Level;

/**
 * Creates desktop directory for shortcuts.
 * Used reflection to access private field. Refactor it asap!
 */
public class ShortcutHelper {

    void createDesktopDirectory(final ShortcutPanelLogic shortcutPanelLogic, final String groupName) throws Exception {
        try {
            Shortcut shortcut = getShortcut(shortcutPanelLogic);

            if (shortcut instanceof Win_Shortcut) {
                if (shortcutPanelLogic != null && shortcutPanelLogic.hasDesktopShortcuts()) {
                    List<ShortcutData> desktopShortcuts = getDesktopShortcuts(shortcutPanelLogic);
                    if (desktopShortcuts != null) {
                        for (final ShortcutData shortcutData : desktopShortcuts) {
                            shortcutData.name = groupName + File.separator + shortcutData.name;

                            shortcut.setUserType(shortcutPanelLogic.getUserType());
                            shortcut.setLinkName(shortcutData.name);
                            shortcut.setLinkType(shortcutData.type);
                            File directory = new File(shortcut.getBasePath() + File.separator + groupName);
                            if (!directory.exists())
                                directory.mkdirs();
                        }
                    }
                }
            }
        } catch (Exception e) {
            ShortcutPanel.LOGGER.log(Level.SEVERE, "Create shortcut error", e);
            throw e;
        }
    }

    static ShortcutPanelLogic getShortcutPanelLogic(final Class<?> cls, final Object panel) throws Exception {
        return Utils.getPrivateValue(cls, panel, "shortcutPanelLogic");
    }

    static String getGroupName(final Class<?> cls, final Object panel, final String defaultGroup) {
        try {
            return getGroupName(cls, panel);
        } catch (Exception e) {
            return defaultGroup;
        }
    }

    private static Shortcut getShortcut(final ShortcutPanelLogic shortcutPanelLogic) throws Exception {
        return Utils.getPrivateValue(ShortcutPanelLogic.class, shortcutPanelLogic, "shortcut");
    }

    private static List<ShortcutData> getDesktopShortcuts(final ShortcutPanelLogic shortcutPanelLogic) throws Exception {
        return Utils.getPrivateValue(ShortcutPanelLogic.class, shortcutPanelLogic, "desktopShortcuts");
    }

    private static String getGroupName(final Class<?> cls, final Object panel) throws Exception {
        JTextField programGroup = Utils.getPrivateValue(cls, panel, "programGroup");
        if (programGroup != null)
            return programGroup.getText();

        throw new Exception("Empty program group");
    }
}