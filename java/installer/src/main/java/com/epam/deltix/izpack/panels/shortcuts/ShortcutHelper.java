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
