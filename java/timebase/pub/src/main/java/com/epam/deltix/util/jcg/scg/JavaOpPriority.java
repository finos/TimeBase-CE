package com.epam.deltix.util.jcg.scg;

/**
 *
 * <pre>
 {@literal

postfix 	expr++ expr--
unary 	++expr --expr +expr -expr ~ !
multiplicative 	* / %
additive 	+ -
shift 	<< >> >>>
relational 	< > <= >= instanceof
equality 	== !=
bitwise AND 	&
bitwise exclusive OR 	^
bitwise inclusive OR 	|
logical AND 	&&
logical OR 	||
ternary 	? :
assignment 	= += -= *= /= %= &= ^= |= <<= >>= >>>=

 }
 </pre>
 */
public class JavaOpPriority {
    public static final int                 OPEN = 0;

    public static final int                 ASSIGNMENT =    1;
    public static final int                 TERNARY =       2;
    public static final int                 BOOL_OR =       3;
    public static final int                 BOOL_AND =      4;
    public static final int                 BIT_OR =        5;
    public static final int                 BIT_XOR =       6;
    public static final int                 BIT_AND =       7;
    public static final int                 EQUALITY =      8;
    public static final int                 RELATIONAL =    9;
    public static final int                 SHIFT =         10;
    public static final int                 ADDITIVE =      11;
    public static final int                 MULTIPLICATIVE = 12;
    public static final int                 CAST =          13;
    public static final int                 UNARY =         14;
    public static final int                 POSTFIX =       15;
}
