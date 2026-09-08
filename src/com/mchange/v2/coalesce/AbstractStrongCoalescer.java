package com.mchange.v2.coalesce;

import java.util.*;

class AbstractStrongCoalescer implements Coalescer
{
    Map<Object,Object> coalesced;

    AbstractStrongCoalescer( Map<Object,Object> coalesced )
    { this.coalesced = coalesced; }

    @Override
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

    @Override
    public int countCoalesced()
    { return coalesced.size(); }

    @Override
    public Iterator<Object> iterator()
    { return new CoalescerIterator( coalesced.keySet().iterator() ); }
}



