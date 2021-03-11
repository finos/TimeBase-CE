package com.epam.deltix.util.jcg.scg;

/**
 *
 */
public final class CSCodePrinter extends NiceSourceCodePrinter {
    public enum ImportMode {
        EXPLICIT,
        BY_NAMESPACE
    };
    
    private String              namespace = null;
    private String              topComment = null;
    private ImportTracker       importTracker = null;
    
    public CSCodePrinter (String namespace) {        
        this.namespace = namespace;        
    }

    public void                 setImportMode (ImportMode mode) {
        switch (mode) {
            case EXPLICIT:
                importTracker = null;
                break;
                
            case BY_NAMESPACE:
                importTracker = new CSNSImportTracker ();
                break;                
        }
    }

    @Override
    public ImportTracker        getImportTracker () {
        return (importTracker);
    }    
    
    public String               getNamespace () {
        return namespace;
    }

    public String               getTopComment () {
        return topComment;
    }

    public void                 setTopComment (String topComment) {
        this.topComment = topComment;
    }

    
    @Override
    protected void              doPrintHeader (StringBuilder out) {
        if (importTracker != null) 
            importTracker.printImports (namespace, out);
        
        if (namespace != null) {
            out.append ("namespace ");
            out.append (namespace);
            out.append (" {\n");
        }  
        
        if (topComment != null) {
            out.append ("    /// <summary>");
            
            for (String s : topComment.split ("\n")) {
                out.append ("\n    /// ");
                out.append (s);
            }
            
            out.append ("    /// </summary>\n");
        }                
    }    
    
    @Override
    protected void              doPrintFooter (StringBuilder out) {
        if (namespace != null)
            out.append ("}");
    }
   
    
}
