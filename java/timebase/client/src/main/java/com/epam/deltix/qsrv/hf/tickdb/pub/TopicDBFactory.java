package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Method;

/**
 *  Public methods for adding support of {@link TopicDB} to {@link TickDB}.
 */
@ParametersAreNonnullByDefault
public class TopicDBFactory {

    private static final String factoryClass = "com.epam.deltix.qsrv.hf.tickdb.impl.topic.TopicSupportWrapper";
    private static final String factoryMethod = "wrapStandalone";


    /**
     * Wraps provided DB instance to support {@link deltix.qsrv.hf.tickdb.pub.topic.TopicDB}.
     * @param delegate backing instance for all functionality except topics
     * @return wrapped instance that supports topics
     */
    public static DXTickDB create(DXTickDB delegate) {
        try {
            Class<?> impl = TopicDBFactory.class.getClassLoader().loadClass(factoryClass); // using runtime class loader
            Method factoryMethod = impl.getDeclaredMethod(TopicDBFactory.factoryMethod, DXTickDB.class);
            return (DXTickDB) factoryMethod.invoke(null, delegate);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
