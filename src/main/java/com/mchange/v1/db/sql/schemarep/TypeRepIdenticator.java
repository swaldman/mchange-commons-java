package com.mchange.v1.db.sql.schemarep;

import java.util.Arrays;
import com.mchange.v1.identicator.Identicator;

public class TypeRepIdenticator implements Identicator
{
    private final static TypeRepIdenticator INSTANCE = new TypeRepIdenticator();

    public static TypeRepIdenticator getInstance()
    { return INSTANCE; }

    private TypeRepIdenticator()
    {}

    public boolean identical(Object a, Object b)
    {
	if (a == b)
	    return true;

	TypeRep aa = (TypeRep) a;
	TypeRep bb = (TypeRep) b;

	return 
	    aa.getTypeCode() == bb.getTypeCode() &&
	    Arrays.equals( aa.getTypeSize(), bb.getTypeSize() );
    }

    public int hash(Object o)
    {
	TypeRep tr = (TypeRep) o;
	int out = tr.getTypeCode();

	int[] szArray = tr.getTypeSize();
	if (szArray != null)
	    {
		int len = szArray.length;
		for (int i = 0; i < len; ++i)
		    out ^= szArray[i];
		out ^= len;
	    }
	return out;
    }
}
