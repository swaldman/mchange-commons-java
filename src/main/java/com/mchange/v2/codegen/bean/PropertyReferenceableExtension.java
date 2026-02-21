package com.mchange.v2.codegen.bean;

import java.util.*;
import com.mchange.v2.io.IndentedWriter;
import com.mchange.v2.naming.JavaBeanObjectFactory;
import com.mchange.v2.naming.JavaBeanReferenceMaker;

import java.io.IOException;

public class PropertyReferenceableExtension implements GeneratorExtension
{
    boolean explicit_reference_properties = false;

    String factoryClassName = JavaBeanObjectFactory.class.getName();

    String javaBeanReferenceMakerClassName = JavaBeanReferenceMaker.class.getName();

    public void setUseExplicitReferenceProperties( boolean explicit_reference_properties )
    { this.explicit_reference_properties = explicit_reference_properties; }

    public boolean getUseExplicitReferenceProperties()
    { return explicit_reference_properties; }

    public void setFactoryClassName( String factoryClassName )
    { this.factoryClassName = factoryClassName; }

    public String getFactoryClassName()
    { return factoryClassName; }

//     public void setJavaBeanReferenceMakerClassName( String javaBeanReferenceMakerClassName )
//     { this.javaBeanReferenceMakerClassName = javaBeanReferenceMakerClassName; }

//     public String getJavaBeanReferenceMakerClassName()
//     { return javaBeanReferenceMakerClassName; }

    public Collection extraGeneralImports()
    { 
	Set set = new HashSet();
	return set;
    }

    public Collection extraSpecificImports()
    {
	Set set = new HashSet();
	set.add( "javax.naming.Reference" );
	set.add( "javax.naming.Referenceable" );
	set.add( "javax.naming.NamingException" );
	set.add( "com.mchange.v2.naming.JavaBeanObjectFactory" );
	set.add( "com.mchange.v2.naming.JavaBeanReferenceMaker" );
	set.add( "com.mchange.v2.naming.ReferenceMaker" );
	return set;
    }

    public Collection extraInterfaceNames()
    {
	Set set = new HashSet();
	set.add( "Referenceable" );
	return set;
    }

    public void generate(ClassInfo info, Class superclassType, Property[] props, Class[] propTypes, IndentedWriter iw)
	throws IOException
    {
	iw.println("final static JavaBeanReferenceMaker referenceMaker = new " + javaBeanReferenceMakerClassName + "();");
	iw.println();
	iw.println("static"); 
	iw.println("{"); 
	iw.upIndent();
	
	iw.println("referenceMaker.setFactoryClassName( \"" + factoryClassName + "\" );");
	if ( explicit_reference_properties )
	    {
		for( int i = 0, len = props.length; i < len; ++i)
		    iw.println("referenceMaker.addReferenceProperty(\"" + props[i].getName() + "\");");
	    }

	iw.downIndent();
	iw.println("}");
	iw.println();
	iw.println("public Reference getReference() throws NamingException");
	iw.println("{"); 
	iw.upIndent();
	
	iw.println("return referenceMaker.createReference( this );");

	iw.downIndent();
	iw.println("}");
    }
}
