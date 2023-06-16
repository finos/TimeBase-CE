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
package com.epam.deltix.izpack.panels.target;

import com.izforge.izpack.api.data.Panel;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.gui.LabelFactory;
import com.izforge.izpack.gui.log.Log;
import com.izforge.izpack.installer.data.GUIInstallData;
import com.izforge.izpack.installer.gui.InstallerFrame;
import com.izforge.izpack.panels.target.TargetPanel;
import com.izforge.izpack.panels.target.TargetPanelHelper;
import com.epam.deltix.izpack.Utils;

import javax.swing.*;
import java.io.File;

/**
 *
 */
public class CheckedTargetPanel extends TargetPanel {

    private JLabel warningLabel;

    public CheckedTargetPanel(Panel panel, InstallerFrame parent, GUIInstallData installData, Resources resources, Log log) {
        super(panel, parent, installData, resources, log);
    }

    public void createLayoutBottom() {
        warningLabel = LabelFactory.create(
                getString(Utils.PROGRAM_FILES_X86_STR),
                parent.getIcons().get("preferences"),
                LEADING);
        add(warningLabel, NEXT_LINE);
    }

    @Override
    public void panelActivate() {
        String path = Utils.PREFS.get(Utils.INST_FOLDER_VAR, TargetPanelHelper.getPath(installData));

        warningLabel.setVisible(false);
        if (Utils.IS_X64 &&
                "x86".equals(System.getProperty("os.arch")) &&
                new File(Utils.getProgramFiles(), "Deltix").getPath().equals(path)) {
            warningLabel.setVisible(true);
        }

        installData.setInstallPath(path);

        super.panelActivate();
    }

    @Override
    public boolean isValidated() {
        if (super.isValidated()) {
            Utils.PREFS.put(Utils.INST_FOLDER_VAR, getPath());
            return true;
        }

        return false;
    }

}