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
package com.epam.deltix.qsrv.hf.tickdb.lang.parser;

import java_cup.runtime.*;
import com.epam.deltix.util.parsers.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 * QQL 5.0 Lexer.
 */

final class Lexer implements java_cup.runtime.Scanner {

  /** This character denotes the end of file */
  public static final int YYEOF = -1;

  /** initial size of the lookahead buffer */
  private static final int ZZ_BUFFERSIZE = 16384;

  /** lexical states */
  public static final int STRING = 2;
  public static final int YYINITIAL = 0;
  public static final int ESCID = 4;

  /**
   * ZZ_LEXSTATE[l] is the state in the DFA for the lexical state l
   * ZZ_LEXSTATE[l+1] is the state in the DFA for the lexical state l
   *                  at the beginning of a line
   * l is of the form l = 2*k, k a non negative integer
   */
  private static final int ZZ_LEXSTATE[] = { 
     0,  0,  1,  1,  2, 2
  };

  /** 
   * Translates characters to character classes
   */
  private static final String ZZ_CMAP_PACKED = 
    "\1\10\1\57\7\10\1\3\1\2\1\0\1\3\1\1\16\10\4\0"+
    "\1\3\1\52\1\44\1\0\1\7\2\0\1\45\1\55\1\56\1\5"+
    "\1\15\1\46\1\6\1\13\1\4\1\11\11\12\1\50\1\47\1\54"+
    "\1\51\1\53\2\0\1\33\1\40\1\24\1\34\1\14\1\31\1\36"+
    "\1\27\1\20\1\43\1\35\1\23\1\32\1\17\1\21\1\37\1\7"+
    "\1\30\1\22\1\25\1\16\1\42\1\26\1\60\1\41\1\7\1\0"+
    "\1\61\2\0\1\7\1\0\1\33\1\40\1\24\1\34\1\14\1\31"+
    "\1\36\1\27\1\20\1\43\1\35\1\23\1\32\1\17\1\21\1\37"+
    "\1\7\1\30\1\22\1\25\1\16\1\42\1\26\1\60\1\41\1\7"+
    "\4\0\41\10\2\0\4\7\4\0\1\7\2\0\1\10\7\0\1\7"+
    "\4\0\1\7\5\0\27\7\1\0\37\7\1\0\u01ca\7\4\0\14\7"+
    "\16\0\5\7\7\0\1\7\1\0\1\7\21\0\160\10\5\7\1\0"+
    "\2\7\2\0\4\7\10\0\1\7\1\0\3\7\1\0\1\7\1\0"+
    "\24\7\1\0\123\7\1\0\213\7\1\0\5\10\2\0\236\7\11\0"+
    "\46\7\2\0\1\7\7\0\47\7\11\0\55\10\1\0\1\10\1\0"+
    "\2\10\1\0\2\10\1\0\1\10\10\0\33\7\5\0\3\7\15\0"+
    "\4\10\7\0\1\7\4\0\13\10\5\0\53\7\37\10\4\0\2\7"+
    "\1\10\143\7\1\0\1\7\10\10\1\0\6\10\2\7\2\10\1\0"+
    "\4\10\2\7\12\10\3\7\2\0\1\7\17\0\1\10\1\7\1\10"+
    "\36\7\33\10\2\0\131\7\13\10\1\7\16\0\12\10\41\7\11\10"+
    "\2\7\4\0\1\7\5\0\26\7\4\10\1\7\11\10\1\7\3\10"+
    "\1\7\5\10\22\0\31\7\3\10\244\0\4\10\66\7\3\10\1\7"+
    "\22\10\1\7\7\10\12\7\2\10\2\0\12\10\1\0\7\7\1\0"+
    "\7\7\1\0\3\10\1\0\10\7\2\0\2\7\2\0\26\7\1\0"+
    "\7\7\1\0\1\7\3\0\4\7\2\0\1\10\1\7\7\10\2\0"+
    "\2\10\2\0\3\10\1\7\10\0\1\10\4\0\2\7\1\0\3\7"+
    "\2\10\2\0\12\10\4\7\7\0\1\7\5\0\3\10\1\0\6\7"+
    "\4\0\2\7\2\0\26\7\1\0\7\7\1\0\2\7\1\0\2\7"+
    "\1\0\2\7\2\0\1\10\1\0\5\10\4\0\2\10\2\0\3\10"+
    "\3\0\1\10\7\0\4\7\1\0\1\7\7\0\14\10\3\7\1\10"+
    "\13\0\3\10\1\0\11\7\1\0\3\7\1\0\26\7\1\0\7\7"+
    "\1\0\2\7\1\0\5\7\2\0\1\10\1\7\10\10\1\0\3\10"+
    "\1\0\3\10\2\0\1\7\17\0\2\7\2\10\2\0\12\10\1\0"+
    "\1\7\17\0\3\10\1\0\10\7\2\0\2\7\2\0\26\7\1\0"+
    "\7\7\1\0\2\7\1\0\5\7\2\0\1\10\1\7\7\10\2\0"+
    "\2\10\2\0\3\10\10\0\2\10\4\0\2\7\1\0\3\7\2\10"+
    "\2\0\12\10\1\0\1\7\20\0\1\10\1\7\1\0\6\7\3\0"+
    "\3\7\1\0\4\7\3\0\2\7\1\0\1\7\1\0\2\7\3\0"+
    "\2\7\3\0\3\7\3\0\14\7\4\0\5\10\3\0\3\10\1\0"+
    "\4\10\2\0\1\7\6\0\1\10\16\0\12\10\11\0\1\7\7\0"+
    "\3\10\1\0\10\7\1\0\3\7\1\0\27\7\1\0\12\7\1\0"+
    "\5\7\3\0\1\7\7\10\1\0\3\10\1\0\4\10\7\0\2\10"+
    "\1\0\2\7\6\0\2\7\2\10\2\0\12\10\22\0\2\10\1\0"+
    "\10\7\1\0\3\7\1\0\27\7\1\0\12\7\1\0\5\7\2\0"+
    "\1\10\1\7\7\10\1\0\3\10\1\0\4\10\7\0\2\10\7\0"+
    "\1\7\1\0\2\7\2\10\2\0\12\10\1\0\2\7\17\0\2\10"+
    "\1\0\10\7\1\0\3\7\1\0\51\7\2\0\1\7\7\10\1\0"+
    "\3\10\1\0\4\10\1\7\10\0\1\10\10\0\2\7\2\10\2\0"+
    "\12\10\12\0\6\7\2\0\2\10\1\0\22\7\3\0\30\7\1\0"+
    "\11\7\1\0\1\7\2\0\7\7\3\0\1\10\4\0\6\10\1\0"+
    "\1\10\1\0\10\10\22\0\2\10\15\0\60\7\1\10\2\7\7\10"+
    "\4\0\10\7\10\10\1\0\12\10\47\0\2\7\1\0\1\7\2\0"+
    "\2\7\1\0\1\7\2\0\1\7\6\0\4\7\1\0\7\7\1\0"+
    "\3\7\1\0\1\7\1\0\1\7\2\0\2\7\1\0\4\7\1\10"+
    "\2\7\6\10\1\0\2\10\1\7\2\0\5\7\1\0\1\7\1\0"+
    "\6\10\2\0\12\10\2\0\2\7\42\0\1\7\27\0\2\10\6\0"+
    "\12\10\13\0\1\10\1\0\1\10\1\0\1\10\4\0\2\10\10\7"+
    "\1\0\44\7\4\0\24\10\1\0\2\10\5\7\13\10\1\0\44\10"+
    "\11\0\1\10\71\0\53\7\24\10\1\7\12\10\6\0\6\7\4\10"+
    "\4\7\3\10\1\7\3\10\2\7\7\10\3\7\4\10\15\7\14\10"+
    "\1\7\17\10\2\0\46\7\12\0\53\7\1\0\1\7\3\0\u0149\7"+
    "\1\0\4\7\2\0\7\7\1\0\1\7\1\0\4\7\2\0\51\7"+
    "\1\0\4\7\2\0\41\7\1\0\4\7\2\0\7\7\1\0\1\7"+
    "\1\0\4\7\2\0\17\7\1\0\71\7\1\0\4\7\2\0\103\7"+
    "\2\0\3\10\40\0\20\7\20\0\125\7\14\0\u026c\7\2\0\21\7"+
    "\1\0\32\7\5\0\113\7\3\0\3\7\17\0\15\7\1\0\4\7"+
    "\3\10\13\0\22\7\3\10\13\0\22\7\2\10\14\0\15\7\1\0"+
    "\3\7\1\0\2\10\14\0\64\7\40\10\3\0\1\7\3\0\2\7"+
    "\1\10\2\0\12\10\41\0\3\10\2\0\12\10\6\0\130\7\10\0"+
    "\51\7\1\10\1\7\5\0\106\7\12\0\35\7\3\0\14\10\4\0"+
    "\14\10\12\0\12\10\36\7\2\0\5\7\13\0\54\7\4\0\21\10"+
    "\7\7\2\10\6\0\12\10\46\0\27\7\5\10\4\0\65\7\12\10"+
    "\1\0\35\10\2\0\13\10\6\0\12\10\15\0\1\7\130\0\5\10"+
    "\57\7\21\10\7\7\4\0\12\10\21\0\11\10\14\0\3\10\36\7"+
    "\12\10\3\0\2\7\12\10\6\0\46\7\16\10\14\0\44\7\24\10"+
    "\10\0\12\10\3\0\3\7\12\10\44\7\122\0\3\10\1\0\25\10"+
    "\4\7\1\10\4\7\1\10\15\0\300\7\47\10\25\0\4\10\u0116\7"+
    "\2\0\6\7\2\0\46\7\2\0\6\7\2\0\10\7\1\0\1\7"+
    "\1\0\1\7\1\0\1\7\1\0\37\7\2\0\65\7\1\0\7\7"+
    "\1\0\1\7\3\0\3\7\1\0\7\7\3\0\4\7\2\0\6\7"+
    "\4\0\15\7\5\0\3\7\1\0\7\7\16\0\5\10\32\0\5\10"+
    "\20\0\2\7\23\0\1\7\13\0\5\10\5\0\6\10\1\0\1\7"+
    "\15\0\1\7\20\0\15\7\3\0\32\7\26\0\15\10\4\0\1\10"+
    "\3\0\14\10\21\0\1\7\4\0\1\7\2\0\12\7\1\0\1\7"+
    "\3\0\5\7\6\0\1\7\1\0\1\7\1\0\1\7\1\0\4\7"+
    "\1\0\13\7\2\0\4\7\5\0\5\7\4\0\1\7\21\0\51\7"+
    "\u0a77\0\57\7\1\0\57\7\1\0\205\7\6\0\4\7\3\10\16\0"+
    "\46\7\12\0\66\7\11\0\1\7\17\0\1\10\27\7\11\0\7\7"+
    "\1\0\7\7\1\0\7\7\1\0\7\7\1\0\7\7\1\0\7\7"+
    "\1\0\7\7\1\0\7\7\1\0\40\10\57\0\1\7\u01d5\0\3\7"+
    "\31\0\11\7\6\10\1\0\5\7\2\0\5\7\4\0\126\7\2\0"+
    "\2\10\2\0\3\7\1\0\132\7\1\0\4\7\5\0\51\7\3\0"+
    "\136\7\21\0\33\7\65\0\20\7\u0200\0\u19b6\7\112\0\u51cc\7\64\0"+
    "\u048d\7\103\0\56\7\2\0\u010d\7\3\0\20\7\12\10\2\7\24\0"+
    "\57\7\1\10\14\0\2\10\1\0\31\7\10\0\120\7\2\10\45\0"+
    "\11\7\2\0\147\7\2\0\4\7\1\0\2\7\16\0\12\7\120\0"+
    "\10\7\1\10\3\7\1\10\4\7\1\10\27\7\5\10\20\0\1\7"+
    "\7\0\64\7\14\0\2\10\62\7\21\10\13\0\12\10\6\0\22\10"+
    "\6\7\3\0\1\7\4\0\12\10\34\7\10\10\2\0\27\7\15\10"+
    "\14\0\35\7\3\0\4\10\57\7\16\10\16\0\1\7\12\10\46\0"+
    "\51\7\16\10\11\0\3\7\1\10\10\7\2\10\2\0\12\10\6\0"+
    "\27\7\3\0\1\7\1\10\4\0\60\7\1\10\1\7\3\10\2\7"+
    "\2\10\5\7\2\10\1\7\1\10\1\7\30\0\3\7\43\0\6\7"+
    "\2\0\6\7\2\0\6\7\11\0\7\7\1\0\7\7\221\0\43\7"+
    "\10\10\1\0\2\10\2\0\12\10\6\0\u2ba4\7\14\0\27\7\4\0"+
    "\61\7\u2104\0\u012e\7\2\0\76\7\2\0\152\7\46\0\7\7\14\0"+
    "\5\7\5\0\1\7\1\10\12\7\1\0\15\7\1\0\5\7\1\0"+
    "\1\7\1\0\2\7\1\0\2\7\1\0\154\7\41\0\u016b\7\22\0"+
    "\100\7\2\0\66\7\50\0\15\7\3\0\20\10\20\0\7\10\14\0"+
    "\2\7\30\0\3\7\31\0\1\7\6\0\5\7\1\0\207\7\2\0"+
    "\1\10\4\0\1\7\13\0\12\10\7\0\32\7\4\0\1\7\1\0"+
    "\32\7\13\0\131\7\3\0\6\7\2\0\6\7\2\0\6\7\2\0"+
    "\3\7\3\0\2\7\3\0\2\7\22\0\3\10\4\0";

  /** 
   * Translates characters to character classes
   */
  private static final char [] ZZ_CMAP = zzUnpackCMap(ZZ_CMAP_PACKED);

  /** 
   * Translates DFA states to action switch labels.
   */
  private static final int [] ZZ_ACTION = zzUnpackAction();

  private static final String ZZ_ACTION_PACKED_0 =
    "\3\0\1\1\2\2\1\3\1\4\1\5\1\6\2\7"+
    "\1\10\1\6\1\11\20\6\1\12\1\13\1\14\1\15"+
    "\1\16\1\17\1\1\1\20\1\21\1\22\1\23\1\24"+
    "\1\25\1\26\1\25\1\27\2\0\1\30\5\6\1\31"+
    "\1\32\1\33\11\6\1\34\11\6\1\35\10\6\1\36"+
    "\2\0\1\37\1\40\1\41\1\42\1\43\1\44\1\45"+
    "\1\46\3\0\3\6\1\47\1\6\1\50\25\6\1\51"+
    "\10\6\4\0\2\30\1\52\2\6\1\53\6\6\1\54"+
    "\4\6\1\55\1\56\5\6\1\57\7\6\1\60\2\6"+
    "\1\61\1\0\1\62\1\63\10\6\1\64\2\6\1\65"+
    "\2\6\1\66\1\67\1\6\1\70\1\71\3\6\1\72"+
    "\1\6\1\73\2\6\1\74\1\75\1\76\1\77\2\6"+
    "\1\100\3\6\1\101\5\6\1\102\1\103\1\104\2\6"+
    "\1\105\1\106\1\107\1\6\1\110\2\6\1\111\1\112"+
    "\1\6\1\113\2\6\1\114";

  private static int [] zzUnpackAction() {
    int [] result = new int[241];
    int offset = 0;
    offset = zzUnpackAction(ZZ_ACTION_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAction(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /** 
   * Translates a state to a row index in the transition table
   */
  private static final int [] ZZ_ROWMAP = zzUnpackRowMap();

  private static final String ZZ_ROWMAP_PACKED_0 =
    "\0\0\0\62\0\144\0\226\0\310\0\226\0\372\0\226"+
    "\0\u012c\0\u015e\0\u0190\0\u01c2\0\226\0\u01f4\0\226\0\u0226"+
    "\0\u0258\0\u028a\0\u02bc\0\u02ee\0\u0320\0\u0352\0\u0384\0\u03b6"+
    "\0\u03e8\0\u041a\0\u044c\0\u047e\0\u04b0\0\u04e2\0\u0514\0\226"+
    "\0\u0546\0\226\0\226\0\226\0\226\0\u0578\0\u05aa\0\u05dc"+
    "\0\226\0\226\0\226\0\u060e\0\u0640\0\u0672\0\u06a4\0\u06d6"+
    "\0\u0708\0\u073a\0\u076c\0\u079e\0\u07d0\0\u0802\0\u0834\0\u0866"+
    "\0\u015e\0\u015e\0\u0898\0\u08ca\0\u08fc\0\u092e\0\u0960\0\u0992"+
    "\0\u09c4\0\u09f6\0\u0a28\0\u015e\0\u0a5a\0\u0a8c\0\u0abe\0\u0af0"+
    "\0\u0b22\0\u0b54\0\u0b86\0\u0bb8\0\u0bea\0\u015e\0\u0c1c\0\u0c4e"+
    "\0\u0c80\0\u0cb2\0\u0ce4\0\u0d16\0\u0d48\0\u0d7a\0\u015e\0\u0dac"+
    "\0\u0dde\0\226\0\226\0\226\0\226\0\226\0\226\0\226"+
    "\0\226\0\u0e10\0\u0e42\0\u0e74\0\u0ea6\0\u0ed8\0\u0f0a\0\u015e"+
    "\0\u0f3c\0\u015e\0\u0f6e\0\u0fa0\0\u0fd2\0\u1004\0\u1036\0\u1068"+
    "\0\u109a\0\u10cc\0\u10fe\0\u1130\0\u1162\0\u1194\0\u11c6\0\u11f8"+
    "\0\u122a\0\u125c\0\u128e\0\u12c0\0\u12f2\0\u1324\0\u1356\0\u015e"+
    "\0\u1388\0\u13ba\0\u13ec\0\u141e\0\u1450\0\u1482\0\u14b4\0\u14e6"+
    "\0\u1518\0\u154a\0\u157c\0\u15ae\0\226\0\u15e0\0\u015e\0\u1612"+
    "\0\u1644\0\u015e\0\u1676\0\u16a8\0\u16da\0\u170c\0\u173e\0\u1770"+
    "\0\u015e\0\u17a2\0\u17d4\0\u1806\0\u1838\0\u015e\0\u015e\0\u186a"+
    "\0\u189c\0\u18ce\0\u1900\0\u1932\0\u015e\0\u1964\0\u1996\0\u19c8"+
    "\0\u19fa\0\u1a2c\0\u1a5e\0\u1a90\0\u015e\0\u1ac2\0\u1af4\0\226"+
    "\0\u1b26\0\u015e\0\u015e\0\u1b58\0\u1b8a\0\u1bbc\0\u1bee\0\u1c20"+
    "\0\u1c52\0\u1c84\0\u1cb6\0\u015e\0\u1ce8\0\u1d1a\0\u015e\0\u1d4c"+
    "\0\u1d7e\0\u015e\0\u015e\0\u1db0\0\u015e\0\u015e\0\u1de2\0\u1e14"+
    "\0\u1e46\0\u015e\0\u1e78\0\226\0\u1eaa\0\u1edc\0\u015e\0\u015e"+
    "\0\u015e\0\u015e\0\u1f0e\0\u1f40\0\u015e\0\u1f72\0\u1fa4\0\u1fd6"+
    "\0\u015e\0\u2008\0\u203a\0\u206c\0\u209e\0\u20d0\0\u015e\0\u015e"+
    "\0\u015e\0\u2102\0\u2134\0\u015e\0\u015e\0\u015e\0\u2166\0\u015e"+
    "\0\u2198\0\u21ca\0\u015e\0\u015e\0\u21fc\0\u015e\0\u222e\0\u2260"+
    "\0\u015e";

  private static int [] zzUnpackRowMap() {
    int [] result = new int[241];
    int offset = 0;
    offset = zzUnpackRowMap(ZZ_ROWMAP_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackRowMap(String packed, int offset, int [] result) {
    int i = 0;  /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int high = packed.charAt(i++) << 16;
      result[j++] = high | packed.charAt(i++);
    }
    return j;
  }

  /** 
   * The transition table of the DFA
   */
  private static final int [] ZZ_TRANS = zzUnpackTrans();

  private static final String ZZ_TRANS_PACKED_0 =
    "\1\4\1\5\2\6\1\7\1\10\1\11\1\12\1\4"+
    "\1\13\1\14\1\15\1\16\1\17\1\20\1\21\1\22"+
    "\1\23\1\24\1\25\1\26\1\27\1\30\1\12\1\31"+
    "\1\32\1\33\1\34\1\35\1\12\1\36\1\12\1\37"+
    "\3\12\1\40\1\41\1\42\1\43\1\44\1\45\1\46"+
    "\1\47\1\50\1\51\1\52\1\53\1\12\1\4\1\54"+
    "\2\4\42\54\1\55\14\54\1\56\2\4\41\56\1\57"+
    "\14\56\1\4\64\0\1\6\64\0\1\60\62\0\1\61"+
    "\62\0\4\12\1\0\1\12\1\0\26\12\13\0\2\12"+
    "\14\0\1\62\57\0\2\14\1\62\55\0\4\12\1\0"+
    "\1\12\1\0\1\12\1\63\24\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\12\1\0\1\12\1\64\24\12\13\0"+
    "\2\12\10\0\4\12\1\0\1\65\1\0\1\66\2\12"+
    "\1\67\22\12\13\0\2\12\10\0\4\12\1\0\1\12"+
    "\1\0\1\12\1\70\2\12\1\71\21\12\13\0\2\12"+
    "\10\0\4\12\1\0\1\12\1\0\12\12\1\72\6\12"+
    "\1\73\1\74\3\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\75\1\0\7\12\1\76\16\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\12\1\0\2\12\1\77\23\12\13\0"+
    "\2\12\10\0\4\12\1\0\1\12\1\0\3\12\1\100"+
    "\1\12\1\101\4\12\1\102\2\12\1\103\10\12\13\0"+
    "\2\12\10\0\4\12\1\0\1\12\1\0\3\12\1\104"+
    "\6\12\1\105\13\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\12\1\0\11\12\1\106\14\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\107\1\0\1\110\25\12\13\0\2\12"+
    "\10\0\4\12\1\0\1\12\1\0\5\12\1\111\4\12"+
    "\1\112\2\12\1\113\10\12\13\0\2\12\10\0\4\12"+
    "\1\0\1\12\1\0\3\12\1\114\22\12\13\0\2\12"+
    "\10\0\4\12\1\0\1\12\1\0\1\12\1\115\2\12"+
    "\1\116\1\117\4\12\1\120\13\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\121\1\0\1\122\1\12\1\123\7\12"+
    "\1\124\13\12\13\0\2\12\10\0\4\12\1\0\1\12"+
    "\1\0\12\12\1\125\13\12\13\0\2\12\10\0\4\12"+
    "\1\0\1\126\1\0\23\12\1\127\2\12\13\0\2\12"+
    "\1\0\1\130\2\0\42\130\1\131\14\130\51\0\1\132"+
    "\61\0\1\133\61\0\1\134\10\0\1\54\2\0\42\54"+
    "\1\0\14\54\25\0\1\135\6\0\1\136\10\0\1\137"+
    "\12\0\1\140\1\0\1\56\2\0\41\56\1\0\14\56"+
    "\45\0\1\141\15\0\5\142\1\143\54\142\1\61\1\5"+
    "\1\6\57\61\11\0\2\62\1\0\1\144\54\0\4\12"+
    "\1\0\1\12\1\0\1\145\25\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\12\1\0\2\12\1\146\13\12\1\147"+
    "\7\12\13\0\2\12\10\0\4\12\1\0\1\12\1\0"+
    "\10\12\1\150\15\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\12\1\0\5\12\1\151\20\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\12\1\0\7\12\1\152\16\12\13\0"+
    "\2\12\10\0\4\12\1\0\1\12\1\0\4\12\1\153"+
    "\21\12\13\0\2\12\10\0\4\12\1\0\1\12\1\0"+
    "\7\12\1\154\16\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\12\1\0\25\12\1\155\13\0\2\12\10\0\4\12"+
    "\1\0\1\12\1\0\5\12\1\156\20\12\13\0\2\12"+
    "\10\0\4\12\1\0\1\12\1\0\12\12\1\157\2\12"+
    "\1\160\10\12\13\0\2\12\10\0\4\12\1\0\1\12"+
    "\1\0\17\12\1\161\6\12\13\0\2\12\10\0\4\12"+
    "\1\0\1\12\1\0\1\12\1\162\12\12\1\163\11\12"+
    "\13\0\2\12\10\0\4\12\1\0\1\12\1\0\15\12"+
    "\1\164\10\12\13\0\2\12\10\0\4\12\1\0\1\165"+
    "\1\0\26\12\13\0\2\12\10\0\4\12\1\0\1\12"+
    "\1\0\4\12\1\166\21\12\13\0\2\12\10\0\4\12"+
    "\1\0\1\12\1\0\1\167\14\12\1\170\10\12\13\0"+
    "\2\12\10\0\4\12\1\0\1\171\1\0\26\12\13\0"+
    "\2\12\10\0\4\12\1\0\1\12\1\0\5\12\1\172"+
    "\20\12\13\0\2\12\10\0\4\12\1\0\1\12\1\0"+
    "\1\12\1\173\24\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\12\1\0\15\12\1\174\10\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\12\1\0\3\12\1\175\22\12\13\0"+
    "\2\12\10\0\4\12\1\0\1\12\1\0\5\12\1\176"+
    "\20\12\13\0\2\12\10\0\4\12\1\0\1\12\1\0"+
    "\16\12\1\177\7\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\12\1\0\16\12\1\200\7\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\12\1\0\7\12\1\201\16\12\13\0"+
    "\2\12\10\0\4\12\1\0\1\12\1\0\12\12\1\202"+
    "\13\12\13\0\2\12\10\0\4\12\1\0\1\12\1\0"+
    "\13\12\1\203\12\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\12\1\0\12\12\1\204\13\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\12\1\0\4\12\1\205\21\12\13\0"+
    "\2\12\10\0\4\12\1\0\1\12\1\0\3\12\1\206"+
    "\22\12\13\0\2\12\10\0\4\12\1\0\1\12\1\0"+
    "\3\12\1\207\22\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\12\1\0\7\12\1\210\16\12\13\0\2\12\46\0"+
    "\1\211\61\0\1\212\14\0\5\142\1\213\54\142\4\0"+
    "\1\6\1\143\62\0\1\214\2\0\1\215\1\216\2\0"+
    "\1\214\53\0\4\12\1\0\1\12\1\0\14\12\1\217"+
    "\11\12\13\0\2\12\10\0\4\12\1\0\1\12\1\0"+
    "\3\12\1\220\22\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\221\1\0\26\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\12\1\0\5\12\1\222\20\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\12\1\0\7\12\1\223\16\12\13\0"+
    "\2\12\10\0\4\12\1\0\1\12\1\0\2\12\1\224"+
    "\23\12\13\0\2\12\10\0\4\12\1\0\1\225\1\0"+
    "\26\12\13\0\2\12\10\0\4\12\1\0\1\226\1\0"+
    "\26\12\13\0\2\12\10\0\4\12\1\0\1\227\1\0"+
    "\26\12\13\0\2\12\10\0\4\12\1\0\1\12\1\0"+
    "\7\12\1\230\16\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\231\1\0\26\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\12\1\0\13\12\1\232\12\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\12\1\0\14\12\1\233\11\12\13\0"+
    "\2\12\10\0\4\12\1\0\1\12\1\0\4\12\1\234"+
    "\21\12\13\0\2\12\10\0\4\12\1\0\1\12\1\0"+
    "\15\12\1\235\10\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\12\1\0\7\12\1\236\16\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\237\1\0\26\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\12\1\0\1\12\1\240\24\12\13\0"+
    "\2\12\10\0\4\12\1\0\1\12\1\0\12\12\1\241"+
    "\13\12\13\0\2\12\10\0\4\12\1\0\1\12\1\0"+
    "\15\12\1\242\10\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\12\1\0\1\12\1\243\24\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\12\1\0\20\12\1\244\5\12\13\0"+
    "\2\12\10\0\4\12\1\0\1\12\1\0\14\12\1\245"+
    "\11\12\13\0\2\12\10\0\4\12\1\0\1\12\1\0"+
    "\4\12\1\246\21\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\12\1\0\2\12\1\247\23\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\250\1\0\26\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\12\1\0\15\12\1\251\10\12\13\0"+
    "\2\12\10\0\4\12\1\0\1\12\1\0\15\12\1\252"+
    "\10\12\13\0\2\12\10\0\4\12\1\0\1\12\1\0"+
    "\15\12\1\253\10\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\12\1\0\7\12\1\254\16\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\12\1\0\21\12\1\255\4\12\13\0"+
    "\2\12\10\0\4\12\1\0\1\12\1\0\1\256\25\12"+
    "\13\0\2\12\10\0\4\12\1\0\1\12\1\0\10\12"+
    "\1\257\15\12\13\0\2\12\25\0\1\260\102\0\1\261"+
    "\14\0\4\142\1\6\1\213\54\142\11\0\1\215\1\216"+
    "\60\0\2\216\56\0\4\12\1\0\1\12\1\0\1\12"+
    "\1\262\24\12\13\0\2\12\10\0\4\12\1\0\1\12"+
    "\1\0\12\12\1\263\13\12\13\0\2\12\10\0\4\12"+
    "\1\0\1\12\1\0\15\12\1\264\10\12\13\0\2\12"+
    "\10\0\4\12\1\0\1\12\1\0\3\12\1\265\22\12"+
    "\13\0\2\12\10\0\4\12\1\0\1\12\1\0\6\12"+
    "\1\266\17\12\13\0\2\12\10\0\4\12\1\0\1\12"+
    "\1\0\6\12\1\267\17\12\13\0\2\12\10\0\4\12"+
    "\1\0\1\12\1\0\15\12\1\270\10\12\13\0\2\12"+
    "\10\0\4\12\1\0\1\12\1\0\2\12\1\271\23\12"+
    "\13\0\2\12\10\0\4\12\1\0\1\12\1\0\2\12"+
    "\1\272\23\12\13\0\2\12\10\0\4\12\1\0\1\273"+
    "\1\0\26\12\13\0\2\12\10\0\4\12\1\0\1\12"+
    "\1\0\4\12\1\274\21\12\13\0\2\12\10\0\4\12"+
    "\1\0\1\12\1\0\7\12\1\275\16\12\13\0\2\12"+
    "\10\0\4\12\1\0\1\12\1\0\4\12\1\276\21\12"+
    "\13\0\2\12\10\0\4\12\1\0\1\277\1\0\26\12"+
    "\13\0\2\12\10\0\4\12\1\0\1\12\1\0\7\12"+
    "\1\300\16\12\13\0\2\12\10\0\4\12\1\0\1\12"+
    "\1\0\2\12\1\301\23\12\13\0\2\12\10\0\4\12"+
    "\1\0\1\12\1\0\4\12\1\302\21\12\13\0\2\12"+
    "\10\0\4\12\1\0\1\303\1\0\26\12\13\0\2\12"+
    "\10\0\4\12\1\0\1\12\1\0\13\12\1\304\12\12"+
    "\13\0\2\12\10\0\4\12\1\0\1\12\1\0\12\12"+
    "\1\305\13\12\13\0\2\12\10\0\4\12\1\0\1\12"+
    "\1\0\23\12\1\306\2\12\13\0\2\12\10\0\4\12"+
    "\1\0\1\12\1\0\1\307\25\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\12\1\0\22\12\1\310\3\12\13\0"+
    "\2\12\10\0\4\12\1\0\1\12\1\0\2\12\1\311"+
    "\23\12\13\0\2\12\10\0\4\12\1\0\1\12\1\0"+
    "\21\12\1\312\4\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\313\1\0\26\12\13\0\2\12\25\0\1\314\44\0"+
    "\4\12\1\0\1\12\1\0\1\12\1\315\24\12\13\0"+
    "\2\12\10\0\4\12\1\0\1\12\1\0\1\12\1\316"+
    "\24\12\13\0\2\12\10\0\4\12\1\0\1\12\1\0"+
    "\7\12\1\317\16\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\12\1\0\7\12\1\320\16\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\12\1\0\14\12\1\321\11\12\13\0"+
    "\2\12\10\0\4\12\1\0\1\12\1\0\6\12\1\322"+
    "\17\12\13\0\2\12\10\0\4\12\1\0\1\12\1\0"+
    "\12\12\1\323\13\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\12\1\0\1\12\1\324\24\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\325\1\0\26\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\12\1\0\2\12\1\326\23\12\13\0"+
    "\2\12\10\0\4\12\1\0\1\12\1\0\2\12\1\327"+
    "\23\12\13\0\2\12\10\0\4\12\1\0\1\12\1\0"+
    "\1\12\1\330\24\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\12\1\0\23\12\1\331\2\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\12\1\0\5\12\1\332\20\12\13\0"+
    "\2\12\10\0\4\12\1\0\1\12\1\0\5\12\1\333"+
    "\20\12\13\0\2\12\10\0\4\12\1\0\1\12\1\0"+
    "\1\12\1\334\24\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\335\1\0\26\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\12\1\0\7\12\1\336\16\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\12\1\0\4\12\1\337\21\12\13\0"+
    "\2\12\10\0\4\12\1\0\1\12\1\0\14\12\1\340"+
    "\11\12\13\0\2\12\10\0\4\12\1\0\1\12\1\0"+
    "\7\12\1\341\16\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\342\1\0\26\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\12\1\0\24\12\1\343\1\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\12\1\0\20\12\1\344\5\12\13\0"+
    "\2\12\10\0\4\12\1\0\1\12\1\0\7\12\1\345"+
    "\16\12\13\0\2\12\10\0\4\12\1\0\1\346\1\0"+
    "\26\12\13\0\2\12\10\0\4\12\1\0\1\12\1\0"+
    "\6\12\1\347\17\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\12\1\0\1\12\1\350\24\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\12\1\0\2\12\1\351\23\12\13\0"+
    "\2\12\10\0\4\12\1\0\1\12\1\0\1\12\1\352"+
    "\24\12\13\0\2\12\10\0\4\12\1\0\1\353\1\0"+
    "\26\12\13\0\2\12\10\0\4\12\1\0\1\12\1\0"+
    "\7\12\1\354\16\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\12\1\0\15\12\1\355\10\12\13\0\2\12\10\0"+
    "\4\12\1\0\1\12\1\0\7\12\1\356\16\12\13\0"+
    "\2\12\10\0\4\12\1\0\1\12\1\0\22\12\1\357"+
    "\3\12\13\0\2\12\10\0\4\12\1\0\1\12\1\0"+
    "\5\12\1\360\20\12\13\0\2\12\10\0\4\12\1\0"+
    "\1\361\1\0\26\12\13\0\2\12\1\0";

  private static int [] zzUnpackTrans() {
    int [] result = new int[8850];
    int offset = 0;
    offset = zzUnpackTrans(ZZ_TRANS_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackTrans(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      value--;
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /* error codes */
  private static final int ZZ_UNKNOWN_ERROR = 0;
  private static final int ZZ_NO_MATCH = 1;
  private static final int ZZ_PUSHBACK_2BIG = 2;

  /* error messages for the codes above */
  private static final String ZZ_ERROR_MSG[] = {
    "Unkown internal scanner error",
    "Error: could not match input",
    "Error: pushback value was too large"
  };

  /**
   * ZZ_ATTRIBUTE[aState] contains the attributes of state <code>aState</code>
   */
  private static final int [] ZZ_ATTRIBUTE = zzUnpackAttribute();

  private static final String ZZ_ATTRIBUTE_PACKED_0 =
    "\3\0\1\11\1\1\1\11\1\1\1\11\4\1\1\11"+
    "\1\1\1\11\20\1\1\11\1\1\4\11\3\1\3\11"+
    "\4\1\2\0\46\1\2\0\10\11\3\0\44\1\4\0"+
    "\1\11\42\1\1\11\1\0\32\1\1\11\45\1";

  private static int [] zzUnpackAttribute() {
    int [] result = new int[241];
    int offset = 0;
    offset = zzUnpackAttribute(ZZ_ATTRIBUTE_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAttribute(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }

  /** the input device */
  private java.io.Reader zzReader;

  /** the current state of the DFA */
  private int zzState;

  /** the current lexical state */
  private int zzLexicalState = YYINITIAL;

  /** this buffer contains the current text to be matched and is
      the source of the yytext() string */
  private char zzBuffer[] = new char[ZZ_BUFFERSIZE];

  /** the textposition at the last accepting state */
  private int zzMarkedPos;

  /** the current text position in the buffer */
  private int zzCurrentPos;

  /** startRead marks the beginning of the yytext() string in the buffer */
  private int zzStartRead;

  /** endRead marks the last character in the buffer, that has been read
      from input */
  private int zzEndRead;

  /** number of newlines encountered up to the start of the matched text */
  private int yyline;

  /** the number of characters up to the start of the matched text */
  private int yychar;

  /**
   * the number of characters from the last newline up to the start of the 
   * matched text
   */
  private int yycolumn;

  /** 
   * zzAtBOL == true <=> the scanner is currently at the beginning of a line
   */
  private boolean zzAtBOL = true;

  /** zzAtEOF == true <=> the scanner is at the EOF */
  private boolean zzAtEOF;

  /** denotes if the user-EOF-code has already been executed */
  private boolean zzEOFDone;

  /* user code: */
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


  /**
   * Creates a new scanner
   * There is also a java.io.InputStream version of this constructor.
   *
   * @param   in  the java.io.Reader to read input from.
   */
  Lexer(java.io.Reader in) {
    this.zzReader = in;
  }

  /**
   * Creates a new scanner.
   * There is also java.io.Reader version of this constructor.
   *
   * @param   in  the java.io.Inputstream to read input from.
   */
  Lexer(java.io.InputStream in) {
    this(new java.io.InputStreamReader(in));
  }

  /** 
   * Unpacks the compressed character translation table.
   *
   * @param packed   the packed character translation table
   * @return         the unpacked character translation table
   */
  private static char [] zzUnpackCMap(String packed) {
    char [] map = new char[0x10000];
    int i = 0;  /* index in packed string  */
    int j = 0;  /* index in unpacked array */
    while (i < 2256) {
      int  count = packed.charAt(i++);
      char value = packed.charAt(i++);
      do map[j++] = value; while (--count > 0);
    }
    return map;
  }


  /**
   * Refills the input buffer.
   *
   * @return      <code>false</code>, iff there was new input.
   * 
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  private boolean zzRefill() throws java.io.IOException {

    /* first: make room (if you can) */
    if (zzStartRead > 0) {
      System.arraycopy(zzBuffer, zzStartRead,
                       zzBuffer, 0,
                       zzEndRead-zzStartRead);

      /* translate stored positions */
      zzEndRead-= zzStartRead;
      zzCurrentPos-= zzStartRead;
      zzMarkedPos-= zzStartRead;
      zzStartRead = 0;
    }

    /* is the buffer big enough? */
    if (zzCurrentPos >= zzBuffer.length) {
      /* if not: blow it up */
      char newBuffer[] = new char[zzCurrentPos*2];
      System.arraycopy(zzBuffer, 0, newBuffer, 0, zzBuffer.length);
      zzBuffer = newBuffer;
    }

    /* finally: fill the buffer with new input */
    int numRead = zzReader.read(zzBuffer, zzEndRead,
                                            zzBuffer.length-zzEndRead);

    if (numRead > 0) {
      zzEndRead+= numRead;
      return false;
    }
    // unlikely but not impossible: read 0 characters, but not at end of stream    
    if (numRead == 0) {
      int c = zzReader.read();
      if (c == -1) {
        return true;
      } else {
        zzBuffer[zzEndRead++] = (char) c;
        return false;
      }     
    }

	// numRead < 0
    return true;
  }

    
  /**
   * Closes the input stream.
   */
  public final void yyclose() throws java.io.IOException {
    zzAtEOF = true;            /* indicate end of file */
    zzEndRead = zzStartRead;  /* invalidate buffer    */

    if (zzReader != null)
      zzReader.close();
  }


  /**
   * Resets the scanner to read from a new input stream.
   * Does not close the old reader.
   *
   * All internal variables are reset, the old input stream 
   * <b>cannot</b> be reused (internal buffer is discarded and lost).
   * Lexical state is set to <code>ZZ_INITIAL</code>.
   *
   * @param reader   the new input stream 
   */
  public final void yyreset(java.io.Reader reader) {
    zzReader = reader;
    zzAtBOL  = true;
    zzAtEOF  = false;
    zzEOFDone = false;
    zzEndRead = zzStartRead = 0;
    zzCurrentPos = zzMarkedPos = 0;
    yyline = yychar = yycolumn = 0;
    zzLexicalState = YYINITIAL;
  }


  /**
   * Returns the current lexical state.
   */
  public final int yystate() {
    return zzLexicalState;
  }


  /**
   * Enters a new lexical state
   *
   * @param newState the new lexical state
   */
  public final void yybegin(int newState) {
    zzLexicalState = newState;
  }


  /**
   * Returns the text matched by the current regular expression.
   */
  public final String yytext() {
    return new String( zzBuffer, zzStartRead, zzMarkedPos-zzStartRead );
  }


  /**
   * Returns the character at position <code>pos</code> from the
   * matched text. 
   * 
   * It is equivalent to yytext().charAt(pos), but faster
   *
   * @param pos the position of the character to fetch. 
   *            A value from 0 to yylength()-1.
   *
   * @return the character at position pos
   */
  public final char yycharat(int pos) {
    return zzBuffer[zzStartRead+pos];
  }


  /**
   * Returns the length of the matched text region.
   */
  public final int yylength() {
    return zzMarkedPos-zzStartRead;
  }


  /**
   * Reports an error that occured while scanning.
   *
   * In a wellformed scanner (no or only correct usage of 
   * yypushback(int) and a match-all fallback rule) this method 
   * will only be called with things that "Can't Possibly Happen".
   * If this method is called, something is seriously wrong
   * (e.g. a JFlex bug producing a faulty scanner etc.).
   *
   * Usual syntax/scanner level error handling should be done
   * in error fallback rules.
   *
   * @param   errorCode  the code of the errormessage to display
   */
  private void zzScanError(int errorCode) {
    String message;
    try {
      message = ZZ_ERROR_MSG[errorCode];
    }
    catch (ArrayIndexOutOfBoundsException e) {
      message = ZZ_ERROR_MSG[ZZ_UNKNOWN_ERROR];
    }

    throw new Error(message);
  } 


  /**
   * Pushes the specified amount of characters back into the input stream.
   *
   * They will be read again by then next call of the scanning method
   *
   * @param number  the number of characters to be read again.
   *                This number must not be greater than yylength()!
   */
  public void yypushback(int number)  {
    if ( number > yylength() )
      zzScanError(ZZ_PUSHBACK_2BIG);

    zzMarkedPos -= number;
  }


  /**
   * Contains user EOF-code, which will be executed exactly once,
   * when the end of file is reached
   */
  private void zzDoEOF() throws java.io.IOException {
    if (!zzEOFDone) {
      zzEOFDone = true;
      yyclose();
    }
  }


  /**
   * Resumes scanning until the next regular expression is matched,
   * the end of input is encountered or an I/O-Error occurs.
   *
   * @return      the next token
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  public java_cup.runtime.Symbol next_token() throws java.io.IOException {
    int zzInput;
    int zzAction;

    // cached fields:
    int zzCurrentPosL;
    int zzMarkedPosL;
    int zzEndReadL = zzEndRead;
    char [] zzBufferL = zzBuffer;
    char [] zzCMapL = ZZ_CMAP;

    int [] zzTransL = ZZ_TRANS;
    int [] zzRowMapL = ZZ_ROWMAP;
    int [] zzAttrL = ZZ_ATTRIBUTE;

    while (true) {
      zzMarkedPosL = zzMarkedPos;

      boolean zzR = false;
      for (zzCurrentPosL = zzStartRead; zzCurrentPosL < zzMarkedPosL;
                                                             zzCurrentPosL++) {
        switch (zzBufferL[zzCurrentPosL]) {
        case '\u000B':
        case '\u000C':
        case '\u0085':
        case '\u2028':
        case '\u2029':
          yyline++;
          yycolumn = 0;
          zzR = false;
          break;
        case '\r':
          yyline++;
          yycolumn = 0;
          zzR = true;
          break;
        case '\n':
          if (zzR)
            zzR = false;
          else {
            yyline++;
            yycolumn = 0;
          }
          break;
        default:
          zzR = false;
          yycolumn++;
        }
      }

      if (zzR) {
        // peek one character ahead if it is \n (if we have counted one line too much)
        boolean zzPeek;
        if (zzMarkedPosL < zzEndReadL)
          zzPeek = zzBufferL[zzMarkedPosL] == '\n';
        else if (zzAtEOF)
          zzPeek = false;
        else {
          boolean eof = zzRefill();
          zzEndReadL = zzEndRead;
          zzMarkedPosL = zzMarkedPos;
          zzBufferL = zzBuffer;
          if (eof) 
            zzPeek = false;
          else 
            zzPeek = zzBufferL[zzMarkedPosL] == '\n';
        }
        if (zzPeek) yyline--;
      }
      zzAction = -1;

      zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;
  
      zzState = ZZ_LEXSTATE[zzLexicalState];


      zzForAction: {
        while (true) {
    
          if (zzCurrentPosL < zzEndReadL)
            zzInput = zzBufferL[zzCurrentPosL++];
          else if (zzAtEOF) {
            zzInput = YYEOF;
            break zzForAction;
          }
          else {
            // store back cached positions
            zzCurrentPos  = zzCurrentPosL;
            zzMarkedPos   = zzMarkedPosL;
            boolean eof = zzRefill();
            // get translated positions and possibly new buffer
            zzCurrentPosL  = zzCurrentPos;
            zzMarkedPosL   = zzMarkedPos;
            zzBufferL      = zzBuffer;
            zzEndReadL     = zzEndRead;
            if (eof) {
              zzInput = YYEOF;
              break zzForAction;
            }
            else {
              zzInput = zzBufferL[zzCurrentPosL++];
            }
          }
          int zzNext = zzTransL[ zzRowMapL[zzState] + zzCMapL[zzInput] ];
          if (zzNext == -1) break zzForAction;
          zzState = zzNext;

          int zzAttributes = zzAttrL[zzState];
          if ( (zzAttributes & 1) == 1 ) {
            zzAction = zzState;
            zzMarkedPosL = zzCurrentPosL;
            if ( (zzAttributes & 8) == 8 ) break zzForAction;
          }

        }
      }

      // store back cached position
      zzMarkedPos = zzMarkedPosL;

      switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {
        case 38: 
          { string.append ('\"');
          }
        case 77: break;
        case 64: 
          { return symbol (Symbols.CREATE);
          }
        case 78: break;
        case 26: 
          { return symbol (Symbols.IS);
          }
        case 79: break;
        case 50: 
          { return symbol (Symbols.UNION);
          }
        case 80: break;
        case 67: 
          { return symbol (Symbols.CONFIRM);
          }
        case 81: break;
        case 59: 
          { return symbol (Symbols.CHAR_LITERAL, '\'');
          }
        case 82: break;
        case 17: 
          { return symbol (Symbols.LT);
          }
        case 83: break;
        case 31: 
          { return symbol (Symbols.NEQ);
          }
        case 84: break;
        case 1: 
          { int  n = pos ();

    throw new SyntaxErrorException (
        "Illegal character: '"+ yytext ()+ "'",
        Location.combine (n, n + yylength ())
    );
          }
        case 85: break;
        case 48: 
          { return symbol (Symbols.DROP);
          }
        case 86: break;
        case 73: 
          { return symbol (Symbols.RELATIVE);
          }
        case 87: break;
        case 46: 
          { return symbol (Symbols.TRUE);
          }
        case 88: break;
        case 45: 
          { return symbol (Symbols.CAST);
          }
        case 89: break;
        case 18: 
          { return symbol (Symbols.LPAREN);
          }
        case 90: break;
        case 8: 
          { return symbol (Symbols.DOT);
          }
        case 91: break;
        case 66: 
          { return symbol (Symbols.OPTIONS);
          }
        case 92: break;
        case 36: 
          { string.append ('\'');
          }
        case 93: break;
        case 43: 
          { return symbol (Symbols.NULL);
          }
        case 94: break;
        case 35: 
          { return buffered (Symbols.DATE_LITERAL);
          }
        case 95: break;
        case 58: 
          { return symbol (Symbols.GROUP);
          }
        case 96: break;
        case 9: 
          { return symbol (Symbols.PLUS);
          }
        case 97: break;
        case 41: 
          { return symbol (Symbols.AND);
          }
        case 98: break;
        case 25: 
          { return symbol (Symbols.IN);
          }
        case 99: break;
        case 22: 
          { return buffered (Symbols.STRING);
          }
        case 100: break;
        case 61: 
          { return symbol (Symbols.SELECT);
          }
        case 101: break;
        case 71: 
          { return symbol (Symbols.DURABLE);
          }
        case 102: break;
        case 27: 
          { return symbol (Symbols.OR);
          }
        case 103: break;
        case 75: 
          { return symbol (Symbols.TRANSIENT);
          }
        case 104: break;
        case 69: 
          { return symbol (Symbols.RUNNING);
          }
        case 105: break;
        case 40: 
          { return symbol (Symbols.NOT);
          }
        case 106: break;
        case 24: 
          { return symbol (Symbols.FP, yytext ());
          }
        case 107: break;
        case 42: 
          { return symbol (Symbols.ENUM);
          }
        case 108: break;
        case 44: 
          { return symbol (Symbols.LIKE);
          }
        case 109: break;
        case 76: 
          { return symbol (Symbols.INSTANTIABLE);
          }
        case 110: break;
        case 52: 
          { return symbol (Symbols.CLASS);
          }
        case 111: break;
        case 37: 
          { return buffered (Symbols.BIN_LITERAL);
          }
        case 112: break;
        case 32: 
          { return symbol (Symbols.GE);
          }
        case 113: break;
        case 28: 
          { return symbol (Symbols.TO);
          }
        case 114: break;
        case 55: 
          { return symbol (Symbols.FALSE);
          }
        case 115: break;
        case 49: 
          { return symbol (Symbols.CHAR_LITERAL, yytext ().charAt (1));
          }
        case 116: break;
        case 47: 
          { return symbol (Symbols.FROM);
          }
        case 117: break;
        case 54: 
          { return symbol (Symbols.FLAGS);
          }
        case 118: break;
        case 33: 
          { return symbol (Symbols.LE);
          }
        case 119: break;
        case 65: 
          { return symbol (Symbols.MODIFY);
          }
        case 120: break;
        case 20: 
          { yycolumn--; return symbol (Symbols.X_TYPE);
          }
        case 121: break;
        case 11: 
          { startBuffering (); yybegin (STRING);
          }
        case 122: break;
        case 51: 
          { return symbol (Symbols.UNDER);
          }
        case 123: break;
        case 60: 
          { return symbol (Symbols.OBJECT);
          }
        case 124: break;
        case 4: 
          { return symbol (Symbols.STAR);
          }
        case 125: break;
        case 62: 
          { return symbol (Symbols.STREAM);
          }
        case 126: break;
        case 30: 
          { return symbol (Symbols.BY);
          }
        case 127: break;
        case 21: 
          { string.append (yytext ());
          }
        case 128: break;
        case 63: 
          { return symbol (Symbols.STATIC);
          }
        case 129: break;
        case 68: 
          { return symbol (Symbols.COMMENT);
          }
        case 130: break;
        case 5: 
          { return symbol (Symbols.MINUS);
          }
        case 131: break;
        case 74: 
          { return symbol (Symbols.DISTINCT);
          }
        case 132: break;
        case 23: 
          { return buffered (Symbols.IDENTIFIER);
          }
        case 133: break;
        case 14: 
          { return symbol (Symbols.COLON);
          }
        case 134: break;
        case 57: 
          { return symbol (Symbols.ARRAY);
          }
        case 135: break;
        case 29: 
          { return symbol (Symbols.AS);
          }
        case 136: break;
        case 56: 
          { return symbol (Symbols.ALTER);
          }
        case 137: break;
        case 70: 
          { return symbol (Symbols.DEFAULT);
          }
        case 138: break;
        case 53: 
          { return symbol (Symbols.WHERE);
          }
        case 139: break;
        case 13: 
          { return symbol (Symbols.SEMICOLON);
          }
        case 140: break;
        case 10: 
          { startBuffering (); yybegin (ESCID);
          }
        case 141: break;
        case 6: 
          { return symbol (Symbols.IDENTIFIER, yytext ().toUpperCase ());
          }
        case 142: break;
        case 2: 
          { /* ignore */
          }
        case 143: break;
        case 7: 
          { return symbol (Symbols.UINT, yytext ());
          }
        case 144: break;
        case 39: 
          { return symbol (Symbols.NEW);
          }
        case 145: break;
        case 19: 
          { return symbol (Symbols.RPAREN);
          }
        case 146: break;
        case 3: 
          { return symbol (Symbols.SLASH);
          }
        case 147: break;
        case 72: 
          { return symbol (Symbols.BETWEEN);
          }
        case 148: break;
        case 34: 
          { return buffered (Symbols.TIME_LITERAL);
          }
        case 149: break;
        case 16: 
          { return symbol (Symbols.GT);
          }
        case 150: break;
        case 12: 
          { return symbol (Symbols.COMMA);
          }
        case 151: break;
        case 15: 
          { return symbol (Symbols.EQ);
          }
        case 152: break;
        default: 
          if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
            zzAtEOF = true;
            zzDoEOF();
              { return new java_cup.runtime.Symbol(Symbols.EOF); }
          } 
          else {
            zzScanError(ZZ_NO_MATCH);
          }
      }
    }
  }


}
