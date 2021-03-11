package com.epam.deltix.qsrv.hf.pub.md;

import com.epam.deltix.util.collections.Visitor;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.xml.bind.annotation.*;

/**
 *
 */
@XmlType (name = "extendableClass")
public abstract class ExtendableClassDescriptor <T extends ExtendableClassDescriptor>
    extends ClassDescriptor
{
	private static final long serialVersionUID = 1L;
	
    @XmlElement (name = "abstract")
    private boolean                     isAbstract;
    
    @XmlIDREF @XmlElement (name = "parent")
    private T                           parent;

    protected ExtendableClassDescriptor (
        String              name, 
        String              title, 
        boolean             isAbstract, 
        T                   parent
    )
    {
        super (name, title);
        
        this.isAbstract = isAbstract;
        this.parent = parent;
    }

    protected ExtendableClassDescriptor (ExtendableClassDescriptor <T> from) {
        super(from);

        isAbstract = from.isAbstract;
        parent = from.parent;
    }
        
    protected ExtendableClassDescriptor () {
        isAbstract = false;
        parent = null;
    }
    
    protected ExtendableClassDescriptor (Class <?> cls, T inParent) {
        this (cls, ClassAnnotator.DEFAULT, inParent);
    }  
    
    protected ExtendableClassDescriptor (Class <?> cls, ClassAnnotator annotator, T inParent) {
        super (cls, annotator);
        
        isAbstract = annotator.isAbstract (cls);
        parent = inParent;       
    }

    public boolean                  isAbstract () {
        return isAbstract;
    }

    public T                        getParent () {
        return parent;
    }    

    public void                     setParent (T parent) {
        this.parent = parent;
    }    
    
    @Override
    public boolean        visitDependencies (Visitor <ClassDescriptor> out) {
        return (
            super.visitDependencies (out) &&
            (parent == null || out.visit (parent))
        );
    }
    
    @Override
    public void                     writeTo (DataOutputStream out, int serial)
        throws IOException
    {
        super.writeTo (out, serial);

        out.writeBoolean (isAbstract);

        boolean     hasParent = parent != null;

        out.writeBoolean (hasParent);

        if (hasParent)
            parent.writeReference (out);
    }

    @Override
    @SuppressWarnings ("unchecked")
    protected void                  readFields (
        DataInputStream     in,
        TypeResolver        resolver,
        int                 serial
    )
        throws IOException
    {
        super.readFields (in, resolver, serial);

        isAbstract = in.readBoolean ();
        boolean     hasParent = in.readBoolean ();

        if (hasParent)
            parent = (T) readReference (in, resolver);
        else
            parent = null;
    }
}
