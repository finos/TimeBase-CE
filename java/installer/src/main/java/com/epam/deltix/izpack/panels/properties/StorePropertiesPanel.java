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