package com.mchange.v2.codegen.bean;

class PropertyComparator
{
    public int compare(Object a, Object b)
    {
	Property aa = (Property) a;
	Property bb = (Property) b;

	return (aa.getName().compareTo(bb.getName()));
    }
}
