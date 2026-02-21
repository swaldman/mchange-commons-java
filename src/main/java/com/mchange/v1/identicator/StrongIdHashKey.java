package com.mchange.v1.identicator;

// revisit equals() if ever made non-final

final class StrongIdHashKey extends IdHashKey
{
    Object      keyObj;
    
    public StrongIdHashKey(Object keyObj, Identicator id)
    {
	super( id );
	this.keyObj = keyObj;
    }

    public Object getKeyObj()
    { return keyObj; }

    public boolean equals(Object o)
    {
	//  fast type-exact match for final class
	if (o instanceof StrongIdHashKey)
	    return id.identical( keyObj, ((StrongIdHashKey) o).keyObj );
	else
	    return false;
    }

    public int hashCode()
    { return id.hash( keyObj ); }
}
