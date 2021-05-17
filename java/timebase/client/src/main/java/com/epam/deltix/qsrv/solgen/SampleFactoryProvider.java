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
package com.epam.deltix.qsrv.solgen;

import com.epam.deltix.qsrv.solgen.base.SampleFactory;
import com.epam.deltix.qsrv.solgen.base.SampleFactoryProviderBase;
import com.epam.deltix.qsrv.solgen.cpp.CppSampleFactory;
import com.epam.deltix.qsrv.solgen.java.JavaSampleFactory;
import com.epam.deltix.qsrv.solgen.net.NetSampleFactory;
import com.epam.deltix.qsrv.solgen.python.PythonSampleFactory;

public class SampleFactoryProvider implements SampleFactoryProviderBase {

    private static final SampleFactoryProvider INSTANCE = new SampleFactoryProvider();

    public static SampleFactoryProvider getInstance() {
        return INSTANCE;
    }

    @Override
    public SampleFactory createForJava() {
        return new JavaSampleFactory();
    }

    @Override
    public SampleFactory createForNET() {
        return new NetSampleFactory();
    }

    @Override
    public SampleFactory createForPython() {
        return new PythonSampleFactory();
    }

    @Override
    public SampleFactory createForCpp() {
        return new CppSampleFactory();
    }

    @Override
    public SampleFactory createForGo() {
        return null;
    }
}
