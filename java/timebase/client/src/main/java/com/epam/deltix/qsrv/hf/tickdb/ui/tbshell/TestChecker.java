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
package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell;

import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.text.*;
import java.io.*;

/**
 *
 */
public class TestChecker {
    public static final String  EOF = "!end";
    public static final String  REGEXP = "!regexp";
    public static final String  SHELLMATCH = "!shellmatch";
    public static final String  PRECISE = "!precise";
    
    public static abstract class Discrepancy extends RuntimeException {
        public final String     inputId;
        public final int        inputLineNo;
        public final String     inputLine;
        public final int        resultLineNo;
        public final String     resultLine;

        public Discrepancy (
            String              inputId,
            int                 inputLineNo, 
            String              message,
            String              inputLine, 
            int                 resultLineNo,
            String              resultLine
        ) 
        {
            super (
                String.format (
                    "\nAt %s:%d (result:%d)\n%s", 
                    inputId, inputLineNo, resultLineNo, message
                )
            );
            
            this.inputId = inputId;
            this.inputLineNo = inputLineNo;
            this.inputLine = inputLine;
            this.resultLineNo = resultLineNo;
            this.resultLine = resultLine;
        }

        @Override
        public Throwable        fillInStackTrace () {
            return (null);
        }                
    }
    
    public static final class EndOfTest extends Discrepancy {
        public EndOfTest (
            String              inputId,
            int                 inputLineNo, 
            int                 resultLineNo,
            String              resultLine
        ) 
        {
            super (
                inputId, inputLineNo,
                "Actual: " + resultLine,
                null, resultLineNo, resultLine
            );
        }         
    }
    
    public static final class EndOfResult extends Discrepancy {
        public EndOfResult (
            String              inputId,
            int                 inputLineNo, 
            String              inputLine, 
            int                 resultLineNo
        ) 
        {
            super (
                inputId, inputLineNo,
                "Expected: " + inputLine,
                inputLine, resultLineNo, null
            );
        }         
    }
    
    public static final class LineDiff extends Discrepancy {
        public LineDiff (
            String              inputId,
            int                 inputLineNo, 
            String              inputLine, 
            int                 resultLineNo,
            String              resultLine
        ) 
        {
            super (
                inputId, inputLineNo,
                "Expected: " + inputLine + "\nActual:   " + resultLine,
                inputLine, resultLineNo, resultLine
            );
        }         
    }
    
    private final String                testId;
    private final LineNumberReader      testReader;
    private boolean                     echo;
    private int                         resultLineNo = 0;
    private boolean                     readTestEOF = false;
    private StringBuilder               line = new StringBuilder ();
    private CSMatcher                   matcher = PreciseCSMatcher.INSTANCE;    
    
    private final Writer                resultWriter =
        new Writer () {
            
            @Override
            public void close () throws IOException {
                resultDone ();
            }

            @Override
            public void flush () {                
            }

            @Override
            public void write (char[] cbuf, int off, int len) throws IOException {
                for (int ii = 0; ii < len; ii++)
                    write (cbuf [ii]);
            }

            @Override
            public void write (int c) throws IOException {
                switch (c) {
                    case '\r':
                        break;
                
                    case '\n':
                        lineDone ();
                        line.setLength (0);
                        break;
                        
                    default:
                        line.append ((char) c);
                        break;
                }
            }                    
        };
    
    private final PrintWriter           resultPrintWriter = 
        new PrintWriter (resultWriter);

    public TestChecker (String testId, LineNumberReader testReader) {
        this.testId = testId;
        this.testReader = testReader;
    }

    public boolean          isEcho () {
        return echo;
    }

    public void             setEcho (boolean echo) {
        this.echo = echo;
    }
        
    public PrintWriter      getResultWriter () {
        return (resultPrintWriter);
    }    
    
    private void            lineDone () throws IOException {
        resultLineNo++;
        
        String                  expected;
        
        for (;;) {
            expected = testReader.readLine ();

            if (expected == null)
                throw new EOFException (EOF + " expected");
            
            expected = StringUtils.trimTrailingWhitespace (expected);
             
            if (echo)
                System.out.println (expected);
            
            if (!expected.startsWith ("!")) 
                break;
            
            if (expected.equals (EOF)) {
                readTestEOF = true;
                throw new EndOfTest (testId, testReader.getLineNumber (), resultLineNo, line.toString ());                    
            }            
            else if (expected.equals (PRECISE))
                matcher = PreciseCSMatcher.INSTANCE;
            else if (expected.equals (REGEXP))
                matcher = RegexpCSMatcher.INSTANCE;
            else if (expected.equals (SHELLMATCH))
                matcher = ShellPatternCSMatcher.INSTANCE;
            // else is just a comment
        } 
        
        if (!matcher.matches (line, expected))
            throw new LineDiff (testId, testReader.getLineNumber (), expected, resultLineNo, line.toString ());
    }
    
    public void             recoverUnlessDone () throws IOException {
        if (readTestEOF)
            return;
        
        for (;;) {
            String          expected = testReader.readLine ();

            if (expected == null)
                throw new EOFException (EOF + " expected");
            
            if (expected.equals (EOF)) {
                readTestEOF = true;
                break;                  
            }
        }
    }
    
    public void             resultDone () throws IOException {
        for (;;) {
            String          expected = testReader.readLine ();

            if (expected == null)
                throw new EOFException (EOF + " expected");
            
            expected = expected.trim ();
            
            if (!expected.startsWith ("!")) 
                throw new EndOfResult (testId, testReader.getLineNumber (), expected, resultLineNo);
            
            if (expected.equals (EOF)) {
                readTestEOF = true;
                break;                  
            }
        }                
    }
}
