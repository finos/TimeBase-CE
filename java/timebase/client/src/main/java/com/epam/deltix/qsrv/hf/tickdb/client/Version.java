package com.epam.deltix.qsrv.hf.tickdb.client;

/**
 * Timebase Client Version.
 * @author Alex Karpovich on 4/4/2018.
 */
public class Version {

    // valid only when package exists only in single jar
    private static final String version = Version.class.getPackage().getImplementationVersion();

//    static {
//        try {
//            URL url = cl.findResource("META-INF/MANIFEST.MF");
//            Manifest manifest = new Manifest(url.openStream());
//            Attributes mainAttributes = manifest.getMainAttributes();
//            version = mainAttributes.getValue("Implementation-Version");
//        } catch (IOException E) {
//            // handle
//        }
//   }

    public static String    getVersion() {
        return version;
    }
}
