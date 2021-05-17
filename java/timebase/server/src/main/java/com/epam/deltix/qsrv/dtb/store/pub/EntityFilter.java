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
package com.epam.deltix.qsrv.dtb.store.pub;

/**
 *
 */
public interface EntityFilter {
    public static final EntityFilter    ALL = 
        new EntityFilter () {
            @Override
            public boolean      acceptAll () {
                return (true);
            }

            @Override
            public boolean      accept (int entity) {
                return (true);
            }

            @Override
            public boolean restrictAll() {
                return false;
            }

            @Override
            public long         acceptFrom(int entity) {
                return Long.MIN_VALUE;
            }
        };

    public static final EntityFilter    NONE =
            new EntityFilter () {
                @Override
                public boolean      acceptAll () {
                    return (false);
                }

                @Override
                public boolean      accept (int entity) {
                    return (false);
                }

                @Override
                public boolean      restrictAll() {
                    return true;
                }

                @Override
                public long         acceptFrom(int entity) {
                    return Long.MAX_VALUE;
                }
        };
    
    public boolean          acceptAll ();
    
    public boolean          accept (int entity);

    /*
        Returns time from which given entity is accepted.
     */
    public long             acceptFrom (int entity);

    public boolean          restrictAll ();
}
