package deltix.buildtools.jarsearch;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Tells which jar file in the folder contains the specified class.
 *
 * @author Alexei Osipov
 */
public class JarFinder {
    private final URLClassLoader loader;

    private JarFinder(List<URL> urls) {
        this(urls.toArray(new URL[0]));
    }

    private JarFinder(URL[] urls) {
        this.loader = new URLClassLoader(urls);
    }

    public static JarFinder createFromJarDirectory(File jarDirectory) {
        if (!jarDirectory.exists()) {
            throw new RuntimeException("Jar directory does not exist");
        }
        File[] files = jarDirectory.listFiles();
        if (files == null) {
            throw new RuntimeException("Jar directory is empty");
        }
        List<URL> urls = new ArrayList<>(files.length);

        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".jar")) {
                try {
                    urls.add(file.toURI().toURL());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return new JarFinder(urls);
    }

    /**
     * Returns name of JAR files that contains specified class file.
     * Actual content of class file is not loaded and not validated to ve a valid .class file.
     *
     * @param className Java class name
     * @return JAR file name or {@code null}
     */
    public String getJarNameByClassName(String className) {
        String path = className.replace('.', '/').concat(".class");
        URL resource = loader.findResource(path);
        if (resource == null) {
            // Class not found
            return null;
        }
        String fullClassLocation = resource.getPath();
        int tailStart = fullClassLocation.lastIndexOf("!");
        if (tailStart == -1) {
            throw new IllegalStateException();
        }
        String jarPath = fullClassLocation.substring(0, tailStart);
        return new File(jarPath).getName();
    }
}
