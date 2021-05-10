package com.epam.deltix.qsrv.hf.codec;

import com.epam.deltix.containers.*;
import com.epam.deltix.containers.interfaces.BinaryArrayReadOnly;
import com.epam.deltix.containers.interfaces.BinaryArrayReadWrite;
import com.epam.deltix.util.collections.generated.*;
import com.epam.deltix.util.lang.Util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 *
 */
public abstract class ArrayTypeUtil {
    // types, which are bound to ARRAY field
    private static final Class<?> ARRAY_TYPES[] = {
            BinaryArray.class,
            BinaryArrayReadOnly.class,
            BinaryArrayReadWrite.class,
            BooleanArrayList.class,
            BooleanList.class,
            CharacterArrayList.class,
            ByteArrayList.class,
            ShortArrayList.class,
            IntegerArrayList.class,
            LongArrayList.class,
            LongList.class,
            FloatArrayList.class,
            DoubleArrayList.class,
            ObjectArrayList.class
    };

    // underline types, which correspond XArrayList classes
    private static final Class<?> ARRAY_UNDERLINE_TYPES[] = {
            byte.class,
            byte.class,
            byte.class,
            boolean.class,
            boolean.class,
            char.class,
            byte.class,
            short.class,
            int.class,
            long.class,
            long.class,
            float.class,
            double.class,
            Object.class
    };

    // Boxed underline types, which correspond XArrayList classes
    private static final String ARRAY_UNDERLINE_TYPES_BOXED[] = {
            "Byte",
            "Byte",
            "Byte",
            "Boolean",
            "Boolean",
            "Character",
            "Byte",
            "Short",
            "Integer",
            "Long",
            "Long",
            "Float",
            "Double",
            "Object"
    };

    public static boolean isSupported(Class<?> boundClass) {
        return Util.indexOf(ARRAY_TYPES, boundClass) != -1;
    }

    public static Class<?> getUnderline(Class<?> boundClass, Type genericType) {
        if (ObjectArrayList.class == boundClass) {
            return (genericType instanceof ParameterizedType) ?
                    (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0] : Object.class;
        }

        final int idx = Util.indexOf(ARRAY_TYPES, boundClass);
        return ARRAY_UNDERLINE_TYPES[idx];
    }

    public static Class<?> getUnderline(Class<?> boundClass) {
        final int idx = Util.indexOf(ARRAY_TYPES, boundClass);
        return ARRAY_UNDERLINE_TYPES[idx];
    }

    public static String getUnderlineBoxed(Class<?> boundClass) {

        final int idx = Util.indexOf(ARRAY_TYPES, boundClass);
        return ARRAY_UNDERLINE_TYPES_BOXED[idx];
    }
}
