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
package com.epam.deltix.qsrv.hf.pub.codec;


import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.collections.generated.*;
import com.epam.deltix.util.lang.Util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public final class RecordLayout implements RecordClassInfo {
    //public static final int                 STATIC_FIELD = -2;
    public static final int                 VARIABLE_SIZE = -1;

    private static class CollectionData {
        Deque <FieldLayout>                 dq = new ArrayDeque <> ();
        int                                 numStaticFields = 0;
        int                                 numNonStaticFields = 0;
        int                                 numPkFields = 0;
        NonStaticDataField                  varSizeField = null;
    }

    private final TypeLoader                loader;
    private final RecordClassDescriptor     rcd;
    private final Map <String, FieldLayout> nameToFieldMap =
        new HashMap <> ();
    private int                             fixedSize = 0;
    private int                             numDoubleBaseFields = 0;
    private int                             numFloatBaseFields = 0;
    private final StaticFieldLayout []      staticFields;
    private final NonStaticFieldLayout []   nonStaticFields;
    private final NonStaticFieldLayout []   pkFields;
    private Class <?>                       targetClass = null;
    private boolean                         embedded;
    
    public RecordLayout (RecordClassDescriptor rcd) {
        this (null, rcd);
    }
    
    private void        collectFields (RecordClassDescriptor x, CollectionData data) {
        if (x.getParent () != null)
            collectFields (x.getParent (), data);

        for (DataField df : x.getFields ()) {
            FieldLayout                 fieldLayout;

            if (df instanceof StaticDataField) {
                fieldLayout = new StaticFieldLayout (x, (StaticDataField) df);
                data.numStaticFields++;
            }
            else {
                final NonStaticDataField  nsdf = (NonStaticDataField) df;

                fieldLayout = new NonStaticFieldLayout (x, nsdf);

                data.numNonStaticFields++;

                if (fixedSize != VARIABLE_SIZE && getFixedSize (nsdf.getType ()) <= 0) {
                    data.varSizeField = nsdf;
                    fixedSize = VARIABLE_SIZE;
                }

                if (nsdf.isPk ())
                    data.numPkFields++;
            }

            String          name = df.getName ();

            if (name != null) {
                FieldLayout     dup =
                    nameToFieldMap.put (name.toLowerCase(), fieldLayout);

                if (dup != null)
                    throw new IllegalArgumentException (
                        "Name conflict between " + fieldLayout +
                        " and " + dup
                    );
            }

            data.dq.offer (fieldLayout);
        }
    }

    public RecordLayout (TypeLoader loader, RecordClassDescriptor rcd) {
        this.rcd = rcd;
        this.loader = loader;

        if (loader != null) {
            try {
                bindClass(loader);
            } catch (Exception x) {
                throw new MetaDataBindException("Failed to bind descriptor " + rcd.getName(), x);
            }
        }

        CollectionData  data = new CollectionData ();
                
        collectFields (rcd, data);
        
        if (data.numStaticFields != 0)
            staticFields = new StaticFieldLayout [data.numStaticFields];
        else
            staticFields = null;
        
        if (data.numNonStaticFields != 0)
            nonStaticFields = new NonStaticFieldLayout [data.numNonStaticFields];
        else
            nonStaticFields = null;
        
        if (data.numPkFields != 0)
            pkFields = new NonStaticFieldLayout [data.numPkFields];
        else
            pkFields = null;
        
        data.numStaticFields = 0;
        data.numNonStaticFields = 0;
        data.numPkFields = 0;
        
        for (;;) {
            FieldLayout     fieldLayout = data.dq.poll ();
            
            if (fieldLayout == null)
                break;
            
            if (fieldLayout instanceof StaticFieldLayout)
                staticFields [data.numStaticFields++] = (StaticFieldLayout) fieldLayout;
            else {
                NonStaticFieldLayout    nsfl = (NonStaticFieldLayout) fieldLayout;
                
                processNonStatic (nsfl, data);
            }
        }

        //  Check that all types are compatible with variable-size encoding
        if (data.varSizeField != null)
            for (NonStaticFieldLayout nsfl : nonStaticFields) {
                DataType        type = nsfl.getType ();

                if (!isVariableSizeEncodingCompatible (type))
                    throw new RuntimeException (
                        "Type " + type + " of field " + nsfl +
                        " is incompatible with variable-size encoding," +
                        " forced at least by field " + data.varSizeField.getName ()
                    );
            }

        if (loader != null) {
            try {
                bind(loader);
            } catch (Exception x) {
                throw new MetaDataBindException("Failed to bind descriptor " + rcd.getName(), x);
            }
        }
    }

    public void                 setStaticFields (Object message) {
        if (staticFields != null) {
            for (StaticFieldLayout f : staticFields) {
                try {
                    f.set(message);
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    throw new RuntimeException(f.toString(), ex);
                }
            }
        }
    }   

    @Override
    public FieldLayout          getField (String name) {
        return (nameToFieldMap.get (name.toLowerCase()));
    }

    @Override
    public String               toString () {
        return (rcd.getName ());
    }

    public boolean              isBound () {
        return (targetClass != null);
    }

    private void                processNonStatic (
        NonStaticFieldLayout        nsfl,
        CollectionData              data
    )
    {                
        final String        relativeName = nsfl.getField ().getRelativeTo ();

        if (relativeName != null) {        
            FieldLayout         baseLayout = nameToFieldMap.get (relativeName.toLowerCase());

            if (baseLayout == null) 
                throw new IllegalArgumentException (
                    "Field " + nsfl + " is tagged as relative to field " + 
                    relativeName + " which is not found"
                );

            if (!(baseLayout instanceof NonStaticFieldLayout)) 
                throw new IllegalArgumentException (
                    "Field " + nsfl + 
                    " may not be relative to the static field " + relativeName
                );

            NonStaticFieldLayout    nsbase = (NonStaticFieldLayout) baseLayout;

            if (nsbase.ordinal < 0) {
                if (data.dq.removeFirstOccurrence (nsbase))
                    processNonStatic (nsbase, data);
                else
                    throw new IllegalArgumentException (
                        "Relative placement cycle, involving at least " + nsfl + 
                        " and " + nsbase
                    );
            }
            
            final Class <?>         nativeType = nsbase.getNativeType ();

            if (nsfl.getNativeType () != nativeType)
                throw new IllegalArgumentException (
                    nsfl + " and " + baseLayout +
                    " are of different types/sizes, which is illegal for relative placement"
                );

            if (nsbase.ownBaseIndex < 0) {  
                if (nativeType == float.class)
                    nsbase.ownBaseIndex = numFloatBaseFields++;
                else if (nativeType == double.class)
                    nsbase.ownBaseIndex = numDoubleBaseFields++;
                else                
                    throw new IllegalArgumentException (
                        "Type " + nativeType + " of " + nsbase +
                        " referenced from " + nsfl +
                        " is not supported for relative placement"
                    );
            }        

            nsfl.relativeTo = nsbase;
        }
        
        nsfl.ordinal = data.numNonStaticFields;
        nonStaticFields [data.numNonStaticFields++] = nsfl;
                
        if (fixedSize != VARIABLE_SIZE) {
            nsfl.fixedOffset = fixedSize;
            fixedSize += getFixedSize (nsfl.getType ());
        }

        if (nsfl.getField ().isPk ()) 
            pkFields [data.numPkFields++] = nsfl;
    }      
    
    @Override
    public Object           newInstance () {
        if (targetClass == null)
            throw new IllegalStateException ("Not bound");
        
        return (Util.newInstanceNoX (targetClass));
    }
        
    static Class <?>        getClassFor (TypeLoader loader, ClassDescriptor cd)
        throws ClassNotFoundException
    {
//        final String        javaClassName = cd.getJavaClassName ();
//
//        if (javaClassName == null)
//            throw new IllegalArgumentException (
//                "Class " + cd.getName () +
//                " is not associated with a run-time class."
//            );
//        
        try {
            final Class<?> cls = loader.load(cd);
            if (!Modifier.isPublic(cls.getModifiers()))
                throw new RuntimeException("Cannot processing not public classes! Class: " + cls.getName());
            return cls;
//            return (loader.load(cd));
        } catch (ClassNotFoundException e) {
            throw e;
        }
    }

    @SuppressWarnings ("unchecked")
    private void            bindField (TypeLoader loader, FieldLayout field)
            throws ClassNotFoundException
    {
        // first of all try to bind to the target record class
        //try {
            field.bind(targetClass);
        //} catch (NoSuchFieldException e) { never happens?
        //    field.bind(loader);
        //}
    }

    public void             bindClass(TypeLoader loader) throws ClassNotFoundException {
        targetClass = getClassFor (loader, rcd);
    }

    public void             bind (TypeLoader loader)
        throws ClassNotFoundException
    {
        targetClass = getClassFor (loader, rcd);

        if (nonStaticFields != null) 
            for (NonStaticFieldLayout f : nonStaticFields) 
                bindField(loader, f);
            
        if (staticFields != null) 
            for (StaticFieldLayout f : staticFields) 
                bindField(loader, f);
    }
    
    /**
     *  Return the fixed size of the record, or 0 if the size is variable.
     */
    public int              getInitialOutputSize () {
        return (fixedSize == VARIABLE_SIZE ? 0 : fixedSize);
    }
    
    @Override
    public NonStaticFieldLayout [] getNonStaticFields () {
        return nonStaticFields;
    }

    public NonStaticFieldLayout [] getDeclaredNonStaticFields () {
        if (nonStaticFields == null)
            return (null);
        
        int     numDeclared = nonStaticFields.length;
        
        for (; --numDeclared >= 0;)
            if (nonStaticFields [numDeclared].getOwner () != rcd)
                break;
        
        if (++numDeclared == nonStaticFields.length)
            return (null);
        
        int fieldsCount = nonStaticFields.length - numDeclared;
        NonStaticFieldLayout [] ret = new NonStaticFieldLayout [fieldsCount];
        
        System.arraycopy (nonStaticFields, numDeclared, ret, 0, fieldsCount);
        
        return (ret);
    }

    @Override
    public NonStaticFieldLayout [] getPrimaryKeyFields () {
        return pkFields;
    }

    @Override
    public StaticFieldLayout [] getStaticFields () {
        return staticFields;
    }
    
    public int              getNumDoubleBaseFields () {
        return numDoubleBaseFields;
    }

    public int              getNumFloatBaseFields () {
        return numFloatBaseFields;
    }

    @Override
    public RecordClassDescriptor getDescriptor () {
        return rcd;
    }

    public Class <?>        getTargetClass () {
        return targetClass;
    }

    public TypeLoader getLoader() {
        return loader;
    }

    /// usages???
    public boolean isEmbedded() {
        return embedded;
    }

    public void setEmbedded() {
        this.embedded = true;
    }

    public static int getFixedSize(DataType type) {
        if (type instanceof FloatDataType)
            switch (((FloatDataType) type).getScale()) {
                case FloatDataType.FIXED_FLOAT:
                    return (4);
                case FloatDataType.FIXED_DOUBLE:
                    return (8);
                default:
                    return (-1);
            }
        if (type instanceof IntegerDataType) {
            final int size = ((IntegerDataType) type).getSize();
            switch (size) {
                case IntegerDataType.PACKED_UNSIGNED_INT:
                case IntegerDataType.PACKED_UNSIGNED_LONG:
                case IntegerDataType.PACKED_INTERVAL:
                    return -1;
                default:
                    return (size);
            }
        } else if (type instanceof EnumDataType)
            return ((EnumDataType) type).descriptor.computeStorageSize();
        else if (type instanceof VarcharDataType)
            switch (((VarcharDataType) type).getEncodingType()) {
                case VarcharDataType.FORWARD_VARSIZE:
                    return (4);
                case VarcharDataType.INLINE_VARSIZE:
                case VarcharDataType.ALPHANUMERIC:
                    return (-1);
                default:
                    throw new RuntimeException();
            }
        else if (type instanceof BooleanDataType)
            return 1;
        else if (type instanceof CharDataType)
            return 2;
        else if (type instanceof DateTimeDataType)
            return 8;
        else if (type instanceof TimeOfDayDataType)
            return 4;
        else if (type instanceof BinaryDataType)
            return -1;
        else if (type instanceof ClassDataType)
            return -1;
        else if (type instanceof ArrayDataType)
            return -1;
        else
            throw new IllegalArgumentException(type.toString());
    }

    public static boolean isVariableSizeEncodingCompatible(DataType type) {
        if (type instanceof VarcharDataType)
            return ((VarcharDataType) type).getEncodingType() != VarcharDataType.FORWARD_VARSIZE;
        else
            return (true);
    }

    public static Class<?> getNativeType(DataType type) {
        if (type instanceof FloatDataType) {
            FloatDataType dataType = (FloatDataType) type;
            return dataType.isFloat() ? float.class : double.class;
        }
        if (type instanceof IntegerDataType)
            switch (((IntegerDataType) type).getSize()) {
                case 1:
                    return (byte.class);

                case 2:
                    return (short.class);

                case 4:
                case IntegerDataType.PACKED_UNSIGNED_INT:
                case IntegerDataType.PACKED_INTERVAL:
                    return (int.class);

                default:
                    return (long.class);
            }
        else if (type instanceof EnumDataType)
            return (Object.class);
        else if (type instanceof VarcharDataType)
            return (String.class);
        else if (type instanceof BooleanDataType)
            return (boolean.class);
        else if (type instanceof CharDataType)
            return (char.class);
        else if (type instanceof DateTimeDataType)
            return (long.class);
        else if (type instanceof TimeOfDayDataType)
            return (int.class);
        else if (type instanceof BinaryDataType)
            return byte[].class;
        else if (type instanceof ClassDataType)
            return Class.class;
        else if (type instanceof ArrayDataType) {
            DataType u = ((ArrayDataType) type).getElementDataType();
            if (u instanceof IntegerDataType) {
                switch (((IntegerDataType) u).getNativeTypeSize()) {
                    case 1:
                        return ByteArrayList.class;
                    case 2:
                        return ShortArrayList.class;
                    case 4:
                        return IntegerArrayList.class;
                    case 6:
                    case 8:
                        return LongArrayList.class;
                    default:
                        throw new IllegalArgumentException("unexpected encoding " + u.getEncoding());
                }
            } else if (u instanceof FloatDataType)
                return ((FloatDataType) u).isFloat() ? FloatArrayList.class : DoubleArrayList.class;
            else if (u instanceof BooleanDataType)
                return u.isNullable() ? ByteArrayList.class : BooleanArrayList.class;
            else if (u instanceof CharDataType)
                return CharacterArrayList.class;
            else if (u instanceof VarcharDataType) {
                final VarcharDataType vdt = (VarcharDataType) u;
                if (vdt.getEncodingType() == VarcharDataType.ALPHANUMERIC && vdt.getLength() <= 10) {
                    return LongArrayList.class;
                } else if (vdt.getEncodingType() == VarcharDataType.INLINE_VARSIZE) {
                    return ObjectArrayList.class;
                } else {
                    throw new UnsupportedOperationException(u.getClass().getSimpleName());
                }
            } else
                return Object.class;
        } else
            throw new IllegalArgumentException(type.toString());
    }
}