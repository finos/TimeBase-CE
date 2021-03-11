package com.epam.deltix.snmp.parser;

import java_cup.runtime.*;
import com.epam.deltix.util.parsers.*;

/**
 * ASN.1 Lexer.
 */
%%

%class Lexer
%final
%unicode
%cupsym Symbols
%cup
%line
%column

%{
    private int             pos () {
        return ((yyline << 16) | yycolumn);
    }

    private Symbol charString () {
        StringBuilder       sb = new StringBuilder ();
        String              quoted = yytext ();
        int                 len = quoted.length ();

        if (len < 2)
            throw new IllegalArgumentException ("len == " + len + " < 2");
        
        len--;

        if (quoted.charAt (0) != '"' ||
            quoted.charAt (len) != '"')
            throw new IllegalArgumentException (quoted);

        for (int ii = 1; ii < len; ii++) {
            char            c = quoted.charAt (ii);

            if (c == '"') {
                ii++;
                c = quoted.charAt (ii);

                if (c != '"')
                    throw new IllegalArgumentException ("Illegal quote in " + quoted);
            }

            sb.append (c);
        }
            
        return symbol (Symbols.CharString, sb.toString ());
    }

    private Symbol symbol (int type) {
        return symbol (type, yytext ());
    }

    private Symbol symbol (int type, Object value) {
        int         n = pos ();

        return new Symbol (type, n, n + yylength (), value);
    }   
%}

/***********    X680-0207 Section 11.1.6    *********************************/

NewLine =                           \r | \n | \f | \r\n 

WhiteSpace =                        {NewLine} | \t | " "

Up =                                [A-Z]

Low =                               [a-z]

AlphaNumeric =                      [A-Za-z0-9]

/***********    X680-0207 Section 11.2      *********************************/

TypeId =                            {Up} {AlphaNumeric}* ( "-" {AlphaNumeric}+ )*

ValueId =                           {Low} {AlphaNumeric}* ( "-" {AlphaNumeric}+ )*

/***********    X680-0207 Section 11.6      *********************************/

Comment =                           "/*" ~ "*/" | "--" ~ ( "--" | {NewLine} )

/***********    X680-0207 Section 11.8      *********************************/

Number =                            "0" | [1-9][0-9]*

/***********    X680-0207 Section 11.9      *********************************/

RealNumber =                        {Number} ( "." ( [0-9]* )? )? ( [eE] [+-]? {Number} )?

/***********    X680-0207 Section 11.10     *********************************/

BinaryDigit =                       [01]

BinaryString =                      "'" ( {BinaryDigit} | {WhiteSpace} )* "'B"

/***********    X680-0207 Section 11.12     *********************************/

HexDigit =                          [A-F0-9]

HexString =                         "'" ( {HexDigit} | {WhiteSpace} )* "'H"

/***********    X680-0207 Section 11.14     *********************************/

CharString =                        "\"" ( [^\"] | "\"\"" )* "\""

%%

<YYINITIAL> {

    "::="       { return symbol (Symbols.ASSIGN); }         /* 11.16 */
    "..."       { return symbol (Symbols.ELLIPSIS); }       /* 11.18 */
    ".."        { return symbol (Symbols.RANGESEP); }       /* 11.17 */
    "[["        { return symbol (Symbols.LVB); }            /* 11.19 */
    "]]"        { return symbol (Symbols.RVB); }            /* 11.20 */

    /* 11.26 */

    "{"         { return symbol (Symbols.LCB); } 
    "}"         { return symbol (Symbols.RCB); } 
    "<"         { return symbol (Symbols.LT); }
    ">"         { return symbol (Symbols.GT); }
    ","         { return symbol (Symbols.COMMA); }
    "."         { return symbol (Symbols.DOT); }
    "("         { return symbol (Symbols.LP); }
    ")"         { return symbol (Symbols.RP); }
    "["         { return symbol (Symbols.LBKT); }
    "]"         { return symbol (Symbols.RBKT); }
    "-"         { return symbol (Symbols.HYPHEN); }
    ":"         { return symbol (Symbols.COLON); }
    "="         { return symbol (Symbols.EQ); }
    "\""        { return symbol (Symbols.DQUOT); }
    "'"         { return symbol (Symbols.SQUOT); }
    ";"         { return symbol (Symbols.SEMIC); }
    "@"         { return symbol (Symbols.AT); }
    "|"         { return symbol (Symbols.PIPE); }
    "!"         { return symbol (Symbols.EXC); }
    "^"         { return symbol (Symbols.CARET); }

    /* comments */
    {Comment}                   { /* ignore */ }
 
    /* whitespace */
    {WhiteSpace}                { /* ignore */ }

    /* rfc2578 Keywords */
    
    "ABSENT" { return symbol (Symbols.ABSENT); }
    "ACCESS" { return symbol (Symbols.ACCESS); }
    "AGENT-CAPABILITIES" { return symbol (Symbols.AGENT_CAPABILITIES); }
    "ANY" { return symbol (Symbols.ANY); }
    "APPLICATION" { return symbol (Symbols.APPLICATION); }
    "AUGMENTS" { return symbol (Symbols.AUGMENTS); }
    "BEGIN" { return symbol (Symbols.BEGIN); }
    "BIT" { return symbol (Symbols.BIT); }
    "BITS" { return symbol (Symbols.BITS); }
    "BOOLEAN" { return symbol (Symbols.BOOLEAN); }
    "BY" { return symbol (Symbols.BY); }
    "CHOICE" { return symbol (Symbols.CHOICE); }
    "COMPONENT" { return symbol (Symbols.COMPONENT); }
    "COMPONENTS" { return symbol (Symbols.COMPONENTS); }
    "CONTACT-INFO" { return symbol (Symbols.CONTACT_INFO); }
    "CREATION-REQUIRES" { return symbol (Symbols.CREATION_REQUIRES); }
    "DEFAULT" { return symbol (Symbols.DEFAULT); }
    "DEFINED" { return symbol (Symbols.DEFINED); }
    "DEFINITIONS" { return symbol (Symbols.DEFINITIONS); }
    "DEFVAL" { return symbol (Symbols.DEFVAL); }
    "DESCRIPTION" { return symbol (Symbols.DESCRIPTION); }
    "DISPLAY-HINT" { return symbol (Symbols.DISPLAY_HINT); }
    "END" { return symbol (Symbols.END); }
    "ENUMERATED" { return symbol (Symbols.ENUMERATED); }
    "ENTERPRISE" { return symbol (Symbols.ENTERPRISE); }
    "EXPLICIT" { return symbol (Symbols.EXPLICIT); }
    "EXPORTS" { return symbol (Symbols.EXPORTS); }
    "EXTERNAL" { return symbol (Symbols.EXTERNAL); }
    "FALSE" { return symbol (Symbols.FALSE); }
    "FROM" { return symbol (Symbols.FROM); }
    "GROUP" { return symbol (Symbols.GROUP); }
    "IDENTIFIER" { return symbol (Symbols.IDENTIFIER); }
    "IMPLICIT" { return symbol (Symbols.IMPLICIT); }
    "IMPLIED" { return symbol (Symbols.IMPLIED); }
    "IMPORTS" { return symbol (Symbols.IMPORTS); }
    "INCLUDES" { return symbol (Symbols.INCLUDES); }
    "INDEX" { return symbol (Symbols.INDEX); }
    "INTEGER" { return symbol (Symbols.INTEGER); }
    "LAST-UPDATED" { return symbol (Symbols.LAST_UPDATED); }
    "MACRO" { return symbol (Symbols.MACRO); }
    "MANDATORY-GROUPS" { return symbol (Symbols.MANDATORY_GROUPS); }
    "MAX" { return symbol (Symbols.MAX); }
    "MAX-ACCESS" { return symbol (Symbols.MAX_ACCESS); }
    "MIN" { return symbol (Symbols.MIN); }
    "MIN-ACCESS" { return symbol (Symbols.MIN_ACCESS); }
    "MINUS-INFINITY" { return symbol (Symbols.MINUS_INFINITY); }
    "MODULE" { return symbol (Symbols.MODULE); }
    "MODULE-COMPLIANCE" { return symbol (Symbols.MODULE_COMPLIANCE); }
    "MODULE-IDENTITY" { return symbol (Symbols.MODULE_IDENTITY); }
    "NOTIFICATION-GROUP" { return symbol (Symbols.NOTIFICATION_GROUP); }
    "NOTIFICATION-TYPE" { return symbol (Symbols.NOTIFICATION_TYPE); }
    "NOTIFICATIONS" { return symbol (Symbols.NOTIFICATIONS); }
    "NULL" { return symbol (Symbols.NULL); }
    "OBJECT" { return symbol (Symbols.OBJECT); }
    "OBJECT-GROUP" { return symbol (Symbols.OBJECT_GROUP); }
    "OBJECT-IDENTITY" { return symbol (Symbols.OBJECT_IDENTITY); }
    "OBJECT-TYPE" { return symbol (Symbols.OBJECT_TYPE); }
    "OBJECTS" { return symbol (Symbols.OBJECTS); }
    "OCTET" { return symbol (Symbols.OCTET); }
    "OF" { return symbol (Symbols.OF); }
    "OPTIONAL" { return symbol (Symbols.OPTIONAL); }
    "ORGANIZATION" { return symbol (Symbols.ORGANIZATION); }
    "PLUS-INFINITY" { return symbol (Symbols.PLUS_INFINITY); }
    "PRESENT" { return symbol (Symbols.PRESENT); }
    "PRIVATE" { return symbol (Symbols.PRIVATE); }
    "PRODUCT-RELEASE" { return symbol (Symbols.PRODUCT_RELEASE); }
    "REAL" { return symbol (Symbols.REAL); }
    "REFERENCE" { return symbol (Symbols.REFERENCE); }
    "REVISION" { return symbol (Symbols.REVISION); }
    "SEQUENCE" { return symbol (Symbols.SEQUENCE); }
    "SET" { return symbol (Symbols.SET); }
    "SIZE" { return symbol (Symbols.SIZE); }
    "STATUS" { return symbol (Symbols.STATUS); }
    "STRING" { return symbol (Symbols.STRING); }
    "SUPPORTS" { return symbol (Symbols.SUPPORTS); }
    "SYNTAX" { return symbol (Symbols.SYNTAX); }
    "TAGS" { return symbol (Symbols.TAGS); }
    "TEXTUAL-CONVENTION" { return symbol (Symbols.TEXTUAL_CONVENTION); }
    "TRAP-TYPE" { return symbol (Symbols.TRAP_TYPE); }
    "TRUE" { return symbol (Symbols.TRUE); }
    "UNITS" { return symbol (Symbols.UNITS); }
    "UNIVERSAL" { return symbol (Symbols.UNIVERSAL); }
    "VARIABLES" { return symbol (Symbols.VARIABLES); }
    "VARIATION" { return symbol (Symbols.VARIATION); }
    "WITH" { return symbol (Symbols.WITH); }
    "WRITE-SYNTAX" { return symbol (Symbols.WRITE_SYNTAX); }

    {TypeId}    { return symbol (Symbols.TypeId); }
    {ValueId}   { return symbol (Symbols.ValueId); }
    {CharString} { return charString (); }
    {Number}    { return symbol (Symbols.Number, new java.math.BigInteger (yytext (), 10)); }
}

/* error fallback */
.|\n  {
    int  n = pos ();

    throw new SyntaxErrorException (
        "Illegal character: '"+ yytext ()+ "'",
        Location.combine (n, n + yylength ())
    );
}
