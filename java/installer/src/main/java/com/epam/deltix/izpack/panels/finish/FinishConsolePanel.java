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
