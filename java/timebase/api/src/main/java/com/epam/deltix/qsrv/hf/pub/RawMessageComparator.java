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
package com.epam.deltix.qsrv.hf.pub;

import com.epam.deltix.util.lang.MathUtil;
import com.epam.deltix.util.lang.Util;
import java.util.Comparator;

public class RawMessageComparator implements Comparator <RawMessage> {
    public static final RawMessageComparator     ALL_COMPARING_INSTANCE =
        new RawMessageComparator (true, true);
    
    private boolean         compareTime;
    private boolean         compareEntity;
    private boolean         compareType;

    public RawMessageComparator (boolean compareTime, boolean compareEntity) {
        this(compareTime, compareEntity, true);
    }

    public RawMessageComparator(boolean compareTime, boolean compareEntity, boolean compareType) {
        this.compareTime = compareTime;
        this.compareEntity = compareEntity;
        this.compareType = compareType;
    }

    public int      compare (RawMessage o1, RawMessage o2) {
        long         dif;
        
        if (compareTime) {
            dif = MathUtil.sign (o1.getTimeStampMs() - o2.getTimeStampMs());
            
            if (dif != 0)
                return (int)(dif);
            
            dif = o1.getNanoTime() - o2.getNanoTime();
            
            if (dif != 0)
                return (int)(dif);
        }
        
        if (compareEntity) {
            dif = Util.compare (o1.getSymbol(), o2.getSymbol(), false);
            
            if (dif != 0)
                return (int)(dif);
        }

        if (compareType) {
            dif = o1.type.compareTo (o2.type);

            if (dif != 0)
                return (int)(dif);
        }

        return (Util.arraycomp (o1.data, o1.offset, o1.length, o2.data, o2.offset, o2.length));
    }  
}