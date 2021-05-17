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
package com.epam.deltix.izpack.panels.properties;

import com.izforge.izpack.api.adaptator.IXMLElement;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.data.Overrides;
import com.izforge.izpack.installer.automation.PanelAutomation;

/**
 *
 */
public class InitPanelAutomation implements PanelAutomation {
    public InitPanelAutomation() {
    }

    @Override
    public void createInstallationRecord(InstallData installData, IXMLElement panelRoot) {
    }

    @Override
    public void runAutomated(InstallData installData, IXMLElement panelRoot) {
        PropertiesHelper propertiesHelper = new PropertiesHelper(installData);
        propertiesHelper.loadProperties();
        String errorMessage = propertiesHelper.checkPlatform();
        if (errorMessage != null)
            throw new RuntimeException(errorMessage);
    }

    @Override
    public void processOptions(InstallData installData, Overrides overrides) {

    }

}
