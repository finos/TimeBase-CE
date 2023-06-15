/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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