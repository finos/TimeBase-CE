/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.impl;

/**
 *
 */
public class TickStreamProperties {
    // !!! In case of any changes here, please change TDBProtocol.VERSION

    public static final int KEY =               1;
    public static final int NAME =              2;
    public static final int DESCRIPTION =       3;
    public static final int PERIODICITY =       4;
    public static final int SCHEMA =            6;
    public static final int TIME_RANGE =        7;
    public static final int ENTITIES =          8;
    public static final int HIGH_AVAILABILITY = 9;
    public static final int UNIQUE =            10;
    public static final int BUFFER_OPTIONS =    11;

    public static final int VERSIONING =        12;
    public static final int DATA_VERSION =      13;
    public static final int REPLICA_VERSION =   14;

    public static final int BG_PROCESS =        15;
    public static final int WRITER_CREATED =    16;
    public static final int WRITER_CLOSED =     17;

    public static final int SCOPE =             20;
    public static final int DF =                21;

    public static final int OWNER =             22;

    public static final int LOCATION =          23;


    // number of properties
    public static final int COUNT = 24;

}