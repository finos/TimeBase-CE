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
package com.epam.deltix.izpack.panels.finish;

import com.izforge.izpack.api.GuiId;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.data.Pack;
import com.izforge.izpack.api.data.Panel;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.gui.ButtonFactory;
import com.izforge.izpack.gui.LabelFactory;
import com.izforge.izpack.gui.log.Log;
import com.izforge.izpack.installer.data.GUIInstallData;
import com.izforge.izpack.installer.data.UninstallData;
import com.izforge.izpack.installer.data.UninstallDataWriter;
import com.izforge.izpack.installer.gui.InstallerFrame;
import com.epam.deltix.izpack.Utils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class FinishPanel extends com.izforge.izpack.panels.finish.FinishPanel {

    private final static Logger LOGGER = Logger.getLogger(FinishPanel.class.getName());

    private final Log izpackLog;
    private final UninstallDataWriter uninstallDataWriter;

    private final JCheckBox launchArchitect = new JCheckBox(getString(Utils.FINISH_LAUNCH_ARCHITECT_STR), true);
    private final JCheckBox launchQuantOffice = new JCheckBox(getString(Utils.FINISH_LAUNCH_QO_STR), true);

    public FinishPanel(Panel panel, InstallerFrame parent, GUIInstallData installData, Resources resources, UninstallDataWriter uninstallDataWriter, UninstallData uninstallData, Log log) {
        super(panel, parent, installData, resources, uninstallDataWriter, uninstallData, log);
        this.uninstallDataWriter = uninstallDataWriter;
        this.izpackLog = log;
    }

    @Override
    public void panelActivate() {
        this.parent.lockNextButton();
        this.parent.lockPrevButton();
        this.parent.setQuitButtonText(this.getI18nStringForClass("done"));
        this.parent.setQuitButtonIcon("done");
        Insets inset = new Insets(10, 20, 2, 2);
        GridBagConstraints constraints = new GridBagConstraints(0, 0, 1, 1, 0.0D, 0.0D, 21, 10, inset, 0, 0);
        if (this.installData.isInstallSuccess()) {
            JLabel jLabel = LabelFactory.create(this.getI18nStringForClass("success"), (Icon)this.parent.getIcons().get("preferences"), 10);
            jLabel.setName(GuiId.FINISH_PANEL_LABEL.id);
            this.add(jLabel, constraints);
            ++constraints.gridy;
            if (this.uninstallDataWriter.isUninstallRequired()) {
                String path = this.translatePath(this.installData.getInfo().getUninstallerPath());
                this.add(LabelFactory.create(this.getI18nStringForClass("uninst.info"), (Icon)this.parent.getIcons().get("preferences"), 10), constraints);
                ++constraints.gridy;
                this.add(LabelFactory.create(path, (Icon)this.parent.getIcons().get("empty"), 10), constraints);
                ++constraints.gridy;
            }

            if (!Utils.isExpress(installData)) {
                this.autoButton = ButtonFactory.createButton(this.getI18nStringForClass("auto"), (Icon) this.parent.getIcons().get("edit"), this.installData.buttonsHColor);
                this.autoButton.setName(GuiId.FINISH_PANEL_AUTO_BUTTON.id);
                this.autoButton.setToolTipText(this.getI18nStringForClass("auto.tip"));
                this.autoButton.addActionListener(this);
                this.add(this.autoButton, constraints);
                ++constraints.gridy;
            }
        } else {
            this.add(LabelFactory.create(this.getI18nStringForClass("fail"), (Icon)this.parent.getIcons().get("stop"), 10), constraints);
        }

        this.getLayoutHelper().completeLayout();
        this.izpackLog.informUser();

        if (Utils.isExpress(installData)) {
            registerLauncher(launchQuantOffice, getQuantOfficeLauncher(installData));
        } else if (installData.isInstallSuccess() && qsInstalled()) {
            registerLauncher(launchArchitect, getArchitectLauncher(installData));
        }
    }

    private void registerLauncher(JCheckBox launchCheckBox, File launcher) {
        if (launcher == null) {
            return;
        }

        // Add checkbox to standard FinishPanel
        // IzPack don't provide access to UI of FinishPanel
        Insets inset = new Insets(10, 20, 2, 2);
        GridBagConstraints constraints = new GridBagConstraints(
                0, 4, 1, 1, 0, 0,
                GridBagConstraints.LINE_START, GridBagConstraints.CENTER, inset, 0, 0);
        add(launchCheckBox, constraints);

        // IzPack don't call isValidated() for FinishPanel and just closes application
        // so to launch qs architect we need add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
            try {
                if (launchCheckBox.isSelected()) {
                    if (launcher.exists())
                        new ProcessBuilder(launcher.getAbsolutePath()).start();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error call QuantServer Architect", e);
            }
            }
        });
    }

    private boolean qsInstalled() {
        for (final Pack pack : installData.getSelectedPacks()) {
            if (pack.getName().equals(Utils.QUANT_SERVER))
                return true;
        }

        return false;
    }

    private File getArchitectLauncher(InstallData installData) {
        String installPath = installData.getVariable(Utils.INSTALL_PATH_VAR);
        String platform = installData.getVariable(Utils.PLATFORM_VAR);
        if (Utils.LINUX_PLATFORM.equalsIgnoreCase(platform) || Utils.MACOS_PLATFORM.equalsIgnoreCase(platform))
            return new File(installPath, Utils.QSADMIN_LINUX);
        else if (Utils.WINDOWS_PLATFORM.equalsIgnoreCase(platform))
            return new File(installPath, Utils.QSADMIN_WINDOWS);
        else
            return null;
    }

    private File getQuantOfficeLauncher(InstallData installData) {
        String installPath = installData.getVariable(Utils.INSTALL_PATH_VAR);
        String platform = installData.getVariable(Utils.PLATFORM_VAR);
        if (Utils.WINDOWS_PLATFORM.equalsIgnoreCase(platform))
            return new File(installPath, Utils.QO_LAUNCHER_WINDOWS);
        else
            return null;
    }
}
