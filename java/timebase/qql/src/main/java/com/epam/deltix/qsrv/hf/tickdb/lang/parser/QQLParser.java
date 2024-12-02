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
package com.epam.deltix.qsrv.hf.tickdb.lang.parser;

import com.epam.deltix.util.parsers.Element;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.EmptyProgramException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

import java.io.*;
import java.util.stream.Collectors;

import java_cup.runtime.*;

/**
 *
 */
public class QQLParser {

    private interface TryParse {
        Symbol parse(Scanner scanner) throws Exception;
    }

    public static TextMap createTextMap() {
        return (new TextMapImpl());
    }

    public static Element parse(Reader r, TextMap map) {
        return parse(readToString(r), map);
    }

    public static Element parse(String query, TextMap map) {
        if (isDDL(query)) {
            return parse(new StringReader(query), map, (scanner) -> new DDLParser(scanner).parse());
        } else {
            return parse(new StringReader(query), map, (scanner) -> new Parser(scanner).parse());
        }
    }

    public static boolean isDDL(String query) {
        Lexer scanner = new Lexer(new StringReader(query));
        try {
            Symbol symbol = scanner.next_token();
            switch (symbol.sym) {
                case Symbols.ALTER:
                case Symbols.CREATE:
                case Symbols.MODIFY:
                case Symbols.DROP:
                case Symbols.X_TYPE:
                    return true;
                default:
                    return false;
            }
        } catch (IOException e) {
            return false;
        }
    }

    private static String readToString(Reader r) {
        try (BufferedReader reader = new BufferedReader(r)) {
            return reader.lines().collect(Collectors.joining());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Element parse(Reader r, TextMap map, TryParse parser) {
        Scanner scanner = new Lexer(r);
        TextMapBuilder builder = null;

        if (map != null) {
            TextMapImpl mapImpl = (TextMapImpl) map;
            mapImpl.clear();
            scanner = builder = new TextMapBuilder(scanner, mapImpl);
        }

        try {
            Symbol ret = parser.parse(scanner);

            if (ret.value == null)
                throw new EmptyProgramException();

            return ((Element) ret.value);
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new RuntimeException(x);
        } finally {
            if (map != null)
                builder.finish();
        }
    }
}
