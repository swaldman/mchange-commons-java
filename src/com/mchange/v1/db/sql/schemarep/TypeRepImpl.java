package com.mchange.v1.db.sql.schemarep;

public final class TypeRepImpl implements TypeRep
{
    int   type_code;
    int[] typeSize;

    public TypeRepImpl( int type_code, int[] typeSize )
    {
	this.type_code = type_code;
	this.typeSize  = typeSize;
    }

    public int   getTypeCode()
    { return type_code; }

    public int[] getTypeSize()
    { return typeSize; }

    public boolean equals(Object o)
    {
	if (this == o) 
	    return true;
	else if (o instanceof TypeRep)
	    return TypeRepIdenticator.getInstance().identical(this, o);
	else
	    return false;
    }

    public int hashCode()
    { return TypeRepIdenticator.getInstance().hash(this); }
}



