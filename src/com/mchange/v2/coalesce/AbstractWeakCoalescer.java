package com.mchange.v2.coalesce;

import java.util.*;
import java.lang.ref.WeakReference;

class AbstractWeakCoalescer implements Coalescer
{
    Map wcoalesced;

    AbstractWeakCoalescer( Map wcoalesced )
    { this.wcoalesced = wcoalesced; }

    public Object coalesce( Object o )
    {
	//System.err.println("AbstractWeakCoalescer.coalesce( " + o + " )");
	Object out = null;

	WeakReference wr = (WeakReference) wcoalesced.get( o );
	if ( wr != null ) 
	    out = wr.get(); //there is a conceivable race that would
    	                    //permit wr be cleared
	if ( out == null )
	    {
		wcoalesced.put( o , new WeakReference(o) );
		out = o;
	    }
	return out;
    }

    public int countCoalesced()
    { return wcoalesced.size(); }

    public Iterator iterator()
    { return new CoalescerIterator( wcoalesced.keySet().iterator() ); }
}



