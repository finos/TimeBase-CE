package deltix.buildtools.jarsearch;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

/**
 * Note: this test assumes that you have already built all java modules and have populated "lib" folder.
 *
 * @author Alexei Osipov
 */
public class JarFinderTest {

    private final JarFinder jarFinder;

    public JarFinderTest() {
        String deltixHome = System.getenv("DELTIX_HOME");
        if (deltixHome == null) {
            throw new IllegalArgumentException("DELTIX_HOME is not set");
        }
        File lib = Paths.get(deltixHome, "lib").toFile();
        if (!lib.exists()) {
            Assert.fail();
        }
        this.jarFinder = JarFinder.createFromJarDirectory(lib);
    }

    @Test
    public void test1() {
        String value = jarFinder.getJarNameByClassName("com.epam.deltix.qsrv.comm.cat.TomcatCmd");
        Assert.assertTrue(value.startsWith("deltix-quantserver-all-"));
        Assert.assertTrue(value.endsWith(".jar"));
    }
}