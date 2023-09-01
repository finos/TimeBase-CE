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
package com.epam.deltix.qsrv.hf.tickdb.pub;

/**
 *  <p>The top-level interface to the Deltix Tic Database engine. Instances of
 *  this interface are created by static methods of {@link TickDBFactory}.</p>
 * 
 *  <p>At the physical level, a database consists of a number of folders 
 *  (directories) on the hard disk. While the database is closed, the files 
 *  can be freely moved around in order to manage disk space and/or take advantage
 *  of parallel access to several hard disk devices. It is even possible to 
 *  increase or reduce the number of folders, as long as all necessary folders
 *  are supplied when a database instance is constructed. While a database is
 *  open, external processes must obviously not interfere with the files.</p>
 */
public interface WritableTickDB extends TickDB {
    /**
     *  Looks up an existing stream by key.
     *
     *  @param key      Identifies the stream.
     *  @return         A stream object, or <code>null</code> if the key was not found.
     *  @throws         java.security.AccessControlException when user is not authorized to READ given stream
     */
    WritableTickStream                       getStream (
        String                                  key
    );
    
    /**
     *  Enumerates existing streams.
     * 
     *  @return         An array of existing stream objects.
     */
    WritableTickStream []                    listStreams ();
}