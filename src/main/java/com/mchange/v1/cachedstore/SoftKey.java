package com.mchange.v1.cachedstore;

import java.lang.ref.*;

final class SoftKey extends SoftReference
{
    int hash_code;
    
    SoftKey(Object o, ReferenceQueue queue)
    {
	super( o , queue );
	this.hash_code = o.hashCode();
    }
    
    public int hashCode()
    { return hash_code; }
    
    /**
     *  we equals ourself, and any soft key whose ref equals ours.
     *  If we are cleared, we only equals ourself 
     */
    public boolean equals( Object o ) 
    {
	if (this == o) return true;
	else
	    {
		Object r1 = this.get();
		if (r1 == null)
		    return false;
		if (this.getClass() == o.getClass())
		    {
			SoftKey other = (SoftKey) o;
			return r1.equals( other.get() );
		    }
		else
		    return false;
	    }
    }
}
