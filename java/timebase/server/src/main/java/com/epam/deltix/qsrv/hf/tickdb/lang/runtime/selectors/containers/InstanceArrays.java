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
package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.containers;

import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.Instance;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.InstancePool;
import com.epam.deltix.util.memory.MemoryDataInput;

public class InstanceArrays extends InstanceArray {

    public InstanceArrays(InstancePool pool) {
        super(pool);
    }

    public InstanceArrays(InstancePool pool, RecordClassDescriptor[] descriptors, Class<?>[] classes) {
        super(pool, descriptors, classes);
    }

    @Override
    public void addFromInput(MemoryDataInput mdi) {
        setInstance();
        setChanged();
        MessageSizeCodec.read(mdi); // message size
        int length = MessageSizeCodec.read(mdi);
        for (int i = 0; i < length; ++i) {
            if (mdi.hasAvail()) {
                Instance element = pool.borrow();
                element.decode(mdi);
                array.add(element);
            } else {
                array.add(null);
            }
        }
    }

}