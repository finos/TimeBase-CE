package com.epam.deltix.util.time;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

final class Library {
    static final String DISABLE_PROPERTY = "deltix.clock.disable";
    static final String OS = os();
    static final String ARCH = arch();
    static final String EXT;
    static final boolean LOADED;

    private Library() {
    }

    private static boolean load() {
        try {
            boolean enabled = !Boolean.getBoolean("deltix.clock.disable");
            boolean osSupported = OS.equals("linux") || OS.equals("windows") || OS.equals("macOsX");
            boolean archSupported = ARCH.equals("x86") || ARCH.equals("x64") || ARCH.equals("arm64");
            if (enabled && osSupported && archSupported) {
                String file = "native/" + OS + "/" + ARCH + "/libclock" + EXT;
                URL url = Thread.currentThread().getContextClassLoader().getResource(file);
                if (url == null) {
                    throw new IllegalArgumentException("No file on classpath: " + file);
                }

                Path temp = Files.createTempFile("libclock", EXT).toAbsolutePath();

                try {
                    copy(url, temp);
                    System.load(temp.toString());
                } finally {
                    delete(temp);
                }

                return true;
            }
        } catch (UnsatisfiedLinkError var11) {
            System.err.println("Can't link deltix clock library: " + var11.getMessage());
        } catch (Throwable var12) {
            System.err.println("Can't load deltix clock library: " + var12.getMessage());
            var12.printStackTrace(System.err);
        }

        return false;
    }

    private static void delete(Path temp) {
        try {
            if (Files.exists(temp, new LinkOption[0])) {
                if (OS.equals("windows")) {
                    temp.toFile().deleteOnExit();
                } else {
                    Files.delete(temp);
                }
            }
        } catch (Throwable var2) {
            System.err.println("Can't delete temporary deltix clock library: " + var2.getMessage());
            var2.printStackTrace(System.err);
        }

    }

    private static String arch() {
        String arch = System.getProperty("os.arch");

        switch (arch) {
            case "amd64":
            case "x86_64":
                return "x64";
            case "x86":
            case "i386":
                return "x86";
            case "arm64":
            case "aarch64":
            case "aarch64e":
                return "arm64";
            default:
                return arch;
        }
    }

    private static String os() {
        String name = System.getProperty("os.name");
        if (name.contains("Linux")) {
            return "linux";
        } else if (name.contains("Mac")) {
            return "macOsX";
        } else if (name.contains("Windows")) {
            return "windows";
        } else {
            return !name.contains("Solaris") && !name.contains("SunOS") ? name : "solaris";
        }
    }

    private static String ext(String os) {
        switch (os) {
            case "linux":
                return ".so";
            case "macOsX":
                return ".dylib";
            case "windows":
                return ".dll";
            default:
                return null;
        }
    }

    private static void copy(URL from, Path to) throws IOException {
        InputStream input = from.openStream();

        try {
            OutputStream output = new FileOutputStream(to.toFile());

            try {
                byte[] data = new byte[Math.max(input.available(), 8192)];

                while(true) {
                    int length = input.read(data);
                    if (length < 0) {
                        break;
                    }

                    output.write(data, 0, length);
                }
            } catch (Throwable var8) {
                try {
                    output.close();
                } catch (Throwable var7) {
                    var8.addSuppressed(var7);
                }

                throw var8;
            }

            output.close();
        } catch (Throwable var9) {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable var6) {
                    var9.addSuppressed(var6);
                }
            }

            throw var9;
        }

        if (input != null) {
            input.close();
        }

    }

    static {
        EXT = ext(OS);
        LOADED = load();
    }
}
