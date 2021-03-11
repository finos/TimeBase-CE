package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.*;
import com.epam.deltix.util.parsers.Location;

/**
 *
 */
public final class ModifyStreamStatement extends Statement {
    public final Identifier             id;
    public final String                 title;
    public final String                 comment;
    public final OptionElement []       options;
    public final ClassDef []            members;
    public final ConversionConfirmation confirm;

    public ModifyStreamStatement (
        long                        location,
        Identifier                  id, 
        String                      title,                                  
        String                      comment, 
        OptionElement []            options,
        ClassDef []                 members,
        ConversionConfirmation      confirm
    )
    {
        super (location);
        
        this.id = id;
        this.title = title;
        this.comment = comment;
        this.options = options;
        this.members = members;
        this.confirm = confirm;
    }
    
    public ModifyStreamStatement (
        Identifier                  id, 
        String                      title,                                  
        String                      comment, 
        OptionElement []            options,
        ClassDef []                 members,
        ConversionConfirmation      confirm
    )
    {
        this (
            Location.NONE, 
            id, title, comment, 
            options, members, confirm
        );
    }
    
    @Override
    public void                     print (StringBuilder s) {
        s.append ("MODIFY STREAM ");
        id.print (s);
        
        if (title != null) {
            s.append (" ");
            GrammarUtil.escapeStringLiteral (title, s);
        }
        
        s.append (" (\n");
        
        boolean     first = true;

        for (ClassDef cd : members) {
            if (first)
                first = false;
            else
                s.append (";\n");

            cd.print (s);
        }

        s.append (")");
        
        OptionElement.print (options, s);
        
        if (comment != null) {
            s.append ("\nCOMMENT ");
            GrammarUtil.escapeStringLiteral (comment, s);
        }
        
        if (confirm != null) {
            s.append ("\nCONFIRM ");
            s.append (confirm.name ());
        }
    }        
}
