package com.epam.deltix.qsrv.solgen.base;

import java.util.List;
import java.util.Properties;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class Property {

    protected final String name;

    protected final String doc;

    protected final boolean required;

    protected final List<Property> dependsOn;

    protected final BiPredicate<String, Properties> valueValidator;

    protected final String defaultValue;

    protected Property(String name, String doc, boolean required, Predicate<String> valueValidator, String defaultValue) {
        this.name = name;
        this.doc = doc;
        this.required = required;
        this.valueValidator = (s, p) -> valueValidator.test(s);
        this.defaultValue = defaultValue;
        this.dependsOn = null;
    }

    protected Property(String name, String doc, boolean required, BiPredicate<String, Properties> valueValidator,
                       String defaultValue, List<Property> dependsOn) {
        this.name = name;
        this.doc = doc;
        this.required = required;
        this.valueValidator = valueValidator;
        this.defaultValue = defaultValue;
        this.dependsOn = dependsOn;
    }

    public String getName() {
        return name;
    }

    public String getDoc() {
        return doc;
    }

    public boolean isRequired() {
        return required;
    }

    public BiPredicate<String, Properties> getValueValidator() {
        return valueValidator;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public boolean isValid(String value, Properties properties) {
        return valueValidator.test(value, properties);
    }

    public List<Property> getDependencies() {
        return dependsOn;
    }

    public boolean isDependent() {
        return dependsOn != null && !dependsOn.isEmpty();
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Property property = (Property) o;
        return name.equals(property.name);
    }
}
