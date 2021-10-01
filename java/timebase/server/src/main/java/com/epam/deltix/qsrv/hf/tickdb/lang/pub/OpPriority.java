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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *
 */
public abstract class OpPriority {
    public static final int                 OPEN = 0;

    public static final int                 UNION = 1;
    public static final int                 QUERY = 2;
    public static final int                 COMMA = 3;
    public static final int                 LOGICAL_OR = 4;
    public static final int                 LOGICAL_AND = 5;
    public static final int                 RELATIONAL = 6;
    public static final int                 ADDITION = 7;
    public static final int                 MULTIPLICATION = 8;
    public static final int                 PREFIX = 9;
    public static final int                 POSTFIX = 10;
}