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
package com.epam.deltix.izpack.panels.finish;

import com.izforge.izpack.api.factory.ObjectFactory;
import com.izforge.izpack.api.handler.Prompt;
import com.izforge.izpack.installer.console.ConsoleInstaller;
import com.izforge.izpack.installer.console.ConsolePanel;
import com.izforge.izpack.installer.data.UninstallData;
import com.izforge.izpack.installer.panel.PanelView;
import com.izforge.izpack.util.PlatformModelMatcher;

/**
 *
 */
public class FinishConsolePanel extends com.izforge.izpack.panels.finish.FinishConsolePanel {
    public FinishConsolePanel(ObjectFactory factory,
                              ConsoleInstaller parent,
                              PlatformModelMatcher matcher,
                              UninstallData uninstallData,
                              Prompt prompt,
                              PanelView<ConsolePanel> panel)
    {
        super(factory, parent, matcher, uninstallData, prompt, panel);
    }
}