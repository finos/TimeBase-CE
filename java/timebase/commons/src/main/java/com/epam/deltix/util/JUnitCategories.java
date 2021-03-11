package com.epam.deltix.util;

/**
 * This class is test-related and should not be actually placed in "main" code.
 * However we need to access this class from all test modules and the simplest way to achieve this is to put it into
 * a common module (like this one). Alternative is to have a dedicated module for test utility classes.
 * TODO: Move this class to a test utility module if such module appears.
 */
public final class JUnitCategories {
    public interface Utils extends All {}

    public interface UHFFramework extends All {}

    public interface TickDB extends All {}

    public interface TickDBFast extends TickDB {}

    public interface TickDBQQL extends TickDB {}

    public interface TickDBSlow extends TickDBStress {}

    public interface TickDBStress {}

    public interface TickDBCodecs extends TickDB {}

    public interface RAMDisk extends All {}

    public interface UHFUtils extends All {}

    public interface All {}

    /**
     * Test that depend on external data.
     */
    public interface External {}
}


