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

import java.util.*;

/**
 *  Options for loading data into a stream.
 */
public class LoadingOptions extends CommonOptions {

    /**
     * Globally sort all messages before loading.
     * <p>
     * This option is mutually exclusive with the others sorting options.
     * Currently there is no implementation for global sorting.
     * </p>
     */
    public boolean      globalSorting = false;

    /**
     *  Data Partition. Contains unique number of instruments or Time Ranges.
     */
    public String       space = null;

    /**
     * Whether apply rules at loader level or file level (by symbol)
     * <p>
     * Default value: file level (by symbol).<br/>
     * Sometimes pricing data goes symbol by symbol. This violates time ordering required by tickdb.
     * Use distribution factor <code>MAX</code> and apply sorting rule at file level (<code>loaderLevel = false</code>)
     * in this case.
     * </p>
     */
    //public boolean      loaderLevel = false;


    /**
     *  QQL Expression which allows filtering messages to be send.
     */
    public String       filterExpression;

    public WriteMode    writeMode = WriteMode.REWRITE;

    // TODO: Rename
    public boolean      allowExperimentalTransport = false;

    private final HashMap<Class <? extends LoadingError>, ErrorAction>
                        mapping = new HashMap<Class <? extends LoadingError>, ErrorAction>();

    public LoadingOptions () {
    }

    public LoadingOptions (boolean raw) {        
        this.raw = raw;
    }

    public boolean isGlobalSorting () {
        return globalSorting;
    }

    public void setGlobalSorting (boolean globalSorting) {
        this.globalSorting = globalSorting;
    }

    public LoadingOptions(WriteMode writeMode) {
        this.writeMode = writeMode;
    }

//    public boolean isLoaderLevel () {
//        return loaderLevel;
//    }
//
//    public void setLoaderLevel (boolean loaderLevel) {
//        this.loaderLevel = loaderLevel;
//    }

    public boolean isRaw () {
        return raw;
    }

    public void setRaw (boolean raw) {
        this.raw = raw;
    }

    public void addErrorAction(Class <? extends LoadingError> clazz, ErrorAction action) {
        mapping.put(clazz, action);
    }

    @SuppressWarnings ("unchecked")
    public ErrorAction getErrorAction(Class clazz){
        ErrorAction action = mapping.get(clazz);
        if (action == null && clazz.getSuperclass() != Object.class) {
            action = getErrorAction(clazz.getSuperclass());
            if (action != null)
                mapping.put(clazz, action);
        }

        return action != null ? action : ErrorAction.NotifyAndContinue; 
    }

    public Class[] getMappedClasses() {
        Collection<Class<? extends LoadingError>> classSet = mapping.keySet();
        return classSet.toArray(new Class[classSet.size()]);
    }

    public enum ErrorAction {
        NotifyAndContinue,
        NotifyAndAbort,
        Continue                
    }

    public enum WriteMode {
        /**
         Adds only new data into a stream without truncation
         */
        APPEND,

        /**
         Adds data into a stream and removes previous data by truncating using first new message time
         */
        REWRITE,

        /**
          Stream truncated every time when loader writes messages earlier that last
         */
        TRUNCATE,

        /**
          New data inserts into a stream without truncation
         */
        INSERT
    }

    public void copy(LoadingOptions template) {
        super.copy(template);

        this.raw = template.raw;
        this.globalSorting = template.globalSorting;
        //this.loaderLevel = template.loaderLevel;
        this.writeMode = template.writeMode;
        this.mapping.putAll(template.mapping);
        this.filterExpression = template.filterExpression;
    }
}