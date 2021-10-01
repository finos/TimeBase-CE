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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.pub.md.ArrayDataType;
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.EnumClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.EnumValue;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.CompiledExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Expression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.FieldIdentifier;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Identifier;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.NamedObjectType;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.QuantQueryCompiler;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Statement;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.TypeIdentifier;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.PreparedQuery;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.parsers.Element;

import java.util.Objects;

/**
 *
 */
public class QQLCompiler implements QuantQueryCompiler {
    public static final String      KEYWORD_ENTITY = "ENTITY";
    public static final String      KEYWORD_TIMESTAMP = "TIMESTAMP";
    public static final String      KEYWORD_SYMBOL = "SYMBOL";
    //public static final String      KEYWORD_TYPE = "TYPE";
    public static final String      KEYWORD_THIS = "THIS";
    public static final String      KEYWORD_LAST = "LAST";
    public static final String      KEYWORD_FIRST = "FIRST";
    public static final String      KEYWORD_REVERSE = "REVERSE";
    public static final String      KEYWORD_LIVE = "LIVE";
    public static final String      KEYWORD_HYBRID = "HYBRID";
    public static final String      KEYWORD_POSITION = "POSITION";
    public static final String      KEYWORD_NOW = "NOW";

    private final DXTickDB          db;
    private Environment             env;
    
    public QQLCompiler (DXTickDB db, Environment env) {
        this.db = db;
        this.env = env;        
    }

    public PreparedQuery            compileStatement (Statement s) {
        return (new DDLCompiler (db, env).compileStatement (s));
    }
    
    @Override
    public CompiledExpression       compile (Expression e, DataType expectedType) {
        return (new QQLExpressionCompiler (env).compile (e, expectedType));
    }

    static void                     setUpEnv (
        EnvironmentFrame                env, 
        ClassDescriptor                 type
    )
    {
        if (type instanceof RecordClassDescriptor) {
            RecordClassDescriptor   rcd = (RecordClassDescriptor) type;
            
            for (DataField f : rcd.getFields ())
                env.bind (
                    NamedObjectType.VARIABLE, 
                    f.getName (),   
                    new DataFieldRef (rcd, f)
                );
        }        
        else if (type instanceof EnumClassDescriptor) {
            EnumClassDescriptor     ecd = (EnumClassDescriptor) type;
            
            for (EnumValue ev : ecd.getValues ())
                env.bind (
                    NamedObjectType.VARIABLE, 
                    ev.symbol, 
                    new EnumValueRef (ecd, ev)
                ); 
        }
    }
    
    public static DataType          unionEx (DataType a, DataType b) {
        if (a == null || b == null)
            return (null);
        
        return (union (a, b));
    }
    
    public static DataType          union (DataType a, DataType b) {
        //TODO: handle conversion to base classes
        if (a.getClass () != b.getClass ())
            return (null);
        
        if (a.isNullable () && !b.isNullable ())
            return (a);
        
        return (b);
    }
    
    public static boolean           isCompatibleWithoutConversion (DataType from, DataType to) {
        //TODO: handle conversion to base classes at the very least
        if (from instanceof FloatDataType && to instanceof FloatDataType) {
            FloatDataType fromType = (FloatDataType) from;
            FloatDataType toType = (FloatDataType) to;
            return isDecimal64(fromType) && isDecimal64(toType) || !isDecimal64(fromType) && !isDecimal64(toType);
        } if (from instanceof ArrayDataType && to instanceof ArrayDataType) {
            DataType e1 = ((ArrayDataType) from).getElementDataType();
            DataType e2 = ((ArrayDataType) to).getElementDataType();
            return e1.getClass() == e2.getClass() && Objects.equals(e1.getEncoding(), e2.getEncoding());
        }
        return (to.getClass () == from.getClass ());
    }

    private static boolean          isDecimal64(FloatDataType floatDataType) {
        return floatDataType.getScale() == FloatDataType.SCALE_DECIMAL64;
    }

    public static int               paramTypeCompatibilityHashCode (DataType t) {
        int     ret = t.getClass ().hashCode ();
        
        if (t.isNullable ())
            ret += 1;
        
        if (t instanceof IntegerDataType) {
            IntegerDataType     idt = (IntegerDataType) t;
            
            ret = ret * 31 + Util.xhashCode (idt.min);
            ret = ret * 31 + Util.xhashCode (idt.max);
        }
        else if (t instanceof FloatDataType) {
            FloatDataType     fdt = (FloatDataType) t;
            
            ret = ret * 31 + Util.xhashCode (fdt.min);
            ret = ret * 31 + Util.xhashCode (fdt.max);
        }
        
        return (ret);
    }
    
    public static boolean           isParamTypeCompatible (DataType a, DataType b) {
        if (a.getClass () != b.getClass () ||
            a.isNullable () != b.isNullable ())
            return (false);
        
        if (a instanceof IntegerDataType) {
            IntegerDataType     aa = (IntegerDataType) a;
            IntegerDataType     bb = (IntegerDataType) b;
            
            if (!Util.xequals (aa.min, bb.min) ||
                !Util.xequals (aa.max, bb.max))
                return (false);
        }
        
        if (a instanceof FloatDataType) {
            FloatDataType       aa = (FloatDataType) a;
            FloatDataType       bb = (FloatDataType) b;
            
            if (!Util.xequals (aa.min, bb.min) ||
                !Util.xequals (aa.max, bb.max))
                return (false);
        }
        
        return (true);
    }
    
    public static Object            lookUpVariable (Environment e, Identifier id) {
        return (e.lookUp (NamedObjectType.VARIABLE, id.id, id.location));
    }
    
    public static Object            lookUpField (Environment e, FieldIdentifier id) {
        return (e.lookUp (NamedObjectType.VARIABLE, id.fieldName, id.location));
    }
    
    public static Object            lookUpType (Environment e, TypeIdentifier id) {
        return (e.lookUp (NamedObjectType.TYPE, id.typeName, id.location));
    }

    public static Object            lookUpType (Environment e, String name) {
        return (e.lookUp (NamedObjectType.TYPE, name, Element.NO_LOCATION));
    }
}