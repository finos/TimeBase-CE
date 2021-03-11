package com.epam.deltix.util.jcg.scg;

/**
 *
 */
public final class JavaCodePrinter extends NiceSourceCodePrinter {
    public enum ImportMode {
        EXPLICIT,
        BY_CLASS,
        BY_PACKAGE
    };
    
    private String              pack = null;
    private String              topComment = null;
    private ImportTracker       importTracker = null;
    
    public JavaCodePrinter (String pack) {        
        this.pack = pack;        
    }

    public void                 setImportMode (ImportMode mode) {
        switch (mode) {
            case EXPLICIT:
                importTracker = null;
                break;
                
            case BY_PACKAGE:
                importTracker = new JPackImportTracker ();
                break;
                
            case BY_CLASS:
                importTracker = new JClassImportTracker ();
                break;
        }
    }

    @Override
    public ImportTracker        getImportTracker () {
        return (importTracker);
    }    
    
    public String               getPackage () {
        return pack;
    }

    public String               getTopComment () {
        return topComment;
    }

    public void                 setTopComment (String topComment) {
        this.topComment = topComment;
    }
   
    @Override
    protected void              doPrintHeader (StringBuilder out) {
        if (pack.length () != 0) {
            out.append ("package ");
            out.append (pack);
            out.append (";\n\n");
        }
        
        if (importTracker != null) 
            importTracker.printImports (pack, out);
        
        if (topComment != null) {
            out.append ("/**");
            for (String s : topComment.split ("\n")) {
                out.append ("\n * ");
                out.append (s);
            }
            out.append ("\n */\n");
        }
    }          
}
