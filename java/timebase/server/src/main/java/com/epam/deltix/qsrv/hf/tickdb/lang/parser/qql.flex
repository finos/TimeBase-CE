package com.epam.deltix.qsrv.hf.tickdb.lang.parser;

import java_cup.runtime.*;
import com.epam.deltix.util.parsers.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.CharDataType;

/**
 * QQL 5.0 Lexer.
 */
%%

%class Lexer
%final
%unicode
%cupsym Symbols
%cup
%line
%column
%ignorecase

%{
    private StringBuffer    string = new StringBuffer();
    private int             start = -1;
    
    private int             pos () {
        return ((yyline << 16) | yycolumn);
    }

    private void startBuffering () {
        string.setLength (0);
        start = pos ();
    }

    private Symbol symbol (int type) {
        return (symbol (type, null));
    }

    private Symbol symbol (int type, Object value) {
        int         n = pos ();

        return new Symbol (type, n, n + yylength (), value);
    }

    private Symbol buffered (int symtype) {
        yybegin (YYINITIAL);

        int         n = pos ();
        
        return new Symbol (symtype, start, n + yylength (), string.toString ());
    }

    private static char parseChar(String s) {
        switch (s) {
            case "\\\\":
                return '\\';
            case "\\\"":
                return '"';
            case "\\'":
                return '\'';
            case "\\t":
                return '\t';
            case "\\b":
                return '\b';
            case "\\r":
                return '\r';
            case "\\f":
                return '\f';
            case "\\n":
                return '\n';
            default:
                return CharDataType.staticParse(s);
        }
    }
%}

LineTerminator =                    \r|\n|\r\n
InputCharacter =                    [^\r\n]
WhiteSpace =                        {LineTerminator} | [ \t\f]

/* comments */
Comment =                           {TraditionalComment} | {EndOfLineComment}

TraditionalComment =                "/*" [^*] ~"*/" | "/*" "*"+ "/"

EndOfLineComment =                  "--" {InputCharacter}* {LineTerminator}

UnescapedIdentifier =               [:jletter:] [:jletterdigit:]*

UnsignedInteger =                   0 | [1-9][0-9]*

UnsignedLong =                      {UnsignedInteger}L

FloatingPointLiteral =              {UnsignedInteger} "." [0-9]* ( e [+-]? {UnsignedInteger} )?

DoubleLiteral =                     {FloatingPointLiteral} f

TimeInterval =                      ([0-9]+d([0-9]+h)?([0-9]+m)?([0-9]+s)?([0-9]+ms)?([0-9]+ns)?) | ([0-9]+h([0-9]+m)?([0-9]+s)?([0-9]+ms)?([0-9]+ns)?) | ([0-9]+m([0-9]+s)?([0-9]+ms)?([0-9]+ns)?) | ([0-9]+s([0-9]+ms)?([0-9]+ns)?) | ([0-9]+ms([0-9]+ns)?) | [0-9]+ns

CharLiteral =                       \'(([^\n\r\'\\]) | (\\\') | (\\\") | (\\\\) | (\\t) | (\\b) | (\\r) | (\\f) | (\\n))\'c

%state STRING
%state ESCID

%%


<YYINITIAL> {
    "union"                         { return symbol (Symbols.UNION); }
    "select"                        { return symbol (Symbols.SELECT); }
    "where"                         { return symbol (Symbols.WHERE); }
    "from"                          { return symbol (Symbols.FROM); }
    "and"                           { return symbol (Symbols.AND); }
    "or"                            { return symbol (Symbols.OR); }
    "in"                            { return symbol (Symbols.IN); }
    "like"                          { return symbol (Symbols.LIKE); }
    "is"                            { return symbol (Symbols.IS); }
    "not"                           { return symbol (Symbols.NOT); }
    "null"                          { return symbol (Symbols.NULL); }
    "as"                            { return symbol (Symbols.AS); }
    "new"                           { return symbol (Symbols.NEW); }
    "true"                          { return symbol (Symbols.TRUE); }
    "false"                         { return symbol (Symbols.FALSE); }
    "distinct"                      { return symbol (Symbols.DISTINCT); }
    "cast"                          { return symbol (Symbols.CAST); }
    "group"                         { return symbol (Symbols.GROUP); }
    "by"                            { return symbol (Symbols.BY); }
    "between"                       { return symbol (Symbols.BETWEEN); }
    "running"                       { return symbol (Symbols.RUNNING); }
    "create"                        { return symbol (Symbols.CREATE); }
    "stream"                        { return symbol (Symbols.STREAM); }
    "options"                       { return symbol (Symbols.OPTIONS); }
    "transient"                     { return symbol (Symbols.TRANSIENT); }
    "durable"                       { return symbol (Symbols.DURABLE); }
    "class"                         { return symbol (Symbols.CLASS); }
    "instantiable"                  { return symbol (Symbols.INSTANTIABLE); }
    "auxiliary"                     { return symbol (Symbols.AUXILIARY); }
    "relative"                      { return symbol (Symbols.RELATIVE); }
    "to"                            { return symbol (Symbols.TO); }
    "comment"                       { return symbol (Symbols.COMMENT); }
    "enum"                          { return symbol (Symbols.ENUM); }
    "flags"                         { return symbol (Symbols.FLAGS); }
    "under"                         { return symbol (Symbols.UNDER); }
    "static"                        { return symbol (Symbols.STATIC); }
    "drop"                          { return symbol (Symbols.DROP); }
    "array"                         { return symbol (Symbols.ARRAY); }
    "object"                        { return symbol (Symbols.OBJECT); }
    "alter"                         { return symbol (Symbols.ALTER); }
    "modify"                        { return symbol (Symbols.MODIFY); }
    "default"                       { return symbol (Symbols.DEFAULT); }
    "confirm"                       { return symbol (Symbols.CONFIRM); }
    "join"                          { return symbol (Symbols.JOIN); }
    "left"                          { return symbol (Symbols.LEFT); }
    "tags"                          { return symbol (Symbols.TAGS); }
    "trigger"                       { return symbol (Symbols.TRIGGER); }
    "reset"                         { return symbol (Symbols.RESET); }
    "over"                          { return symbol (Symbols.OVER); }
    "every"                         { return symbol (Symbols.EVERY); }
    "type"                          { return symbol (Symbols.TYPE); }
    "with"                          { return symbol (Symbols.WITH); }
    "limit"                         { return symbol (Symbols.LIMIT); }
    "offset"                        { return symbol (Symbols.OFFSET); }
    
    /* identifiers */
    {UnescapedIdentifier}           { return symbol (Symbols.IDENTIFIER, yytext ().toUpperCase ()); }
    \"                              { startBuffering (); yybegin (ESCID); }
 
    /* literals */
    {UnsignedInteger}               { return symbol (Symbols.UINT, yytext ()); }
    {UnsignedLong}                  { return symbol (Symbols.ULONG, yytext()); }
    {FloatingPointLiteral}          { return symbol (Symbols.FP, yytext ()); }
    {DoubleLiteral}                 { return symbol (Symbols.DOUBLE, yytext()); }
    {TimeInterval}                  { return symbol (Symbols.TIME_INTERVAL_LITERAL, yytext()); }

    {CharLiteral}                   { return symbol (Symbols.CHAR_LITERAL, parseChar(yytext().substring(1, yylength() - 2))); }
    \'                              { startBuffering (); yybegin (STRING); }

    /* operators */
    ","                             { return symbol (Symbols.COMMA); }
    "."                             { return symbol (Symbols.DOT); }
    ";"                             { return symbol (Symbols.SEMICOLON); }
    ":"                             { return symbol (Symbols.COLON); }
    "="                             { return symbol (Symbols.ASSIGN); }
    "=="                            { return symbol (Symbols.EQ); }
    "!="                            { return symbol (Symbols.NEQ); }
    "==="                           { return symbol (Symbols.STRICT_EQ); }
    "!=="                           { return symbol (Symbols.STRICT_NEQ); }
    "+"                             { return symbol (Symbols.PLUS); }
    "-"                             { return symbol (Symbols.MINUS); }
    "*"                             { return symbol (Symbols.STAR); }
    "/"                             { return symbol (Symbols.SLASH); }
    ">"                             { return symbol (Symbols.GT); }
    "<"                             { return symbol (Symbols.LT); }
    ">="                            { return symbol (Symbols.GE); }
    "<="                            { return symbol (Symbols.LE); }
    "("                             { return symbol (Symbols.LPAREN); }
    ")"                             { return symbol (Symbols.RPAREN); }
    "%"                             { return symbol (Symbols.PERCENT); }
    "["                             { return symbol (Symbols.LBRACKET); }
    "]"                             { return symbol (Symbols.RBRACKET); }
    "{"                             { return symbol (Symbols.LBRACE); }
    "}"                             { return symbol (Symbols.RBRACE); }
    "?"                             { return symbol (Symbols.QUESTION); }

    /* comments */
    {Comment}                       { /* ignore */ }
 
    /* whitespace */
    {WhiteSpace}                    { /* ignore */ }

    "\x01"                          { yycolumn--; return symbol (Symbols.X_TYPE); }
}

<STRING> {
    \'D                             { return buffered (Symbols.DATE_LITERAL); }
    \'T                             { return buffered (Symbols.TIME_LITERAL); }
    \'X                             { return buffered (Symbols.BIN_LITERAL); }
    \'                              { return buffered (Symbols.STRING); }

    [^\n\r\'\\]+                    { string.append (yytext ()); }

    \\\'                            { string.append ("\'"); }
    \\\"                            { string.append ("\""); }
    \\\\                            { string.append ("\\"); }
    \\t                             { string.append ("\t"); }
    \\b                             { string.append ("\b"); }
    \\r                             { string.append ("\r"); }
    \\f                             { string.append ("\f"); }
    \\n                             { string.append ("\n"); }
}

<ESCID> {
    \"                              { return buffered (Symbols.IDENTIFIER); }

    [^\n\r\"\\]+                    { string.append (yytext ()); }

    \"\"                            { string.append ('\"'); }
}

/* error fallback */
.|\n  {
    int  n = pos ();

    throw new SyntaxErrorException (
        "Illegal character: '"+ yytext ()+ "'",
        Location.combine (n, n + yylength ())
    );
}
