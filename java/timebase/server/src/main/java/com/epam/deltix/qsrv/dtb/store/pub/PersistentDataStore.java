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

import com.epam.deltix.qsrv.dtb.fs.pub.*;

import javax.annotation.Nullable;

/**
 *
 */
public interface PersistentDataStore { 
    public boolean          isStarted ();
    
    public boolean          isReadOnly ();
    
    public void             setReadOnly (boolean readOnly);

    public void             startShutdown ();
    
    public boolean          waitUntilDataStored (int timeout);
    
    public void             setNumWriterThreads (int n);
    
    public void             start ();
    
    public void             shutdown ();
    
    public boolean          waitForShutdown (int timeout);

    public TSRoot           createRoot (@Nullable String space, AbstractFileSystem fs, String path);

    public TSRoot           createRoot(@Nullable String space, AbstractPath path);
    
    /**
     *  Factory method for creating a reusable object for writing 
     *  messages.
     * 
     *  @return         An instance of the DataAccessor interface.
     */
    public DataWriter       createWriter ();
    
    /**
     *  Factory method for creating a reusable object for reading 
     *  messages.
     * 
     *  @return         An instance of the DataAccessor interface.
     */
    public DataReader       createReader (boolean live);

    void setEmergencyShutdownControl(EmergencyShutdownControl shutdownControl);
}