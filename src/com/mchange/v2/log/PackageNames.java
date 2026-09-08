package com.mchange.v2.log;

public class PackageNames implements NameTransformer
{
    @Override
    public String transformName( String name )
    { return null; }

    @Override
    public String transformName( Class<?> cl )
    {
	String fqcn = cl.getName();
	int i = fqcn.lastIndexOf('.');
	if (i <= 0)
	    return "";
	else
	    return fqcn.substring(0,i);
    }

    @Override
    public String transformName()
    { return null; }
}
