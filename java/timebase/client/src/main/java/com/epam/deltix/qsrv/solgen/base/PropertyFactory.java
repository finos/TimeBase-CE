package com.epam.deltix.qsrv.solgen.base;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class PropertyFactory {

    public static Property create(String name, String doc, boolean required, Predicate<String> valueValidator) {
        return new Property(name, doc, required, valueValidator, null);
    }

    public static Property create(String name, String doc, boolean required, Predicate<String> valueValidator, String defaultValue) {
        return new Property(name, doc, required, valueValidator, defaultValue);
    }

    public static Property create(String name, String doc, boolean required, BiPredicate<String, Properties> valueValidator,
                                  String defaultValue, List<Property> dependsOn) {
        return new Property(name, doc, required, valueValidator, defaultValue, dependsOn);
    }

    public static Property create(String name, String doc, boolean required, BiPredicate<String, Properties> valueValidator,
                                  String defaultValue, Property... dependsOn) {
        return new Property(name, doc, required, valueValidator, defaultValue, Arrays.asList(dependsOn));
    }

}
