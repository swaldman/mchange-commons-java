package com.mchange.v2.log;

public class PackageNames implements NameTransformer
{
    public String transformName( String name )
    { return null; }

    public String transformName( Class cl )
    {
	String fqcn = cl.getName();
	int i = fqcn.lastIndexOf('.');
	if (i <= 0)
	    return "";
	else
	    return fqcn.substring(0,i);
    }

    public String transformName()
    { return null; }
}
