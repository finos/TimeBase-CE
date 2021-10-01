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

import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.util.collections.generated.IntegerArrayList;

import java.io.IOException;
import java.util.Collection;

/**
 *
 */
public interface SymbolRegistry {
    public static final int     NO_SUCH_SYMBOL = -1;
    public static final int     DUPLICATE_SYMBOL = -2;
    
    /**
     *  Register a new symbol with associated data.
     * 
     * @param symbol
     * @param entityData
     * @return  New id, or id of existing symbol
     */
    public int          registerSymbol (String symbol, String entityData);

    public void         unregisterSymbol (CharSequence symbol);

    public void         renameSymbol(String symbol, String newSymbol, String newEntityData);
    
    public String       getEntityData (int id);
    
//    public void         setEntityData (int id, String data);
//
//    public int          getIdOrRegisterSymbol (CharSequence symbol);
//
    public int          symbolToId (CharSequence symbol);
    
    public void         symbolsToIds (
        CharSequence []     symbols,
        int                 symbolsOffset,
        int                 numSymbols,
        int []              ids,
        int                 idsOffset        
    );
    
    public String       idToSymbol (int id);
    
    public void         idsToSymbols (
        int []              ids,
        int                 idsOffset,
        int                 numIds,
        String []           symbols,
        int                 symbolsOffset
    );
    
    public void         listSymbols (Collection <String> symbols, Collection<String> data);
    
    public void         listIds (IntegerArrayList ids);

    public void         storeIfDirty(AbstractPath folder) throws IOException;
}