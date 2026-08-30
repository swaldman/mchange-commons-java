package com.mchange.v2.coalesce;

import java.util.*;

class AbstractStrongCoalescer implements Coalescer
{
    Map coalesced;

    AbstractStrongCoalescer( Map coalesced )
    { this.coalesced = coalesced; }

    public Object coalesce( Object o )
    {
	Object out = coalesced.get( o );
	if ( out == null )
	    {
		coalesced.put( o , o );
		out = o;
	    }
	return out;
    }

    public int countCoalesced()
    { return coalesced.size(); }

    public Iterator iterator()
    { return new CoalescerIterator( coalesced.keySet().iterator() ); }
}



