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
package com.epam.deltix.qsrv.hf.pub.codec;

import com.epam.deltix.qsrv.hf.pub.md.*;

/**
 *
 */
public class NonStaticFieldLayout
    extends FieldLayout <NonStaticDataField> 
    implements NonStaticFieldInfo
{
    int                                 fixedOffset = RecordLayout.VARIABLE_SIZE;
    int                                 ordinal = -1;

    public int                          ownBaseIndex = -1;
    public NonStaticFieldLayout         relativeTo = null;

    public NonStaticFieldLayout (NonStaticDataField field) {
        super (null, field);
    }

    public NonStaticFieldLayout (RecordClassDescriptor owner, NonStaticDataField field) {
        super (owner, field);
    }

    public NonStaticFieldLayout (NonStaticFieldLayout parent, NonStaticDataField field) {
        super (parent.getOwner(), field);
        this.fieldType = parent.getGenericClass();
    }

    public int                          getOffset () {
        if (fixedOffset < 0)
            throw new IllegalStateException (this + " is not randomly accessible");
        
        return (fixedOffset);
    }

    public int                          getOrdinal () {
        return (ordinal);
    }

    public boolean                      isPrimaryKey () {
        return (getField ().isPk ());
    }
    
    // getters for Velocity template

    public int getOwnBaseIndex() {
        return ownBaseIndex;
    }

    public NonStaticFieldLayout getRelativeTo() {
        return relativeTo;
    }
}
