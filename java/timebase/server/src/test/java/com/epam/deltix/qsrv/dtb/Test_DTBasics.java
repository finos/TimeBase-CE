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
package com.epam.deltix.qsrv.dtb;

import com.epam.deltix.qsrv.dtb.store.pub.*;
import com.epam.deltix.qsrv.dtb.test.*;
import java.io.*;
import org.junit.*;

/**
 *
 */
public class Test_DTBasics {
    private TestConfig      CONFIG = new TestConfig ();
    private DTBCreator      CREATOR = new DTBCreator (CONFIG);
    
    @Test
    public void         createAllAtOnce () throws IOException {
        TSRoot      root = CREATOR.format ();        
        CREATOR.insertMessages (root, 0, CONFIG.numMessages, 1);        
        CREATOR.close (root);
    
        CREATOR.verifyFullDB ();        
    }
    
    @Test
    public void         randomSelection () throws IOException {
        CREATOR.testRandomMessageSelection (CONFIG.numMessages);
    }
    
    @Test
    public void         createInAFewIterations () throws IOException {
        int         n1 = CONFIG.numMessages / 3;
        int         n2 = n1 * 2;
        
        TSRoot      root = CREATOR.format ();        
        CREATOR.insertMessages (root, 0, n1, 1);        
        CREATOR.close (root);
        
        root = CREATOR.open (false);
        CREATOR.insertMessages (root, n1, n2, 1);
        CREATOR.close (root);
        
        root = CREATOR.open (false);
        CREATOR.insertMessages (root, n2, CONFIG.numMessages, 1);
        CREATOR.close (root);
        
        CREATOR.verifyFullDB ();       
    }
}
