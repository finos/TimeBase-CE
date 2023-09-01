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
package com.epam.deltix.izpack.panels.license;

import com.izforge.izpack.api.data.Panel;
import com.izforge.izpack.api.factory.ObjectFactory;
import com.izforge.izpack.api.handler.Prompt;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.api.rules.RulesEngine;
import com.izforge.izpack.installer.data.GUIInstallData;
import com.izforge.izpack.installer.gui.InstallerFrame;
import com.izforge.izpack.panels.userinput.UserInputPanel;
import com.izforge.izpack.util.PlatformModelMatcher;
import com.epam.deltix.izpack.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class CheckLicensePanel extends UserInputPanel {

    public final static Logger LOGGER = Logger.getLogger(CheckLicensePanel.class.getName());

    private final CheckLicenseHelper checkLicenseHelper;

    public CheckLicensePanel(Panel panel, InstallerFrame parent, GUIInstallData installData, Resources resources,
                             RulesEngine rules, ObjectFactory factory, final PlatformModelMatcher matcher, Prompt prompt)
    {
        super(panel, parent, installData, resources, rules, factory, matcher, prompt);

        checkLicenseHelper = new CheckLicenseHelper(installData);
    }

    public boolean isValidated() {
        if (!super.isValidated())
            return false;

        try {
            checkLicenseHelper.setProxyProperties();

            List<String> moduleNames = new ArrayList<>();
            List<String> licenseErrors = new ArrayList<>();
            checkLicenseHelper.checkLicenseForModules(moduleNames, licenseErrors);

            if (licenseErrors.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < moduleNames.size(); ++i)
                    sb.append(moduleNames.get(i)).append(": ").append(licenseErrors.get(i)).append("\n");

                return emitWarning(
                        getString(Utils.LICENSE_ERROR_STR),
                        getString(Utils.ERROR_TEXT_1_STR) + "\n" +
                                sb.toString() +
                                getString(Utils.PROCEED_STR));
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Error checking license.", t);
            return emitWarning(
                    getString(Utils.LICENSE_ERROR_STR),
                    "License Check Error: " + t.getMessage() + "\n" +
                            getString(Utils.PROCEED_STR));
        }

        return true;
    }
}