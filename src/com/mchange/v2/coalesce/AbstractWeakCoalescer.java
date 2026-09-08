package com.mchange.v2.coalesce;

import java.util.*;
import java.lang.ref.WeakReference;

class AbstractWeakCoalescer implements Coalescer
{
    Map<Object,WeakReference<Object>> wcoalesced;

    AbstractWeakCoalescer( Map<Object,WeakReference<Object>> wcoalesced )
    { this.wcoalesced = wcoalesced; }

    @Override
    public Object coalesce( Object o )
    {
	//System.err.println("AbstractWeakCoalescer.coalesce( " + o + " )");
	Object out = null;

	WeakReference<Object> wr = wcoalesced.get( o );
	if ( wr != null ) 
	    out = wr.get(); //there is a conceivable race that would
    	                    //permit wr be cleared
	if ( out == null )
	    {
		wcoalesced.put( o , new WeakReference<Object>(o) );
		out = o;
	    }
	return out;
    }

    @Override
    public int countCoalesced()
    { return wcoalesced.size(); }

    @Override
    public Iterator<Object> iterator()
    { return new CoalescerIterator( wcoalesced.keySet().iterator() ); }
}



