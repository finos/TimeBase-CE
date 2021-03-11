package com.epam.deltix.qsrv;

import com.epam.deltix.util.io.GUID;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.lang.Util;

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Sets default DELTIX_HOME and QuantServer home if needed
 * @author Andy
 *         Date: Mar 4, 2010 3:00:44 PM
 */
public class SetHome {
    private static final String UNIQUE_QSHOME = "unique.qsrv.home";

    public static void check() {
        //checkDeltixHome();
        checkUniqueQSHome();
    }

    private static void checkUniqueQSHome() {
        if (Boolean.getBoolean(UNIQUE_QSHOME)) {
            try {
                Path path = Paths.get(Home.get(), "temp", "parallel", GUID.getSystemUniqueString().toString(), "testhome");
                if (Files.notExists(path))
                    Files.createDirectories(path);

                QSHome.set(path.toAbsolutePath().normalize().toString());
            } catch (IOException e) {
                throw new com.epam.deltix.util.io.UncheckedIOException(e);
            }
        }
        SetHome.checkQSHome();
    }

    private static void checkQSHome() {
        if (System.getProperty(QSHome.QSRV_HOME_SYS_PROP) == null) {
            File qshome = Home.getFile("temp\\testhome");
            if (!qshome.exists())
                qshome.mkdirs();

            QSHome.set(qshome.getAbsolutePath());
        }
    }

//    private static void checkDeltixHome() {
//        if (System.getProperty(Home.DELTIX_HOME_SYS_PROP) == null) {
//            String location = Util.getClassLocation(Util.class).getFile();
//            String file = "deltix/util/lang/Util.class";
//            if (location.endsWith(file)) {
//                String home = location.substring(0, location.indexOf(file));
//                Home.set(home);
//            }
//        }
//    }
}
