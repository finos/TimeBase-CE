package com.epam.deltix.util.jcg.scg;

import java.io.IOException;

/**
 *
 */
interface Printable {
    abstract void       print (SourceCodePrinter out) throws IOException;
}
