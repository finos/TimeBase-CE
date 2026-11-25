/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.dtb.store.impl;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.qsrv.dtb.store.codecs.TSNames;
import com.epam.deltix.qsrv.dtb.store.pub.SymbolRegistry;
import com.epam.deltix.qsrv.dtb.store.pub.TimeRangeNs;
import com.epam.deltix.util.collections.CharSequenceToIntegerMap;
import com.epam.deltix.util.collections.generated.IntegerArrayList;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 *
 */
public class SymbolRegistryImpl implements SymbolRegistry {
    private static final Log LOGGER = PDSImpl.LOGGER;
    private final int                           VERSION = 501;

    private boolean                             isDirty;
    private CharSequenceToIntegerMap            symbolToIdMap = null;
    private ObjectArrayList <SymbolEntry>       symbols = null;


    private static class SymbolEntry {

        public SymbolEntry(String name, String data, int index, boolean active) {
            // TODO: Consider using .intern() on name and data.
            // Note: We must be sure that ne not just store interned value in fields
            // but share interned value with other value holders. Example: we have a string and use that string
            // both to create a SymbolEntry and also store it as a collection key somewhere.
            // Interning this string here in constructor will make
            // SymbolEntry and collection key point to different objects.
            // And that's the opposite of what we want to achieve.
            // So we can't just simply add .intern() here in constructor.
            // In general, we can intern if we sure that not-interned value will not be used anywhere.
            this.name = name;
            this.data = data;
            this.index = index;
            this.active = active;
        }

        public SymbolEntry(String name, String data, int index) {
            this(name, data, index, true);
        }

        String          name;
        String          data;
        final int       index;
        boolean         active;

        // cached time range
        TimeRangeNs       range;
    }

    public SymbolRegistryImpl () {
    }
        
    private int                         oneSymbolToId (CharSequence symbol) {
        if (symbol == null)
            return (NO_SUCH_SYMBOL);
        
        return (symbolToIdMap.get (symbol, NO_SUCH_SYMBOL));
    }
    
    @Override
    public synchronized int             symbolToId (CharSequence symbol) {
        return (oneSymbolToId (symbol));
    }

    @Override
    public synchronized void            symbolsToIds (
        CharSequence []                     symbols, 
        int                                 symbolsOffset, 
        int                                 numSymbols, 
        int []                              ids, 
        int                                 idsOffset
    )
    {
        for (int ii = 0; ii < numSymbols; ii++)
            ids [idsOffset + ii] = oneSymbolToId (symbols [symbolsOffset + ii]);
    }

    private String                      oneIdToSymbol (int id) {
        if (id < 0 || id >= symbols.size ())
            return (null);
        
        return (symbols.getObjectNoRangeCheck (id).name);
    }
    
    private String                      oneIdToData (int id) {
        if (id < 0 || id >= symbols.size ())
            return (null);
        
        return (symbols.getObjectNoRangeCheck (id).data);
    }
    
    @Override
    public synchronized String          idToSymbol (int id) {
        return (oneIdToSymbol (id));
    }

    @Override
    public synchronized void            idsToSymbols (
        int []                              ids, 
        int                                 idsOffset, 
        int                                 numIds, 
        String []                           symbols, 
        int                                 symbolsOffset
    )
    {
        for (int ii = 0; ii < numIds; ii++)
            symbols [symbolsOffset + ii] = oneIdToSymbol (ids [idsOffset + ii]);
    }

    private int                         registerSymbolInternal (
        String                              symbol, 
        String                              entityData
    )
    {
        //assert entityData != null;

        // assume we do not have nulls inside, searching for nulls affects performance a lot
//        int             id = symbols.indexOf (null);
//        if (id < 0)
//            id = symbols.size ();

        int id = symbols.size();

        boolean         check = symbolToIdMap.put (symbol, id);
        
        if (!check) 
            throw new IllegalArgumentException ("Duplicate symbol");
        
        symbols.add (id, new SymbolEntry(symbol, entityData, id));
        
        isDirty = true;

        return (id);
    }
    
    @Override
    public synchronized int             registerSymbol (String symbol, String entityData) {
        int index = symbolToIdMap.get(symbol, NO_SUCH_SYMBOL);

        if (index != -1) {
            SymbolEntry entry = symbols.get(index);

            if (!entry.active) {
                entry.active = true;
                entry.data = entityData;
                isDirty = true;
            }
            return index;
        }

        return (registerSymbolInternal (symbol, entityData));
    }

    @Override
    public synchronized void            unregisterSymbol (CharSequence symbol) {
        int index = symbolToIdMap.get(symbol, -1);

        if (index != -1) {
            symbols.get(index).active = false;
            isDirty = true;
        }
    }

    /**
     *  Renames existing symbol. When newSymbol equals to symbol, change entityData only.
     *  @param symbol symbol to rename
     *  @param newSymbol new name, can be equal to the symbol
     *  @param newEntityData entity data
     *
     *  @throws IllegalArgumentException when symbol is not exists
     **/
    public synchronized void            renameSymbol(String symbol, String newSymbol, String newEntityData) {

        if (!symbol.equals(newSymbol) && symbolToIdMap.get(newSymbol, NO_SUCH_SYMBOL) != NO_SUCH_SYMBOL)
            throw new IllegalArgumentException("Symbol '" + newSymbol + "' already exists!");

        int index = symbolToIdMap.get(symbol, NO_SUCH_SYMBOL);
        if (index == NO_SUCH_SYMBOL)
            throw new IllegalArgumentException("Symbol '" + symbol + "' not exits!");

        SymbolEntry entry = symbols.get(index);

        symbolToIdMap.remove(symbol);
        symbolToIdMap.put(newSymbol, entry.index);

        entry.name = newSymbol;
        entry.data = newEntityData;

        isDirty = true;
    }
    
    public synchronized void          setTimeRange (int id, TimeRangeNs range) {
        if (id >= 0 && id < symbols.size ())
            symbols.get(id).range = range;
    }

    public synchronized TimeRangeNs     getTimeRange (int id) {
        if (id < 0 || id >= symbols.size ())
            return (null);

        return (symbols.getObjectNoRangeCheck (id).range);
    }

    /*
     * Returns global time range.
     * Return null, if at least one symbol range is not cached.
     */
    public synchronized TimeRangeNs     getTimeRange () {
        assert symbols != null;

        int n = symbols.size();

        TimeRangeNs global = new TimeRangeNs();

        for (int id = 0; id < n; id++) {
            TimeRangeNs range = symbols.getObjectNoRangeCheck(id).range;
            if (range == null)
                return null;
            else if (!range.isUndefined())
                global.unionInPlace(range.from, range.to);
        }

        return global;
    }

    synchronized void           clearRange() {
        if (symbols != null) {
            int n = symbols.size();

            for (int id = 0; id < n; id++)
                symbols.getObjectNoRangeCheck(id).range = null;
        }
    }
    
    @Override
    public synchronized String          getEntityData (int id) {
        return (oneIdToData (id));
    }
    
//    @Override
//    public synchronized void            setEntityData (int id, String entityData) {
//        if (id < 0 ||
//                id >= symbols.size () ||
//                symbols.getObjectNoRangeCheck (id) == null)
//            throw new IllegalArgumentException ("Id not found: " + id);
//
//        if (entityData == null)
//            entityData = "";
//
//        SymbolEntry entry = symbols.getObjectNoRangeCheck(id);
//
//        if (!entry.data.equals (entityData)) {
//            entry.data = entityData;
//            isDirty = true;
//        }
//    }
//
//    @Override
//    public synchronized void        listSymbols (Collection <String> out) {
//        int     n = symbols.size ();
//
//        for (int ii = 0; ii < n; ii++) {
//            SymbolEntry e = symbols.getObjectNoRangeCheck(ii);
//            if (e.active)
//                out.add (e.name);
//        }
//    }

    @Override
    public synchronized void        listSymbols (Collection <String> out, Collection<String> data) {
        int     n = symbols.size ();

        for (int ii = 0; ii < n; ii++) {
            SymbolEntry e = symbols.getObjectNoRangeCheck(ii);
            if (e.active) {
                out.add(e.name);
                data.add(e.data);
            }
        }
    }

    @Override
    public synchronized void        listIds (IntegerArrayList ids) {
        int     n = symbols.size ();
        
        for (int id = 0; id < n; id++) {
            if (symbols.getObjectNoRangeCheck (id) != null)
                ids.add (id);
        }
    }
    
//    @Override
//    public synchronized int         getIdOrRegisterSymbol (CharSequence symbol) {
//        int                 id = oneSymbolToId (symbol);
//
//        if (id == NO_SUCH_SYMBOL)
//            id = registerSymbol (symbol.toString (), "");
//
//        return (id);
//    }

    synchronized void                close () {
        symbols = null;
        symbolToIdMap = null;
    }

    synchronized void                format (AbstractPath folder)
        throws IOException
    {
        symbols = new ObjectArrayList <> ();
        symbolToIdMap = new CharSequenceToIntegerMap ();
        isDirty = true;
        
        storeIfDirty (folder);
    }

    synchronized void                format ()
    {
        symbols = new ObjectArrayList <> ();
        symbolToIdMap = new CharSequenceToIntegerMap ();
        isDirty = true;
    }

    public synchronized boolean               load (AbstractPath folder)
            throws IOException
    {
        symbols = new ObjectArrayList <> ();
        symbolToIdMap = new CharSequenceToIntegerMap ();
        isDirty = false;

        // try to restore symbols from temp path
        CharSequenceToIntegerMap            tempSymbolToIdMap = null;
        ObjectArrayList <SymbolEntry>       tempSymbols = null;

        AbstractPath tmpFile = folder.append(TSNames.TMP_PREFIX + TSNames.SYM_REGISTRY_NAME);
        if (tmpFile.exists()) {
            try {
                tempSymbols = new ObjectArrayList<>();
                tempSymbolToIdMap = new CharSequenceToIntegerMap();

                loadData(tmpFile, tempSymbols, tempSymbolToIdMap);
                TreeOps.finalize(tmpFile);

                // At this point we consider that the temp file is valid, and we can use it,
                // and we do not need to check the main file (because we have just overridden it).

                this.symbols = tempSymbols;
                this.symbolToIdMap = tempSymbolToIdMap;
                // We read all data from the temp file
                return true;
            } catch (IOException ex) {
                format();
                LOGGER.info("Skipping invalid temp file: %s").with(tmpFile);
            }
        }

        AbstractPath        fp = folder.append (TSNames.SYM_REGISTRY_NAME);
        if (fp.exists()) {
            loadData(fp, symbols, symbolToIdMap);

            if (tempSymbols != null) {
                // This means that we read some data from the tmp.file but encountered an exception.
                compareDataWithTempData(tempSymbols, tempSymbolToIdMap, tmpFile);
            }
        } else {
            fp = folder.append("symbols.txt");
            if (fp.exists())
                loadText(fp);
        }

        return fp.exists();
    }

    /**
     * It's possible that the temp file has more data than the main file.
     * Here we decide what data to use by checking the max index value.
     */
    private void compareDataWithTempData(ObjectArrayList<SymbolEntry> tempSymbols,
                                         CharSequenceToIntegerMap tempSymbolToIdMap,
                                         AbstractPath tmpFile) {

        int mainSize = symbols.size();
        int tempSize = tempSymbols.size();
        int maxMainIndex = mainSize > 0 ? symbols.get(mainSize - 1).index : -1;
        int maxTempIndex = tempSize > 0 ? tempSymbols.get(tempSize - 1).index : -1;
        if (maxTempIndex > maxMainIndex) {
            // We have more data in the temp file, so we use it
            this.symbols = tempSymbols;
            this.symbolToIdMap = tempSymbolToIdMap;
            LOGGER.info("Using data from a partial temp file: %s. Symbols in main file: size=%s, maxIndex=%s; symbols in temp file: size=%s, maxIndex=%s")
                    .with(tmpFile)
                    .with(mainSize)
                    .with(maxMainIndex)
                    .with(tempSize)
                    .with(maxTempIndex);
        }
    }

    private synchronized void       loadData (AbstractPath fp, ObjectArrayList<SymbolEntry> outSymbols, CharSequenceToIntegerMap outSymbolToIdMap)
            throws IOException
    {
        try (InputStream    is = fp.openInput (0);
             DataInputStream dis = new DataInputStream (BufferedStreamUtil.wrapWithBuffered(is))
        ) {
            int version = dis.readInt();

            assert version == VERSION;

            int n = dis.readInt();

            for (int i = 0; i < n; i++) {
                boolean active = dis.readBoolean();
                int index = dis.readInt();
                String name = dis.readUTF().intern();
                String data = dis.readUTF().intern();

                if ("<NULL>".equals(data))
                    data = null;

                outSymbols.add (new SymbolEntry(name, data, index, active));
                outSymbolToIdMap.put (name, index);

                assert i == index;
            }

            // Verify if we have duplicate symbols in loaded data
            int listSize = outSymbols.size();
            int mapSize = outSymbolToIdMap.size();

            // Fail here if assertions are enabled
            assert listSize == mapSize : "Symbol registry size mismatch: symbols.size=" + listSize + " vs symbolToIdMap.size=" + mapSize;

            //noinspection ConstantValue
            if (listSize != mapSize) {
                LOGGER.warn("Symbol registry size mismatch: symbols.size=%s vs symbolToIdMap.size=%s")
                        .with(listSize)
                        .with(mapSize);
            }
        }
    }

    
    synchronized void               loadText (AbstractPath fp)
        throws IOException 
    {
        try (InputStream    is = fp.openInput (0)) {
            BufferedReader  rd = 
                new BufferedReader (new InputStreamReader (is, StandardCharsets.UTF_8));
            int             id = 0;
            
            for (;; id++) {
                String      line = rd.readLine ();
                
                if (line == null)
                    break;
                                
                if (line.isEmpty ()) {
                    symbols.add (null);
                }
                else {                    
                    int     x = line.indexOf ('\t');
                    
                    if (x < 0) {
                        String symbol = line.intern();
                        symbolToIdMap.put (symbol, id);
                        symbols.add (new SymbolEntry(symbol, "", id));
                    }
                    else {
                        String  symbol = line.substring (0, x).intern();
                        symbolToIdMap.put (symbol, id);
                        String data = line.substring(x + 1).intern();
                        symbols.add (new SymbolEntry(symbol, data, id));
                    }
                }                                
            }
        }               
    }

    protected void                   store(AbstractPath folder) throws IOException {
        AbstractPath tmp = TreeOps.makeTempPath(folder, TSNames.SYM_REGISTRY_NAME);

        if (LOGGER.isDebugEnabled())
            LOGGER.debug().append("Storing symbol registry: ").append(tmp.getPathString()).commit();

        try (OutputStream os = new BufferedOutputStream(tmp.openOutput(0));
             DataOutputStream out = new DataOutputStream(os)
        ) {
            out.writeInt(VERSION);

            int n = symbols.size();
            out.writeInt(n);

            for (int id = 0; id < n; id++) {
                SymbolEntry e = symbols.getObjectNoRangeCheck(id);
                out.writeBoolean(e.active);
                out.writeInt(e.index);
                out.writeUTF(e.name);
                out.writeUTF(e.data != null ? e.data : "<NULL>");
            }
        }

        TreeOps.finalize(tmp);
    }
    
    public synchronized void               storeIfDirty (AbstractPath folder) throws IOException {
        if (isDirty) {
            store(folder);
            isDirty = false;
        }
    }

}