package com.epam.deltix.izpack.panels.license;

import com.izforge.izpack.api.data.Panel;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.gui.log.Log;
import com.izforge.izpack.installer.data.GUIInstallData;
import com.izforge.izpack.installer.gui.InstallerFrame;

/**
 *
 */
public class HTMLLicencePanel extends com.izforge.izpack.panels.htmllicence.HTMLLicencePanel {
    public HTMLLicencePanel(Panel panel, InstallerFrame parent, GUIInstallData installData, Resources resources, Log log) {
        super(panel, parent, installData, resources, log);
    }
}
