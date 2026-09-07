package com.mchange.v2.codegen.bean;

import java.util.*;

import java.io.IOException;
import com.mchange.v2.io.IndentedWriter;

public class PropsToStringGeneratorExtension implements GeneratorExtension 
{
    private Collection<String> excludePropNames = null;

    public void setExcludePropertyNames( Collection<String> excludePropNames )
    { this.excludePropNames = excludePropNames; }

    public Collection<String> getExcludePropertyNames()
    { return excludePropNames; }

    @Override
    public Collection<String> extraGeneralImports()
    { return Collections.<String>emptySet(); }

    @Override
    public Collection<String> extraSpecificImports()
    { return Collections.<String>emptySet(); }

    @Override
    public Collection<String> extraInterfaceNames()
    { return Collections.<String>emptySet(); }

    @Override
    public void generate(ClassInfo info, Class<?> superclassType, Property[] props, Class<?>[] propTypes, IndentedWriter iw)
	throws IOException
    {
	iw.println("public String toString()");
	iw.println("{");
	iw.upIndent();

	iw.println("StringBuffer sb = new StringBuffer();");
	iw.println("sb.append( super.toString() );");
	iw.println("sb.append(\" [ \");");

	for (int i = 0, len = props.length; i < len; ++i)
	    {
		Property prop = props[i];

		if ( excludePropNames != null && excludePropNames.contains( prop.getName() ) )
		    continue;

		iw.println("sb.append( \"" + prop.getName() + " -> \"" + " + " + prop.getName() + " );");
		if ( i != len - 1 )
		    iw.println("sb.append( \", \");");
	    }

	iw.println();
	iw.println("String extraToStringInfo = this.extraToStringInfo();");
	iw.println("if (extraToStringInfo != null)");
	iw.upIndent();
	iw.println("sb.append( extraToStringInfo );");
	iw.downIndent();


	iw.println("sb.append(\" ]\");");
	iw.println("return sb.toString();");
	iw.downIndent();
	iw.println("}");
	iw.println();
	iw.println("protected String extraToStringInfo()");
	iw.println("{ return null; }");
    }
}
