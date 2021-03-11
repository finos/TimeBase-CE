package com.epam.deltix.izpack.panels.target;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.handler.Prompt;
import com.izforge.izpack.installer.console.ConsolePanel;
import com.izforge.izpack.installer.panel.PanelView;
import com.izforge.izpack.panels.target.TargetConsolePanel;
import com.izforge.izpack.panels.target.TargetPanelHelper;
import com.izforge.izpack.util.Console;
import com.epam.deltix.izpack.Utils;

import java.io.File;

/**
 *
 */
public class CheckedTargetConsolePanel extends TargetConsolePanel {

    public CheckedTargetConsolePanel(PanelView<ConsolePanel> panel, InstallData installData, Prompt prompt) {
        super(panel, installData, prompt);
    }

    @Override
    public boolean run(InstallData installData, Console console) {
        String path = Utils.PREFS.get(Utils.INST_FOLDER_VAR, TargetPanelHelper.getPath(installData));

        if (Utils.IS_X64 &&
                "x86".equals(System.getProperty("os.arch")) &&
                new File(Utils.getProgramFiles(), "Deltix").getPath().equals(path)) {
            console.println(installData.getMessages().get(Utils.PROGRAM_FILES_X86_STR));
        }

        installData.setInstallPath(path);

        return super.run(installData, console);
    }
}
