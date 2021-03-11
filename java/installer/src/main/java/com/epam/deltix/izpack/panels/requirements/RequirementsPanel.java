package com.epam.deltix.izpack.panels.requirements;

import com.izforge.izpack.api.data.Panel;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.gui.IzPanelLayout;
import com.izforge.izpack.gui.log.Log;
import com.izforge.izpack.installer.data.GUIInstallData;
import com.izforge.izpack.installer.gui.InstallerFrame;
import com.izforge.izpack.installer.gui.IzPanel;
import com.epam.deltix.izpack.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class RequirementsPanel extends IzPanel {

    public final static Logger LOGGER = Logger.getLogger(RequirementsPanel.class.getName());

    private final RequirementsHelper requirementsHelper;

    public RequirementsPanel(Panel panel, InstallerFrame parent, GUIInstallData installData, Resources resources, Log log) {
        super(panel, parent, installData, new IzPanelLayout(log), resources);

        requirementsHelper = new RequirementsHelper(installData);
    }

    @Override
    public void panelActivate() {
        try {
            List<String> moduleNames = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            requirementsHelper.checkRequirements(moduleNames, errors);
            if (errors.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < moduleNames.size(); ++i) {
                    sb.append(moduleNames.get(i)).append(": ").append(errors.get(i)).append("\n");
                }

                LOGGER.log(Level.WARNING, sb.toString());
//                emitError(getString(Utils.REQUIREMENTS_ERROR_STR),
//                        getString(Utils.ERROR_TEXT_1_STR) + "\n" +
//                        sb.toString()
//                );
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Failed to initialize installer.", t);
            emitError("Initialization",
                    "Failed to initialize installer: " + t.getMessage() + "\n");
        }

        parent.skipPanel();
    }

}